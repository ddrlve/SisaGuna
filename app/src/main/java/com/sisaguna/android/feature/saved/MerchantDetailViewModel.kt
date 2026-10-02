package com.sisaguna.android.feature.saved

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.data.repository.CartRepository
import com.sisaguna.android.data.repository.ListingRepository
import com.sisaguna.android.feature.listing.cartTotal
import com.sisaguna.android.data.repository.MerchantFeed
import com.sisaguna.android.data.repository.SavedMerchantRepository
import com.sisaguna.android.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MerchantDetailUiState {
    data object Loading : MerchantDetailUiState
    data object NotFound : MerchantDetailUiState
    data class Success(
        val merchant: Merchant,
        val isSaved: Boolean,
        val query: String,
        val tierFilter: ListingTier?,
        val listings: List<Listing>,
        val totalListings: Int,
        val distanceKm: Double?,
        val now: Instant,
        val cartCount: Int = 0,
        val cartTotal: Int = 0,
    ) : MerchantDetailUiState
}

private data class DetailFilters(val query: String = "", val tier: ListingTier? = null)

@HiltViewModel
class MerchantDetailViewModel @Inject constructor(
    private val listingRepository: ListingRepository,
    private val savedRepository: SavedMerchantRepository,
    private val cartRepository: CartRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val merchantId: String = savedStateHandle.get<String>(Screen.MerchantDetail.ARG_ID).orEmpty()

    // null = still loading; Result.failure = not found.
    private val feed = MutableStateFlow<Result<MerchantFeed>?>(null)
    private val filters = MutableStateFlow(DetailFilters())

    private val base = combine(feed, filters, savedRepository.savedIds) { result, f, ids ->
            when {
                result == null -> MerchantDetailUiState.Loading
                result.isFailure -> MerchantDetailUiState.NotFound
                else -> {
                    val data = result.getOrThrow()
                    MerchantDetailUiState.Success(
                        merchant = data.merchant,
                        isSaved = data.merchant.id in ids,
                        query = f.query,
                        tierFilter = f.tier,
                        listings = data.listings.filter { l ->
                            (f.tier == null || l.tier == f.tier) &&
                                (f.query.isBlank() || l.title.contains(f.query.trim(), ignoreCase = true))
                        },
                        totalListings = data.listings.size,
                        distanceKm = data.listings.mapNotNull { it.distanceKm }.minOrNull(),
                        now = Instant.now(),
                    )
                }
            }
        }

    val uiState: StateFlow<MerchantDetailUiState> =
        combine(base, cartRepository.cart, listingRepository.listings) { state, cart, all ->
            if (state is MerchantDetailUiState.Success) {
                state.copy(cartCount = cart.itemCount, cartTotal = cartTotal(cart, all))
            } else {
                state
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, MerchantDetailUiState.Loading)

    init {
        viewModelScope.launch {
            val data = listingRepository.getMerchantFeed(merchantId)
            feed.value = if (data != null) Result.success(data) else Result.failure(NoSuchElementException(merchantId))
        }
    }

    fun onQueryChange(query: String) {
        filters.value = filters.value.copy(query = query)
    }

    /** Selecting the active chip again clears the filter (none selected = all tiers). */
    fun onTierToggle(tier: ListingTier) {
        val current = filters.value
        filters.value = current.copy(tier = if (current.tier == tier) null else tier)
    }

    /** Returns true when the merchant is now saved. */
    fun toggleSave(): Boolean {
        return if (merchantId in savedRepository.savedIds.value) {
            savedRepository.unsave(merchantId)
            false
        } else {
            savedRepository.restore(merchantId)
            true
        }
    }
}
