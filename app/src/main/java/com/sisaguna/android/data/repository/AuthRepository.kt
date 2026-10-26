package com.sisaguna.android.data.repository

import com.sisaguna.android.core.ui.AppMessenger
import com.sisaguna.android.data.push.PushTokenStore
import com.sisaguna.android.data.remote.ApiService
import com.sisaguna.android.data.remote.AppException
import com.sisaguna.android.data.remote.SessionEvents
import com.sisaguna.android.data.remote.TokenStore
import com.sisaguna.android.data.remote.Tokens
import com.sisaguna.android.data.remote.apiCall
import com.sisaguna.android.data.remote.apiData
import com.sisaguna.android.data.remote.dto.ForgotPasswordBody
import com.sisaguna.android.data.remote.dto.LoginRequest
import com.sisaguna.android.data.remote.dto.RefreshRequest
import com.sisaguna.android.data.remote.dto.RegisterRequest
import com.sisaguna.android.data.remote.dto.ResetPasswordBody
import com.sisaguna.android.data.remote.dto.SessionDto
import com.sisaguna.android.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Who is signed in. Data repositories watch [isAuthenticated] to load or drop per-user data. */
interface AuthRepository {
    val isAuthenticated: StateFlow<Boolean>

    /** Throws [AppException] (e.g. `invalid_credentials`). */
    suspend fun login(email: String, password: String)

    /** [phone] is digits only, without 0 or 62. */
    suspend fun register(name: String, email: String, phone: String, password: String, merchant: Boolean)

    /** Emails a 6-digit code. The server answers the same whether or not the email is registered. */
    suspend fun requestPasswordReset(email: String)

    /** Sets a new password with the emailed [code] and signs in with the session the server returns. */
    suspend fun resetPassword(email: String, code: String, newPassword: String)

    suspend fun logout()
}

@Singleton
class ApiAuthRepository @Inject constructor(
    private val api: ApiService,
    private val tokens: TokenStore,
    events: SessionEvents,
    private val messenger: AppMessenger,
    private val pushTokens: PushTokenStore,
    @ApplicationScope private val scope: CoroutineScope,
) : AuthRepository {

    private val _isAuthenticated = MutableStateFlow(tokens.get() != null)
    override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    init {
        // The refresh token was rejected somewhere deep in the network layer: the session is gone.
        scope.launch {
            events.expired.collect {
                _isAuthenticated.value = false
                messenger.show("Sesi berakhir. Silakan masuk lagi.")
            }
        }
    }

    override suspend fun login(email: String, password: String) =
        accept(apiData { api.login(LoginRequest(email.trim(), password)) })

    override suspend fun register(name: String, email: String, phone: String, password: String, merchant: Boolean) =
        accept(
            apiData {
                api.register(
                    RegisterRequest(name.trim(), email.trim(), phone, password, if (merchant) "merchant" else "buyer"),
                )
            },
        )

    override suspend fun requestPasswordReset(email: String) {
        apiCall { api.forgotPassword(ForgotPasswordBody(email.trim())) }
    }

    override suspend fun resetPassword(email: String, code: String, newPassword: String) =
        accept(apiData { api.resetPassword(ResetPasswordBody(email.trim(), code.trim(), newPassword)) })

    override suspend fun logout() {
        val current = tokens.get()
        // Drop the local session synchronously so a quick re-login can't be wiped by a late clear().
        tokens.clear()
        _isAuthenticated.value = false
        if (current == null) return
        scope.launch {
            try {
                // The access token lives 15 minutes, and /auth/logout needs a valid one: trade the
                // refresh token for a fresh pair first, then revoke that pair.
                val fresh = apiData { api.refresh(RefreshRequest(current.refresh)) }
                val bearer = "Bearer ${fresh.accessToken}"
                // Stop pushes for this account on this device before the session is revoked.
                pushTokens.token?.let { device ->
                    apiCall { api.unregisterDevice(bearer, device) }
                    pushTokens.token = null
                }
                apiCall { api.logout(bearer, RefreshRequest(fresh.refreshToken)) }
            } catch (_: AppException) {
                // Offline or already revoked: the refresh token dies on its own after 30 days.
            }
        }
    }

    private fun accept(session: SessionDto) {
        tokens.save(Tokens(session.accessToken, session.refreshToken))
        _isAuthenticated.value = true
    }
}
