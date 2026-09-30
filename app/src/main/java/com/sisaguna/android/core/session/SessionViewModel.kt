package com.sisaguna.android.core.session

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** Frontend-only in-memory auth state — no backend, no persistence. Guest is the default so the
 * app is fully browsable without an account; [login] is called from Login/Register success, and
 * a guest tap on a gated tab shows [com.sisaguna.android.core.session.GuestGateSheet] instead of
 * navigating. Reset on process death, same as every other piece of UI state in this repo. */
enum class AuthStatus { GUEST, AUTHENTICATED }

@HiltViewModel
class SessionViewModel @Inject constructor() : ViewModel() {

    private val _status = MutableStateFlow(AuthStatus.GUEST)
    val status: StateFlow<AuthStatus> = _status.asStateFlow()

    fun continueAsGuest() {
        _status.value = AuthStatus.GUEST
    }

    fun login() {
        _status.value = AuthStatus.AUTHENTICATED
    }

    fun logout() {
        _status.value = AuthStatus.GUEST
    }
}
