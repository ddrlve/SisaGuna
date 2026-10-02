package com.sisaguna.android.feature.notifications

import com.sisaguna.android.data.model.NotificationType
import com.sisaguna.android.data.repository.FakeNotificationRepository
import com.sisaguna.android.testutil.MainDispatcherRule
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NotificationFormatTest {

    private val zone = ZoneId.of("Asia/Jakarta")
    private val now = Instant.parse("2026-10-02T05:00:00Z") // 12:00 WIB

    @Test
    fun `day groups follow calendar days in the zone`() {
        assertEquals(DayGroup.TODAY, dayGroup(Instant.parse("2026-10-01T17:30:00Z"), now, zone)) // 00:30 WIB today
        assertEquals(DayGroup.YESTERDAY, dayGroup(Instant.parse("2026-10-01T16:50:00Z"), now, zone)) // 23:50 WIB yesterday
        assertEquals(DayGroup.THIS_WEEK, dayGroup(Instant.parse("2026-09-27T05:00:00Z"), now, zone))
        assertEquals(DayGroup.EARLIER, dayGroup(Instant.parse("2026-09-25T05:00:00Z"), now, zone))
    }

    @Test
    fun `relative time wording`() {
        assertEquals("Baru saja", relativeTime(now.minusSeconds(20), now, zone))
        assertEquals("5 mnt lalu", relativeTime(now.minusSeconds(5 * 60), now, zone))
        assertEquals("3 jam lalu", relativeTime(now.minusSeconds(3 * 3600 + 120), now, zone))
        assertEquals("Kemarin", relativeTime(now.minusSeconds(24 * 3600), now, zone))
        assertEquals("4 hari lalu", relativeTime(now.minusSeconds(4 * 24 * 3600), now, zone))
    }

    @Test
    fun `future timestamps read as just now`() {
        assertEquals("Baru saja", relativeTime(now.plusSeconds(90), now, zone))
    }
}

class NotificationsViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val now = Instant.parse("2026-10-02T05:00:00Z")

    private fun vm(repo: FakeNotificationRepository = FakeNotificationRepository(now)) =
        NotificationsViewModel(repo, { now }, ZoneId.of("Asia/Jakarta"))

    private val NotificationsViewModel.items get() = uiState.value.groups.flatMap { it.items }

    @Test
    fun `orders filter shows pickup and order only`() {
        val vm = vm()
        vm.onFilter(NotificationFilter.ORDERS)
        assertTrue(vm.items.all { it.type == NotificationType.PICKUP || it.type == NotificationType.ORDER })
        assertEquals(3, vm.items.size)
    }

    @Test
    fun `impact only shows under all`() {
        val vm = vm()
        assertTrue(vm.items.any { it.type == NotificationType.IMPACT })
        NotificationFilter.entries.filter { it != NotificationFilter.ALL }.forEach { f ->
            vm.onFilter(f)
            assertFalse(vm.items.any { it.type == NotificationType.IMPACT })
        }
    }

    @Test
    fun `groups are ordered today first`() {
        val groups = vm().uiState.value.groups.map { it.group }
        assertEquals(groups.sorted(), groups)
        assertEquals(DayGroup.TODAY, groups.first())
    }

    @Test
    fun `open marks read and unread count drops`() {
        val vm = vm()
        assertEquals(3, vm.uiState.value.unreadCount)
        vm.onOpen("n1")
        assertEquals(2, vm.uiState.value.unreadCount)
    }

    @Test
    fun `mark all read`() {
        val vm = vm()
        vm.markAllRead()
        assertEquals(0, vm.uiState.value.unreadCount)
    }

    @Test
    fun `delete then undo restores at the old index`() {
        val repo = FakeNotificationRepository(now)
        val vm = vm(repo)
        val deleted = vm.delete("n3")
        assertNotNull(deleted)
        assertFalse(vm.items.any { it.id == "n3" })
        vm.onOpen("n1") // unrelated change in between
        vm.undoDelete(deleted!!)
        assertEquals("n3", repo.notifications.value[2].id)
    }

    @Test
    fun `unread count ignores the active filter`() {
        val vm = vm()
        vm.onFilter(NotificationFilter.PROMOS)
        assertEquals(3, vm.uiState.value.unreadCount)
    }
}

class NotificationSettingsViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun `master off keeps child values`() {
        val repo = FakeNotificationRepository()
        val vm = NotificationSettingsViewModel(repo)
        vm.update { copy(promo = true) }
        vm.update { copy(enabled = false) }
        assertFalse(repo.prefs.value.enabled)
        assertTrue(repo.prefs.value.promo)
        vm.update { copy(enabled = true) }
        assertTrue(repo.prefs.value.promo)
    }
}
