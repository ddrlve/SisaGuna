package com.sisaguna.android.feature.chat

import com.sisaguna.android.data.model.HalalStatus
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.data.model.MerchantStatus
import com.sisaguna.android.data.model.StorageMethod
import com.sisaguna.android.data.repository.FakeChatRepository
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatTest {

    private val now = Instant.parse("2026-10-04T10:00:00Z")
    private val store = Merchant("m1", "Warung Bu Sari", true, MerchantStatus.APPROVED, "Kemang", halal = HalalStatus.HALAL_CERTIFIED, deliveryAvailable = false)
    private val nasi = Listing(
        "l1", "m1", "Nasi Kuning", ListingTier.HUMAN, 25_000, 8_000, false,
        pickupEnd = now.plus(2, ChronoUnit.HOURS), imageUrl = "", distanceKm = 1.0, stock = 6,
        madeAt = now.minus(1, ChronoUnit.HOURS), storage = StorageMethod.ROOM_TEMP,
    )

    @Test
    fun `availability question answers with live stock`() {
        val reply = ChatReplies.replyFor("Masih tersedia?", store, nasi, now)
        assertTrue(reply, "6 porsi" in reply)
    }

    @Test
    fun `halal and delivery answers follow the store data`() {
        assertTrue(ChatReplies.replyFor("Halal?", store, nasi, now).contains("bersertifikat halal"))
        assertTrue(ChatReplies.replyFor("Bisa diantar?", store, nasi, now).contains("ambil sendiri"))
    }

    @Test
    fun `sold out item says so`() {
        val reply = ChatReplies.replyFor("masih ada?", store, nasi.copy(stock = 0), now)
        assertTrue(reply, "habis" in reply)
    }

    @Test
    fun `sending adds my message then the store reply and clears typing`() = runTest {
        val repo = FakeChatRepository(now, readDelayMs = 0, typingMs = 0)
        val before = repo.messages.value.size
        repo.send("m3", "Masih ada?", "l3", "Masih kak")
        val added = repo.messages.value.drop(before)
        assertEquals(listOf(true, false), added.map { it.fromBuyer })
        assertEquals("l3", added.first().listingId)
        assertFalse("m3" in repo.typing.value)
    }

    @Test
    fun `store reply lands in that buyer's thread and the buyer thanks once`() = runTest {
        val repo = FakeChatRepository(now, 0, 0)
        val store = com.sisaguna.android.data.repository.ListingRepository.MY_MERCHANT_ID
        repo.replyAsStore(store, "Clara T.", "Masih ada kak")
        repo.replyAsStore(store, "Clara T.", "Sudah siap diambil")
        val thread = repo.messages.value.filter { it.merchantId == store && it.buyer == "Clara T." }
        assertEquals(2, thread.count { !it.fromBuyer })
        assertEquals(1, thread.count { it.fromBuyer && it.text == "Siap, makasih kak!" })
    }

    @Test
    fun `store opening a thread marks only that buyer's messages read`() {
        val repo = FakeChatRepository(now, 0, 0)
        val store = com.sisaguna.android.data.repository.ListingRepository.MY_MERCHANT_ID
        repo.markReadByStore(store, "Clara T.")
        assertTrue(repo.messages.value.none { it.buyer == "Clara T." && !it.isRead })
        assertTrue(repo.messages.value.any { it.merchantId == "m2" && !it.isRead }) // buyer-side thread untouched
    }

    @Test
    fun `opening a chat marks store messages read`() {
        val repo = FakeChatRepository(now, 0, 0)
        assertTrue(repo.messages.value.any { it.merchantId == "m2" && !it.isRead })
        repo.markRead("m2")
        assertTrue(repo.messages.value.none { it.merchantId == "m2" && !it.isRead })
    }
}
