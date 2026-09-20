package com.sisaguna.android.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.data.repository.HomeFeed
import com.sisaguna.android.data.repository.ListingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Loads the Home feed through [ListingRepository] (never Supabase directly, per
 * ANDROID_CLAUDE.md) and exposes it as a single [HomeUiState] the screen renders 1:1.
 *
 * State handling:
 * - Loading: shown on first load and on [retry].
 * - Empty: [HomeUiState.Success.isEmpty] — same Success shape, zero items in every rail.
 * - Error: any thrown exception (e.g. lost connection mid-fetch) surfaces here with a retry
 *   action; nothing partially renders.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ListingRepository,
) : ViewModel() {

    // Phase 0 always focuses the HUMAN tier feed (ANDROID_CLAUDE.md); the tier chips on this
    // screen are entry points into Category List (filtered by tier), not a Home refetch —
    // Category List isn't built yet, so HomeScreen accepts the callback and does nothing with
    // it for now.
    private var rawFeed: HomeFeed? = null

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeFeed()
    }

    fun retry() = loadHomeFeed()

    fun onSearchQueryChange(query: String) {
        val feed = rawFeed ?: return
        _uiState.value = feed.toUiState(query)
    }

    private fun loadHomeFeed() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val feed = repository.getHomeFeed(ListingTier.HUMAN)
                rawFeed = feed
                val previousQuery = (_uiState.value as? HomeUiState.Success)?.searchQuery.orEmpty()
                _uiState.value = feed.toUiState(previousQuery)
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(
                    e.message ?: "Gagal memuat data. Periksa koneksi internet dan coba lagi.",
                )
            }
        }
    }

    private fun HomeFeed.toUiState(query: String): HomeUiState.Success {
        val matches: (Listing) -> Boolean = { query.isBlank() || it.title.contains(query, ignoreCase = true) }
        return HomeUiState.Success(
            searchQuery = query,
            nearby = nearby.filter(matches).toUi(merchantsById),
            deals = deals.filter(matches).toUi(merchantsById),
            animalFeed = animalFeed.filter(matches).toUi(merchantsById),
            compost = compost.filter(matches).toUi(merchantsById),
            now = Instant.now(),
        )
    }

    private fun List<Listing>.toUi(merchants: Map<String, Merchant>) = map { listing ->
        HomeListingUi(listing, merchants.getValue(listing.merchantId))
    }
}
