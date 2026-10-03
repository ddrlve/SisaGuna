package com.sisaguna.android.feature.category

import com.sisaguna.android.data.model.Courier
import com.sisaguna.android.data.model.DeliveryPricing
import com.sisaguna.android.data.model.DeliverySpeed
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.Merchant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.repository.CategoryFeed
import com.sisaguna.android.data.repository.ListingRepository
import com.sisaguna.android.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Loads one tier's listings through [ListingRepository] (never Supabase directly, per
 * ANDROID_CLAUDE.md). Switching tier re-fetches, matching how a real backend query by tier
 * would behave; search/nearest/free are re-applied client-side against the last fetch.
 */
@HiltViewModel
class CategoryListViewModel @Inject constructor(
    private val repository: ListingRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private var rawFeed: CategoryFeed? = null

    private val _uiState = MutableStateFlow<CategoryListUiState>(CategoryListUiState.Loading)
    val uiState: StateFlow<CategoryListUiState> = _uiState.asStateFlow()

    init {
        val initialTier = savedStateHandle.get<String>(Screen.CategoryList.ARG_TIER)
            ?.let { runCatching { ListingTier.valueOf(it) }.getOrNull() }
            ?: ListingTier.HUMAN
        load(initialTier)
    }

    fun onTierChange(tier: ListingTier) = load(tier)

    fun onSearchQueryChange(query: String) = applyFilters { it.copy(searchQuery = query) }

    fun onSortChange(sort: CategorySort) = applyFilters { it.copy(sort = sort) }

    fun onToggleFree() = applyFilters { it.copy(freeOnly = !it.freeOnly) }

    private fun applyFilters(mutate: (CategoryListUiState.Success) -> CategoryListUiState.Success) {
        val current = _uiState.value as? CategoryListUiState.Success ?: return
        val feed = rawFeed ?: return
        val next = mutate(current)
        _uiState.value = feed.toUiState(next.tier, next.searchQuery, next.sort, next.freeOnly)
    }

    private fun load(tier: ListingTier) {
        viewModelScope.launch {
            _uiState.value = CategoryListUiState.Loading
            try {
                val feed = repository.getListingsByTier(tier)
                rawFeed = feed
                val keepSort = (_uiState.value as? CategoryListUiState.Success)?.sort ?: CategorySort.RECOMMENDED
                _uiState.value = feed.toUiState(tier, searchQuery = "", sort = keepSort, freeOnly = false)
            } catch (e: Exception) {
                _uiState.value = CategoryListUiState.Error(
                    e.message ?: "Gagal memuat data. Periksa koneksi internet dan coba lagi.",
                )
            }
        }
    }

    private fun sorted(list: List<Listing>, sort: CategorySort, merchants: Map<String, Merchant>): List<Listing> {
        fun quote(l: Listing) = DeliveryPricing.quote(
            Courier.GOSEND, DeliverySpeed.STANDARD, l.distanceKm ?: 5.0, l.unitPrice, merchants[l.merchantId]?.prepMinutes ?: 15,
        )
        return when (sort) {
            // Blend of what buyers told us matters: good stores, real savings, close by, popular.
            CategorySort.RECOMMENDED -> list.sortedByDescending { l ->
                val rating = merchants[l.merchantId]?.rating ?: 4.0
                rating * 20 + l.discountPercent * 0.5 + l.soldCount * 0.2 - (l.distanceKm ?: 5.0) * 4
            }
            CategorySort.RATING -> list.sortedByDescending { merchants[it.merchantId]?.rating ?: 0.0 }
            CategorySort.DELIVERY_FEE -> list.sortedBy { quote(it).payable }
            CategorySort.DELIVERY_TIME -> list.sortedBy { quote(it).etaMinMinutes }
            CategorySort.NEAREST -> list.sortedBy { it.distanceKm ?: Double.MAX_VALUE }
            CategorySort.PRICE_LOW -> list.sortedBy { it.unitPrice }
            CategorySort.DISCOUNT -> list.sortedByDescending { it.discountPercent }
            CategorySort.ENDING_SOON -> list.sortedBy { it.pickupEnd }
        }
    }

    private fun CategoryFeed.toUiState(
        tier: ListingTier,
        searchQuery: String,
        sort: CategorySort,
        freeOnly: Boolean,
    ): CategoryListUiState.Success {
        var filtered = listings.filter { it.tier == tier }
        if (searchQuery.isNotBlank()) filtered = filtered.filter { it.title.contains(searchQuery, ignoreCase = true) }
        if (freeOnly) filtered = filtered.filter { it.isFree }
        filtered = sorted(filtered, sort, merchantsById)

        return CategoryListUiState.Success(
            tier = tier,
            searchQuery = searchQuery,
            sort = sort,
            freeOnly = freeOnly,
            listings = filtered.map { CategoryListingUi(it, merchantsById.getValue(it.merchantId)) },
            now = Instant.now(),
        )
    }
}
