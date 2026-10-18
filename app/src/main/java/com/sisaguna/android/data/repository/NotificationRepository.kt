package com.sisaguna.android.data.repository

import com.sisaguna.android.data.model.AppNotification
import com.sisaguna.android.data.model.NotificationPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface NotificationRepository {
    /** Newest first. */
    val notifications: StateFlow<List<AppNotification>>
    val prefs: StateFlow<NotificationPrefs>
    fun markRead(id: String)
    fun markAllRead()
    fun delete(id: String)

    /** Re-inserts [notification] at [index] (clamped) — Snackbar "Batalkan". */
    fun restore(notification: AppNotification, index: Int)
    fun updatePrefs(prefs: NotificationPrefs)

    /** Reloads the inbox from the source (a new order or pickup reminder may have arrived). */
    suspend fun refresh() {}

    /** True while the server still has older entries than the ones loaded. */
    val hasMore: StateFlow<Boolean> get() = MutableStateFlow(false)

    /** Appends the next older page. */
    suspend fun loadMore() {}
}
