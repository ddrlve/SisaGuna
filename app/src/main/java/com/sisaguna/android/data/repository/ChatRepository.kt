package com.sisaguna.android.data.repository

import com.sisaguna.android.data.model.BUYER_ME
import com.sisaguna.android.data.model.ChatMessage
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * One store of conversations for both sides of the app. Buyer screens read the threads where
 * [ChatMessage.buyer] is [BUYER_ME]; partner screens read the threads of the user's own store
 * ([ListingRepository.MY_MERCHANT_ID]).
 */
interface ChatRepository {
    /** Every message, oldest first. */
    val messages: StateFlow<List<ChatMessage>>

    /** Conversations where the other side is typing, keyed by [typingKey]. */
    val typing: StateFlow<Set<String>>

    /**
     * Buyer sends to a store. The fake then plays the store's [reply] after a short typing
     * pause; the real backend will deliver the seller's actual answer instead.
     */
    suspend fun send(merchantId: String, text: String, listingId: String?, reply: String)

    /** Store (partner mode) answers a buyer. The fake buyer may acknowledge it. */
    suspend fun replyAsStore(merchantId: String, buyer: String, text: String)

    /** Buyer opened the chat with [merchantId]. */
    fun markRead(merchantId: String)

    /** Store opened the chat with [buyer]. */
    fun markReadByStore(merchantId: String, buyer: String)

    companion object {
        fun typingKey(merchantId: String, buyer: String) = "$merchantId|$buyer"
    }
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
    private val myStore = ListingRepository.MY_MERCHANT_ID

    private val _messages = MutableStateFlow(
        listOf(
            // Buyer side: the signed-in user asking other stores.
            ChatMessage("c1", "m1", true, "Bu, nasi kuningnya masih ada?", ago(2 * 24 * 60 + 30, now), listingId = "l1"),
            ChatMessage("c2", "m1", false, "Masih ada kak, sampai jam 7 malam ya. Lauknya lengkap.", ago(2 * 24 * 60 + 28, now)),
            ChatMessage("c3", "m1", true, "Oke bu, saya ambil sore ini. Terima kasih!", ago(2 * 24 * 60 + 27, now)),
            ChatMessage("c4", "m2", false, "Halo kak, roti gandum pesananmu sudah siap diambil ya. Tunjukkan kode SG-4821 di kasir.", ago(50, now), isRead = false),
            // Partner side: buyers asking the user's own store (Dapur Budi).
            ChatMessage("s1", myStore, true, "Kak, kue lapisnya masih ada 2 porsi?", ago(12, now), listingId = "l10", isRead = false, buyer = "Clara T."),
            ChatMessage("s2", myStore, true, "Bisa diambil jam 5 sore?", ago(11, now), isRead = false, buyer = "Clara T."),
            ChatMessage("s3", myStore, true, "Kulit buahnya masih ada? Mau buat komposter RW.", ago(3 * 60, now), listingId = "l11", buyer = "Kebun Warga RW 04"),
            ChatMessage("s4", myStore, false, "Masih ada kak, sekitar 3 kg. Silakan diambil.", ago(3 * 60 - 5, now), buyer = "Kebun Warga RW 04"),
            ChatMessage("s5", myStore, true, "Siap, makasih kak!", ago(3 * 60 - 4, now), buyer = "Kebun Warga RW 04"),
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
            playIncoming(ChatRepository.typingKey(merchantId, BUYER_ME)) {
                ChatMessage("st${counter++}", merchantId, false, reply, Instant.now(), isRead = false)
            }
        }
    }

    override suspend fun replyAsStore(merchantId: String, buyer: String, text: String) {
        val body = text.trim()
        if (body.isEmpty()) return
        _messages.update { it + ChatMessage("sr${counter++}", merchantId, false, body, Instant.now(), buyer = buyer) }
        // Demo buyers thank the store once, so the thread doesn't fill with copies of it.
        val thread = _messages.value.filter { it.merchantId == merchantId && it.buyer == buyer }
        if (thread.none { it.fromBuyer && it.text == THANKS }) {
            withContext(NonCancellable) {
                playIncoming(ChatRepository.typingKey(merchantId, buyer)) {
                    ChatMessage("by${counter++}", merchantId, true, THANKS, Instant.now(), isRead = false, buyer = buyer)
                }
            }
        }
    }

    private suspend fun playIncoming(key: String, message: () -> ChatMessage) {
        delay(readDelayMs)
        _typing.update { it + key }
        delay(typingMs)
        _typing.update { it - key }
        _messages.update { it + message() }
    }

    override fun markRead(merchantId: String) = markWhere { it.merchantId == merchantId && it.buyer == BUYER_ME && !it.fromBuyer }

    override fun markReadByStore(merchantId: String, buyer: String) = markWhere { it.merchantId == merchantId && it.buyer == buyer && it.fromBuyer }

    private fun markWhere(match: (ChatMessage) -> Boolean) {
        _messages.update { list -> list.map { if (!it.isRead && match(it)) it.copy(isRead = true) else it } }
    }

    private companion object {
        const val THANKS = "Siap, makasih kak!"
    }
}
