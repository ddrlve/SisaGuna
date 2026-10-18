package com.sisaguna.android.testutil

import com.sisaguna.android.data.remote.AppException
import com.sisaguna.android.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory [AuthRepository]: accepts any credentials unless [failWith] is set. */
class FakeAuthRepository(signedIn: Boolean = false) : AuthRepository {
    private val state = MutableStateFlow(signedIn)
    override val isAuthenticated: StateFlow<Boolean> = state

    var failWith: AppException? = null
    var lastLogin: Pair<String, String>? = null
    var lastRegister: List<Any>? = null

    override suspend fun login(email: String, password: String) {
        failWith?.let { throw it }
        lastLogin = email to password
        state.value = true
    }

    override suspend fun register(name: String, email: String, phone: String, password: String, merchant: Boolean) {
        failWith?.let { throw it }
        lastRegister = listOf(name, email, phone, password, merchant)
        state.value = true
    }

    var lastResetRequest: String? = null
    var lastReset: Triple<String, String, String>? = null

    override suspend fun requestPasswordReset(email: String) {
        failWith?.let { throw it }
        lastResetRequest = email
    }

    override suspend fun resetPassword(email: String, code: String, newPassword: String) {
        failWith?.let { throw it }
        lastReset = Triple(email, code, newPassword)
        state.value = true
    }

    override suspend fun logout() {
        state.value = false
    }
}
