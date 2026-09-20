package com.sisaguna.android.feature.home

import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.Merchant
import java.time.Instant

/** A listing paired with the merchant it belongs to, resolved once so cards don't look it up. */
data class HomeListingUi(
    val listing: Listing,
    val merchant: Merchant,
)

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Error(val message: String) : HomeUiState

    data class Success(
        val searchQuery: String = "",
        val nearby: List<HomeListingUi> = emptyList(),
        val deals: List<HomeListingUi> = emptyList(),
        val animalFeed: List<HomeListingUi> = emptyList(),
        val compost: List<HomeListingUi> = emptyList(),
        val now: Instant = Instant.now(),
    ) : HomeUiState {
        val isEmpty: Boolean
            get() = nearby.isEmpty() && deals.isEmpty() && animalFeed.isEmpty() && compost.isEmpty()
    }
}
