package com.sisaguna.android.feature.auth

import com.sisaguna.android.data.remote.ApiException
import com.sisaguna.android.testutil.FakeAuthRepository
import com.sisaguna.android.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val vm = ForgotPasswordViewModel(auth)

    @Test
    fun `a malformed email never reaches the server`() {
        vm.requestCode("bukan-email")
        assertEquals("Format email tidak valid.", vm.state.value.error)
        assertNull(auth.lastResetRequest)
        assertEquals(ResetStep.EMAIL, vm.state.value.step)
    }

    @Test
    fun `requesting a code moves to the code step and starts the 60 second cooldown`() {
        vm.requestCode(" budi@mail.id ")
        val s = vm.state.value
        assertEquals("budi@mail.id", auth.lastResetRequest)
        assertEquals(ResetStep.CODE, s.step)
        assertEquals("budi@mail.id", s.email)
        assertEquals(60, s.cooldown)

        vm.resend() // too early: nothing is sent
        assertEquals(60, vm.state.value.cooldown)

        main.dispatcher.scheduler.advanceTimeBy(61_000)
        assertEquals(0, vm.state.value.cooldown)
    }

    @Test
    fun `the code must be six digits and the passwords must match`() {
        vm.requestCode("budi@mail.id")
        vm.reset("12345", "password1", "password1")
        assertEquals("Kode harus 6 digit.", vm.state.value.error)
        vm.reset("123456", "short", "short")
        assertEquals("Kata sandi baru minimal 8 karakter.", vm.state.value.error)
        vm.reset("123456", "password1", "password2")
        assertEquals("Konfirmasi kata sandi tidak cocok.", vm.state.value.error)
        assertNull(auth.lastReset)
    }

    @Test
    fun `a valid reset signs the user in`() {
        vm.requestCode("budi@mail.id")
        vm.reset("123456", "password1", "password1")
        assertTrue(vm.state.value.done)
        assertEquals(Triple("budi@mail.id", "123456", "password1"), auth.lastReset)
    }

    @Test
    fun `a wrong or expired code is shown and keeps the form`() {
        vm.requestCode("budi@mail.id")
        auth.failWith = ApiException(400, "invalid_reset_code", "kode salah atau sudah kedaluwarsa")
        vm.reset("123456", "password1", "password1")
        assertFalse(vm.state.value.done)
        assertFalse(vm.state.value.loading)
        assertEquals("Kode salah atau sudah kedaluwarsa", vm.state.value.error)
        assertEquals(ResetStep.CODE, vm.state.value.step)
    }

    @Test
    fun `back returns to the email step`() {
        vm.requestCode("budi@mail.id")
        vm.back()
        assertEquals(ResetStep.EMAIL, vm.state.value.step)
        assertEquals(0, vm.state.value.cooldown)
    }
}
