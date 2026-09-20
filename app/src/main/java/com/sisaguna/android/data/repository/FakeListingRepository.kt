package com.sisaguna.android.data.repository

import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.data.model.MerchantStatus
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock data standing in for supabase-kt until the Supabase project decision in
 * ANDROID_CLAUDE.md #1 is confirmed. Swap the Hilt binding in di/RepositoryModule.kt for a
 * real SupabaseListingRepository once that lands — HomeViewModel doesn't need to change.
 */
@Singleton
class FakeListingRepository @Inject constructor() : ListingRepository {

    private val now = Instant.now()

    // location is the short area name shown on cards ("Alam Sutera · 0.4 km" in the Figma
    // design), not a full address — that's what Merchant.location means on Home/Category cards.
    private val merchants = listOf(
        Merchant("m1", "Warung Bu Sari", isVerified = true, status = MerchantStatus.APPROVED, location = "Kemang"),
        Merchant("m2", "Roti Segar Bakery", isVerified = true, status = MerchantStatus.APPROVED, location = "Blok M"),
        Merchant("m3", "Kantin Pak Budi", isVerified = false, status = MerchantStatus.APPROVED, location = "Cipete"),
        Merchant("m4", "Peternakan Hijau", isVerified = true, status = MerchantStatus.APPROVED, location = "Depok"),
        Merchant("m5", "Kompos Kita", isVerified = true, status = MerchantStatus.APPROVED, location = "Cilandak"),
    )

    private val listings = listOf(
        Listing("l1", "m1", "Nasi Kuning Sisa Katering", ListingTier.HUMAN, 25000, 8000, false, now.plus(2, ChronoUnit.HOURS), "", 0.8),
        Listing("l2", "m2", "Roti Tawar Lewat Best Before", ListingTier.HUMAN, 18000, 5000, false, now.plus(3, ChronoUnit.HOURS), "", 1.2),
        Listing("l3", "m3", "Nasi Box Rapat Berlebih", ListingTier.HUMAN, 20000, null, true, now.plus(1, ChronoUnit.HOURS), "", 0.5),
        Listing("l4", "m1", "Sayur Sop Sisa Hari Ini", ListingTier.HUMAN, 15000, 4000, false, now.plus(4, ChronoUnit.HOURS), "", 0.8),
        Listing("l5", "m2", "Donat Reject Bentuk", ListingTier.HUMAN, 12000, 3000, false, now.plus(5, ChronoUnit.HOURS), "", 1.2),
        Listing("l6", "m4", "Sisa Sayur untuk Pakan Ternak", ListingTier.ANIMAL_FEED, 10000, 2000, false, now.plus(6, ChronoUnit.HOURS), "", 5.4),
        Listing("l7", "m4", "Ampas Tahu Segar", ListingTier.ANIMAL_FEED, null, null, true, now.plus(2, ChronoUnit.HOURS), "", 5.4),
        Listing("l8", "m5", "Sisa Sayur untuk Kompos", ListingTier.COMPOST, null, null, true, now.plus(8, ChronoUnit.HOURS), "", 3.1),
        Listing("l9", "m5", "Ampas Kopi untuk Kompos", ListingTier.COMPOST, null, null, true, now.plus(10, ChronoUnit.HOURS), "", 3.1),
    )

    override suspend fun getHomeFeed(focusTier: ListingTier): HomeFeed {
        delay(600) // simulate network round trip so the loading state is visible in preview

        val visible = listings.filter { it.pickupEnd.isAfter(now) }

        return HomeFeed(
            nearby = visible.sortedBy { it.distanceKm ?: Double.MAX_VALUE }.take(5),
            deals = visible
                .filter { it.tier == ListingTier.HUMAN && !it.isFree && it.priceDiscounted != null }
                .sortedByDescending { l -> 1.0 - (l.priceDiscounted!!.toDouble() / (l.priceOriginal ?: 1)) }
                .take(5),
            animalFeed = visible.filter { it.tier == ListingTier.ANIMAL_FEED }.take(5),
            compost = visible.filter { it.tier == ListingTier.COMPOST }.take(5),
            merchantsById = merchants.associateBy { it.id },
        )
    }
}
