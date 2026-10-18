package com.sisaguna.android.data.push

import com.google.firebase.messaging.FirebaseMessagingService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Receives FCM callbacks. Notifications sent while the app is closed are drawn by the system from the
 * payload (title/body + `notification_id`), and tapping one opens the app; nothing to render here.
 */
@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @Inject lateinit var registrar: PushRegistrar

    override fun onNewToken(token: String) {
        registrar.onNewToken(token)
    }
}
