package com.sisaguna.android.testutil

import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.data.model.MerchantStatus
import com.sisaguna.android.data.repository.CategoryFeed
import com.sisaguna.android.data.repository.HomeFeed
import com.sisaguna.android.data.repository.ListingRepository
import com.sisaguna.android.data.repository.ListingDetail
import com.sisaguna.android.data.repository.MerchantFeed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Small, delay-free listing data for ViewModel tests. */
class TestListingRepository : ListingRepository {

    private val later = Instant.now().plus(3, ChronoUnit.HOURS)

    val merchants = listOf(
        Merchant("m1", "Warung Bu Sari", true, MerchantStatus.APPROVED, "Kemang", 4.9),
        Merchant("m2", "Roti Segar", true, MerchantStatus.APPROVED, "Blok M", null),
        Merchant("m4", "Peternakan Hijau", true, MerchantStatus.APPROVED, "Depok", 4.8),
        Merchant("m5", "Kompos Kita", false, MerchantStatus.APPROVED, "Cilandak", null),
    )

    private val _listings = MutableStateFlow(listOf(
        Listing("l1", "m1", "Nasi Kuning", ListingTier.HUMAN, 25000, 8000, false, later, "", 0.8),
        Listing("l2", "m1", "Ampas Tahu", ListingTier.ANIMAL_FEED, null, null, true, later, "", 0.8),
        Listing("l3", "m1", "Sisa Sayur Kompos", ListingTier.COMPOST, null, null, true, later, "", 0.8),
        Listing("l4", "m2", "Roti Tawar", ListingTier.HUMAN, 18000, 5000, false, later, "", 1.2),
        Listing("l5", "m4", "Dedak Padi", ListingTier.ANIMAL_FEED, null, null, true, later, "", 5.4),
    ))
    override val listings: StateFlow<List<Listing>> = _listings
    private val current get() = _listings.value

    override suspend fun getHomeFeed(focusTier: ListingTier) = HomeFeed(
        nearby = current.filter { it.tier == ListingTier.HUMAN },
        deals = emptyList(),
        animalFeed = current.filter { it.tier == ListingTier.ANIMAL_FEED },
        compost = current.filter { it.tier == ListingTier.COMPOST },
        merchantsById = merchants.associateBy { it.id },
    )

    override suspend fun getListingsByTier(tier: ListingTier) =
        CategoryFeed(current.filter { it.tier == tier }, merchants.associateBy { it.id })

    override suspend fun getMerchants() = merchants

    override suspend fun getVisibleListings() = current

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

    override fun addListing(listing: Listing) {
        _listings.value = listOf(listing) + current
    }

    override fun deleteListing(listingId: String) {
        _listings.value = current.filterNot { it.id == listingId }
    }

    override fun reduceStock(listingId: String, by: Int) {
        _listings.value = current.map { if (it.id == listingId) it.copy(stock = it.stock - by) else it }
    }
}
