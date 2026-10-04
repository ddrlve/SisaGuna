package com.sisaguna.android.data.repository

import com.sisaguna.android.data.model.Address
import com.sisaguna.android.data.model.AppNotification
import com.sisaguna.android.data.model.Cart
import com.sisaguna.android.data.model.Complaint
import com.sisaguna.android.data.model.DeliveryQuote
import com.sisaguna.android.data.model.Fulfillment
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.data.model.MerchantStatus
import com.sisaguna.android.data.model.NotificationType
import com.sisaguna.android.data.model.Order
import com.sisaguna.android.data.model.OrderLine
import com.sisaguna.android.data.model.OrderRating
import com.sisaguna.android.data.model.OrderStatus
import com.sisaguna.android.data.model.PaymentKind
import com.sisaguna.android.data.model.PaymentMethod
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
)

/** Pickup orders get this long to collect, unless the listing window closes first. */
const val PICKUP_GRACE_MINUTES = 45L

interface OrderRepository {
    /** Newest first. */
    val orders: StateFlow<List<Order>>

    /** Simulates the payment round trip, then creates a READY order. */
    suspend fun place(request: PlaceOrderRequest): Order
    fun markPickedUp(orderId: String)
    fun cancel(orderId: String)
    fun rate(orderId: String, rating: OrderRating)
    fun complain(orderId: String, complaint: Complaint)
}

