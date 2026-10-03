package com.sisaguna.android.data.model

import java.time.Instant

enum class MerchantStatus { PENDING, APPROVED, REJECTED }

data class Merchant(
    val id: String,
    val name: String,
    val isVerified: Boolean,
    val status: MerchantStatus,
    val location: String,
    val rating: Double? = null, // null = no ratings yet; UI hides the star row
    val ratingCount: Int = 0,
    val bannerUrl: String = "",
    /** Storefront / kitchen photos for the store gallery. */
    val photos: List<String> = emptyList(),
    val about: String = "",
    val address: String = "",
    val openHours: String = "",
    val halal: HalalStatus = HalalStatus.UNVERIFIED,
    /** Allergens the kitchen handles — declared once per store, shown before checkout. */
    val allergens: Set<Allergen> = emptySet(),
    /** Headline for the store's "Today's offer" strip, null when there's none. */
    val todaysOffer: String? = null,
    val deliveryAvailable: Boolean = true,
    /** Typical minutes from order to "ready to collect". */
    val prepMinutes: Int = 15,
    val distanceKm: Double? = null,
    val totalRescued: Int = 0,
)

data class Review(
    val id: String,
    val merchantId: String,
    val author: String,
    val stars: Int,
    val comment: String,
    val createdAt: Instant,
    val photos: List<String> = emptyList(),
    val itemTitle: String? = null,
    val tags: List<String> = emptyList(),
)
