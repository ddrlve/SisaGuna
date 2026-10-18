package com.sisaguna.android.core.session

import com.sisaguna.android.data.remote.SessionEvents
import com.sisaguna.android.data.settings.AppSettingsRepository
import com.sisaguna.android.data.settings.UserMode
import com.sisaguna.android.testutil.FakeAuthRepository
import com.sisaguna.android.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SessionViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val settings = AppSettingsRepository(null)
    private fun viewModel() = SessionViewModel(auth, settings, SessionEvents())

    @Test
    fun `initial status is guest`() {
        assertEquals(AuthStatus.GUEST, viewModel().status.value)
    }

    @Test
    fun `a stored session starts authenticated`() {
        val viewModel = SessionViewModel(FakeAuthRepository(signedIn = true), settings, SessionEvents())
        assertEquals(AuthStatus.AUTHENTICATED, viewModel.status.value)
    }

    @Test
    fun `status follows the auth repository`() = runTest {
        val viewModel = viewModel()
        auth.login("a@b.id", "password1")
        assertEquals(AuthStatus.AUTHENTICATED, viewModel.status.value)
        viewModel.logout()
        assertEquals(AuthStatus.GUEST, viewModel.status.value)
    }

    @Test
    fun `logout hands the next account the buyer mode`() {
        settings.setMode(UserMode.MERCHANT)
        viewModel().logout()
        assertEquals(UserMode.BUYER, settings.settings.value.mode)
    }

    @Test
    fun `logout is idempotent when already guest`() {
        val viewModel = viewModel()
        viewModel.logout()
        viewModel.logout()
        assertEquals(AuthStatus.GUEST, viewModel.status.value)
    }
}
