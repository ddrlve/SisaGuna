package com.sisaguna.android.core.session

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionViewModelTest {

    @Test
    fun `initial status is guest`() {
        val viewModel = SessionViewModel()
        assertEquals(AuthStatus.GUEST, viewModel.status.value)
    }

    @Test
    fun `login sets status to authenticated`() {
        val viewModel = SessionViewModel()
        viewModel.login()
        assertEquals(AuthStatus.AUTHENTICATED, viewModel.status.value)
    }

    @Test
    fun `login is idempotent`() {
        val viewModel = SessionViewModel()
        viewModel.login()
        viewModel.login()
        assertEquals(AuthStatus.AUTHENTICATED, viewModel.status.value)
    }

    @Test
    fun `logout after login returns to guest`() {
        val viewModel = SessionViewModel()
        viewModel.login()
        viewModel.logout()
        assertEquals(AuthStatus.GUEST, viewModel.status.value)
    }

    @Test
    fun `logout is idempotent when already guest`() {
        val viewModel = SessionViewModel()
        viewModel.logout()
        viewModel.logout()
        assertEquals(AuthStatus.GUEST, viewModel.status.value)
    }

    @Test
    fun `continueAsGuest keeps status guest explicitly`() {
        val viewModel = SessionViewModel()
        viewModel.continueAsGuest()
        assertEquals(AuthStatus.GUEST, viewModel.status.value)
    }
}
