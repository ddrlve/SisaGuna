package com.sisaguna.android.feature.notifications

import com.sisaguna.android.ui.i18n.l

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sisaguna.android.data.model.AppNotification
import com.sisaguna.android.data.model.NotificationPrefs
import com.sisaguna.android.data.model.NotificationType
import com.sisaguna.android.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class DayGroup { TODAY, YESTERDAY, THIS_WEEK, EARLIER }

private fun daysBetween(at: Instant, now: Instant, zone: ZoneId): Long =
    ChronoUnit.DAYS.between(at.atZone(zone).toLocalDate(), now.atZone(zone).toLocalDate())

/** Calendar-day buckets in the device zone, so 23:50 yesterday is "Kemarin", not "Hari ini". */
fun dayGroup(at: Instant, now: Instant, zone: ZoneId): DayGroup {
    val days = daysBetween(at, now, zone)
    return when {
        days <= 0 -> DayGroup.TODAY
        days == 1L -> DayGroup.YESTERDAY
        days <= 6 -> DayGroup.THIS_WEEK
        else -> DayGroup.EARLIER
    }
}

fun relativeTime(at: Instant, now: Instant, zone: ZoneId): String {
    val minutes = Duration.between(at, now).toMinutes().coerceAtLeast(0)
    return when (dayGroup(at, now, zone)) {
        DayGroup.TODAY -> when {
            minutes < 1 -> "Baru saja"
            minutes < 60 -> "$minutes mnt lalu"
            else -> l("${minutes / 60} jam lalu", "${minutes / 60}h ago")
        }
        DayGroup.YESTERDAY -> "Kemarin"
        else -> l("${daysBetween(at, now, zone)} hari lalu", "${daysBetween(at, now, zone)}d ago")
    }
}

/** IMPACT has no chip of its own — it only shows under [ALL] (spec addendum). */
enum class NotificationFilter(val types: Set<NotificationType>) {
    ALL(NotificationType.entries.toSet()),
    ORDERS(setOf(NotificationType.PICKUP, NotificationType.ORDER)),
    MERCHANTS(setOf(NotificationType.MERCHANT)),
    PROMOS(setOf(NotificationType.PROMO)),
}

data class NotificationGroup(val group: DayGroup, val items: List<AppNotification>)

data class NotificationsUiState(
    val filter: NotificationFilter = NotificationFilter.ALL,
    val groups: List<NotificationGroup> = emptyList(),
    val unreadCount: Int = 0,
    val now: Instant = Instant.now(),
) {
    val isEmpty: Boolean get() = groups.isEmpty()
}

/** What [NotificationsViewModel.delete] removed, so the Snackbar can put it back. */
data class DeletedNotification(val notification: AppNotification, val index: Int)

@HiltViewModel
class NotificationsViewModel(
    private val repository: NotificationRepository,
    private val clock: () -> Instant,
    private val zone: ZoneId,
) : ViewModel() {

    @Inject constructor(repository: NotificationRepository) :
        this(repository, Instant::now, ZoneId.systemDefault())

    private val filter = MutableStateFlow(NotificationFilter.ALL)

    val uiState: StateFlow<NotificationsUiState> = combine(repository.notifications, filter) { all, f ->
        val now = clock()
        val visible = all.filter { it.type in f.types }
        NotificationsUiState(
            filter = f,
            groups = visible.groupBy { dayGroup(it.createdAt, now, zone) }
                .toSortedMap()
                .map { (group, items) -> NotificationGroup(group, items) },
            unreadCount = all.count { !it.isRead },
            now = now,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, NotificationsUiState())

    fun onFilter(filter: NotificationFilter) {
        this.filter.value = filter
    }

    fun onOpen(id: String) = repository.markRead(id)

    fun markAllRead() = repository.markAllRead()

    fun delete(id: String): DeletedNotification? {
        val list = repository.notifications.value
        val index = list.indexOfFirst { it.id == id }
        if (index < 0) return null
        repository.delete(id)
        return DeletedNotification(list[index], index)
    }

    fun undoDelete(deleted: DeletedNotification) = repository.restore(deleted.notification, deleted.index)
}

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val repository: NotificationRepository,
) : ViewModel() {

    val prefs: StateFlow<NotificationPrefs> = repository.prefs

    fun update(transform: NotificationPrefs.() -> NotificationPrefs) {
        repository.updatePrefs(repository.prefs.value.transform())
    }
}
