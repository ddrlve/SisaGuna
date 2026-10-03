package com.sisaguna.android.feature.category

import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.Merchant
import java.time.Instant

/** A listing paired with the merchant it belongs to, resolved once so cards don't look it up. */
data class CategoryListingUi(
    val listing: Listing,
    val merchant: Merchant,
)

/** "Lihat semua" sort options. Delivery fee/time use the cheapest standard courier quote
 * ([com.sisaguna.android.data.model.DeliveryPricing]) for each store's distance. */
enum class CategorySort(val label: String, val labelEn: String) {
    RECOMMENDED("Rekomendasi", "Recommended"),
    RATING("Rating tertinggi", "Top rated"),
    DELIVERY_FEE("Ongkir termurah", "Lowest delivery fee"),
    DELIVERY_TIME("Paling cepat sampai", "Fastest delivery"),
    NEAREST("Terdekat", "Nearest"),
    PRICE_LOW("Harga termurah", "Lowest price"),
    DISCOUNT("Diskon terbesar", "Biggest discount"),
    ENDING_SOON("Segera berakhir", "Ending soon"),
}

sealed interface CategoryListUiState {
    data object Loading : CategoryListUiState

    data class Error(val message: String) : CategoryListUiState

    data class Success(
        val tier: ListingTier,
        val searchQuery: String = "",
        val sort: CategorySort = CategorySort.RECOMMENDED,
        val freeOnly: Boolean = false,
        val listings: List<CategoryListingUi> = emptyList(),
        val now: Instant = Instant.now(),
    ) : CategoryListUiState {
        val isEmpty: Boolean
            get() = listings.isEmpty()
        val nearestFirst: Boolean get() = sort == CategorySort.NEAREST
    }
}
