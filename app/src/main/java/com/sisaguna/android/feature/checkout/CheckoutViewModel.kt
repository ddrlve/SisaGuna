package com.sisaguna.android.feature.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.data.model.PaymentKind
import com.sisaguna.android.data.model.PaymentMethod
import com.sisaguna.android.data.repository.CartRepository
import com.sisaguna.android.data.repository.ListingRepository
import com.sisaguna.android.data.repository.OrderRepository
import com.sisaguna.android.data.repository.PaymentMethodRepository
import com.sisaguna.android.data.repository.PlaceOrderRequest
import com.sisaguna.android.data.repository.Voucher
import com.sisaguna.android.data.repository.VoucherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CheckoutLine(val listing: Listing, val quantity: Int) {
    val total: Int get() = listing.unitPrice * quantity
    val originalTotal: Int get() = listing.unitOriginalPrice * quantity
}

/** Where the payment step is. QRIS waits for the user to confirm they scanned; e-wallets and
 * cash go straight to [Processing]. */
sealed interface PaymentStep {
    data object Idle : PaymentStep
    data object AwaitingQris : PaymentStep
    data class Processing(val kind: PaymentKind) : PaymentStep
    data class Done(val orderId: String) : PaymentStep
    data class Failed(val message: String) : PaymentStep
}

data class CheckoutUiState(
    val merchant: Merchant? = null,
    val lines: List<CheckoutLine> = emptyList(),
    val methods: List<PaymentMethod> = emptyList(),
    val selected: PaymentKind = PaymentKind.QRIS,
    val note: String = "",
    val step: PaymentStep = PaymentStep.Idle,
    val vouchers: List<Voucher> = emptyList(),
    val voucher: Voucher? = null,
) {
    val isEmpty: Boolean get() = lines.isEmpty()
    val subtotal: Int get() = lines.sumOf { it.total }
    val voucherDiscount: Int get() = voucher?.discountFor(subtotal) ?: 0
    val total: Int get() = (subtotal - voucherDiscount).coerceAtLeast(0)

    /** Retail price minus what's paid: surplus discount plus voucher. */
    val savings: Int get() = (lines.sumOf { it.originalTotal } - total).coerceAtLeast(0)
    val itemCount: Int get() = lines.sumOf { it.quantity }

    /** A free order has nothing to pay online; it's confirmed like cash. */
    val effectivePayment: PaymentKind get() = if (total == 0) PaymentKind.CASH else selected
    val earliestPickupEnd get() = lines.minOfOrNull { it.listing.pickupEnd }
}

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val listingRepository: ListingRepository,
    private val orderRepository: OrderRepository,
    paymentRepository: PaymentMethodRepository,
    private val voucherRepository: VoucherRepository,
) : ViewModel() {

    private val voucherCode = MutableStateFlow<String?>(null)

    private val selected = MutableStateFlow(paymentRepository.defaultKind.value)
    private val note = MutableStateFlow("")
    private val step = MutableStateFlow<PaymentStep>(PaymentStep.Idle)

    private val base = combine(cartRepository.cart, listingRepository.listings, paymentRepository.methods) { cart, all, methods ->
        val byId = all.associateBy { it.id }
        Triple(
            cart.merchantId?.let { listingRepository.merchant(it) },
            cart.quantities.mapNotNull { (id, q) -> byId[id]?.let { CheckoutLine(it, q) } },
            methods,
        )
    }

    private val vouchersFlow = combine(voucherRepository.vouchers, voucherRepository.claimed, voucherCode) { all, claimed, code ->
        val mine = all.filter { it.code in claimed }
        mine to mine.firstOrNull { it.code == code }
    }

    val uiState: StateFlow<CheckoutUiState> = combine(base, selected, note, step, vouchersFlow) { (merchant, lines, methods), sel, n, st, (mine, v) ->
        CheckoutUiState(merchant, lines, methods, if (methods.any { it.kind == sel }) sel else PaymentKind.QRIS, n, st, mine, v)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CheckoutUiState())

    /** null removes the voucher. One below its minimum stays selectable; the screen says why
     * it gives 0. */
    fun selectVoucher(code: String?) {
        voucherCode.value = code
    }

    fun select(kind: PaymentKind) {
        selected.value = kind
    }

    fun onNoteChange(value: String) {
        note.value = value.take(140)
    }

    fun setQuantity(listingId: String, quantity: Int) {
        val line = uiState.value.lines.firstOrNull { it.listing.id == listingId } ?: return
        cartRepository.setQuantity(listingId, quantity.coerceAtMost(line.listing.stock))
    }

    /** Pay button. QRIS shows the code first; everything else places the order now. */
    fun pay() {
        val s = uiState.value
        if (s.isEmpty || s.step is PaymentStep.Processing) return
        if (s.effectivePayment == PaymentKind.QRIS) step.value = PaymentStep.AwaitingQris else place()
    }

    /** "Saya sudah bayar" on the QRIS sheet. */
    fun confirmQrisPaid() = place()

    fun dismissPayment() {
        if (step.value !is PaymentStep.Processing) step.value = PaymentStep.Idle
    }

    private fun place() {
        val s = uiState.value
        val merchant = s.merchant ?: return
        step.value = PaymentStep.Processing(s.effectivePayment)
        viewModelScope.launch {
            step.value = try {
                val applied = s.voucher?.takeIf { s.voucherDiscount > 0 }
                val order = orderRepository.place(
                    PlaceOrderRequest(
                        merchant.id, s.lines.map { it.listing to it.quantity }, s.effectivePayment, s.note,
                        voucherCode = applied?.code, voucherDiscount = s.voucherDiscount,
                    ),
                )
                applied?.let { voucherRepository.consume(it.code) }
                cartRepository.clear()
                PaymentStep.Done(order.id)
            } catch (e: Exception) {
                PaymentStep.Failed(e.message ?: "Pembayaran gagal. Coba lagi.")
            }
        }
    }
}
