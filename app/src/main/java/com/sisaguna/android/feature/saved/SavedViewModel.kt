package com.sisaguna.android.feature.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.data.repository.ListingRepository
import com.sisaguna.android.data.repository.SavedMerchantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** [distanceKm] is the nearest of the merchant's listings, or null if it has none (spec §3).
 * [availableCount]/[nextPickupEnd] power the "3 tersedia · ambil s/d 19.00" chips. */
data class SavedMerchantUi(
    val merchant: Merchant,
    val distanceKm: Double?,
    val availableCount: Int = 0,
    val nextPickupEnd: Instant? = null,
)

sealed interface SavedUiState {
    data object Loading : SavedUiState
    data class Success(
        val merchants: List<SavedMerchantUi>,
        /** Unsaved merchants with food right now — the way to save new ones. */
        val suggestions: List<SavedMerchantUi> = emptyList(),
    ) : SavedUiState {
        val availableTotal: Int get() = merchants.sumOf { it.availableCount }
    }
}

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val listingRepository: ListingRepository,
    private val savedRepository: SavedMerchantRepository,
) : ViewModel() {

    private val catalog = MutableStateFlow<Pair<List<Merchant>, List<Listing>>?>(null)

    val uiState: StateFlow<SavedUiState> = combine(catalog, savedRepository.savedIds) { data, ids ->
        if (data == null) return@combine SavedUiState.Loading
        val (merchants, listings) = data
        val byId = merchants.associateBy { it.id }
        fun ui(merchant: Merchant): SavedMerchantUi {
            val own = listings.filter { it.merchantId == merchant.id }
            return SavedMerchantUi(
                merchant = merchant,
                distanceKm = own.mapNotNull { it.distanceKm }.minOrNull(),
                availableCount = own.size,
                nextPickupEnd = own.minOfOrNull { it.pickupEnd },
            )
        }
        SavedUiState.Success(
            merchants = ids.mapNotNull { id -> byId[id]?.let(::ui) },
            suggestions = merchants
                .filter { it.id !in ids && it.id != ListingRepository.MY_MERCHANT_ID }
                .map(::ui)
                .filter { it.availableCount > 0 },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SavedUiState.Loading)

    init {
        viewModelScope.launch {
            catalog.value = listingRepository.getMerchants() to listingRepository.getVisibleListings()
        }
    }

    fun unsave(id: String) = savedRepository.unsave(id)

    fun undo(id: String) = savedRepository.restore(id)

    /** Saving a suggestion appends it (restore() appends ids it never removed). */
    fun save(id: String) = savedRepository.restore(id)
}
