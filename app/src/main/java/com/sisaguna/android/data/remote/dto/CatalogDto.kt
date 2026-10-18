package com.sisaguna.android.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PhotoDto(val url: String, val thumbUrl: String? = null)

@Serializable
data class DeliveryHintDto(val fee: Int = 0, val etaMin: Int = 0, val etaMax: Int = 0)

/** Every merchant shape the API returns (card on a listing, store page, `/me/merchant`, list item) in one class; absent fields keep defaults. */
@Serializable
data class MerchantDto(
    val id: String,
    val name: String,
    val status: String = "approved",
    val locationLabel: String = "",
    val isVerified: Boolean = false,
    val about: String = "",
    val address: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val openHours: String = "",
    val halal: String = "unverified",
    val allergens: List<String> = emptyList(),
    val todaysOffer: String? = null,
    val deliveryAvailable: Boolean = true,
    val prepMinutes: Int = 15,
    val ratingAvg: Double? = null,
    val ratingCount: Int = 0,
    val totalRescued: Int = 0,
    val distanceKm: Double? = null,
    val bannerUrl: String? = null,
    val photos: List<PhotoDto> = emptyList(),
    val ratingBreakdown: Map<String, Int> = emptyMap(),
    val listings: List<ListingCardDto> = emptyList(),
    val isSaved: Boolean = false,
    val availableCount: Int = 0,
    val nextPickupEnd: String? = null,
)

@Serializable
data class ListingCardDto(
    val id: String,
    val title: String,
    val tier: String,
    val unit: String = "portion",
    val isFree: Boolean = false,
    val price: Int? = null,
    val priceOriginal: Int? = null,
    val discountPct: Int = 0,
    val stock: Int = 0,
    val halal: String = "unverified",
    val allergens: List<String> = emptyList(),
    val pickupStart: String? = null,
    val pickupEnd: String,
    val distanceKm: Double? = null,
    val coverUrl: String = "",
    val thumbUrl: String? = null,
    val merchant: MerchantDto,
    val soldCount: Int = 0,
    val delivery: DeliveryHintDto? = null,
)

@Serializable
data class ListingDetailDto(
    val id: String,
    val title: String,
    val description: String = "",
    val tier: String,
    val unit: String = "portion",
    val status: String = "active",
    val isFree: Boolean = false,
    val price: Int? = null,
    val priceOriginal: Int? = null,
    val stock: Int = 0,
    val soldCount: Int = 0,
    val halal: String = "unverified",
    val allergens: List<String> = emptyList(),
    val madeAt: String? = null,
    val storage: String = "room_temp",
    val safetyChecks: List<String> = emptyList(),
    val pickupStart: String? = null,
    val pickupEnd: String,
    val distanceKm: Double? = null,
    val photos: List<PhotoDto> = emptyList(),
    val videoUrl: String? = null,
    val merchant: MerchantDto,
    val moreFromMerchant: List<ListingCardDto> = emptyList(),
)

@Serializable
data class HomeDto(
    val nearby: List<ListingCardDto> = emptyList(),
    val deals: List<ListingCardDto> = emptyList(),
    val popular: List<ListingCardDto> = emptyList(),
    val animalFeed: List<ListingCardDto> = emptyList(),
    val compost: List<ListingCardDto> = emptyList(),
    val offerMerchants: List<MerchantDto> = emptyList(),
    val humanCount: Int = 0,
    val farmCount: Int = 0,
    val otherTabMatchCount: Int = 0,
)

/** One row of "Katalog saya" (`GET /me/merchant/listings`): no merchant, description or halal info. */
@Serializable
data class MyListingDto(
    val id: String,
    val title: String,
    val tier: String,
    val unit: String = "portion",
    val status: String = "active",
    val isFree: Boolean = false,
    val price: Int? = null,
    val priceOriginal: Int? = null,
    val stock: Int = 0,
    val soldCount: Int = 0,
    val pickupStart: String? = null,
    val pickupEnd: String,
    val coverUrl: String = "",
)

@Serializable
data class CreateListingRequest(
    val title: String,
    val description: String,
    val tier: String,
    val isFree: Boolean,
    val price: Int? = null,
    val priceOriginal: Int? = null,
    val stock: Int,
    val halal: String,
    val allergens: List<String>,
    val madeAt: String? = null,
    val storage: String,
    val safetyChecks: List<String>,
    val pickupHours: Int,
    val photoMediaIds: List<String>,
    val videoMediaId: String? = null,
)
