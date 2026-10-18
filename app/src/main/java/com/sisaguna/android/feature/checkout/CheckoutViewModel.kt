package com.sisaguna.android.feature.checkout

import com.sisaguna.android.data.model.Address
import com.sisaguna.android.data.model.AppFees
import com.sisaguna.android.data.model.Courier
import com.sisaguna.android.data.model.DeliveryPricing
import com.sisaguna.android.data.model.DeliveryQuote
import com.sisaguna.android.data.model.DeliverySpeed
import com.sisaguna.android.data.model.Fulfillment
import com.sisaguna.android.data.remote.ApiException
import com.sisaguna.android.data.remote.AppException
import com.sisaguna.android.data.repository.AddressRepository
import com.sisaguna.android.data.repository.PICKUP_GRACE_MINUTES
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException

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
import com.sisaguna.android.ui.domain.formatRupiah
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

/** Where the payment step is: the order is placed straight away (the backend settles a simulated payment at once). */
sealed interface PaymentStep {
    data object Idle : PaymentStep
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

/** The server's own numbers, adopted when it answers `price_changed`; they win over the local preview. */
data class ServerPrice(val voucherDiscount: Int, val serviceFee: Int, val deliveryFee: Int, val total: Int)

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
    /** Other listings from the same store that aren't in the order yet ("Mau tambah lagi?"). */
    val moreFromStore: List<Listing> = emptyList(),
    val serverPrice: ServerPrice? = null,
) {
    val isEmpty: Boolean get() = lines.isEmpty()
    val subtotal: Int get() = lines.sumOf { it.total }
    val voucherDiscount: Int get() = serverPrice?.voucherDiscount ?: voucher?.discountFor(subtotal) ?: 0
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

    val deliveryFee: Int get() = serverPrice?.deliveryFee ?: selectedQuote?.payable ?: 0
    val serviceFee: Int get() = serverPrice?.serviceFee ?: AppFees.buyerServiceFee(itemsTotal)
    val total: Int get() = serverPrice?.total ?: (itemsTotal + deliveryFee + serviceFee)

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

private data class CheckoutBase(
    val merchant: Merchant?,
    val lines: List<CheckoutLine>,
    val methods: List<PaymentMethod>,
    val more: List<Listing>,
)

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

    /** Set after a `price_changed` answer; any edit to the basket drops it so the preview is live again. */
    private val serverPrice = MutableStateFlow<ServerPrice?>(null)

    /** One purchase intent = one key, so a retry after a timeout can't create a second order. */
    private var attempt: Pair<PlaceOrderRequest, String>? = null

    fun setFulfillment(f: Fulfillment) { serverPrice.value = null; delivery.value = delivery.value.copy(fulfillment = f) }
    fun setCourier(c: Courier) { serverPrice.value = null; delivery.value = delivery.value.copy(courier = c, fulfillment = Fulfillment.DELIVERY) }
    fun setSpeed(sp: DeliverySpeed) { serverPrice.value = null; delivery.value = delivery.value.copy(speed = sp, fulfillment = Fulfillment.DELIVERY) }

    private val addressFlow = combine(addressRepository.addresses, addressRepository.selectedId) { list, id ->
        list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }

    private val voucherCode = MutableStateFlow<String?>(null)

    private val selected = MutableStateFlow(paymentRepository.defaultKind.value)
    private val note = MutableStateFlow("")
    private val step = MutableStateFlow<PaymentStep>(PaymentStep.Idle)

    private val base = combine(cartRepository.cart, listingRepository.listings, paymentRepository.methods) { cart, all, methods ->
        val byId = all.associateBy { it.id }
        val now = Instant.now()
        CheckoutBase(
            merchant = cart.merchantId?.let { listingRepository.merchant(it) },
            lines = cart.quantities.mapNotNull { (id, q) -> byId[id]?.let { CheckoutLine(it, q) } },
            methods = methods,
            more = all.filter {
                it.merchantId == cart.merchantId && it.id !in cart.quantities && it.stock > 0 && it.pickupEnd.isAfter(now)
            },
        )
    }

    private val vouchersFlow = combine(voucherRepository.vouchers, voucherRepository.claimed, voucherCode) { all, claimed, code ->
        val mine = all.filter { it.code in claimed }
        mine to mine.firstOrNull { it.code == code }
    }

    private val core = combine(base, selected, note, step, vouchersFlow) { b, sel, n, st, (mine, v) ->
        CheckoutUiState(
            b.merchant, b.lines, b.methods, if (b.methods.any { it.kind == sel }) sel else PaymentKind.QRIS, n, st, mine, v,
            moreFromStore = b.more,
        )
    }

    val uiState: StateFlow<CheckoutUiState> = combine(core, delivery, addressFlow, serverPrice) { s, d, a, price ->
        s.copy(delivery = d, address = a, now = Instant.now(), serverPrice = price)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CheckoutUiState())

    /** null removes the voucher. One below its minimum stays selectable; the screen says why
     * it gives 0. */
    fun selectVoucher(code: String?) {
        serverPrice.value = null
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
        serverPrice.value = null
        cartRepository.setQuantity(listingId, quantity.coerceAtMost(line.listing.stock))
    }

    /** "Mau tambah lagi?" rail: one portion of another item from the same store. */
    fun addMore(listing: Listing) {
        serverPrice.value = null
        cartRepository.add(listing, 1)
    }

    /** Pay button: places the order now. */
    fun pay() {
        val s = uiState.value
        if (s.isEmpty || s.step is PaymentStep.Processing) return
        place()
    }

    fun dismissPayment() {
        if (step.value !is PaymentStep.Processing) step.value = PaymentStep.Idle
    }

    private fun place() {
        val s = uiState.value
        val merchant = s.merchant ?: return
        step.value = PaymentStep.Processing(s.effectivePayment)
        viewModelScope.launch {
            val applied = s.voucher?.takeIf { s.voucherDiscount > 0 }
            val request = PlaceOrderRequest(
                merchant.id, s.lines.map { it.listing to it.quantity }, s.effectivePayment, s.note,
                voucherCode = applied?.code, voucherDiscount = s.voucherDiscount,
                fulfillment = if (s.selectedQuote != null) Fulfillment.DELIVERY else Fulfillment.PICKUP,
                delivery = s.selectedQuote,
                deliveryAddress = s.address,
                serviceFee = s.serviceFee,
                expectedTotal = s.total,
            )
            val key = attempt?.takeIf { it.first == request }?.second ?: UUID.randomUUID().toString()
            attempt = request to key
            step.value = try {
                val order = orderRepository.place(request, key)
                attempt = null
                cartRepository.clear()
                refreshAfterOrder()
                PaymentStep.Done(order.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (e.code == "price_changed") {
                    // Adopt the server's numbers and ask again: the user must see what they will really pay.
                    serverPrice.value = ServerPrice(
                        voucherDiscount = e.int("voucher_discount") ?: 0,
                        serviceFee = e.int("service_fee") ?: 0,
                        deliveryFee = e.int("delivery_fee") ?: 0,
                        total = e.int("total") ?: s.total,
                    )
                    attempt = null
                    PaymentStep.Failed("Harga berubah. Total sekarang ${formatRupiah(e.int("total") ?: s.total)}, periksa lagi lalu bayar.")
                } else {
                    // The order was refused for a reason (stock, window, payment): a new tap is a new intent.
                    attempt = null
                    PaymentStep.Failed(e.userMessage)
                }
            } catch (e: AppException) {
                // Network trouble: keep the key so tapping "Bayar" again retries the same purchase safely.
                PaymentStep.Failed(e.userMessage)
            }
        }
    }

    /** The server used up the voucher and stock; pull the fresh state so Home and the voucher list agree. */
    private suspend fun refreshAfterOrder() {
        try {
            voucherRepository.refresh()
        } catch (e: AppException) {
            // Non-critical: the order is placed; the list refreshes on the next launch.
        }
    }
}
