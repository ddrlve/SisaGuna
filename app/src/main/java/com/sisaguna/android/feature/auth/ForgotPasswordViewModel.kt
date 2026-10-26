package com.sisaguna.android.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sisaguna.android.data.remote.AppException
import com.sisaguna.android.data.repository.AuthRepository
import com.sisaguna.android.feature.profile.ProfileValidation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ResetStep { EMAIL, CODE }

data class ForgotPasswordState(
    val step: ResetStep = ResetStep.EMAIL,
    val email: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    /** Seconds until another code may be requested (the server allows one per 60 s). */
    val cooldown: Int = 0,
    val done: Boolean = false,
)

/**
 * Two steps: ask for an emailed 6-digit code, then set a new password with it. A successful reset
 * signs the user in (the server revokes every older session and returns a new one).
 */
@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordState())
    val state: StateFlow<ForgotPasswordState> = _state.asStateFlow()

    private var ticker: Job? = null

    fun requestCode(email: String) {
        val clean = email.trim()
        ProfileValidation.email(clean)?.let { return fail("Format email tidak valid.") }
        if (_state.value.loading) return
        launchCall {
            auth.requestPasswordReset(clean)
            _state.update { it.copy(step = ResetStep.CODE, email = clean) }
            startCooldown()
        }
    }

    fun resend() {
        val s = _state.value
        if (s.cooldown > 0 || s.loading) return
        launchCall {
            auth.requestPasswordReset(s.email)
            startCooldown()
        }
    }

    fun back() {
        ticker?.cancel()
        _state.value = ForgotPasswordState(email = _state.value.email)
    }

    fun reset(code: String, newPassword: String, confirm: String) {
        val problem = when {
            !CODE.matches(code.trim()) -> "Kode harus 6 digit."
            newPassword.length < MIN_PASSWORD -> "Kata sandi baru minimal $MIN_PASSWORD karakter."
            newPassword != confirm -> "Konfirmasi kata sandi tidak cocok."
            else -> null
        }
        if (problem != null) return fail(problem)
        if (_state.value.loading) return
        val email = _state.value.email
        launchCall {
            auth.resetPassword(email, code.trim(), newPassword)
            ticker?.cancel()
            _state.update { it.copy(done = true) }
        }
    }

    private fun fail(message: String) = _state.update { it.copy(error = message) }

    private fun launchCall(block: suspend () -> Unit) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: AppException) {
                _state.update { it.copy(error = e.userMessage) }
            } finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    private fun startCooldown() {
        ticker?.cancel()
        _state.update { it.copy(cooldown = COOLDOWN_SECONDS) }
        ticker = viewModelScope.launch {
            while (_state.value.cooldown > 0) {
                delay(1_000)
                _state.update { it.copy(cooldown = (it.cooldown - 1).coerceAtLeast(0)) }
            }
        }
    }

    private companion object {
        val CODE = Regex("""\d{6}""")
        const val MIN_PASSWORD = 8
        const val COOLDOWN_SECONDS = 60
    }
}
