package com.sisaguna.android.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sisaguna.android.core.ui.AppMessenger
import com.sisaguna.android.data.model.ChatMessage
import com.sisaguna.android.data.model.ChatSide
import com.sisaguna.android.data.model.Conversation
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.data.remote.AppException
import com.sisaguna.android.data.repository.ChatRepository
import com.sisaguna.android.data.repository.ListingRepository
import com.sisaguna.android.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatUiState(
    /** Buyer chat: the store being asked. Store chat: the user's own store. */
    val merchant: Merchant? = null,
    /** The other end's name (store for a buyer, buyer for the store owner); known before the store is cached. */
    val counterpartName: String = "",
    /** The item the chat is about; pinned under the top bar as context. */
    val listing: Listing? = null,
    val messages: List<ChatMessage> = emptyList(),
    val otherTyping: Boolean = false,
    val draft: String = "",
) {
    val canSend: Boolean get() = draft.isNotBlank()
}

/** One inbox row; [merchant] is only known for the buyer's side (for the store photo). */
data class ChatThread(
    val id: String,
    val merchantId: String,
    val name: String,
    val merchant: Merchant?,
    val lastText: String,
    val lastAt: Instant?,
    val lastMine: Boolean,
    val unread: Int,
)

private fun Conversation.toThread(merchant: Merchant?) =
    ChatThread(id, counterpartId, counterpartName, merchant, lastText, lastAt, lastSender == side, unread)

/**
 * Shared by both ends of a thread: loads it, keeps it marked read while it is open, relays typing,
 * and restores the draft if a send fails.
 */
private class ThreadSession(
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val chat: ChatRepository,
    private val messenger: AppMessenger,
) {
    val conversationId = MutableStateFlow<String?>(null)
    private var lastTypingPing = 0L

    /** Opens [find]'s thread, loads its messages, and marks it read as messages arrive while it is open. */
    fun open(find: suspend () -> Conversation?) {
        scope.launch {
            try {
                val conversation = find() ?: return@launch
                // Publish the id only after loading: marking read needs the messages (their ids) in hand.
                try {
                    chat.loadMessages(conversation.id)
                } finally {
                    conversationId.value = conversation.id
                }
            } catch (e: AppException) {
                messenger.show(e)
            }
        }
        scope.launch {
            combine(chat.conversations, conversationId) { all, id -> all.firstOrNull { it.id == id } }.collect { c ->
                if (c != null && c.unread > 0) chat.markRead(c.id)
            }
        }
    }

    fun typing() {
        val id = conversationId.value ?: return
        val now = System.currentTimeMillis()
        if (now - lastTypingPing < TYPING_PING_MS) return
        lastTypingPing = now
        chat.notifyTyping(id)
    }

    /** Returns false when the message could not be sent, so the caller can put the text back. */
    suspend fun send(text: String, listingId: String?): Boolean {
        val id = conversationId.value ?: return false
        return try {
            chat.send(id, text, listingId)
            true
        } catch (e: AppException) {
            messenger.show(e)
            false
        }
    }

    private companion object {
        const val TYPING_PING_MS = 2_000L
    }
}

