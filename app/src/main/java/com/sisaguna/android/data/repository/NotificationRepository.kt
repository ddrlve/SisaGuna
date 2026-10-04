package com.sisaguna.android.data.repository

import com.sisaguna.android.data.model.AppNotification
import com.sisaguna.android.data.model.NotificationPrefs
import com.sisaguna.android.data.model.NotificationType
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    /** Adds a new notification at the top (e.g. an order was placed). */
    fun push(notification: AppNotification)
}

/** 8 seeded items over the past ~9 days, 3 unread (spec addendum). [now] is injectable so
 * tests get stable day groups. */
@Singleton
class FakeNotificationRepository(now: Instant) : NotificationRepository {

    @Inject constructor() : this(Instant.now())

    private val seedNow = now

    private fun ago(minutes: Long): Instant = seedNow.minus(Duration.ofMinutes(minutes))

    private val _notifications = MutableStateFlow(
        listOf(
            AppNotification(
                "n1", NotificationType.PICKUP, "Pickup 30 menit lagi",
                "Nasi Kuning Sisa Katering di Warung Bu Sari siap diambil sampai 19.00. Jangan sampai kelewat ya!",
                ago(4), isRead = false,
            ),
            AppNotification(
                "n2", NotificationType.ORDER, "Pesanan siap diambil",
                "Roti Segar Bakery sudah menyiapkan Roti Gandum Lewat Best Before kamu. Tunjukkan kode SG-4821 saat ambil.",
                ago(52), isRead = false,
            ),
            AppNotification(
                "n3", NotificationType.MERCHANT, "Warung Bu Sari upload menu baru",
                "Sayur Sop Sisa Hari Ini, Rp 4.000 dari Rp 15.000. Penyedia yang kamu simpan baru saja menambah makanan.",
                ago(3 * 60), isRead = false,
            ),
            AppNotification(
                "n4", NotificationType.PROMO, "Hemat ekstra pickup sore ini",
                "Ambil makanan antara 16.00-18.00 dan dapat potongan tambahan dari penyedia Verified.",
                ago(26 * 60), isRead = true,
            ),
            AppNotification(
                "n5", NotificationType.ORDER, "Pesanan selesai",
                "Terima kasih sudah menyelamatkan Donat Glaze Sisa Etalase. Beri rating untuk Roti Segar Bakery?",
                ago(30 * 60), isRead = true,
            ),
            AppNotification(
                "n6", NotificationType.IMPACT, "Dampakmu minggu ini",
                "Kamu menyelamatkan 6 porsi makanan dan mencegah 5 kg emisi karbon. Mantap!",
                ago(3 * 24 * 60), isRead = true,
            ),
            AppNotification(
                "n7", NotificationType.MERCHANT, "Peternakan Hijau menerima sisa sayur",
                "Punya sisa sayur? Peternakan Hijau di Depok menerimanya untuk pakan ternak.",
                ago(4 * 24 * 60), isRead = true,
            ),
            AppNotification(
                "n8", NotificationType.PROMO, "Selamat datang di SisaGuna",
                "Selamatkan makanan pertamamu hari ini dan lihat dampaknya di Profil.",
                ago(9 * 24 * 60), isRead = true,
            ),
        ),
    )
    override val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _prefs = MutableStateFlow(NotificationPrefs())
    override val prefs: StateFlow<NotificationPrefs> = _prefs.asStateFlow()

    override fun markRead(id: String) {
        _notifications.value = _notifications.value.map { if (it.id == id) it.copy(isRead = true) else it }
    }

    override fun markAllRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
    }

    override fun delete(id: String) {
        _notifications.value = _notifications.value.filterNot { it.id == id }
    }

    override fun restore(notification: AppNotification, index: Int) {
        val list = _notifications.value.filterNot { it.id == notification.id }.toMutableList()
        list.add(index.coerceIn(0, list.size), notification)
        _notifications.value = list
    }

    override fun updatePrefs(prefs: NotificationPrefs) {
        _prefs.value = prefs
    }

    override fun push(notification: AppNotification) {
        _notifications.value = listOf(notification) + _notifications.value.filterNot { it.id == notification.id }
    }
}
