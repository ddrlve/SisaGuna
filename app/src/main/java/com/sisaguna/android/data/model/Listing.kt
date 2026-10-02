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
    val description: String = "",
    val stock: Int = 5,
    val pickupStart: Instant? = null,
) {
    /** What one unit costs at checkout: 0 when free. */
    val unitPrice: Int get() = if (isFree) 0 else priceDiscounted ?: priceOriginal ?: 0

    /** What one unit would cost at normal retail price, for the "you saved" line. */
    val unitOriginalPrice: Int get() = priceOriginal ?: unitPrice
}
