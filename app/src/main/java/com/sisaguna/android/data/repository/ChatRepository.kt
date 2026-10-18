package com.sisaguna.android.data.repository

import com.sisaguna.android.data.model.ChatMessage
import com.sisaguna.android.data.model.Conversation
import kotlinx.coroutines.flow.StateFlow
import com.sisaguna.android.data.remote.AppException

/**
 * Conversations for both ends of the app: as a buyer (the counterpart is a store) and, for
 * accounts with a store, as the store owner (the counterpart is a buyer). A message belongs to
 * a server-side conversation id, so the same thread looks identical from both phones.
 */
interface ChatRepository {
    /** Every thread of the user, newest activity first. */
    val conversations: StateFlow<List<Conversation>>

    /** Messages loaded so far across threads, oldest first. */
    val messages: StateFlow<List<ChatMessage>>

    /** Conversation ids where the other side is typing right now. */
    val typing: StateFlow<Set<String>>

    /** Reloads both inboxes. */
    suspend fun refresh()

    /** The buyer's thread with a store; created on first use. */
    suspend fun conversationWith(merchantId: String): Conversation

    /** Loads the newest messages of a thread (or only the ones missed since the last load). */
    suspend fun loadMessages(conversationId: String)

    /** Throws [AppException] when the message could not be delivered. */
    suspend fun send(conversationId: String, text: String, listingId: String?)

    /** Everything received in the thread is now read. */
    fun markRead(conversationId: String)

    /** The user is typing in the thread; the other side sees an indicator for a moment. */
    fun notifyTyping(conversationId: String) {}
}
