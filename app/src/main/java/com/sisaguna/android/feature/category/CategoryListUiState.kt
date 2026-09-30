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

sealed interface CategoryListUiState {
    data object Loading : CategoryListUiState

    data class Error(val message: String) : CategoryListUiState

    data class Success(
        val tier: ListingTier,
        val searchQuery: String = "",
        val nearestFirst: Boolean = false,
        val freeOnly: Boolean = false,
        val listings: List<CategoryListingUi> = emptyList(),
        val now: Instant = Instant.now(),
    ) : CategoryListUiState {
        val isEmpty: Boolean
            get() = listings.isEmpty()
    }
}
