package com.sisaguna.android.data.push

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** The FCM token last registered with the server, kept so logout can unregister exactly this device. Not a secret. */
@Singleton
class PushTokenStore(
    /** null keeps the token in memory only (unit tests). */
    private val prefs: SharedPreferences?,
) {
    @Inject constructor(@ApplicationContext context: Context) :
        this(context.getSharedPreferences("sg_push", Context.MODE_PRIVATE))

    private var memory: String? = null

    var token: String?
        get() = prefs?.getString(KEY, null) ?: memory
        set(value) {
            memory = value
            prefs?.edit()?.putString(KEY, value)?.apply()
        }

    private companion object {
        const val KEY = "fcm_token"
    }
}
