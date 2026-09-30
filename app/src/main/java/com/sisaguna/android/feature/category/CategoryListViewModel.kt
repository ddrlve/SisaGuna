package com.sisaguna.android.feature.category

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

    fun onToggleNearest() = applyFilters { it.copy(nearestFirst = !it.nearestFirst) }

    fun onToggleFree() = applyFilters { it.copy(freeOnly = !it.freeOnly) }

    private fun applyFilters(mutate: (CategoryListUiState.Success) -> CategoryListUiState.Success) {
        val current = _uiState.value as? CategoryListUiState.Success ?: return
        val feed = rawFeed ?: return
        val next = mutate(current)
        _uiState.value = feed.toUiState(next.tier, next.searchQuery, next.nearestFirst, next.freeOnly)
    }

    private fun load(tier: ListingTier) {
        viewModelScope.launch {
            _uiState.value = CategoryListUiState.Loading
            try {
                val feed = repository.getListingsByTier(tier)
                rawFeed = feed
                _uiState.value = feed.toUiState(tier, searchQuery = "", nearestFirst = false, freeOnly = false)
            } catch (e: Exception) {
                _uiState.value = CategoryListUiState.Error(
                    e.message ?: "Gagal memuat data. Periksa koneksi internet dan coba lagi.",
                )
            }
        }
    }

    private fun CategoryFeed.toUiState(
        tier: ListingTier,
        searchQuery: String,
        nearestFirst: Boolean,
        freeOnly: Boolean,
    ): CategoryListUiState.Success {
        var filtered = listings.filter { it.tier == tier }
        if (searchQuery.isNotBlank()) filtered = filtered.filter { it.title.contains(searchQuery, ignoreCase = true) }
        if (freeOnly) filtered = filtered.filter { it.isFree }
        if (nearestFirst) filtered = filtered.sortedBy { it.distanceKm ?: Double.MAX_VALUE }

        return CategoryListUiState.Success(
            tier = tier,
            searchQuery = searchQuery,
            nearestFirst = nearestFirst,
            freeOnly = freeOnly,
            listings = filtered.map { CategoryListingUi(it, merchantsById.getValue(it.merchantId)) },
            now = Instant.now(),
        )
    }
}
