package com.sisaguna.android.feature.home


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.data.repository.HomeFeed
import com.sisaguna.android.data.repository.ListingRepository
import com.sisaguna.android.data.repository.NotificationRepository
import com.sisaguna.android.data.repository.Voucher
import com.sisaguna.android.data.repository.VoucherRepository
import kotlinx.coroutines.flow.combine
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Loads the Home feed through [ListingRepository] (never Supabase directly, per
 * ANDROID_CLAUDE.md) and exposes it as a single [HomeUiState] the screen renders 1:1.
 *
 * State handling:
 * - Loading: shown on first load and on [retry].
 * - Empty: per tab, via [HomeUiState.Success.activeTabIsEmpty].
 * - Error: any thrown exception (e.g. lost connection mid-fetch) surfaces here with a retry
 *   action; nothing partially renders.
 *
 * The selected tab and search query live here rather than in the composable so they survive
 * opening Category List and coming back, and a [retry].
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ListingRepository,
    notificationRepository: NotificationRepository,
    private val voucherRepository: VoucherRepository,
    cartRepository: com.sisaguna.android.data.repository.CartRepository,
    private val settingsRepository: com.sisaguna.android.data.settings.AppSettingsRepository,
) : ViewModel() {

    /** Buyer vs merchant home (user testing asked for them to be separate). */
    val mode: StateFlow<com.sisaguna.android.data.settings.UserMode> = settingsRepository.settings
        .map { it.mode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, settingsRepository.settings.value.mode)

    fun setMode(mode: com.sisaguna.android.data.settings.UserMode) = settingsRepository.setMode(mode)

    /** Persistent cart bar on Home — null while the cart is empty. */
    val cartSummary: StateFlow<CartSummary?> = combine(cartRepository.cart, repository.listings) { cart, listings ->
        if (cart.isEmpty) return@combine null
        val byId = listings.associateBy { it.id }
        CartSummary(
            itemCount = cart.itemCount,
            total = cart.quantities.entries.sumOf { (id, q) -> (byId[id]?.unitPrice ?: 0) * q },
            merchantName = cart.merchantId?.let { repository.merchant(it)?.name },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private var rawFeed: HomeFeed? = null
    private var query: String = ""
    private var tab: HomeTab = HomeTab.SIAP_SANTAP
    private var filter: HomeFilter = HomeFilter()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Drives the bell badge. */
    val unreadNotifications: StateFlow<Int> = notificationRepository.notifications
        .map { list -> list.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        loadHomeFeed()
    }

    fun retry() = loadHomeFeed()

    fun onSearchQueryChange(query: String) {
        this.query = query
        render()
    }

    fun onTabSelected(tab: HomeTab) {
        this.tab = tab
        render()
    }

    fun onFilterChange(filter: HomeFilter) {
        this.filter = filter
        render()
    }

    /** Vouchers for the Home strip, with whether each is already claimed. */
    val vouchers: StateFlow<List<Pair<Voucher, Boolean>>> =
        combine(voucherRepository.vouchers, voucherRepository.claimed) { all, claimed ->
            all.map { it to (it.code in claimed) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun claimVoucher(code: String) = voucherRepository.claim(code)

    private fun render() {
        val feed = rawFeed ?: return
        _uiState.value = feed.toUiState()
    }

    private fun loadHomeFeed() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                rawFeed = repository.getHomeFeed(ListingTier.HUMAN)
                render()
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(
                    e.message ?: "Gagal memuat data. Periksa koneksi internet dan coba lagi.",
                )
            }
        }
    }

    private fun HomeFeed.toUiState(): HomeUiState.Success {
        val matches: (Listing) -> Boolean = { l ->
            (query.isBlank() || l.title.contains(query.trim(), ignoreCase = true)) &&
                (!filter.freeOnly || l.isFree) &&
                (filter.maxDistanceKm == null || (l.distanceKm ?: Double.MAX_VALUE) <= filter.maxDistanceKm!!)
        }
        val nearbyHits = nearby.filter(matches).sortedForFilter()
        val dealsHits = deals.filter(matches).sortedForFilter()
        val animalHits = animalFeed.filter(matches).sortedForFilter()
        val compostHits = compost.filter(matches).sortedForFilter()

        val humanCount = (nearbyHits + dealsHits).distinctBy { it.id }.size
        val farmCount = (animalHits + compostHits).distinctBy { it.id }.size
        val activeCount = if (tab == HomeTab.SIAP_SANTAP) humanCount else farmCount
        val otherCount = if (tab == HomeTab.SIAP_SANTAP) farmCount else humanCount

        return HomeUiState.Success(
            searchQuery = query,
            nearby = nearbyHits.toUi(merchantsById),
            deals = dealsHits.toUi(merchantsById),
            animalFeed = animalHits.toUi(merchantsById),
            compost = compostHits.toUi(merchantsById),
            popular = popular.filter(matches).toUi(merchantsById),
            offerMerchants = merchantsById.values.filter { it.todaysOffer != null && it.id != ListingRepository.MY_MERCHANT_ID }.sortedBy { it.distanceKm },
            selectedTab = tab,
            otherTabMatchCount = if ((query.isNotBlank() || filter.activeCount > 0) && activeCount == 0) otherCount else 0,
            filter = filter,
            now = Instant.now(),
        )
    }

    private fun List<Listing>.sortedForFilter(): List<Listing> = when (filter.sort) {
        HomeSort.RELEVANT -> this
        HomeSort.NEAREST -> sortedBy { it.distanceKm ?: Double.MAX_VALUE }
        HomeSort.CHEAPEST -> sortedBy { it.unitPrice }
        HomeSort.BIGGEST_DISCOUNT -> sortedByDescending { l ->
            if (l.isFree) 1.0 else 1.0 - l.unitPrice.toDouble() / (l.priceOriginal ?: l.unitPrice).coerceAtLeast(1)
        }
        HomeSort.ENDING_SOON -> sortedBy { it.pickupEnd }
    }

    private fun List<Listing>.toUi(merchants: Map<String, Merchant>) = map { listing ->
        HomeListingUi(listing, merchants.getValue(listing.merchantId))
    }
}
