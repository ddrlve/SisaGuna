package com.sisaguna.android.core.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sisaguna.android.data.remote.SessionEvents
import com.sisaguna.android.data.repository.AuthRepository
import com.sisaguna.android.data.settings.AppSettingsRepository
import com.sisaguna.android.data.settings.UserMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Guest is the default so the app is browsable without an account; a guest tap on a gated tab
 * shows [GuestGateSheet] instead of navigating. The session itself lives in [AuthRepository]
 * (tokens survive process death), this ViewModel only maps it for the UI. */
enum class AuthStatus { GUEST, AUTHENTICATED }

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val settings: AppSettingsRepository,
    events: SessionEvents,
) : ViewModel() {

    val status: StateFlow<AuthStatus> = auth.isAuthenticated
        .map { if (it) AuthStatus.AUTHENTICATED else AuthStatus.GUEST }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            if (auth.isAuthenticated.value) AuthStatus.AUTHENTICATED else AuthStatus.GUEST,
        )

    /** The server rejected the refresh token while the user was signed in. */
    val expired: SharedFlow<Unit> = events.expired

    fun logout() {
        // The partner mode belongs to the account that just left; the next one starts as a buyer.
        settings.setMode(UserMode.BUYER)
        viewModelScope.launch { auth.logout() }
    }
}
