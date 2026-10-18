package com.sisaguna.android.feature.auth

import com.sisaguna.android.data.remote.ApiException
import com.sisaguna.android.testutil.FakeAuthRepository
import com.sisaguna.android.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val vm = AuthViewModel(auth)

    @Test
    fun `login needs both fields and never reaches the server when empty`() {
        vm.login("", "")
        assertEquals("Email dan kata sandi wajib diisi.", vm.state.value.error)
        assertNull(auth.lastLogin)
    }

    @Test
    fun `login rejects a malformed email locally`() {
        vm.login("bukan-email", "password1")
        assertEquals("Format email tidak valid.", vm.state.value.error)
        assertNull(auth.lastLogin)
    }

    @Test
    fun `login success completes`() {
        vm.login("budi@mail.id", "password1")
        assertTrue(vm.state.value.done)
        assertNull(vm.state.value.error)
        assertEquals("budi@mail.id" to "password1", auth.lastLogin)
    }

    @Test
    fun `server refusal is shown and does not complete`() {
        auth.failWith = ApiException(401, "invalid_credentials", "email atau password salah")
        vm.login("budi@mail.id", "salahsalah")
        assertFalse(vm.state.value.done)
        assertFalse(vm.state.value.loading)
        assertEquals("Email atau password salah", vm.state.value.error)
    }

    @Test
    fun `register enforces the 8 character password the server requires`() {
        vm.register(AccountType.REGULAR, "Budi", "budi@mail.id", "0812345678", "short")
        assertEquals("Kata sandi minimal 8 karakter.", vm.state.value.error)
        assertNull(auth.lastRegister)
    }

    @Test
    fun `register sends the phone without 0 or 62 and the account type`() {
        vm.register(AccountType.MERCHANT, " Budi ", "budi@mail.id", "+62 812-3456-7890", "password1")
        assertTrue(vm.state.value.done)
        assertEquals(listOf<Any>(" Budi ", "budi@mail.id", "81234567890", "password1", true), auth.lastRegister)
    }

    @Test
    fun `a taken email comes back as a readable message`() {
        auth.failWith = ApiException(409, "email_taken", "email sudah terdaftar")
        vm.register(AccountType.REGULAR, "Budi", "budi@mail.id", "0812345678", "password1")
        assertEquals("Email sudah terdaftar", vm.state.value.error)
        assertFalse(vm.state.value.done)
    }
}