@Singleton
class FakeOrderRepository(
    private val listingRepository: ListingRepository,
    private val notificationRepository: NotificationRepository,
    private val clock: () -> Instant,
    private val paymentDelayMs: Long,
) : OrderRepository {

    @Inject constructor(
        listingRepository: ListingRepository,
        notificationRepository: NotificationRepository,
    ) : this(listingRepository, notificationRepository, Instant::now, 1_800)

    private val now0 = clock()
    private fun ago(minutes: Long) = now0.minus(Duration.ofMinutes(minutes))
    private fun inMinutes(minutes: Long) = now0.plus(Duration.ofMinutes(minutes))

    private fun seedMerchant(id: String) = listingRepository.merchant(id)
        ?: Merchant(id, id, false, MerchantStatus.APPROVED, "")

    private val _orders = MutableStateFlow(
        listOf(
            Order(
                id = "o4", pickupCode = "SG-4821", merchant = seedMerchant("m2"),
                lines = listOf(OrderLine("l2", "Roti Gandum Lewat Best Before", ListingTier.HUMAN, seedImage("roti_tawar"), 2, 5000, 18000)),
                payment = PaymentKind.QRIS, note = "", status = OrderStatus.READY,
                createdAt = ago(55), pickupEnd = inMinutes(95),
            ),
            Order(
                id = "o3", pickupCode = "SG-3307", merchant = seedMerchant("m2"),
                lines = listOf(OrderLine("l5", "Donat Glaze Sisa Etalase", ListingTier.HUMAN, seedImage("donat"), 1, 3000, 12000)),
                payment = PaymentKind.GOPAY, note = "", status = OrderStatus.COMPLETED,
                createdAt = ago(30 * 60), pickupEnd = ago(27 * 60), completedAt = ago(29 * 60),
            ),
            Order(
                id = "o2", pickupCode = "SG-2190", merchant = seedMerchant("m1"),
                lines = listOf(
                    OrderLine("l1", "Nasi Kuning Sisa Katering", ListingTier.HUMAN, seedImage("nasi_kuning"), 2, 8000, 25000),
                    OrderLine("l4", "Sayur Sop Sisa Hari Ini", ListingTier.HUMAN, seedImage("sayur_sop"), 1, 4000, 15000),
                ),
                payment = PaymentKind.CASH, note = "", status = OrderStatus.COMPLETED,
                createdAt = ago(4 * 24 * 60), pickupEnd = ago(4 * 24 * 60 - 120), completedAt = ago(4 * 24 * 60 - 40),
                rating = OrderRating(5, listOf("Makanan enak", "Ramah"), "Porsinya banyak, makasih Bu Sari!"),
            ),
            Order(
                id = "o1", pickupCode = "SG-1044", merchant = seedMerchant("m4"),
                lines = listOf(OrderLine("l7", "Ampas Tahu Segar", ListingTier.ANIMAL_FEED, seedImage("ampas_tahu"), 1, 0, 0)),
                payment = PaymentKind.CASH, note = "", status = OrderStatus.CANCELLED,
                createdAt = ago(6 * 24 * 60), pickupEnd = ago(6 * 24 * 60 - 120),
            ),
        ),
    )
    override val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private var counter = 5

    override suspend fun place(request: PlaceOrderRequest): Order {
        if (request.payment != PaymentKind.CASH) delay(paymentDelayMs)
        val merchant = listingRepository.merchant(request.merchantId)
            ?: throw IllegalArgumentException("Penyedia tidak ditemukan")
        val now = clock()
        val id = "o${counter++}"
        val order = Order(
            id = id,
            pickupCode = "SG-" + (1000 + (now.toEpochMilli() % 9000)).toString(),
            merchant = merchant,
            lines = request.lines.map { (l, q) ->
                OrderLine(l.id, l.title, l.tier, l.imageUrl, q, l.unitPrice, l.unitOriginalPrice, l.unit)
            },
            payment = request.payment,
            note = request.note.trim(),
            status = OrderStatus.READY,
            createdAt = now,
            pickupEnd = request.lines.minOf { it.first.pickupEnd },
            voucherCode = request.voucherCode,
            voucherDiscount = request.voucherDiscount,
            fulfillment = request.fulfillment,
            delivery = request.delivery.takeIf { request.fulfillment == Fulfillment.DELIVERY },
            deliveryAddress = request.deliveryAddress.takeIf { request.fulfillment == Fulfillment.DELIVERY },
            serviceFee = request.serviceFee,
            pickupBy = minOf(now.plus(Duration.ofMinutes(PICKUP_GRACE_MINUTES)), request.lines.minOf { it.first.pickupEnd })
                .takeIf { request.fulfillment == Fulfillment.PICKUP },
        )
        request.lines.forEach { (l, q) -> listingRepository.reduceStock(l.id, q) }
        _orders.value = listOf(order) + _orders.value
        notificationRepository.push(
            AppNotification(
                id = "n-$id", type = NotificationType.ORDER,
                title = "Pesanan ${order.pickupCode} siap diambil",
                body = "${merchant.name} sudah menyiapkan ${order.itemCount} item. Tunjukkan kode ${order.pickupCode} saat ambil.",
                createdAt = now, isRead = false,
            ),
        )
        return order
    }

    private fun update(orderId: String, block: (Order) -> Order) {
        _orders.value = _orders.value.map { if (it.id == orderId) block(it) else it }
    }

    override fun markPickedUp(orderId: String) =
        update(orderId) { if (it.status == OrderStatus.READY) it.copy(status = OrderStatus.COMPLETED, completedAt = clock()) else it }

    override fun cancel(orderId: String) =
        update(orderId) { if (it.status == OrderStatus.READY) it.copy(status = OrderStatus.CANCELLED) else it }

    override fun rate(orderId: String, rating: OrderRating) =
        update(orderId) { if (it.canRate) it.copy(rating = rating.copy(stars = rating.stars.coerceIn(1, 5))) else it }

    override fun complain(orderId: String, complaint: Complaint) {
        update(orderId) { if (it.canComplain) it.copy(complaint = complaint) else it }
        val order = _orders.value.firstOrNull { it.id == orderId } ?: return
        notificationRepository.push(
            AppNotification(
                id = "n-c-$orderId", type = NotificationType.ORDER,
                title = "Komplain ${order.pickupCode} diterima",
                body = "Tim SisaGuna meninjau laporanmu dalam 1×24 jam. Kalau terbukti, dana dikembalikan penuh.",
                createdAt = clock(), isRead = false,
            ),
        )
    }
}

// ---------------------------------------------------------------- Addresses

interface AddressRepository {
    val addresses: StateFlow<List<Address>>