/** Buyer side: the signed-in user talking to one store. */
@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val listingRepository: ListingRepository,
    messenger: AppMessenger,
) : ViewModel() {

    private val merchantId: String = savedStateHandle.get<String>(Screen.Chat.ARG_MERCHANT).orEmpty()
    private val listingId: String? = savedStateHandle.get<String>(Screen.Chat.ARG_LISTING)?.ifBlank { null }
    private val draft = MutableStateFlow("")
    private val session = ThreadSession(viewModelScope, chatRepository, messenger)

    val uiState: StateFlow<ChatUiState> = combine(
        chatRepository.messages,
        chatRepository.typing,
        listingRepository.listings,
        draft,
        session.conversationId,
    ) { all, typing, listings, d, cid ->
        val thread = if (cid == null) emptyList() else all.filter { it.conversationId == cid }
        // Opened from a listing: that one. From the inbox: the item the thread started about.
        val aboutId = listingId ?: thread.firstNotNullOfOrNull { it.listingId }
        ChatUiState(
            merchant = listingRepository.merchant(merchantId),
            counterpartName = chatRepository.conversations.value.firstOrNull { it.id == cid }?.counterpartName.orEmpty(),
            listing = aboutId?.let { id -> listings.firstOrNull { it.id == id } },
            messages = thread,
            otherTyping = cid != null && cid in typing,
            draft = d,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ChatUiState(merchant = listingRepository.merchant(merchantId)))

    init {
        session.open { chatRepository.conversationWith(merchantId) }
    }

    fun onDraftChange(value: String) {
        if (value.length > draft.value.length) session.typing()
        draft.value = value.take(500)
    }

    fun send(text: String = draft.value) {
        val body = text.trim()
        if (body.isEmpty()) return
        val fromDraft = text == draft.value
        if (fromDraft) draft.value = ""
        // Only the first question carries the listing card; later ones are about the same item.
        val attach = listingId.takeIf { uiState.value.messages.none { it.listingId == listingId } }
        viewModelScope.launch {
            if (!session.send(body, attach) && fromDraft && draft.value.isEmpty()) draft.value = body
        }
    }
}

/** Buyer side: one row per store the user has talked to. */
@HiltViewModel
class ChatInboxViewModel @Inject constructor(
    chatRepository: ChatRepository,
    private val listingRepository: ListingRepository,
    messenger: AppMessenger,
) : ViewModel() {

    val threads: StateFlow<List<ChatThread>> = chatRepository.conversations.map { all ->
        all.filter { it.side == ChatSide.BUYER }.map { it.toThread(listingRepository.merchant(it.counterpartId)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val unreadTotal: StateFlow<Int> = threads.map { list -> list.sumOf { it.unread } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    init {
        viewModelScope.launch {
            try {
                chatRepository.refresh()
            } catch (e: AppException) {
                messenger.show(e)
            }
        }
    }
}

/** Partner side: buyers who wrote to the user's own store. */
@HiltViewModel
class SellerInboxViewModel @Inject constructor(
    chatRepository: ChatRepository,
    messenger: AppMessenger,
) : ViewModel() {

    val threads: StateFlow<List<ChatThread>> = chatRepository.conversations.map { all ->
        all.filter { it.side == ChatSide.MERCHANT }.map { it.toThread(merchant = null) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val unreadTotal: StateFlow<Int> = threads.map { list -> list.sumOf { it.unread } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    init {
        viewModelScope.launch {
            try {
                chatRepository.refresh()
            } catch (e: AppException) {
                messenger.show(e)
            }
        }
    }
}

/** Partner side: the user's store answering one buyer. The route argument is the conversation id. */
@HiltViewModel
class SellerChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val listingRepository: ListingRepository,
    messenger: AppMessenger,
) : ViewModel() {

    private val conversationId: String = savedStateHandle.get<String>(Screen.SellerChat.ARG_BUYER).orEmpty()
    private val draft = MutableStateFlow("")
    private val session = ThreadSession(viewModelScope, chatRepository, messenger)

    val uiState: StateFlow<ChatUiState> = combine(
        chatRepository.messages,
        chatRepository.typing,
        listingRepository.listings,
        draft,
        chatRepository.conversations,
    ) { all, typing, listings, d, conversations ->
        val thread = all.filter { it.conversationId == conversationId }
        ChatUiState(
            merchant = listingRepository.myMerchant.value,
            counterpartName = conversations.firstOrNull { it.id == conversationId }?.counterpartName.orEmpty(),
            listing = thread.firstNotNullOfOrNull { it.listingId }?.let { id -> listings.firstOrNull { it.id == id } },
            messages = thread,
            otherTyping = conversationId in typing,
            draft = d,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ChatUiState())

    init {
        session.open { chatRepository.conversations.value.firstOrNull { it.id == conversationId } }
    }

    fun onDraftChange(value: String) {
        if (value.length > draft.value.length) session.typing()
        draft.value = value.take(500)
    }

    fun send(text: String = draft.value) {
        val body = text.trim()
        if (body.isEmpty()) return
        val fromDraft = text == draft.value
        if (fromDraft) draft.value = ""
        viewModelScope.launch {
            if (!session.send(body, null) && fromDraft && draft.value.isEmpty()) draft.value = body
        }
    }
}
