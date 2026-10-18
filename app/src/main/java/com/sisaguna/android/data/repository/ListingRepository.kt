package com.sisaguna.android.data.repository

import com.sisaguna.android.data.model.Allergen
import com.sisaguna.android.data.model.HalalStatus
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.data.model.SafetyCheck
import com.sisaguna.android.data.model.StorageMethod
import java.time.Instant
import java.util.UUID
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
    /** Best sellers across stores, for the "Paling laris" rail. */
    val popular: List<Listing> = emptyList(),
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
 * What the seller filled in at upload. Photos and the video are still local URIs: the repository
 * uploads them first. The server derives `pickup_end` from [pickupHours] and refuses food that is
 * past its safe window or has an incomplete checklist.
 */
data class ListingDraft(
    val title: String,
    val description: String,
    val tier: ListingTier,
    val isFree: Boolean,
    val price: Int?,
    val priceOriginal: Int?,
    val stock: Int,
    val halal: HalalStatus,
    val allergens: Set<Allergen>,
    val madeAt: Instant?,
    val storage: StorageMethod,
    val safetyChecks: Set<SafetyCheck>,
    val pickupHours: Int,
    val photoUris: List<String>,
    val videoUri: String?,
)

/** What a new partner fills in to open a store; it stays `pending` until an admin approves it. */
data class StoreDraft(
    val name: String,
    val about: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val locationLabel: String,
    val openHours: String,
    val halal: HalalStatus,
    val allergens: Set<Allergen>,
    val prepMinutes: Int,
)

/** Everything the buyer and seller screens read about food and stores. */
interface ListingRepository {
    /** Cache of every listing seen so far (feeds, details, the user's own catalog), newest data wins. */
    val listings: StateFlow<List<Listing>>

    /** The signed-in user's own store; null for guests and for accounts that haven't opened one. */
    val myMerchant: StateFlow<Merchant?>

    /** [maxKm] limits every rail to that distance from the user's location (null = no limit). */
    suspend fun getHomeFeed(focusTier: ListingTier = ListingTier.HUMAN, maxKm: Double? = null): HomeFeed
    suspend fun getListingsByTier(tier: ListingTier): CategoryFeed

    /** Stores with food to pick up now, plus the user's saved stores. */
    suspend fun getMerchants(): List<Merchant>

    /** Store from the cache filled by every feed/detail call; null if it hasn't been seen yet. */
    fun merchant(id: String): Merchant?

    /** Null when [merchantId] doesn't exist. */
    suspend fun getMerchantFeed(merchantId: String): MerchantFeed?

    /** Null when [listingId] doesn't exist. */
    suspend fun getListingDetail(listingId: String): ListingDetail?

    /** The user's own catalog, including sold-out and expired rows. */
    suspend fun getMyListings(): List<Listing>

    /** Opens the user's store (status `pending` until approved) and makes it [myMerchant]. */
    suspend fun createStore(draft: StoreDraft): Merchant

    /**
     * [idempotencyKey] identifies one publish intent: retrying after a timeout with the same key can
     * not post the listing twice. (Photos are uploaded again on a retry; the server drops orphans after 24 h.)
     */
    suspend fun createListing(draft: ListingDraft, idempotencyKey: String = UUID.randomUUID().toString()): Listing
    suspend fun deleteListing(listingId: String)

    companion object {
        /** Id of the demo store in [FakeListingRepository]; the real id comes from [myMerchant]. */
        const val MY_MERCHANT_ID = "m0"
    }
}

val ListingRepository.myMerchantId: String? get() = myMerchant.value?.id
