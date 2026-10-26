package com.sisaguna.android.data.repository

import com.sisaguna.android.data.model.Address
import com.sisaguna.android.data.model.AppNotification
import com.sisaguna.android.data.model.Complaint
import com.sisaguna.android.data.model.Fulfillment
import com.sisaguna.android.data.model.GeoPoint
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
import com.sisaguna.android.testutil.StockTracking
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class FakeOrderRepository(
    private val listingRepository: ListingRepository,
    private val notificationRepository: FakeNotificationRepository,
    private val clock: () -> Instant,
    private val paymentDelayMs: Long,
) : OrderRepository {

    @Inject constructor(
        listingRepository: ListingRepository,
        notificationRepository: FakeNotificationRepository,
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

    override suspend fun place(request: PlaceOrderRequest, idempotencyKey: String): Order {
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
        request.lines.forEach { (l, q) -> (listingRepository as? StockTracking)?.reduceStock(l.id, q) }
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

    /** Test helper: the real flow has the merchant hand the order over, not the buyer. */
    fun markPickedUp(orderId: String) = update(orderId) {
        if (it.status == OrderStatus.READY) {
            it.copy(status = OrderStatus.COMPLETED, completedAt = clock(), canCancel = false, canRate = true, canComplain = true)
        } else it
    }

    override suspend fun cancel(orderId: String) =
        update(orderId) { if (it.status == OrderStatus.READY) it.copy(status = OrderStatus.CANCELLED, canCancel = false) else it }

    override suspend fun rate(orderId: String, rating: OrderRating) =
        update(orderId) { if (it.canRate) it.copy(rating = rating.copy(stars = rating.stars.coerceIn(1, 5)), canRate = false) else it }

    override suspend fun complain(orderId: String, complaint: Complaint) {
        update(orderId) { if (it.canComplain) it.copy(complaint = complaint, canComplain = false) else it }
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

    private val _searchLocation = MutableStateFlow<GeoPoint?>(null)
    override val searchLocation: StateFlow<GeoPoint?> = _searchLocation.asStateFlow()

    override suspend fun upsert(address: Address): Address {
        val list = _addresses.value
        _addresses.value = if (list.any { it.id == address.id }) list.map { if (it.id == address.id) address else it } else list + address
        return address
    }

    override suspend fun delete(id: String) {
        _addresses.value = _addresses.value.filterNot { it.id == id }
        if (_selectedId.value == id) _selectedId.value = _addresses.value.firstOrNull()?.id
    }

    override fun select(id: String) {
        val address = _addresses.value.firstOrNull { it.id == id } ?: return
        _selectedId.value = id
        _currentLabel.value = null
        _searchLocation.value = GeoPoint(address.latitude, address.longitude)
    }

    override fun selectCurrentLocation(label: String, point: GeoPoint) {
        _selectedId.value = null
        _currentLabel.value = label
        _searchLocation.value = point
    }
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
