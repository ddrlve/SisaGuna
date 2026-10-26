package com.sisaguna.android.testutil

import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.data.model.MerchantStatus
import com.sisaguna.android.data.repository.CategoryFeed
import com.sisaguna.android.data.repository.HomeFeed
import com.sisaguna.android.data.repository.ListingDraft
import com.sisaguna.android.data.repository.StoreDraft
import com.sisaguna.android.data.repository.ListingRepository
import com.sisaguna.android.data.repository.ListingDetail
import com.sisaguna.android.data.repository.MerchantFeed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Small, delay-free listing data for ViewModel tests. */
class TestListingRepository : ListingRepository, StockTracking {

    private val later = Instant.now().plus(3, ChronoUnit.HOURS)

    val merchants = listOf(
        Merchant("m1", "Warung Bu Sari", true, MerchantStatus.APPROVED, "Kemang", 4.9, distanceKm = 0.8, availableCount = 3, nextPickupEnd = later),
        Merchant("m2", "Roti Segar", true, MerchantStatus.APPROVED, "Blok M", null, distanceKm = 1.2, availableCount = 1, nextPickupEnd = later),
        Merchant("m4", "Peternakan Hijau", true, MerchantStatus.APPROVED, "Depok", 4.8, distanceKm = 5.4, availableCount = 1, nextPickupEnd = later),
        Merchant("m5", "Kompos Kita", false, MerchantStatus.APPROVED, "Cilandak", null),
    )

    override val myMerchant: StateFlow<Merchant?> =
        MutableStateFlow(Merchant(ListingRepository.MY_MERCHANT_ID, "Dapur Tes", true, MerchantStatus.APPROVED, "Tangerang"))

    /** The draft behind the last [createListing], for assertions. */
    var lastDraft: ListingDraft? = null
    var lastKey: String? = null

    fun add(listing: Listing) {
        _listings.value = listOf(listing) + current
    }

    private val _listings = MutableStateFlow(listOf(
        Listing("l1", "m1", "Nasi Kuning", ListingTier.HUMAN, 25000, 8000, false, later, "", 0.8),
        Listing("l2", "m1", "Ampas Tahu", ListingTier.ANIMAL_FEED, null, null, true, later, "", 0.8),
        Listing("l3", "m1", "Sisa Sayur Kompos", ListingTier.COMPOST, null, null, true, later, "", 0.8),
        Listing("l4", "m2", "Roti Tawar", ListingTier.HUMAN, 18000, 5000, false, later, "", 1.2),
        Listing("l5", "m4", "Dedak Padi", ListingTier.ANIMAL_FEED, null, null, true, later, "", 5.4),
    ))
    override val listings: StateFlow<List<Listing>> = _listings
    private val current get() = _listings.value

    var lastHomeMaxKm: Double? = null
    var homeLoads = 0

    override suspend fun getHomeFeed(focusTier: ListingTier, maxKm: Double?): HomeFeed {
        lastHomeMaxKm = maxKm
        homeLoads++
        return buildHome()
    }

    private fun buildHome() = HomeFeed(
        nearby = current.filter { it.tier == ListingTier.HUMAN },
        deals = emptyList(),
        animalFeed = current.filter { it.tier == ListingTier.ANIMAL_FEED },
        compost = current.filter { it.tier == ListingTier.COMPOST },
        merchantsById = merchants.associateBy { it.id },
    )

    override suspend fun getListingsByTier(tier: ListingTier) =
        CategoryFeed(current.filter { it.tier == tier }, merchants.associateBy { it.id })

    override suspend fun getMerchants() = merchants

    override suspend fun getMerchantFeed(merchantId: String): MerchantFeed? {
        val merchant = merchants.firstOrNull { it.id == merchantId } ?: return null
        return MerchantFeed(merchant, current.filter { it.merchantId == merchantId })
    }

    override fun merchant(id: String) = merchants.firstOrNull { it.id == id }

    override suspend fun getListingDetail(listingId: String): ListingDetail? {
        val listing = current.firstOrNull { it.id == listingId } ?: return null
        val merchant = merchant(listing.merchantId) ?: return null
        return ListingDetail(listing, merchant, current.filter { it.merchantId == merchant.id && it.id != listingId })
    }

    override suspend fun createStore(draft: StoreDraft) =
        Merchant("m-new", draft.name, false, MerchantStatus.PENDING, draft.locationLabel)

    override suspend fun getMyListings() = current.filter { it.merchantId == ListingRepository.MY_MERCHANT_ID }

    override suspend fun createListing(draft: ListingDraft, idempotencyKey: String): Listing {
        lastKey = idempotencyKey
        lastDraft = draft
        val listing = Listing(
            "u-test", ListingRepository.MY_MERCHANT_ID, draft.title, draft.tier, draft.priceOriginal, draft.price,
            draft.isFree, later, draft.photoUris.firstOrNull().orEmpty(), null, stock = draft.stock,
        )
        add(listing)
        return listing
    }

    override suspend fun deleteListing(listingId: String) {
        _listings.value = current.filterNot { it.id == listingId }
    }

    override fun reduceStock(listingId: String, by: Int) {
        _listings.value = current.map { if (it.id == listingId) it.copy(stock = it.stock - by) else it }
    }
}
