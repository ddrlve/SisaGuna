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
) {
    val total: Int get() = unitPrice * quantity
    val originalTotal: Int get() = unitOriginalPrice * quantity
}

data class OrderRating(
    val stars: Int,
    val tags: List<String>,
    val comment: String,
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
) {
    val subtotal: Int get() = lines.sumOf { it.total }
    val total: Int get() = (subtotal - voucherDiscount).coerceAtLeast(0)
    val originalTotal: Int get() = lines.sumOf { it.originalTotal }
    val savings: Int get() = (originalTotal - total).coerceAtLeast(0)
    val itemCount: Int get() = lines.sumOf { it.quantity }
    val canRate: Boolean get() = status == OrderStatus.COMPLETED && rating == null
}

data class Address(
    val id: String,
    val label: String,
    val fullAddress: String,
    val latitude: Double,
    val longitude: Double,
    val note: String = "",
)
