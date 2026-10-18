package com.sisaguna.android.feature.chat

import androidx.lifecycle.SavedStateHandle
import com.sisaguna.android.core.ui.AppMessenger
import com.sisaguna.android.data.model.ChatSide
import com.sisaguna.android.data.remote.NetworkException
import com.sisaguna.android.data.repository.FakeChatRepository
import com.sisaguna.android.navigation.Screen
import com.sisaguna.android.testutil.MainDispatcherRule
import com.sisaguna.android.testutil.TestListingRepository
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val chat = FakeChatRepository()
    private val listings = TestListingRepository()
    private val messenger = AppMessenger()

    private fun buyerVm(listing: String = "") = ChatViewModel(
        SavedStateHandle(mapOf(Screen.Chat.ARG_MERCHANT to "m1", Screen.Chat.ARG_LISTING to listing)),
        chat, listings, messenger,
    )

    @Test
    fun `opening a store chat creates the thread and sending lands in it`() {
        val vm = buyerVm("l1")
        vm.onDraftChange("Masih ada?")
        vm.send()

        val state = vm.uiState.value
        assertEquals(listOf("Masih ada?"), state.messages.map { it.text })
        assertEquals("", state.draft)
        assertEquals("l1", state.messages.single().listingId) // the first question carries the listing card
        assertEquals("Warung Bu Sari", state.merchant?.name)
    }

    @Test
    fun `only the first message carries the listing`() {
        val vm = buyerVm("l1")
        vm.send("Masih ada?")
        vm.send("Dimasak jam berapa?")
        assertEquals(listOf("l1", null), vm.uiState.value.messages.map { it.listingId })
    }

    @Test
    fun `a failed send gives the text back to the draft`() {
        val vm = buyerVm()
        chat.failSend = NetworkException(IOException("offline"))
        vm.onDraftChange("Halo kak")
        vm.send()
        assertEquals("Halo kak", vm.uiState.value.draft)
        assertTrue(vm.uiState.value.messages.isEmpty())
    }

    @Test
    fun `messages that arrive while the chat is open are marked read`() {
        val vm = buyerVm()
        vm.send("Halo")
        val conversation = chat.conversations.value.single()
        chat.receive(conversation.id, "Halo juga kak")
        assertEquals(0, chat.conversations.value.single().unread)
        assertEquals(2, vm.uiState.value.messages.size)
    }

    @Test
    fun `typing indicator follows the repository`() {
        val vm = buyerVm()
        assertFalse(vm.uiState.value.otherTyping)
        assertEquals(ChatSide.BUYER, chat.conversations.value.single().side)
    }
}
