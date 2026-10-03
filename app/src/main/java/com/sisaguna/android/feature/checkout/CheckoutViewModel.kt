package com.sisaguna.android.feature.checkout

import com.sisaguna.android.data.model.Address
import com.sisaguna.android.data.model.Courier
import com.sisaguna.android.data.model.DeliveryPricing
import com.sisaguna.android.data.model.DeliveryQuote
import com.sisaguna.android.data.model.DeliverySpeed
import com.sisaguna.android.data.model.Fulfillment
import com.sisaguna.android.data.repository.AddressRepository
import com.sisaguna.android.data.repository.PICKUP_GRACE_MINUTES
import java.time.Duration
import java.time.Instant

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

/** Pickup vs courier, and for courier which provider and speed. */
data class DeliveryChoice(
    val fulfillment: Fulfillment = Fulfillment.PICKUP,
    val courier: Courier = Courier.GOSEND,
    val speed: DeliverySpeed = DeliverySpeed.STANDARD,
)

data class CheckoutUiState(
    val merchant: Merchant? = null,
    val lines: List<CheckoutLine> = emptyList(),
    val methods: List<PaymentMethod> = emptyList(),
    val selected: PaymentKind = PaymentKind.QRIS,
    val note: String = "",
    val step: PaymentStep = PaymentStep.Idle,
    val vouchers: List<Voucher> = emptyList(),
    val voucher: Voucher? = null,
    val delivery: DeliveryChoice = DeliveryChoice(),
    val address: Address? = null,
    val now: Instant = Instant.now(),
) {
    val isEmpty: Boolean get() = lines.isEmpty()
    val subtotal: Int get() = lines.sumOf { it.total }
    val voucherDiscount: Int get() = voucher?.discountFor(subtotal) ?: 0
    val itemsTotal: Int get() = (subtotal - voucherDiscount).coerceAtLeast(0)

    /** Store → buyer distance. Fake: the store's distance from the buyer's saved area. */
    val distanceKm: Double get() = merchant?.distanceKm ?: lines.firstNotNullOfOrNull { it.listing.distanceKm } ?: 3.0
    val deliveryAvailable: Boolean get() = merchant?.deliveryAvailable != false && distanceKm <= DeliveryPricing.MAX_DISTANCE_KM

    /** Every courier's quote for the chosen speed, cheapest first. */
    val quotes: List<DeliveryQuote>
        get() = DeliveryPricing.allQuotes(distanceKm, subtotal, merchant?.prepMinutes ?: 15, delivery.speed)

    /** The quote for each speed with the chosen courier — for the Prioritas/Standar/Hemat cards. */
    val speedQuotes: List<DeliveryQuote>
        get() = DeliverySpeed.entries.map { DeliveryPricing.quote(delivery.courier, it, distanceKm, subtotal, merchant?.prepMinutes ?: 15) }

    val selectedQuote: DeliveryQuote?
        get() = if (delivery.fulfillment == Fulfillment.DELIVERY && deliveryAvailable) {
            DeliveryPricing.quote(delivery.courier, delivery.speed, distanceKm, subtotal, merchant?.prepMinutes ?: 15)
        } else null

    val deliveryFee: Int get() = selectedQuote?.payable ?: 0
    val total: Int get() = itemsTotal + deliveryFee

    /** Pickup: arrive within the grace window, but never after the listing closes. */
    val pickupBy: Instant?
        get() = earliestPickupEnd?.let { minOf(now.plus(Duration.ofMinutes(PICKUP_GRACE_MINUTES)), it) }

    /** Retail price minus what's paid: surplus discount plus voucher. */
    val savings: Int get() = (lines.sumOf { it.originalTotal } - itemsTotal).coerceAtLeast(0)
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
    addressRepository: AddressRepository,
) : ViewModel() {

    private val delivery = MutableStateFlow(DeliveryChoice())

    fun setFulfillment(f: Fulfillment) { delivery.value = delivery.value.copy(fulfillment = f) }
    fun setCourier(c: Courier) { delivery.value = delivery.value.copy(courier = c, fulfillment = Fulfillment.DELIVERY) }
    fun setSpeed(sp: DeliverySpeed) { delivery.value = delivery.value.copy(speed = sp, fulfillment = Fulfillment.DELIVERY) }

    private val addressFlow = combine(addressRepository.addresses, addressRepository.selectedId) { list, id ->
        list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }

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

    private val core = combine(base, selected, note, step, vouchersFlow) { (merchant, lines, methods), sel, n, st, (mine, v) ->
        CheckoutUiState(merchant, lines, methods, if (methods.any { it.kind == sel }) sel else PaymentKind.QRIS, n, st, mine, v)
    }

    val uiState: StateFlow<CheckoutUiState> = combine(core, delivery, addressFlow) { s, d, a ->
        s.copy(delivery = d, address = a, now = Instant.now())
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
                        fulfillment = if (s.selectedQuote != null) Fulfillment.DELIVERY else Fulfillment.PICKUP,
                        delivery = s.selectedQuote,
                        deliveryAddress = s.address,
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
