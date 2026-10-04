package com.sisaguna.android.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sisaguna.android.data.model.ChatMessage
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.data.repository.ChatRepository
import com.sisaguna.android.data.repository.ListingRepository
import com.sisaguna.android.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatUiState(
    val merchant: Merchant? = null,
    /** The item the chat was opened from; pinned under the top bar as context. */
    val listing: Listing? = null,
    val messages: List<ChatMessage> = emptyList(),
    val storeTyping: Boolean = false,
    val draft: String = "",
) {
    val canSend: Boolean get() = draft.isNotBlank()
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val listingRepository: ListingRepository,
) : ViewModel() {

    private val merchantId: String = savedStateHandle.get<String>(Screen.Chat.ARG_MERCHANT).orEmpty()
    private val listingId: String? = savedStateHandle.get<String>(Screen.Chat.ARG_LISTING)?.ifBlank { null }
    private val draft = MutableStateFlow("")

    val uiState: StateFlow<ChatUiState> = combine(
        chatRepository.messages,
        chatRepository.typing,
        listingRepository.listings,
        draft,
    ) { all, typing, listings, d ->
        ChatUiState(
            merchant = listingRepository.merchant(merchantId),
            listing = listingId?.let { id -> listings.firstOrNull { it.id == id } },
            messages = all.filter { it.merchantId == merchantId },
            storeTyping = merchantId in typing,
            draft = d,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ChatUiState(merchant = listingRepository.merchant(merchantId)))

    init {
        // Everything that arrives while the chat is open counts as read.
        viewModelScope.launch {
            chatRepository.messages.collect { list ->
                if (list.any { it.merchantId == merchantId && !it.isRead }) chatRepository.markRead(merchantId)
            }
        }
    }

    fun onDraftChange(value: String) {
        draft.value = value.take(500)
    }

    fun send(text: String = draft.value) {
        val body = text.trim()
        val merchant = uiState.value.merchant ?: return
        if (body.isEmpty()) return
        if (text == draft.value) draft.value = ""
        // Only the first question carries the listing card, later ones are about the same item.
        val attach = listingId.takeIf { uiState.value.messages.none { it.listingId == listingId } }
        val reply = ChatReplies.replyFor(body, merchant, uiState.value.listing)
        viewModelScope.launch { chatRepository.send(merchantId, body, attach, reply) }
    }
}

data class ChatThread(
    val merchant: Merchant,
    val last: ChatMessage,
    val unread: Int,
)

@HiltViewModel
class ChatInboxViewModel @Inject constructor(
    chatRepository: ChatRepository,
    private val listingRepository: ListingRepository,
) : ViewModel() {

    /** One row per store, newest conversation first. */
    val threads: StateFlow<List<ChatThread>> = chatRepository.messages.map { all ->
        all.groupBy { it.merchantId }.mapNotNull { (id, msgs) ->
            val merchant = listingRepository.merchant(id) ?: return@mapNotNull null
            ChatThread(merchant, msgs.maxBy { it.sentAt }, msgs.count { !it.fromMe && !it.isRead })
        }.sortedByDescending { it.last.sentAt }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val unreadTotal: StateFlow<Int> = threads.map { list -> list.sumOf { it.unread } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
}
