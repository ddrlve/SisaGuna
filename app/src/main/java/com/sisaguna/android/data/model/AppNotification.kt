package com.sisaguna.android.data.model

import java.time.Instant

enum class NotificationType { PICKUP, ORDER, MERCHANT, PROMO, IMPACT }

data class AppNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val createdAt: Instant,
    val isRead: Boolean,
)

/** Per-type switches keep their value while [enabled] is off, so turning the master switch
 * back on restores the user's previous choices. */
data class NotificationPrefs(
    val enabled: Boolean = true,
    val pickup: Boolean = true,
    val order: Boolean = true,
    val merchant: Boolean = true,
    val promo: Boolean = false,
    val impact: Boolean = true,
)
