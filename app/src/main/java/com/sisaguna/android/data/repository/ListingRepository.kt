package com.sisaguna.android.data.repository

import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.Merchant
import kotlinx.coroutines.flow.StateFlow

/**
 * Aggregated data the Home feed needs in one round trip: rails split by tier/distance/price,
 * plus the merchants those listings belong to (looked up by [Listing.merchantId]).
 */
data class HomeFeed(
    val nearby: List<Listing>,
    val deals: List<Listing>,
    val animalFeed: List<Listing>,
    val compost: List<Listing>,
    val merchantsById: Map<String, Merchant>,
)

/** Everything Category List needs for one tier: the matching listings plus the merchants they
 * belong to (looked up by [Listing.merchantId]). */
data class CategoryFeed(
    val listings: List<Listing>,
    val merchantsById: Map<String, Merchant>,
)

/** One merchant plus its currently visible listings (store page / Saved → merchant). */
data class MerchantFeed(
    val merchant: Merchant,
    val listings: List<Listing>,
)

/** Listing detail page: the listing, its merchant, and the merchant's other listings so the
 * user can add more to the same pickup. */
data class ListingDetail(
    val listing: Listing,
    val merchant: Merchant,
    val moreFromMerchant: List<Listing>,
)

/**
 * Composable never calls Supabase directly (see ANDROID_CLAUDE.md) — everything goes through
 * this interface. [SupabaseListingRepository] (Supabase-backed) will implement this once the
 * project/table decision is confirmed; [FakeListingRepository] is the only impl for now.
 */
interface ListingRepository {
    /** Every listing, including expired and sold-out ones (Katalog saya shows those too). */
    val listings: StateFlow<List<Listing>>

    suspend fun getHomeFeed(focusTier: ListingTier = ListingTier.HUMAN): HomeFeed
    suspend fun getListingsByTier(tier: ListingTier): CategoryFeed
    suspend fun getMerchants(): List<Merchant>
    fun merchant(id: String): Merchant?

    /** Every listing whose pickup window hasn't ended and that still has stock. */
    suspend fun getVisibleListings(): List<Listing>

    /** Null when [merchantId] doesn't exist. */
    suspend fun getMerchantFeed(merchantId: String): MerchantFeed?

    /** Null when [listingId] doesn't exist. */
    suspend fun getListingDetail(listingId: String): ListingDetail?

    fun addListing(listing: Listing)
    fun deleteListing(listingId: String)
    fun reduceStock(listingId: String, by: Int)

    companion object {
        /** The signed-in user's own store — where uploads land. */
        const val MY_MERCHANT_ID = "m0"
    }
}