    /** Id of the address Home delivers listings around; null = current GPS label. */
    val selectedId: StateFlow<String?>

    /** Short label shown on Home's location chip when the selection is a GPS fix. */
    val currentLocationLabel: StateFlow<String?>
    fun upsert(address: Address)
    fun delete(id: String)
    fun select(id: String)
    fun selectCurrentLocation(label: String)
}

@Singleton
class FakeAddressRepository @Inject constructor() : AddressRepository {
    private val _addresses = MutableStateFlow(
        listOf(
            Address("a1", "Rumah", "Jl. Sutera Onyx XII No.30, Kunciran, Kec. Pinang, Kota Tangerang, Banten 15144", -6.2275, 106.6544),
            Address("a2", "Kantor", "Business Park Kebon Jeruk Ruko AB-6, Meruya Utara, Kembangan, Jakarta Barat 11620", -6.1931, 106.7638, note = "Lantai 2, sebelah lift"),
        ),
    )
    override val addresses: StateFlow<List<Address>> = _addresses.asStateFlow()

    private val _selectedId = MutableStateFlow<String?>("a1")
    override val selectedId: StateFlow<String?> = _selectedId.asStateFlow()

    private val _currentLabel = MutableStateFlow<String?>(null)
    override val currentLocationLabel: StateFlow<String?> = _currentLabel.asStateFlow()

    override fun upsert(address: Address) {
        val list = _addresses.value
        _addresses.value = if (list.any { it.id == address.id }) list.map { if (it.id == address.id) address else it } else list + address
    }

    override fun delete(id: String) {
        _addresses.value = _addresses.value.filterNot { it.id == id }
        if (_selectedId.value == id) _selectedId.value = _addresses.value.firstOrNull()?.id
    }

    override fun select(id: String) {
        if (_addresses.value.any { it.id == id }) {
            _selectedId.value = id
            _currentLabel.value = null
        }
    }

    override fun selectCurrentLocation(label: String) {
        _selectedId.value = null
        _currentLabel.value = label
    }
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

@Singleton
class FakePaymentMethodRepository @Inject constructor() : PaymentMethodRepository {
    private val _methods = MutableStateFlow(
        listOf(
            PaymentMethod("p-qris", PaymentKind.QRIS, "QRIS", "Bayar pakai aplikasi bank atau e-wallet apa pun"),
            PaymentMethod("p-gopay", PaymentKind.GOPAY, "GoPay", "0812 •••• 7890"),
            PaymentMethod("p-cash", PaymentKind.CASH, "Bayar saat ambil", "Tunai langsung ke penyedia"),
        ),
    )
    override val methods: StateFlow<List<PaymentMethod>> = _methods.asStateFlow()

    private val _default = MutableStateFlow(PaymentKind.QRIS)
    override val defaultKind: StateFlow<PaymentKind> = _default.asStateFlow()

    override fun setDefault(kind: PaymentKind) {
        if (_methods.value.any { it.kind == kind }) _default.value = kind
    }

    override fun link(kind: PaymentKind, phone: String) {
        val digits = phone.filter(Char::isDigit)
        val masked = if (digits.length >= 8) "${digits.take(4)} •••• ${digits.takeLast(4)}" else digits
        val label = when (kind) {
            PaymentKind.GOPAY -> "GoPay"
            PaymentKind.OVO -> "OVO"
            PaymentKind.DANA -> "DANA"
            else -> return
        }
        val method = PaymentMethod("p-${kind.name.lowercase()}", kind, label, masked)
        val list = _methods.value.filterNot { it.kind == kind }
        // Keep e-wallets grouped after QRIS, before cash.
        _methods.value = (list.filter { it.kind != PaymentKind.CASH } + method + list.filter { it.kind == PaymentKind.CASH })
    }

    override fun unlink(kind: PaymentKind) {
        if (kind == PaymentKind.QRIS || kind == PaymentKind.CASH) return
        _methods.value = _methods.value.filterNot { it.kind == kind }
        if (_default.value == kind) _default.value = PaymentKind.QRIS
    }
}
