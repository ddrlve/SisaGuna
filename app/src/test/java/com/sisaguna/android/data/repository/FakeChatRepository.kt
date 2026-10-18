package com.sisaguna.android.data.repository

import com.sisaguna.android.data.model.ChatMessage
import com.sisaguna.android.data.model.ChatSide
import com.sisaguna.android.data.model.Conversation
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.sisaguna.android.data.remote.AppException

/** In-memory stand-in for tests: no server, no replies. */
@Singleton
class FakeChatRepository @Inject constructor() : ChatRepository {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    override val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    override val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _typing = MutableStateFlow<Set<String>>(emptySet())
    override val typing: StateFlow<Set<String>> = _typing.asStateFlow()

    private val ids = AtomicLong(1)

    /** When set, the next sends fail with it (nothing is stored), like a dropped connection. */
    var failSend: AppException? = null

    override suspend fun refresh() {}

    override suspend fun conversationWith(merchantId: String): Conversation =
        _conversations.value.firstOrNull { it.side == ChatSide.BUYER && it.counterpartId == merchantId }
            ?: Conversation("conv-$merchantId", merchantId, merchantId, ChatSide.BUYER).also { c -> _conversations.update { listOf(c) + it } }

    override suspend fun loadMessages(conversationId: String) {}

    override suspend fun send(conversationId: String, text: String, listingId: String?) {
        failSend?.let { throw it }
        val conversation = _conversations.value.firstOrNull { it.id == conversationId } ?: return
        deliver(conversation, fromBuyer = conversation.side == ChatSide.BUYER, text = text.trim(), listingId = listingId)
    }

    /** Test helper: the other side writes. */
    fun receive(conversationId: String, text: String) {
        val conversation = _conversations.value.first { it.id == conversationId }
        deliver(conversation, fromBuyer = conversation.side != ChatSide.BUYER, text = text, listingId = null)
        _conversations.update { list -> list.map { if (it.id == conversationId) it.copy(unread = it.unread + 1) else it } }
    }

    private fun deliver(conversation: Conversation, fromBuyer: Boolean, text: String, listingId: String?) {
        val message = ChatMessage(
            id = "m${ids.getAndIncrement()}", merchantId = conversation.counterpartId, fromBuyer = fromBuyer,
            text = text, sentAt = Instant.now(), listingId = listingId, conversationId = conversation.id,
        )
        _messages.update { it + message }
        _conversations.update { list ->
            list.map {
                if (it.id == conversation.id) it.copy(lastText = text, lastAt = message.sentAt, lastSender = if (fromBuyer) ChatSide.BUYER else ChatSide.MERCHANT) else it
            }
        }
    }

    override fun markRead(conversationId: String) {
        _conversations.update { list -> list.map { if (it.id == conversationId) it.copy(unread = 0) else it } }
    }
}
