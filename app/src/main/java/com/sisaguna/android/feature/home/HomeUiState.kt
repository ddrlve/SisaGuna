package com.sisaguna.android.feature.home

import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.Merchant
import java.time.Instant

/** A listing paired with the merchant it belongs to, resolved once so cards don't look it up. */
data class HomeListingUi(
    val listing: Listing,
    val merchant: Merchant,
)

/** Home's segmented switch (spec §2, variant B chosen on device). */
enum class HomeTab { SIAP_SANTAP, TERNAK_KOMPOS }

enum class HomeSort(val label: String) {
    RELEVANT("Paling relevan"),
    NEAREST("Terdekat"),
    CHEAPEST("Termurah"),
    BIGGEST_DISCOUNT("Diskon terbesar"),
    ENDING_SOON("Segera berakhir"),
}

/** Search filter sheet state. [maxDistanceKm] null = any distance. */
data class HomeFilter(
    val sort: HomeSort = HomeSort.RELEVANT,
    val freeOnly: Boolean = false,
    val maxDistanceKm: Double? = null,
) {
    val activeCount: Int
        get() = listOf(sort != HomeSort.RELEVANT, freeOnly, maxDistanceKm != null).count { it }
}

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Error(val message: String) : HomeUiState

    data class Success(
        val searchQuery: String = "",
        val nearby: List<HomeListingUi> = emptyList(),
        val deals: List<HomeListingUi> = emptyList(),
        val animalFeed: List<HomeListingUi> = emptyList(),
        val compost: List<HomeListingUi> = emptyList(),
        val selectedTab: HomeTab = HomeTab.SIAP_SANTAP,
        /** > 0 only while searching, when the active tab has no match but the other one does. */
        val otherTabMatchCount: Int = 0,
        val filter: HomeFilter = HomeFilter(),
        val now: Instant = Instant.now(),
    ) : HomeUiState {
        val humanIsEmpty: Boolean get() = nearby.isEmpty() && deals.isEmpty()
        val farmIsEmpty: Boolean get() = animalFeed.isEmpty() && compost.isEmpty()
        val farmCount: Int get() = animalFeed.size + compost.size
        val activeTabIsEmpty: Boolean
            get() = if (selectedTab == HomeTab.SIAP_SANTAP) humanIsEmpty else farmIsEmpty
    }
}
