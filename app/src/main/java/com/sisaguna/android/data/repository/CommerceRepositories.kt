package com.sisaguna.android.data.repository

import com.sisaguna.android.data.model.Address
import com.sisaguna.android.data.model.Cart
import com.sisaguna.android.data.model.Complaint
import com.sisaguna.android.data.model.DeliveryQuote
import com.sisaguna.android.data.model.Fulfillment
import com.sisaguna.android.data.model.GeoPoint
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.Order
import com.sisaguna.android.data.model.OrderRating
import com.sisaguna.android.data.model.PaymentKind
import com.sisaguna.android.data.model.PaymentMethod
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.sisaguna.android.data.remote.AppException

// ---------------------------------------------------------------- Cart

sealed interface AddToCartResult {
    data object Added : AddToCartResult

    /** The cart holds another merchant's items; caller asks before calling [CartRepository.replace]. */
    data class DifferentMerchant(val currentMerchantId: String) : AddToCartResult
}

interface CartRepository {
    val cart: StateFlow<Cart>
    fun add(listing: Listing, quantity: Int): AddToCartResult
    fun replace(listing: Listing, quantity: Int)

    /** 0 removes the line; clamped to [Listing.stock] by callers that know it. */
    fun setQuantity(listingId: String, quantity: Int)
    fun clear()
}

@Singleton
class InMemoryCartRepository @Inject constructor() : CartRepository {
    private val _cart = MutableStateFlow(Cart())
    override val cart: StateFlow<Cart> = _cart.asStateFlow()

    override fun add(listing: Listing, quantity: Int): AddToCartResult {
        val current = _cart.value
        if (current.merchantId != null && current.merchantId != listing.merchantId && !current.isEmpty) {
            return AddToCartResult.DifferentMerchant(current.merchantId)
        }
        val newQty = ((current.quantities[listing.id] ?: 0) + quantity).coerceAtMost(listing.stock)
        _cart.value = Cart(listing.merchantId, current.quantities + (listing.id to newQty))
        return AddToCartResult.Added
    }

    override fun replace(listing: Listing, quantity: Int) {
        _cart.value = Cart(listing.merchantId, mapOf(listing.id to quantity.coerceAtMost(listing.stock)))
    }

    override fun setQuantity(listingId: String, quantity: Int) {
        val current = _cart.value
        val next = if (quantity <= 0) current.quantities - listingId else current.quantities + (listingId to quantity)
        _cart.value = if (next.isEmpty()) Cart() else current.copy(quantities = next)
    }

    override fun clear() {
        _cart.value = Cart()
    }
}

// ---------------------------------------------------------------- Orders

data class PlaceOrderRequest(
    val merchantId: String,
    val lines: List<Pair<Listing, Int>>,
    val payment: PaymentKind,
    val note: String,
    val voucherCode: String? = null,
    val voucherDiscount: Int = 0,
    val fulfillment: Fulfillment = Fulfillment.PICKUP,
    val delivery: DeliveryQuote? = null,
    val deliveryAddress: Address? = null,
    val serviceFee: Int = 0,
    /** The total shown on screen; the server answers `price_changed` with fresh numbers if it differs. */
    val expectedTotal: Int? = null,
)

/** Pickup orders get this long to collect, unless the listing window closes first. */
const val PICKUP_GRACE_MINUTES = 45L

interface OrderRepository {
    /** Newest first. */
    val orders: StateFlow<List<Order>>

    /**
     * Creates a READY order. [idempotencyKey] identifies one purchase intent: pass the same key when
     * retrying after a timeout so the order can't be placed twice.
     */
    suspend fun place(request: PlaceOrderRequest, idempotencyKey: String = UUID.randomUUID().toString()): Order

    /** Reloads the user's orders (another device, or the merchant, may have changed them). */
    suspend fun refresh() {}

    /** True while the server still has older history than the orders loaded. */
    val hasMoreHistory: StateFlow<Boolean> get() = MutableStateFlow(false)

    /** Appends the next older page of history. */
    suspend fun loadMoreHistory() {}

    /** Throws [AppException] when the server refuses (e.g. the cancel window closed). */
    suspend fun cancel(orderId: String)
    suspend fun rate(orderId: String, rating: OrderRating)

    /** [Complaint.photos] are local URIs; the repository uploads them. */
    suspend fun complain(orderId: String, complaint: Complaint)
}

// ---------------------------------------------------------------- Addresses

interface AddressRepository {
    val addresses: StateFlow<List<Address>>

    /** Id of the address Home delivers listings around; null = current GPS label. */
    val selectedId: StateFlow<String?>

    /** Short label shown on Home's location chip when the selection is a GPS fix. */
    val currentLocationLabel: StateFlow<String?>

    /** Where catalog calls measure `distance_km` from: the selected address, else the last GPS fix. */
    val searchLocation: StateFlow<GeoPoint?>

    /** Saves a new ([Address.id] unknown) or edited address and returns it as stored. */
    suspend fun upsert(address: Address): Address
    suspend fun delete(id: String)
    fun select(id: String)
    fun selectCurrentLocation(label: String, point: GeoPoint)
}

// ---------------------------------------------------------------- Payment methods

interface PaymentMethodRepository {
    val methods: StateFlow<List<PaymentMethod>>
    val defaultKind: StateFlow<PaymentKind>
    fun setDefault(kind: PaymentKind)

    /** Links an e-wallet with a phone number (fake — no OTP). */
    fun link(kind: PaymentKind, phone: String)
    fun unlink(kind: PaymentKind)
}
