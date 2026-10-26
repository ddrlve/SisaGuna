package com.sisaguna.android.data.repository

import com.sisaguna.android.core.ui.AppMessenger
import com.sisaguna.android.data.model.AppNotification
import com.sisaguna.android.data.model.NotificationPrefs
import com.sisaguna.android.data.remote.ApiService
import com.sisaguna.android.data.remote.AppException
import com.sisaguna.android.data.remote.RealtimeClient
import com.sisaguna.android.data.remote.RealtimeEvent
import com.sisaguna.android.data.remote.apiCall
import com.sisaguna.android.data.remote.apiData
import com.sisaguna.android.data.remote.dto.NotificationPrefsDto
import com.sisaguna.android.data.remote.toNotification
import com.sisaguna.android.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The bell inbox, owned by the server (orders, pickups and followed stores create entries there).
 * Read/delete/preference changes show instantly and roll back with a message if refused.
 */
@Singleton
class ApiNotificationRepository @Inject constructor(
    private val api: ApiService,
    private val auth: AuthRepository,
    private val messenger: AppMessenger,
    realtime: RealtimeClient,
    @ApplicationScope private val scope: CoroutineScope,
) : NotificationRepository {

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    override val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _prefs = MutableStateFlow(NotificationPrefs())
    override val prefs: StateFlow<NotificationPrefs> = _prefs.asStateFlow()

    /** Where the next older page starts; null when everything has been loaded. */
    private var nextCursor: String? = null
    private val _hasMore = MutableStateFlow(false)
    override val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()
    private val pagingLock = Mutex()

    init {
        scope.launch {
            auth.isAuthenticated.collect { signedIn ->
                if (!signedIn) {
                    setCursor(null)
                    _notifications.value = emptyList()
                    _prefs.value = NotificationPrefs()
                    return@collect
                }
                try {
                    refresh()
                    _prefs.value = apiData { api.notificationPrefs() }.toPrefs()
                } catch (e: AppException) {
                    messenger.show(e)
                }
            }
        }
    }

    init {
        scope.launch {
            realtime.events.collect { event ->
                if (auth.isAuthenticated.value && (event == RealtimeEvent.NotificationCreated || event == RealtimeEvent.Connected)) {
                    try {
                        refresh()
                    } catch (e: AppException) {
                        // Background refresh: the next event or screen open retries.
                    }
                }
            }
        }
    }

    override suspend fun refresh() {
        // A guest has nothing to load (and the server would answer 401).
        if (!auth.isAuthenticated.value) return
        pagingLock.withLock {
            val envelope = apiCall { api.notifications() }
            val page = envelope.data.map { it.toNotification() }
            // Older pages the user already scrolled to stay (their cursor too); only the newest page is renewed.
            val oldest = page.minOfOrNull { it.createdAt }
            val older = if (oldest == null) emptyList() else _notifications.value.filter { n -> n.createdAt < oldest && page.none { it.id == n.id } }
            _notifications.value = page + older
            if (older.isEmpty()) setCursor(envelope.meta?.nextCursor)
        }
    }

    override suspend fun loadMore() {
        if (!auth.isAuthenticated.value) return
        pagingLock.withLock {
            val cursor = nextCursor ?: return
            val envelope = apiCall { api.notifications(cursor) }
            val more = envelope.data.map { it.toNotification() }
            _notifications.value = (_notifications.value + more).distinctBy { it.id }
            setCursor(envelope.meta?.nextCursor)
        }
    }

    private fun setCursor(cursor: String?) {
        nextCursor = cursor
        _hasMore.value = cursor != null
    }

    override fun markRead(id: String) {
        val target = _notifications.value.firstOrNull { it.id == id }?.takeIf { !it.isRead } ?: return
        change(optimistic = { replace(target.copy(isRead = true)) }, rollback = { replace(target) }) {
            api.markNotificationRead(id, JsonObject(mapOf("read" to JsonPrimitive(true))))
        }
    }

    override fun markAllRead() {
        val before = _notifications.value
        if (before.none { !it.isRead }) return
        change(optimistic = { _notifications.value = before.map { it.copy(isRead = true) } }, rollback = { _notifications.value = before }) {
            api.markAllNotificationsRead()
        }
    }

    override fun delete(id: String) {
        val before = _notifications.value
        change(optimistic = { _notifications.value = before.filterNot { it.id == id } }, rollback = { _notifications.value = before }) {
            api.deleteNotification(id)
        }
    }

    override fun restore(notification: AppNotification, index: Int) {
        val before = _notifications.value
        change(
            optimistic = {
                val list = before.filterNot { it.id == notification.id }.toMutableList()
                list.add(index.coerceIn(0, list.size), notification)
                _notifications.value = list
            },
            rollback = { _notifications.value = before },
        ) {
            api.restoreNotification(notification.id)
        }
    }

    override fun updatePrefs(prefs: NotificationPrefs) {
        val before = _prefs.value
        change(optimistic = { _prefs.value = prefs }, rollback = { _prefs.value = before }) {
            api.setNotificationPrefs(NotificationPrefsDto(prefs.enabled, prefs.pickup, prefs.order, prefs.merchant, prefs.promo, prefs.impact))
        }
    }

    private fun replace(item: AppNotification) {
        _notifications.value = _notifications.value.map { if (it.id == item.id) item else it }
    }

    /** Shows the change now; if the server refuses it, [rollback] restores the old state and says why. */
    private fun change(optimistic: () -> Unit, rollback: () -> Unit, call: suspend () -> Unit) {
        if (!auth.isAuthenticated.value) return
        optimistic()
        scope.launch {
            try {
                apiCall { call() }
            } catch (e: AppException) {
                rollback()
                messenger.show(e)
            }
        }
    }
}

private fun NotificationPrefsDto.toPrefs() = NotificationPrefs(enabled, pickup, order, merchant, promo, impact)
