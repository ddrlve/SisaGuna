package com.sisaguna.android.data.repository

import com.sisaguna.android.data.model.BUYER_ME
import com.sisaguna.android.data.model.ChatMessage
import com.sisaguna.android.data.model.ChatSide
import com.sisaguna.android.data.model.Conversation
import com.sisaguna.android.data.remote.ApiService
import com.sisaguna.android.data.remote.AppException
import com.sisaguna.android.data.remote.RealtimeClient
import com.sisaguna.android.data.remote.RealtimeEvent
import com.sisaguna.android.data.remote.apiCall
import com.sisaguna.android.data.remote.apiData
import com.sisaguna.android.data.remote.dto.MessageDto
import com.sisaguna.android.data.remote.dto.ReadBody
import com.sisaguna.android.data.remote.dto.SendMessageBody
import com.sisaguna.android.data.remote.dto.StartConversationBody
import com.sisaguna.android.data.remote.toChatMessage
import com.sisaguna.android.data.remote.toConversation
import com.sisaguna.android.di.ApplicationScope
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Buyer/store chat over REST, kept live by [RealtimeClient]. REST is the source of truth: the
 * socket only says "something changed", and after a reconnect everything missed is fetched again
 * (`after_id`), because events sent while offline are not replayed.
 */
@Singleton
class ApiChatRepository @Inject constructor(
    private val api: ApiService,
    private val auth: AuthRepository,
    private val listings: ListingRepository,
    private val realtime: RealtimeClient,
    @ApplicationScope private val scope: CoroutineScope,
) : ChatRepository {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    override val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    override val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _typing = MutableStateFlow<Set<String>>(emptySet())
    override val typing: StateFlow<Set<String>> = _typing.asStateFlow()

    /** Highest of the user's own message ids the other side has read, per conversation. */
    private val peerRead = java.util.concurrent.ConcurrentHashMap<String, Long>()

    init {
        scope.launch {
            auth.isAuthenticated.collect { signedIn ->
                if (!signedIn) {
                    _conversations.value = emptyList()
                    _messages.value = emptyList()
                    _typing.value = emptySet()
                    peerRead.clear()
                } else {
                    runQuietly { refresh() }
                }
            }
        }
        // The store lookup finishes after login; a new store also means a new inbox to show.
        scope.launch { listings.myMerchant.collect { if (it != null) runQuietly { refresh() } } }
        scope.launch { realtime.events.collect(::onEvent) }
    }

    override suspend fun refresh() {
        // A guest has nothing to load (and the server would answer 401).
        if (!auth.isAuthenticated.value) return
        val asBuyer = apiData { api.conversations("buyer") }.map { it.toConversation(ChatSide.BUYER) }
        val asStore = if (listings.myMerchant.value != null) {
            apiData { api.conversations("merchant") }.map { it.toConversation(ChatSide.MERCHANT) }
        } else {
            emptyList()
        }
        val fresh = asBuyer + asStore
        // A refresh can finish after the user opened a chat: keep threads the server list doesn't show yet.
        _conversations.update { local ->
            (fresh + local.filter { l -> fresh.none { it.id == l.id } }).sortedByDescending { it.lastAt ?: Instant.EPOCH }
        }
    }

    override suspend fun conversationWith(merchantId: String): Conversation {
        _conversations.value.firstOrNull { it.side == ChatSide.BUYER && it.counterpartId == merchantId }?.let { return it }
        val created = apiData { api.startConversation(StartConversationBody(merchantId)) }.toConversation(ChatSide.BUYER)
        _conversations.update { list -> listOf(created) + list.filterNot { it.id == created.id } }
        return created
    }

    override suspend fun loadMessages(conversationId: String) {
        val conversation = conversationOf(conversationId) ?: return
        val lastKnown = _messages.value.filter { it.conversationId == conversationId }.maxOfOrNull { it.id.toLongOrNull() ?: 0L }
        val envelope = apiCall { api.messages(conversationId, afterId = lastKnown) }
        envelope.meta?.peerLastReadId?.let { peerRead[conversationId] = it }
        upsert(envelope.data.map { it.toMessage(conversation) })
    }

    override suspend fun send(conversationId: String, text: String, listingId: String?) {
        val conversation = conversationOf(conversationId) ?: return
        val body = text.trim()
        if (body.isEmpty()) return
        val clientId = UUID.randomUUID().toString()
        // Show it at once as "mine"; the server's copy (same text, real id) replaces this placeholder.
        val pending = ChatMessage(
            id = "pending-$clientId", merchantId = conversation.counterpartId, fromBuyer = conversation.side == ChatSide.BUYER,
            text = body, sentAt = Instant.now(), listingId = listingId, isRead = false,
            buyer = buyerKey(conversation), conversationId = conversationId,
        )
        _messages.update { it + pending }
        try {
            val sent = apiData { api.sendMessage(conversationId, SendMessageBody(clientId, body, listingId)) }
            _messages.update { list -> list.filterNot { it.id == pending.id } }
            upsert(listOf(sent.toMessage(conversation)))
        } catch (e: AppException) {
            _messages.update { list -> list.filterNot { it.id == pending.id } }
            throw e
        }
    }

    override fun markRead(conversationId: String) {
        // Without a loaded incoming message there is nothing to acknowledge; keep the unread count honest.
        val lastIncoming = incomingIds(conversationId).maxOrNull() ?: return
        _conversations.update { list -> list.map { if (it.id == conversationId) it.copy(unread = 0) else it } }
        scope.launch { runQuietly { apiCall { api.readConversation(conversationId, ReadBody(lastIncoming)) } } }
    }

    override fun notifyTyping(conversationId: String) = realtime.typing(conversationId)

    // ------------------------------------------------------------ realtime

    internal suspend fun onEvent(event: RealtimeEvent) {
        when (event) {
            RealtimeEvent.Connected -> {
                runQuietly { refresh() }
                // Catch up the threads that are loaded; the others fetch on open.
                _messages.value.map { it.conversationId }.distinct().forEach { id -> runQuietly { loadMessages(id) } }
            }
            is RealtimeEvent.MessageCreated -> onIncoming(event.message)
            is RealtimeEvent.ConversationRead -> {
                peerRead[event.conversationId] = event.lastReadId
                val side = conversationOf(event.conversationId)?.side
                _messages.update { list ->
                    list.map {
                        val mine = side != null && it.fromBuyer == (side == ChatSide.BUYER)
                        val covered = (it.id.toLongOrNull() ?: Long.MAX_VALUE) <= event.lastReadId
                        if (it.conversationId == event.conversationId && mine && covered) it.copy(isRead = true) else it
                    }
                }
            }
            is RealtimeEvent.Typing -> {
                _typing.update { it + event.conversationId }
                scope.launch {
                    delay(TYPING_MS)
                    _typing.update { it - event.conversationId }
                }
            }
            else -> Unit // orders, notifications, stock: handled by their own repositories
        }
    }

    private suspend fun onIncoming(dto: MessageDto) {
        val conversation = conversationOf(dto.conversationId) ?: run {
            runQuietly { refresh() } // someone wrote to us for the first time
            conversationOf(dto.conversationId)
        } ?: return
        val message = dto.toMessage(conversation)
        val fromOtherSide = message.fromBuyer != (conversation.side == ChatSide.BUYER)
        upsert(listOf(message))
        _typing.update { it - conversation.id }
        _conversations.update { list ->
            list.map {
                if (it.id != conversation.id) it else it.copy(
                    lastText = message.text, lastAt = message.sentAt,
                    lastSender = if (message.fromBuyer) ChatSide.BUYER else ChatSide.MERCHANT,
                    unread = if (fromOtherSide) it.unread + 1 else it.unread,
                )
            }.sortedByDescending { it.lastAt ?: Instant.EPOCH }
        }
    }

    // ------------------------------------------------------------ helpers

    private fun conversationOf(id: String): Conversation? = _conversations.value.firstOrNull { it.id == id }

    /** Id of the end of the thread that is *not* the user's store: the buyer's identity key. */
    private fun buyerKey(c: Conversation) = if (c.side == ChatSide.BUYER) BUYER_ME else c.counterpartName

    private fun MessageDto.toMessage(c: Conversation): ChatMessage {
        val mine = (senderSide == "buyer") == (c.side == ChatSide.BUYER)
        val read = !mine || (peerRead[c.id] ?: 0L) >= id
        return toChatMessage(merchantId = c.counterpartId, buyerKey = buyerKey(c), isRead = read)
    }

    private fun incomingIds(conversationId: String): List<Long> {
        val side = conversationOf(conversationId)?.side ?: return emptyList()
        return _messages.value
            .filter { it.conversationId == conversationId && it.fromBuyer != (side == ChatSide.BUYER) }
            .mapNotNull { it.id.toLongOrNull() }
    }

    /** Adds new messages and refreshes known ones (matching ids), keeping the list oldest first. */
    private fun upsert(incoming: List<ChatMessage>) {
        _messages.update { current ->
            val byId = LinkedHashMap<String, ChatMessage>()
            current.forEach { byId[it.id] = it }
            incoming.forEach { byId[it.id] = it }
            byId.values.sortedWith(compareBy({ it.sentAt }, { it.id.toLongOrNull() ?: Long.MAX_VALUE }))
        }
    }

    private suspend fun runQuietly(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: AppException) {
            // Chat refreshes are background work; the next event or screen open retries.
        }
    }

    private companion object {
        const val TYPING_MS = 3_000L
    }
}
