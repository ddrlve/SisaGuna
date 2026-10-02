package com.sisaguna.android.data.repository

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeSavedMerchantRepositoryTest {

    @Test
    fun `unsave then restore returns id to its original position`() {
        val repo = FakeSavedMerchantRepository()
        repo.unsave("m2")
        assertEquals(listOf("m1", "m4", "m5"), repo.savedIds.value.toList())
        repo.restore("m2")
        assertEquals(listOf("m1", "m2", "m4", "m5"), repo.savedIds.value.toList())
    }

    @Test
    fun `restore of an id that was never saved appends it`() {
        val repo = FakeSavedMerchantRepository()
        repo.restore("m3")
        assertEquals(listOf("m1", "m2", "m4", "m5", "m3"), repo.savedIds.value.toList())
    }

    @Test
    fun `restore after more removals clamps to the list size`() {
        val repo = FakeSavedMerchantRepository()
        repo.unsave("m5") // was index 3
        repo.unsave("m1")
        repo.unsave("m2")
        repo.restore("m5")
        assertEquals(listOf("m4", "m5"), repo.savedIds.value.toList())
    }
}

class FakeNotificationRepositoryTest {

    private val now = Instant.parse("2026-10-02T12:00:00Z")

    @Test
    fun `seed has three unread`() {
        val repo = FakeNotificationRepository(now)
        assertEquals(3, repo.notifications.value.count { !it.isRead })
    }

    @Test
    fun `markAllRead leaves zero unread`() {
        val repo = FakeNotificationRepository(now)
        repo.markAllRead()
        assertEquals(0, repo.notifications.value.count { !it.isRead })
    }

    @Test
    fun `delete then restore puts the item back at its index`() {
        val repo = FakeNotificationRepository(now)
        val target = repo.notifications.value[2]
        repo.delete(target.id)
        assertEquals(7, repo.notifications.value.size)
        repo.restore(target, 2)
        assertEquals(target, repo.notifications.value[2])
        assertEquals(8, repo.notifications.value.size)
    }
}
