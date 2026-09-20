package com.sisaguna.android.data.repository

import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.Merchant

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

/**
 * Composable never calls Supabase directly (see ANDROID_CLAUDE.md) — everything goes through
 * this interface. [SupabaseListingRepository] (Supabase-backed) will implement this once the
 * project/table decision is confirmed; [FakeListingRepository] is the only impl for now.
 */
interface ListingRepository {
    suspend fun getHomeFeed(focusTier: ListingTier = ListingTier.HUMAN): HomeFeed
}
