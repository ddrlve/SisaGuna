package com.sisaguna.android.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.Order
import com.sisaguna.android.data.model.OrderStatus
import com.sisaguna.android.data.model.PaymentKind
import com.sisaguna.android.data.model.PaymentMethod
import com.sisaguna.android.data.repository.ListingRepository
import com.sisaguna.android.data.repository.OrderRepository
import com.sisaguna.android.data.repository.PaymentMethodRepository
import com.sisaguna.android.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ---------------------------------------------------------------- Riwayat Penyelamatan

data class RescueHistoryUiState(val orders: List<Order> = emptyList()) {
    val portions: Int get() = orders.sumOf { it.itemCount }
    val saved: Int get() = orders.sumOf { it.savings }

    /** Rough estimate used on the impact card: ~0.9 kg CO₂e avoided per rescued portion. */
    val carbonKg: Int get() = (portions * 0.9).toInt()
}

@HiltViewModel
class RescueHistoryViewModel @Inject constructor(orderRepository: OrderRepository) : ViewModel() {
    val uiState: StateFlow<RescueHistoryUiState> = orderRepository.orders
        .map { list -> RescueHistoryUiState(list.filter { it.status == OrderStatus.COMPLETED }) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, RescueHistoryUiState())
}

// ---------------------------------------------------------------- Metode Pembayaran

data class PaymentMethodsUiState(
    val methods: List<PaymentMethod> = emptyList(),
    val defaultKind: PaymentKind = PaymentKind.QRIS,
) {
    /** E-wallets not linked yet — what "Tambah e-wallet" offers. */
    val linkable: List<PaymentKind>
        get() = listOf(PaymentKind.GOPAY, PaymentKind.OVO, PaymentKind.DANA).filter { k -> methods.none { it.kind == k } }
}

@HiltViewModel
class PaymentMethodsViewModel @Inject constructor(
    private val repository: PaymentMethodRepository,
) : ViewModel() {
    val uiState: StateFlow<PaymentMethodsUiState> = combine(repository.methods, repository.defaultKind) { m, d ->
        PaymentMethodsUiState(m, d)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PaymentMethodsUiState(repository.methods.value, repository.defaultKind.value))

    fun setDefault(kind: PaymentKind) = repository.setDefault(kind)

    /** False when the phone number isn't a plausible Indonesian mobile number. */
    fun link(kind: PaymentKind, phone: String): Boolean {
        val digits = ProfileValidation.normalizePhone(phone)
        if (digits.length !in 9..13 || !digits.startsWith("8")) return false
        repository.link(kind, "0$digits")
        return true
    }

    fun unlink(kind: PaymentKind) = repository.unlink(kind)
}

// ---------------------------------------------------------------- Ganti Password

enum class PasswordField { CURRENT, NEW, CONFIRM }

data class PasswordForm(
    val current: String = "",
    val new: String = "",
    val confirm: String = "",
    val touched: Set<PasswordField> = emptySet(),
    val submitted: Boolean = false,
    val busy: Boolean = false,
    val serverError: String? = null,
) {
    val hasLength: Boolean get() = new.length >= 8
    val hasLetterAndDigit: Boolean get() = new.any(Char::isLetter) && new.any(Char::isDigit)
    val differsFromCurrent: Boolean get() = new.isNotEmpty() && new != current

    fun error(field: PasswordField): String? = when (field) {
        PasswordField.CURRENT -> if (current.isBlank()) "Masukkan password saat ini" else serverError
        PasswordField.NEW -> when {
            !hasLength -> "Minimal 8 karakter"
            !hasLetterAndDigit -> "Gabungkan huruf dan angka"
            !differsFromCurrent -> "Harus beda dari password lama"
            else -> null
        }
        PasswordField.CONFIRM -> if (confirm != new || confirm.isEmpty()) "Konfirmasi tidak sama" else null
    }

    fun visibleError(field: PasswordField): String? =
        if (submitted || field in touched || (field == PasswordField.CURRENT && serverError != null)) error(field) else null

    val isValid: Boolean get() = PasswordField.entries.all { f -> if (f == PasswordField.CURRENT) current.isNotBlank() else error(f) == null }
}

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    private val _form = MutableStateFlow(PasswordForm())
    val form: StateFlow<PasswordForm> = _form.asStateFlow()

    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    fun update(transform: PasswordForm.() -> PasswordForm) {
        _form.value = _form.value.transform().copy(serverError = null)
    }

    fun touch(field: PasswordField) {
        _form.value = _form.value.copy(touched = _form.value.touched + field)
    }

    fun submit() {
        val f = _form.value
        if (!f.isValid) {
            _form.value = f.copy(submitted = true)
            return
        }
        _form.value = f.copy(busy = true)
        viewModelScope.launch {
            val ok = profileRepository.changePassword(f.current, f.new)
            _form.value = _form.value.copy(busy = false, serverError = if (ok) null else "Password saat ini salah")
            if (ok) _done.value = true
        }
    }
}

// ---------------------------------------------------------------- Katalog saya

enum class CatalogStatus { ACTIVE, SOLD_OUT, EXPIRED }

data class CatalogItem(val listing: Listing, val status: CatalogStatus)

data class MyCatalogUiState(val items: List<CatalogItem> = emptyList()) {
    val activeCount: Int get() = items.count { it.status == CatalogStatus.ACTIVE }
}

@HiltViewModel
class MyCatalogViewModel @Inject constructor(
    private val listingRepository: ListingRepository,
) : ViewModel() {
    val uiState: StateFlow<MyCatalogUiState> = listingRepository.listings
        .map { all ->
            val now = Instant.now()
            MyCatalogUiState(
                all.filter { it.merchantId == ListingRepository.MY_MERCHANT_ID }.map { l ->
                    CatalogItem(
                        l,
                        when {
                            !l.pickupEnd.isAfter(now) -> CatalogStatus.EXPIRED
                            l.stock <= 0 -> CatalogStatus.SOLD_OUT
                            else -> CatalogStatus.ACTIVE
                        },
                    )
                }.sortedBy { it.status.ordinal },
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MyCatalogUiState())

    fun delete(id: String) = listingRepository.deleteListing(id)
}
