package com.sisaguna.android.feature.profile

import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.PaymentKind
import com.sisaguna.android.data.repository.FakePaymentMethodRepository
import com.sisaguna.android.data.repository.FakeProfileRepository
import com.sisaguna.android.data.repository.ListingRepository
import com.sisaguna.android.testutil.MainDispatcherRule
import com.sisaguna.android.testutil.TestListingRepository
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PasswordFormTest {
    @Test
    fun `rules for new password`() {
        assertEquals("Minimal 8 karakter", PasswordForm(new = "abc1").error(PasswordField.NEW))
        assertEquals("Gabungkan huruf dan angka", PasswordForm(new = "abcdefgh").error(PasswordField.NEW))
        assertEquals("Harus beda dari password lama", PasswordForm(current = "abcdefg1", new = "abcdefg1").error(PasswordField.NEW))
        assertEquals("Konfirmasi tidak sama", PasswordForm(new = "abcdefg1", confirm = "abcdefg2").error(PasswordField.CONFIRM))
        assertTrue(PasswordForm(current = "x", new = "abcdefg1", confirm = "abcdefg1").isValid)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChangePasswordViewModelTest {
    @get:Rule
    val main = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `wrong current password shows server error`() = runTest(main.dispatcher) {
        val vm = ChangePasswordViewModel(FakeProfileRepository(TestListingRepository()))
        vm.update { copy(current = "salah123", new = "baru12345", confirm = "baru12345") }
        vm.submit()
        advanceUntilIdle()
        assertEquals("Password saat ini salah", vm.form.value.visibleError(PasswordField.CURRENT))
        assertFalse(vm.done.value)
    }

    @Test
    fun `correct current password completes`() = runTest(main.dispatcher) {
        val vm = ChangePasswordViewModel(FakeProfileRepository(TestListingRepository()))
        vm.update { copy(current = "sisaguna123", new = "baru12345", confirm = "baru12345") }
        vm.submit()
        advanceUntilIdle()
        assertTrue(vm.done.value)
    }
}

class PaymentAndCatalogViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun `link rejects bad numbers and accepts 08 numbers`() {
        val vm = PaymentMethodsViewModel(FakePaymentMethodRepository())
        assertFalse(vm.link(PaymentKind.OVO, "12345"))
        assertTrue(vm.link(PaymentKind.OVO, "0812 9876 5432"))
        assertTrue(vm.uiState.value.methods.any { it.kind == PaymentKind.OVO })
        assertEquals(listOf(PaymentKind.DANA), vm.uiState.value.linkable)
    }

    @Test
    fun `catalog shows only my store with status`() {
        val repo = TestListingRepository()
        val later = Instant.now().plus(2, ChronoUnit.HOURS)
        repo.addListing(Listing("mine1", ListingRepository.MY_MERCHANT_ID, "A", ListingTier.HUMAN, null, null, true, later, "", 0.1))
        repo.addListing(Listing("mine2", ListingRepository.MY_MERCHANT_ID, "B", ListingTier.HUMAN, null, null, true, later, "", 0.1, stock = 0))
        val vm = MyCatalogViewModel(repo)
        assertEquals(listOf(CatalogStatus.ACTIVE, CatalogStatus.SOLD_OUT), vm.uiState.value.items.map { it.status })
        vm.delete("mine1")
        assertEquals(listOf("mine2"), vm.uiState.value.items.map { it.listing.id })
    }
}
