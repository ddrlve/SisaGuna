package com.sisaguna.android.feature.listing

import com.sisaguna.android.ui.i18n.l

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sisaguna.android.data.model.Cart
import com.sisaguna.android.data.remote.AppException
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.repository.AddToCartResult
import com.sisaguna.android.data.repository.CartRepository
import com.sisaguna.android.data.repository.ListingDetail
import com.sisaguna.android.data.repository.ListingRepository
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
import com.sisaguna.android.data.model.Review
import com.sisaguna.android.data.repository.ReviewRepository

sealed interface ListingDetailUiState {
    data object Loading : ListingDetailUiState
    data object NotFound : ListingDetailUiState
    data class Success(
        val detail: ListingDetail,
        val quantity: Int,
        val inCart: Int,
        val cart: Cart,
        val cartTotal: Int,
        val now: Instant,
        val reviews: List<Review> = emptyList(),
    ) : ListingDetailUiState {
        val listing: Listing get() = detail.listing
        val isExpired: Boolean get() = !listing.pickupEnd.isAfter(now)
        val isSoldOut: Boolean get() = listing.stock - inCart <= 0
        val canAdd: Boolean get() = !isExpired && !isSoldOut
        val maxQuantity: Int get() = (listing.stock - inCart).coerceAtLeast(1)
    }
}

/** Returned to the screen after "Tambah ke keranjang" so it can confirm or ask to replace. */
sealed interface AddOutcome {
    data class Added(val quantity: Int) : AddOutcome
    data class NeedsReplace(val currentMerchantName: String) : AddOutcome
}

/** Sum of unit prices × quantities for what's in [cart]; lines whose listing vanished count 0. */
fun cartTotal(cart: Cart, listings: List<Listing>): Int {
    val byId = listings.associateBy { it.id }
    return cart.quantities.entries.sumOf { (id, q) -> (byId[id]?.unitPrice ?: 0) * q }
}

@HiltViewModel
class ListingDetailViewModel @Inject constructor(
    private val listingRepository: ListingRepository,
    private val cartRepository: CartRepository,
    savedStateHandle: SavedStateHandle,
    private val reviewRepository: ReviewRepository,
) : ViewModel() {

    private val listingId: String = savedStateHandle.get<String>(Screen.ListingDetail.ARG_ID).orEmpty()
    private val detail = MutableStateFlow<Result<ListingDetail>?>(null)
    private val quantity = MutableStateFlow(1)

    val uiState: StateFlow<ListingDetailUiState> =
        combine(detail, quantity, cartRepository.cart, listingRepository.listings, reviewRepository.reviews) { result, qty, cart, all, _ ->
            when {
                result == null -> ListingDetailUiState.Loading
                result.isFailure -> ListingDetailUiState.NotFound
                else -> {
                    val d = result.getOrThrow()
                    val inCart = cart.quantities[d.listing.id] ?: 0
                    val max = (d.listing.stock - inCart).coerceAtLeast(1)
                    ListingDetailUiState.Success(
                        d, qty.coerceIn(1, max), inCart, cart, cartTotal(cart, all), Instant.now(),
                        reviews = reviewRepository.forMerchant(d.merchant.id),
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, ListingDetailUiState.Loading)

    init {
        viewModelScope.launch {
            detail.value = try {
                listingRepository.getListingDetail(listingId)?.let { Result.success(it) }
                    ?: Result.failure(NoSuchElementException(listingId))
            } catch (e: AppException) {
                Result.failure(e)
            }
            detail.value?.getOrNull()?.let { d ->
                try {
                    reviewRepository.load(d.merchant.id)
                } catch (e: AppException) {
                    // Reviews are a nicety on this page; the listing itself already loaded.
                }
            }
        }
    }

    fun setQuantity(value: Int) {
        quantity.value = value.coerceAtLeast(1)
    }

    fun addToCart(): AddOutcome? {
        val s = uiState.value as? ListingDetailUiState.Success ?: return null
        if (!s.canAdd) return null
        return when (val result = cartRepository.add(s.listing, s.quantity)) {
            AddToCartResult.Added -> {
                quantity.value = 1
                AddOutcome.Added(s.quantity)
            }
            is AddToCartResult.DifferentMerchant -> AddOutcome.NeedsReplace(
                listingRepository.merchant(result.currentMerchantId)?.name ?: l("toko lain", "another store"),
            )
        }
    }

    fun replaceCart() {
        val s = uiState.value as? ListingDetailUiState.Success ?: return
        cartRepository.replace(s.listing, s.quantity)
        quantity.value = 1
    }
}
