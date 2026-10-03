package com.sisaguna.android.data.model

import java.time.Instant

/** Single-merchant cart (pickup happens at one place), keyed by listing id → quantity. */
data class Cart(
    val merchantId: String? = null,
    val quantities: Map<String, Int> = emptyMap(),
) {
    val itemCount: Int get() = quantities.values.sum()
    val isEmpty: Boolean get() = quantities.isEmpty()
}

enum class PaymentKind { QRIS, GOPAY, OVO, DANA, CASH }

data class PaymentMethod(
    val id: String,
    val kind: PaymentKind,
    val label: String,
    val detail: String,
)

enum class OrderStatus { READY, COMPLETED, CANCELLED }

/** Snapshot of a listing at checkout — later edits to the listing don't rewrite history. */
data class OrderLine(
    val listingId: String,
    val title: String,
    val tier: ListingTier,
    val imageUrl: String,
    val quantity: Int,
    val unitPrice: Int,
    val unitOriginalPrice: Int,
    val unit: QuantityUnit = if (tier == ListingTier.HUMAN) QuantityUnit.PORTION else QuantityUnit.KILOGRAM,
) {
    val total: Int get() = unitPrice * quantity
    val originalTotal: Int get() = unitOriginalPrice * quantity
}

data class OrderRating(
    val stars: Int,
    val tags: List<String>,
    val comment: String,
)

enum class ComplaintReason(val label: String) {
    NOT_FRESH("Makanan basi / tidak layak"),
    WRONG_ITEM("Pesanan tidak sesuai"),
    MISSING("Jumlah kurang"),
    PACKAGING("Kemasan rusak / tumpah"),
    HYGIENE("Kebersihan meragukan"),
    MERCHANT("Penyedia tidak ada di lokasi"),
    OTHER("Lainnya"),
}

enum class ComplaintStatus(val label: String) {
    SUBMITTED("Terkirim"),
    REVIEWING("Sedang ditinjau tim SisaGuna"),
    RESOLVED("Selesai · dana dikembalikan"),
}

/** Buyer-side half of the food-safety loop (see [FoodSafety]). */
data class Complaint(
    val reason: ComplaintReason,
    val detail: String,
    val photos: List<String>,
    val createdAt: Instant,
    val status: ComplaintStatus = ComplaintStatus.SUBMITTED,
)

data class Order(
    val id: String,
    val pickupCode: String,
    val merchant: Merchant,
    val lines: List<OrderLine>,
    val payment: PaymentKind,
    val note: String,
    val status: OrderStatus,
    val createdAt: Instant,
    val pickupEnd: Instant,
    val completedAt: Instant? = null,
    val rating: OrderRating? = null,
    val voucherCode: String? = null,
    val voucherDiscount: Int = 0,
    val fulfillment: Fulfillment = Fulfillment.PICKUP,
    val delivery: DeliveryQuote? = null,
    val deliveryAddress: Address? = null,
    /** Pickup: the latest time the buyer should arrive (order time + grace, capped by the listing window). */
    val pickupBy: Instant? = null,
    val complaint: Complaint? = null,
) {
    val subtotal: Int get() = lines.sumOf { it.total }
    val deliveryFee: Int get() = delivery?.payable ?: 0
    val total: Int get() = (subtotal - voucherDiscount).coerceAtLeast(0) + deliveryFee
    val originalTotal: Int get() = lines.sumOf { it.originalTotal }
    val savings: Int get() = (originalTotal - (total - deliveryFee)).coerceAtLeast(0)
    val itemCount: Int get() = lines.sumOf { it.quantity }
    val canRate: Boolean get() = status == OrderStatus.COMPLETED && rating == null
    val canComplain: Boolean get() = status == OrderStatus.COMPLETED && complaint == null
}

data class Address(
    val id: String,
    val label: String,
    val fullAddress: String,
    val latitude: Double,
    val longitude: Double,
    val note: String = "",
)
