package com.sisaguna.android.data.push

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** Where the device's push token comes from; a seam so the registration logic can be tested without Firebase. */
interface PushTokenSource {
    /** Null when push isn't available (no Firebase config in this build, or the token couldn't be fetched). */
    suspend fun current(): String?
}

@Singleton
class FirebaseTokenSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : PushTokenSource {
    override suspend fun current(): String? {
        // No google-services.json in the build means no default FirebaseApp: skip instead of crashing.
        if (FirebaseApp.getApps(context).isEmpty()) return null
        return suspendCancellableCoroutine { cont ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (cont.isActive) cont.resume(task.result.takeIf { task.isSuccessful })
            }
        }
    }
}
