package com.sisaguna.android.data.repository

import com.sisaguna.android.data.model.ChatMessage
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface ChatRepository {
    /** Every message, oldest first. Screens filter by merchant. */
    val messages: StateFlow<List<ChatMessage>>

    /** Stores currently "typing" a reply. */
    val typing: StateFlow<Set<String>>

    /**
     * Sends the buyer's message. The fake then plays the store's [reply] after a short typing
     * pause; the real backend will push the seller's actual answer instead.
     */
    suspend fun send(merchantId: String, text: String, listingId: String?, reply: String)

    fun markRead(merchantId: String)
}

@Singleton
class FakeChatRepository(
    now: Instant,
    private val readDelayMs: Long,
    private val typingMs: Long,
) : ChatRepository {

    @Inject constructor() : this(Instant.now(), 700, 1_400)

    private var counter = 0
    private fun ago(minutes: Long, now: Instant) = now.minus(Duration.ofMinutes(minutes))

    private val _messages = MutableStateFlow(
        listOf(
            ChatMessage("c1", "m1", true, "Bu, nasi kuningnya masih ada?", ago(2 * 24 * 60 + 30, now), listingId = "l1"),
            ChatMessage("c2", "m1", false, "Masih ada kak, sampai jam 7 malam ya. Lauknya lengkap.", ago(2 * 24 * 60 + 28, now)),
            ChatMessage("c3", "m1", true, "Oke bu, saya ambil sore ini. Terima kasih!", ago(2 * 24 * 60 + 27, now)),
            ChatMessage("c4", "m2", false, "Halo kak, roti gandum pesananmu sudah siap diambil ya. Tunjukkan kode SG-4821 di kasir.", ago(50, now), isRead = false),
        ),
    )
    override val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _typing = MutableStateFlow<Set<String>>(emptySet())
    override val typing: StateFlow<Set<String>> = _typing.asStateFlow()

    override suspend fun send(merchantId: String, text: String, listingId: String?, reply: String) {
        val body = text.trim()
        if (body.isEmpty()) return
        _messages.update { it + ChatMessage("me${counter++}", merchantId, true, body, Instant.now(), listingId) }
        // The seller still answers if the buyer leaves the chat mid-wait, like a real one would.
        withContext(NonCancellable) {
            delay(readDelayMs)
            _typing.update { it + merchantId }
            delay(typingMs)
            _typing.update { it - merchantId }
            _messages.update { it + ChatMessage("st${counter++}", merchantId, false, reply, Instant.now()) }
        }
    }

    override fun markRead(merchantId: String) {
        _messages.update { list -> list.map { if (it.merchantId == merchantId && !it.isRead) it.copy(isRead = true) else it } }
    }
}
