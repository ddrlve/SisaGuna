package com.sisaguna.android.feature.profile

import com.sisaguna.android.data.repository.FakeAddressRepository
import com.sisaguna.android.data.repository.FakeNotificationRepository
import com.sisaguna.android.data.repository.FakeOrderRepository
import com.sisaguna.android.data.repository.FakePaymentMethodRepository
import com.sisaguna.android.data.repository.FakeProfileRepository
import com.sisaguna.android.testutil.MainDispatcherRule
import com.sisaguna.android.testutil.TestListingRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileValidationTest {

    @Test
    fun `name is required and capped at 50`() {
        assertEquals("Nama wajib diisi", ProfileValidation.name("   "))
        assertNotNull(ProfileValidation.name("a".repeat(51)))
        assertNull(ProfileValidation.name("Budi"))
    }

    @Test
    fun `email needs basic shape`() {
        assertEquals("Format email tidak valid", ProfileValidation.email("budi@"))
        assertEquals("Format email tidak valid", ProfileValidation.email("budi gmail.com"))
        assertNull(ProfileValidation.email(" budi@gmail.com "))
    }

    @Test
    fun `phone strips leading zero and country code`() {
        assertEquals("812345678901", ProfileValidation.normalizePhone("0812345678901"))
        assertEquals("81234567890", ProfileValidation.normalizePhone("+62 812-3456-7890"))
        assertEquals("81234567890", ProfileValidation.normalizePhone("6281234567890"))
    }

    @Test
    fun `phone must be 9 to 13 digits after normalizing`() {
        assertEquals("Nomor harus 9-13 digit", ProfileValidation.phone("08123"))
        assertNull(ProfileValidation.phone("0812345678901")) // 12 digits after the 0
        assertNotNull(ProfileValidation.phone("81234567890123")) // 14
    }
}

class EditProfileViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val repo = FakeProfileRepository(TestListingRepository())

    @Test
    fun `save disabled when unchanged`() {
        val vm = EditProfileViewModel(repo)
        assertFalse(vm.canSave)
    }

    @Test
    fun `save disabled when invalid even if dirty`() {
        val vm = EditProfileViewModel(repo)
        vm.onEmailChange("nope")
        assertTrue(vm.isDirty)
        assertFalse(vm.canSave)
    }

    @Test
    fun `typing 0 prefix of the same number is not dirty`() {
        val vm = EditProfileViewModel(repo)
        vm.onPhoneChange("0" + repo.profile.value.phone)
        assertFalse(vm.isDirty)
    }

    @Test
    fun `errors hidden until blur or submit`() {
        val vm = EditProfileViewModel(repo)
        vm.onNameChange("")
        assertNull(vm.form.value.visibleError(ProfileField.NAME))
        vm.onBlur(ProfileField.NAME)
        assertEquals("Nama wajib diisi", vm.form.value.visibleError(ProfileField.NAME))
    }

    @Test
    fun `failed save reveals all errors`() {
        val vm = EditProfileViewModel(repo)
        vm.onEmailChange("x")
        assertFalse(vm.save())
        assertNotNull(vm.form.value.visibleError(ProfileField.EMAIL))
    }

    @Test
    fun `save updates repository and profile screen sees it`() {
        val listings = TestListingRepository()
        val profileVm = ProfileViewModel(
            repo, listings, FakeNotificationRepository(), FakeAddressRepository(), FakePaymentMethodRepository(),
            FakeOrderRepository(listings, FakeNotificationRepository(), { java.time.Instant.now() }, 0),
        )
        val vm = EditProfileViewModel(repo)
        vm.onNameChange("  Sari Dewi ")
        vm.onPhoneChange("0811111111")
        assertTrue(vm.save())
        assertEquals("Sari Dewi", profileVm.uiState.value.profile.name)
        assertEquals("811111111", profileVm.uiState.value.profile.phone)
        assertEquals('S', profileVm.uiState.value.profile.initial)
    }
}
