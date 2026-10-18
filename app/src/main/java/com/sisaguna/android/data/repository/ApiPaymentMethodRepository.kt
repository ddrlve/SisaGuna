package com.sisaguna.android.data.repository

import com.sisaguna.android.core.ui.AppMessenger
import com.sisaguna.android.data.model.PaymentKind
import com.sisaguna.android.data.model.PaymentMethod
import com.sisaguna.android.data.remote.ApiService
import com.sisaguna.android.data.remote.AppException
import com.sisaguna.android.data.remote.apiData
import com.sisaguna.android.data.remote.dto.DefaultPaymentBody
import com.sisaguna.android.data.remote.dto.LinkPaymentBody
import com.sisaguna.android.data.remote.dto.PaymentMethodDto
import com.sisaguna.android.data.remote.toEnum
import com.sisaguna.android.data.remote.toMethod
import com.sisaguna.android.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Payment methods per account. Linking an e-wallet is simulated by the backend (no OTP), but the
 * list and the default live on the server. Every call answers with the full new list, which
 * replaces the optimistic copy; a refusal rolls it back.
 */
@Singleton
class ApiPaymentMethodRepository @Inject constructor(
    private val api: ApiService,
    private val auth: AuthRepository,
    private val messenger: AppMessenger,
    @ApplicationScope private val scope: CoroutineScope,
) : PaymentMethodRepository {

    private val _methods = MutableStateFlow(BASELINE)
    override val methods: StateFlow<List<PaymentMethod>> = _methods.asStateFlow()

    private val _default = MutableStateFlow(PaymentKind.QRIS)
    override val defaultKind: StateFlow<PaymentKind> = _default.asStateFlow()

    init {
        scope.launch {
            auth.isAuthenticated.collect { signedIn ->
                if (!signedIn) {
                    reset()
                    return@collect
                }
                try {
                    show(apiData { api.paymentMethods() })
                } catch (e: AppException) {
                    messenger.show(e)
                }
            }
        }
    }

    override fun setDefault(kind: PaymentKind) {
        if (_methods.value.none { it.kind == kind }) return
        val previous = _default.value
        _default.value = kind
        sync(rollback = { _default.value = previous }) {
            apiData { api.setDefaultPaymentMethod(DefaultPaymentBody(kind.name.lowercase())) }
        }
    }

    override fun link(kind: PaymentKind, phone: String) {
        val previous = _methods.value
        val digits = phone.filter(Char::isDigit)
        val masked = if (digits.length >= 8) "${digits.take(4)} •••• ${digits.takeLast(4)}" else digits
        val optimistic = PaymentMethod("p-${kind.name.lowercase()}", kind, kind.name, masked)
        val rest = previous.filterNot { it.kind == kind }
        // Keep e-wallets grouped after QRIS, before cash.
        _methods.value = rest.filter { it.kind != PaymentKind.CASH } + optimistic + rest.filter { it.kind == PaymentKind.CASH }
        sync(rollback = { _methods.value = previous }) {
            apiData { api.linkPaymentMethod(LinkPaymentBody(kind.name.lowercase(), phone)) }
        }
    }

    override fun unlink(kind: PaymentKind) {
        if (kind == PaymentKind.QRIS || kind == PaymentKind.CASH) return
        val previousMethods = _methods.value
        val previousDefault = _default.value
        _methods.value = previousMethods.filterNot { it.kind == kind }
        if (previousDefault == kind) _default.value = PaymentKind.QRIS
        sync(rollback = {
            _methods.value = previousMethods
            _default.value = previousDefault
        }) {
            apiData { api.unlinkPaymentMethod(kind.name.lowercase()) }
        }
    }

    private fun sync(rollback: () -> Unit, call: suspend () -> List<PaymentMethodDto>) {
        scope.launch {
            try {
                show(call())
            } catch (e: AppException) {
                rollback()
                messenger.show(e)
            }
        }
    }

    private fun show(list: List<PaymentMethodDto>) {
        _methods.value = list.map { it.toMethod() }.ifEmpty { BASELINE }
        _default.value = list.firstOrNull { it.isDefault }?.kind.toEnum(PaymentKind.QRIS)
    }

    private fun reset() {
        _methods.value = BASELINE
        _default.value = PaymentKind.QRIS
    }

    private companion object {
        // What every account has before the server answers (and what a guest sees at checkout).
        val BASELINE = listOf(
            PaymentMethod("p-qris", PaymentKind.QRIS, "QRIS", "Bayar pakai aplikasi bank atau e-wallet apa pun"),
            PaymentMethod("p-cash", PaymentKind.CASH, "Bayar saat ambil", "Tunai langsung ke penyedia"),
        )
    }
}
