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
    /** Units left, counted in [unit] (portions, or kilograms for feed/compost). */
    val stock: Int = 5,
    val pickupStart: Instant? = null,
    /** Extra photos after [imageUrl] for the detail carousel. */
    val gallery: List<String> = emptyList(),
    val unit: QuantityUnit = if (tier == ListingTier.HUMAN) QuantityUnit.PORTION else QuantityUnit.KILOGRAM,
    val halal: HalalStatus = HalalStatus.UNVERIFIED,
    val allergens: Set<Allergen> = emptySet(),
    /** When it was cooked / produced — drives the food-safety window. */
    val madeAt: Instant? = null,
    val storage: StorageMethod = StorageMethod.ROOM_TEMP,
    val safetyChecks: Set<SafetyCheck> = emptySet(),
    /** Optional ≤30s clip recorded at upload, shown on the detail page. */
    val videoUrl: String? = null,
    val soldCount: Int = 0,
) {
    /** What one unit costs at checkout: 0 when free. */
    val unitPrice: Int get() = if (isFree) 0 else priceDiscounted ?: priceOriginal ?: 0

    /** What one unit would cost at normal retail price, for the "you saved" line. */
    val unitOriginalPrice: Int get() = priceOriginal ?: unitPrice

    val photos: List<String> get() = (listOf(imageUrl) + gallery).filter { it.isNotBlank() }

    val discountPercent: Int
        get() = when {
            isFree -> 100
            priceOriginal == null || priceOriginal == 0 -> 0
            else -> 100 - (unitPrice * 100 / priceOriginal)
        }
}
