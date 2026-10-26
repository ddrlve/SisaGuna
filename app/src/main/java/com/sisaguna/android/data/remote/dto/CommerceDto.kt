package com.sisaguna.android.data.remote.dto

import kotlinx.serialization.Serializable

// ---- Orders (buyer and merchant share one shape; the server hides what each side may not see)

@Serializable
data class OrderPartyDto(
    val id: String,
    val name: String,
    val locationLabel: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
)

@Serializable
data class OrderItemDto(
    val listingId: String,
    val title: String,
    val tier: String,
    val unit: String = "portion",
    val quantity: Int,
    val unitPrice: Int = 0,
    val unitOriginalPrice: Int = 0,
    val imageUrl: String? = null,
)

@Serializable
data class OrderDeliveryDto(
    val courier: String,
    val courierLabel: String = "",
    val speed: String,
    val speedLabel: String = "",
    val fee: Int = 0,
    val discount: Int = 0,
    val payable: Int = 0,
    val etaMin: Int = 0,
    val etaMax: Int = 0,
    val promoLabel: String? = null,
    val promoLabelEn: String? = null,
)

@Serializable
data class OrderAddressDto(
    val id: String,
    val label: String,
    val fullAddress: String,
    val note: String = "",
    val lat: Double,
    val lng: Double,
)

@Serializable
data class OrderRatingDto(val stars: Int, val tags: List<String> = emptyList(), val comment: String = "")

@Serializable
data class OrderComplaintDto(
    val reason: String,
    val detail: String = "",
    val status: String = "submitted",
    val photoUrls: List<String> = emptyList(),
    val createdAt: String? = null,
)

@Serializable
data class OrderPaymentDto(
    val kind: String = "cash",
    /** `unpaid` or `paid`. */
    val status: String = "unpaid",
    /** `dummy` while the backend simulates the gateway. */
    val provider: String? = null,
    val paidAt: String? = null,
)

@Serializable
data class OrderDto(
    val id: String,
    /** Only present for the buyer: the merchant has to be told the code in person. */
    val code: String? = null,
    val status: String,
    val fulfillment: String = "pickup",
    val paymentKind: String = "cash",
    val note: String = "",
    val merchant: OrderPartyDto,
    val buyerName: String? = null,
    val items: List<OrderItemDto> = emptyList(),
    val subtotal: Int = 0,
    val originalTotal: Int = 0,
    val voucherCode: String? = null,
    val voucherDiscount: Int = 0,
    val serviceFee: Int = 0,
    val deliveryFee: Int = 0,
    val total: Int = 0,
    val pickupBy: String? = null,
    val pickupEnd: String,
    val createdAt: String,
    val completedAt: String? = null,
    val rating: OrderRatingDto? = null,
    val complaint: OrderComplaintDto? = null,
    val delivery: OrderDeliveryDto? = null,
    val deliveryAddress: OrderAddressDto? = null,
    val payment: OrderPaymentDto? = null,
    val canCancel: Boolean = false,
    val canRate: Boolean = false,
    val canComplain: Boolean = false,
)

@Serializable
data class OrderLineRequest(val listingId: String, val quantity: Int)

@Serializable
data class PlaceOrderBody(
    val merchantId: String,
    val lines: List<OrderLineRequest>,
    val paymentKind: String,
    val fulfillment: String,
    val voucherCode: String? = null,
    val courier: String? = null,
    val speed: String? = null,
    val addressId: String? = null,
    val note: String = "",
    val expectedTotal: Int? = null,
)

@Serializable
data class CancelBody(val reason: String = "")

@Serializable
data class ConfirmBody(val code: String)

@Serializable
data class ReviewBody(val stars: Int, val tags: List<String>, val comment: String, val mediaIds: List<String> = emptyList())

@Serializable
data class ComplaintBody(val reason: String, val detail: String, val mediaIds: List<String> = emptyList())

// ---- Payment methods and vouchers

@Serializable
data class PaymentMethodDto(
    val kind: String,
    val label: String = "",
    val detail: String = "",
    val isDefault: Boolean = false,
)

@Serializable
data class LinkPaymentBody(val kind: String, val phone: String)

@Serializable
data class DefaultPaymentBody(val kind: String)

@Serializable
data class VoucherDto(
    val code: String,
    val title: String,
    val description: String = "",
    val percentOff: Int? = null,
    val flatOff: Int? = null,
    val maxDiscount: Int? = null,
    val minSpend: Int = 0,
    val merchantName: String? = null,
    val appliesTo: String = "",
    val imageUrl: String? = null,
    val expiresAt: String,
    val terms: List<String> = emptyList(),
    val accentHex: String = "",
    /** `available`, `claimed` or `used`. */
    val status: String = "available",
)

// ---- Merchant side

@Serializable
data class MerchantDashboardDto(
    val salesToday: Int = 0,
    val commissionToday: Int = 0,
    val netToday: Int = 0,
    val completedToday: Int = 0,
    val pendingOrders: Int = 0,
    val activeListings: Int = 0,
)

@Serializable
data class CreateMerchantBody(
    val name: String,
    val about: String = "",
    val address: String,
    val lat: Double,
    val lng: Double,
    val locationLabel: String = "",
    val openHours: String = "",
    val halal: String = "unverified",
    val allergens: List<String> = emptyList(),
    val prepMinutes: Int? = null,
)

@Serializable
data class ReviewDto(
    val id: String,
    val stars: Int,
    val tags: List<String> = emptyList(),
    val comment: String = "",
    val buyerName: String = "",
    val photos: List<PhotoDto> = emptyList(),
    val createdAt: String,
)

// ---- Notifications

@Serializable
data class NotificationDto(
    val id: String,
    val type: String,
    val title: String,
    val body: String = "",
    /** Carries `order_id` or `merchant_id` so a tap can open the right screen. */
    val data: kotlinx.serialization.json.JsonObject? = null,
    val createdAt: String,
    val readAt: String? = null,
)

@Serializable
data class DeviceBody(val token: String, val platform: String = "android")

/** One class for both directions: `PUT /me/notification-prefs` replaces all six switches. */
@Serializable
data class NotificationPrefsDto(
    val enabled: Boolean = true,
    val pickup: Boolean = true,
    val order: Boolean = true,
    val merchant: Boolean = true,
    val promo: Boolean = false,
    val impact: Boolean = true,
)
