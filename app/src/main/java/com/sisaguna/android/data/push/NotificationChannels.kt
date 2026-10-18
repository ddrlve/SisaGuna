package com.sisaguna.android.data.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/** The one channel used by pushes (declared as the FCM default in the manifest). Safe to call on every launch. */
object NotificationChannels {
    const val DEFAULT_ID = "sisaguna_default"

    fun ensure(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(DEFAULT_ID, "Pesanan dan pengingat", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Status pesanan, pengingat pengambilan, dan kabar dari toko"
            },
        )
    }
}
