package com.sisaguna.android.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sisaguna.android.data.remote.AppException
import com.sisaguna.android.data.repository.AuthRepository
import com.sisaguna.android.feature.profile.ProfileValidation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    /** Set once the server accepted the credentials; the screen navigates away. */
    val done: Boolean = false,
)

/** Shared by Login and Register: validates locally, then asks the backend. */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun login(email: String, password: String) {
        val problem = when {
            email.isBlank() || password.isEmpty() -> "Email dan kata sandi wajib diisi."
            ProfileValidation.email(email) != null -> "Format email tidak valid."
            else -> null
        }
        submit(problem) { auth.login(email, password) }
    }

    fun register(type: AccountType, name: String, email: String, phone: String, password: String) {
        val problem = ProfileValidation.name(name)
            ?: ProfileValidation.email(email)
            ?: ProfileValidation.phone(phone)
            ?: if (password.length < MIN_PASSWORD) "Kata sandi minimal $MIN_PASSWORD karakter." else null
        submit(problem) {
            auth.register(name, email, ProfileValidation.normalizePhone(phone), password, merchant = type == AccountType.MERCHANT)
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    private fun submit(validationError: String?, call: suspend () -> Unit) {
        if (_state.value.loading) return
        if (validationError != null) {
            _state.value = AuthUiState(error = validationError)
            return
        }
        _state.value = AuthUiState(loading = true)
        viewModelScope.launch {
            _state.value = try {
                call()
                AuthUiState(done = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: AppException) {
                AuthUiState(error = e.userMessage)
            }
        }
    }

    private companion object {
        const val MIN_PASSWORD = 8
    }
}
