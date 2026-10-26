package com.sisaguna.android.data.remote

import com.sisaguna.android.BuildConfig
import com.sisaguna.android.data.remote.dto.MessageDto
import com.sisaguna.android.data.repository.AuthRepository
import com.sisaguna.android.di.ApplicationScope
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import com.sisaguna.android.di.BareClient

/** What the server pushes. The socket only *notifies*; the REST API stays the source of truth. */
sealed interface RealtimeEvent {
    /** The socket (re)connected: anything that happened while offline was missed and must be re-fetched. */
    data object Connected : RealtimeEvent
    data class MessageCreated(val message: MessageDto) : RealtimeEvent
    data class ConversationRead(val conversationId: String, val lastReadId: Long) : RealtimeEvent
    data class Typing(val conversationId: String) : RealtimeEvent
    data class OrderChanged(val orderId: String?) : RealtimeEvent
    data object NotificationCreated : RealtimeEvent
    data object MerchantStatusChanged : RealtimeEvent
    data class StockChanged(val listingId: String, val stock: Int) : RealtimeEvent
}

/**
 * One WebSocket for the whole app, open while the app is in the foreground and the user is
 * signed in. Each connection needs a fresh single-use ticket; after a drop it reconnects with
 * 1, 2, 4 … 30 s back-off.
 */
@Singleton
class RealtimeClient @Inject constructor(
    private val api: ApiService,
    @BareClient bare: OkHttpClient,
    private val json: Json,
    private val auth: AuthRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    // No read timeout (the server may be quiet) but a ping so a dead network is noticed.
    private val client = bare.newBuilder().readTimeout(0, TimeUnit.SECONDS).pingInterval(25, TimeUnit.SECONDS).build()

    private val _events = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<RealtimeEvent> = _events.asSharedFlow()

    private var loop: Job? = null
    @Volatile private var socket: WebSocket? = null

    /** App came to the foreground. */
    @Synchronized
    fun start() {
        if (loop?.isActive == true) return
        loop = scope.launch { run() }
    }

    /** App went to the background: drop the socket, there are no push notifications to fall back on yet. */
    @Synchronized
    fun stop() {
        loop?.cancel()
        loop = null
        socket?.close(NORMAL_CLOSE, null)
        socket = null
    }

    /** Tells the other side we are typing (the server relays it for ~3 s). */
    fun typing(conversationId: String) {
        socket?.send("""{"type":"typing","conversation_id":"$conversationId"}""")
    }

    private suspend fun run() {
        var backoffMs = FIRST_BACKOFF_MS
        while (scope.isActive) {
            if (!auth.isAuthenticated.value) auth.isAuthenticated.first { it }
            try {
                val ticket = apiData { api.wsTicket() }.ticket
                val closed = CompletableDeferred<Boolean>()
                val ws = client.newWebSocket(
                    Request.Builder().url("${BuildConfig.WS_URL}?ticket=$ticket").build(),
                    listener(closed),
                )
                socket = ws
                // A socket belongs to the account that opened it: drop it the moment that session ends.
                val signOutWatcher = scope.launch {
                    auth.isAuthenticated.first { !it }
                    ws.cancel()
                    closed.complete(false)
                }
                try {
                    if (closed.await()) backoffMs = FIRST_BACKOFF_MS
                } finally {
                    signOutWatcher.cancel()
                    ws.cancel()
                    socket = null
                }
            } catch (e: AppException) {
                // No ticket (offline, or the session ended): retry below.
            }
            delay(backoffMs)
            backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
        }
    }

    /** Completes with whether the connection ever opened (so a flapping server keeps its back-off). */
    private fun listener(closed: CompletableDeferred<Boolean>) = object : WebSocketListener() {
        private var opened = false

        override fun onOpen(webSocket: WebSocket, response: Response) {
            opened = true
            _events.tryEmit(RealtimeEvent.Connected)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            parse(text)?.let { _events.tryEmit(it) }
        }

        // OkHttp only reaches onClosed once we answer the server's close frame.
        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(NORMAL_CLOSE, null)
            closed.complete(opened)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            closed.complete(opened)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            closed.complete(opened)
        }
    }

    internal fun parse(text: String): RealtimeEvent? {
        val frame = try {
            json.parseToJsonElement(text).jsonObject
        } catch (e: Exception) {
            return null // a malformed frame must not take the connection down
        }
        val data = frame["data"] as? JsonObject
        fun string(key: String) = data?.get(key)?.jsonPrimitive?.contentOrNull
        return try {
            when (frame["type"]?.jsonPrimitive?.contentOrNull) {
                "message.created" -> data?.let { RealtimeEvent.MessageCreated(json.decodeFromJsonElement<MessageDto>(it)) }
                "conversation.read" -> string("conversation_id")?.let {
                    RealtimeEvent.ConversationRead(it, data?.get("last_read_id")?.jsonPrimitive?.longOrNull ?: 0L)
                }
                "typing" -> string("conversation_id")?.let { RealtimeEvent.Typing(it) }
                "order.created", "order.updated" -> RealtimeEvent.OrderChanged(string("order_id"))
                "notification.created" -> RealtimeEvent.NotificationCreated
                "merchant.status_changed" -> RealtimeEvent.MerchantStatusChanged
                "listing.stock_changed" -> string("listing_id")?.let {
                    RealtimeEvent.StockChanged(it, data?.get("stock")?.jsonPrimitive?.intOrNull ?: 0)
                }
                else -> null // unknown frames (errors, future events) are ignored
            }
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        const val NORMAL_CLOSE = 1000
        const val FIRST_BACKOFF_MS = 1_000L
        const val MAX_BACKOFF_MS = 30_000L
    }
}
