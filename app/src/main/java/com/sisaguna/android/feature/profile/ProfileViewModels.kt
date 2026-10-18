package com.sisaguna.android.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sisaguna.android.core.ui.AppMessenger
import com.sisaguna.android.data.model.ImpactStats
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.data.model.UserProfile
import com.sisaguna.android.data.model.OrderStatus
import com.sisaguna.android.data.repository.AddressRepository
import com.sisaguna.android.data.remote.AppException
import com.sisaguna.android.data.repository.ListingRepository
import com.sisaguna.android.data.repository.OrderRepository
import com.sisaguna.android.data.repository.PaymentMethodRepository
import kotlinx.coroutines.flow.map
import com.sisaguna.android.data.repository.NotificationRepository
import com.sisaguna.android.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: UserProfile,
    val impact: ImpactStats? = null,
    val catalog: List<Pair<Listing, Merchant>> = emptyList(),
    val notificationsEnabled: Boolean = true,
    val addressCount: Int = 0,
    val paymentCount: Int = 0,
    val rescueCount: Int = 0,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val listingRepository: ListingRepository,
    notificationRepository: NotificationRepository,
    addressRepository: AddressRepository,
    paymentRepository: PaymentMethodRepository,
    orderRepository: OrderRepository,
    private val messenger: AppMessenger,
) : ViewModel() {

    private val impact = MutableStateFlow<ImpactStats?>(null)

    fun setPhoto(uri: String?) = launchUpdate { profileRepository.setPhoto(uri) }

    fun setAvatar(key: String) = launchUpdate { profileRepository.setAvatar(key) }

    private fun launchUpdate(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: AppException) {
                messenger.show(e)
            }
        }
    }

    /** Live: an upload or delete in Katalog saya shows up here without reopening Profile. */
    private val catalog = combine(listingRepository.listings, listingRepository.myMerchant) { all, me ->
        if (me == null) emptyList() else all.filter { it.merchantId == me.id && it.stock > 0 }.map { it to me }
    }

    private val counts = combine(addressRepository.addresses, paymentRepository.methods, orderRepository.orders) { a, p, o ->
        Triple(a.size, p.size, o.count { it.status == OrderStatus.COMPLETED })
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        profileRepository.profile,
        impact,
        catalog,
        notificationRepository.prefs,
        counts,
    ) { profile, impactStats, items, prefs, (addresses, payments, rescues) ->
        ProfileUiState(profile, impactStats, items, prefs.enabled, addresses, payments, rescues)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ProfileUiState(profileRepository.profile.value))

    init {
        viewModelScope.launch {
            try {
                impact.value = profileRepository.getImpact()
            } catch (e: AppException) {
                messenger.show(e)
            }
        }
    }
}

/** Spec §5 Edit Profile rules. Pure so they're testable without a ViewModel. */
object ProfileValidation {
    private val emailShape = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun name(value: String): String? = when {
        value.isBlank() -> "Nama wajib diisi"
        value.trim().length > 50 -> "Nama maksimal 50 karakter"
        else -> null
    }

    fun email(value: String): String? = if (emailShape.matches(value.trim())) null else "Format email tidak valid"

    /** Digits only, without a typed "+62"/"62" country code or a leading 0. Indonesian mobile
     * numbers start with 8 after the country code, so a leading "62" is never part of the
     * subscriber number. */
    fun normalizePhone(value: String): String =
        value.filter(Char::isDigit).removePrefix("62").removePrefix("0")

    fun phone(value: String): String? = if (normalizePhone(value).length in 9..13) null else "Nomor harus 9-13 digit"
}

enum class ProfileField { NAME, EMAIL, PHONE }

data class EditProfileForm(
    val name: String,
    val email: String,
    val phone: String,
    val touched: Set<ProfileField> = emptySet(),
    val submitted: Boolean = false,
) {
    val nameError: String? get() = ProfileValidation.name(name)
    val emailError: String? get() = ProfileValidation.email(email)
    val phoneError: String? get() = ProfileValidation.phone(phone)
    val isValid: Boolean get() = nameError == null && emailError == null && phoneError == null

    /** Errors only show after the field lost focus once, or after a save attempt. */
    fun visibleError(field: ProfileField): String? {
        if (!submitted && field !in touched) return null
        return when (field) {
            ProfileField.NAME -> nameError
            ProfileField.EMAIL -> emailError
            ProfileField.PHONE -> phoneError
        }
    }
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val messenger: AppMessenger,
) : ViewModel() {

    private val original = profileRepository.profile.value

    /** For the avatar preview: photo or fruit, matching Profile. */
    val profile = profileRepository.profile

    private val _form = MutableStateFlow(EditProfileForm(original.name, original.email, original.phone))
    val form: StateFlow<EditProfileForm> = _form.asStateFlow()

    val isDirty: Boolean
        get() = _form.value.let {
            it.name.trim() != original.name ||
                it.email.trim() != original.email ||
                ProfileValidation.normalizePhone(it.phone) != original.phone
        }

    val canSave: Boolean get() = isDirty && _form.value.isValid

    fun onNameChange(value: String) = _form.update { copy(name = value) }
    fun onEmailChange(value: String) = _form.update { copy(email = value) }
    fun onPhoneChange(value: String) = _form.update { copy(phone = value.filter { it.isDigit() || it == '+' }.take(16)) }
    fun onBlur(field: ProfileField) = _form.update { copy(touched = touched + field) }

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    /** Reveals every error when invalid; otherwise saves on the server and calls [onSaved] once it accepted. */
    fun save(onSaved: () -> Unit) {
        val current = _form.value
        if (!current.isValid) {
            _form.value = current.copy(submitted = true)
            return
        }
        if (!isDirty || _saving.value) return
        _saving.value = true
        viewModelScope.launch {
            try {
                profileRepository.update(current.name, current.email, ProfileValidation.normalizePhone(current.phone))
                onSaved()
            } catch (e: AppException) {
                messenger.show(e) // e.g. "Email sudah dipakai": stay on the form so it can be fixed
            } finally {
                _saving.value = false
            }
        }
    }

    private inline fun MutableStateFlow<EditProfileForm>.update(block: EditProfileForm.() -> EditProfileForm) {
        value = value.block()
    }
}
