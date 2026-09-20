package com.sisaguna.android.data.model

import java.time.Instant

enum class ListingTier { HUMAN, ANIMAL_FEED, COMPOST }

data class Listing(
    val id: String,
    val merchantId: String,
    val title: String,
    val tier: ListingTier,
    val priceOriginal: Int?, // null kalau gratis
    val priceDiscounted: Int?, // null kalau gratis
    val isFree: Boolean,
    val pickupEnd: Instant, // listing lewat waktu ini tidak tampil di feed
    val imageUrl: String,
    val distanceKm: Double?,
)
