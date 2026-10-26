package com.sisaguna.android.data.push

import com.sisaguna.android.data.remote.ApiService
import com.sisaguna.android.data.remote.AppException
import com.sisaguna.android.data.remote.apiCall
import com.sisaguna.android.data.remote.dto.DeviceBody
import com.sisaguna.android.data.repository.AuthRepository
import com.sisaguna.android.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Keeps the server's list of push devices current: registers the token after sign-in and whenever
 * Firebase rotates it. The server only pushes to users with no live WebSocket, so a foreground app
 * never gets a duplicate. Unregistering on logout is done by the auth repository.
 */
@Singleton
class PushRegistrar @Inject constructor(
    private val api: ApiService,
    private val auth: AuthRepository,
    private val source: PushTokenSource,
    private val store: PushTokenStore,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch { auth.isAuthenticated.collect { signedIn -> if (signedIn) registerCurrent() } }
    }

    internal suspend fun registerCurrent() {
        source.current()?.let { send(it) }
    }

    /** Firebase issued a new token (first run, reinstall, or rotation). */
    fun onNewToken(token: String) {
        if (!auth.isAuthenticated.value) {
            store.token = token // registered at the next sign-in
            return
        }
        scope.launch { send(token) }
    }

    private suspend fun send(token: String) {
        try {
            apiCall { api.registerDevice(DeviceBody(token, "android")) }
            store.token = token
        } catch (e: AppException) {
            // Offline or refused: tried again at the next sign-in or token change.
        }
    }
}
