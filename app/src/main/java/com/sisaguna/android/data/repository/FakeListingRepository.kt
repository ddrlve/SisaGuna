package com.sisaguna.android.data.repository

import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.data.model.MerchantStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock data standing in for supabase-kt until the Supabase project decision in
 * ANDROID_CLAUDE.md #1 is confirmed. Swap the Hilt binding in di/RepositoryModule.kt for a
 * real SupabaseListingRepository once that lands — HomeViewModel doesn't need to change.
 *
 * Listings live in a StateFlow so uploads ([addListing]) show up on Home, the store page, and
 * Katalog saya without a restart.
 */
@Singleton
class FakeListingRepository @Inject constructor() : ListingRepository {

    private val now = Instant.now()

    // location is the short area name shown on cards ("Alam Sutera · 0.4 km" in the Figma
    // design), not a full address — that's what Merchant.location means on Home/Category cards.
    private val merchants = listOf(
        Merchant(ListingRepository.MY_MERCHANT_ID, "Dapur Budi", isVerified = true, status = MerchantStatus.APPROVED, location = "Tangerang", rating = 4.8),
        Merchant("m1", "Warung Bu Sari", isVerified = true, status = MerchantStatus.APPROVED, location = "Kemang", rating = 4.9),
        Merchant("m2", "Roti Segar Bakery", isVerified = true, status = MerchantStatus.APPROVED, location = "Blok M", rating = 4.7),
        Merchant("m3", "Kantin Pak Budi", isVerified = false, status = MerchantStatus.APPROVED, location = "Cipete", rating = 4.5),
        Merchant("m4", "Peternakan Hijau", isVerified = true, status = MerchantStatus.APPROVED, location = "Depok", rating = 4.8),
        Merchant("m5", "Kompos Kita", isVerified = true, status = MerchantStatus.APPROVED, location = "Cilandak"),
    )

    private fun hours(h: Long) = now.plus(h, ChronoUnit.HOURS)

    private val _listings = MutableStateFlow(
        listOf(
            Listing(
                "l1", "m1", "Nasi Kuning Sisa Katering", ListingTier.HUMAN, 25000, 8000, false, hours(2), "", 0.8,
                description = "Nasi kuning lengkap dengan ayam suwir, telur balado, dan sambal dari pesanan katering kantor siang ini. Dimasak jam 10 pagi, disimpan tertutup. Paling enak dihangatkan sebentar sebelum dimakan.",
                stock = 6, pickupStart = hours(-1),
            ),
            Listing(
                "l2", "m2", "Roti Tawar Lewat Best Before", ListingTier.HUMAN, 18000, 5000, false, hours(3), "", 1.2,
                description = "Roti tawar gandum lewat tanggal best before 1 hari. Masih lembut dan aman dikonsumsi, cocok dipanggang untuk sarapan besok.",
                stock = 8, pickupStart = hours(-2),
            ),
            Listing(
                "l3", "m3", "Nasi Box Rapat Berlebih", ListingTier.HUMAN, 20000, null, true, hours(1), "", 0.5,
                description = "Nasi box sisa rapat: nasi putih, ayam goreng, tumis buncis, dan kerupuk. Gratis untuk yang bisa ambil sebelum kantin tutup.",
                stock = 4, pickupStart = hours(-1),
            ),
            Listing(
                "l4", "m1", "Sayur Sop Sisa Hari Ini", ListingTier.HUMAN, 15000, 4000, false, hours(4), "", 0.8,
                description = "Sayur sop wortel, kentang, dan kol dengan kaldu ayam. Bawa wadah sendiri ya biar lebih ramah lingkungan.",
                stock = 5, pickupStart = hours(-1),
            ),
            Listing(
                "l5", "m2", "Donat Reject Bentuk", ListingTier.HUMAN, 12000, 3000, false, hours(5), "", 1.2,
                description = "Donat gula dan cokelat yang bentuknya kurang rapi. Rasa tetap sama dengan donat etalase. Per porsi isi 3 donat.",
                stock = 10, pickupStart = hours(0),
            ),
            Listing(
                "l6", "m4", "Sisa Sayur untuk Pakan Ternak", ListingTier.ANIMAL_FEED, 10000, 2000, false, hours(6), "", 5.4,
                description = "Sayuran layu dan potongan sayur dari pasar pagi. Sudah dipilah dari plastik, cocok untuk pakan kambing, ayam, atau bebek. Per porsi sekitar 5 kg.",
                stock = 7, pickupStart = hours(-3),
            ),
            Listing(
                "l7", "m4", "Ampas Tahu Segar", ListingTier.ANIMAL_FEED, null, null, true, hours(2), "", 5.4,
                description = "Ampas tahu hasil produksi pagi ini, masih segar. Bagus untuk campuran pakan sapi dan ayam. Bawa karung sendiri.",
                stock = 3, pickupStart = hours(-2),
            ),
            Listing(
                "l8", "m5", "Sisa Sayur untuk Kompos", ListingTier.COMPOST, null, null, true, hours(8), "", 3.1,
                description = "Sisa kupasan dan sayur busuk yang tidak layak pakan. Langsung masuk komposter, sudah bebas plastik.",
                stock = 12, pickupStart = hours(-4),
            ),
            Listing(
                "l9", "m5", "Ampas Kopi untuk Kompos", ListingTier.COMPOST, null, null, true, hours(10), "", 3.1,
                description = "Ampas kopi dari kedai mitra, kaya nitrogen untuk kompos dan media tanam. Per porsi sekitar 2 kg.",
                stock = 9, pickupStart = hours(-4),
            ),
            Listing(
                "l10", ListingRepository.MY_MERCHANT_ID, "Kue Lapis Sisa Arisan", ListingTier.HUMAN, 30000, 10000, false, hours(5), "", 0.3,
                description = "Kue lapis legit sisa arisan keluarga, masih utuh setengah loyang. Dipotong rapi per porsi.",
                stock = 4, pickupStart = hours(0),
            ),
            Listing(
                "l11", ListingRepository.MY_MERCHANT_ID, "Kulit Buah untuk Kompos", ListingTier.COMPOST, null, null, true, hours(9), "", 0.3,
                description = "Kulit pisang, semangka, dan jeruk dari dapur rumah. Gratis untuk yang punya komposter.",
                stock = 6, pickupStart = hours(0),
            ),
        ),
    )
    override val listings: StateFlow<List<Listing>> = _listings.asStateFlow()

    private fun visible() = _listings.value.filter { it.pickupEnd.isAfter(Instant.now()) && it.stock > 0 }

    override suspend fun getHomeFeed(focusTier: ListingTier): HomeFeed {
        delay(600) // simulate network round trip so the loading state is visible in preview

        val visible = visible()

        return HomeFeed(
            nearby = visible.filter { it.tier == ListingTier.HUMAN }.sortedBy { it.distanceKm ?: Double.MAX_VALUE }.take(6),
            deals = visible
                .filter { it.tier == ListingTier.HUMAN }
                .sortedByDescending { l -> if (l.isFree) 1.0 else 1.0 - (l.unitPrice.toDouble() / (l.priceOriginal ?: 1)) }
                .take(6),
            animalFeed = visible.filter { it.tier == ListingTier.ANIMAL_FEED }.take(6),
            compost = visible.filter { it.tier == ListingTier.COMPOST }.take(6),
            merchantsById = merchants.associateBy { it.id },
        )
    }

    override suspend fun getListingsByTier(tier: ListingTier): CategoryFeed {
        delay(600) // simulate network round trip so the loading state is visible in preview

        return CategoryFeed(
            listings = visible().filter { it.tier == tier },
            merchantsById = merchants.associateBy { it.id },
        )
    }

    override suspend fun getMerchants(): List<Merchant> {
        delay(300)
        return merchants
    }

    override fun merchant(id: String): Merchant? = merchants.firstOrNull { it.id == id }

    override suspend fun getVisibleListings(): List<Listing> {
        delay(300)
        return visible()
    }

    override suspend fun getMerchantFeed(merchantId: String): MerchantFeed? {
        delay(400)
        val merchant = merchant(merchantId) ?: return null
        return MerchantFeed(merchant = merchant, listings = visible().filter { it.merchantId == merchantId })
    }

    override suspend fun getListingDetail(listingId: String): ListingDetail? {
        delay(350)
        val listing = _listings.value.firstOrNull { it.id == listingId } ?: return null
        val merchant = merchant(listing.merchantId) ?: return null
        return ListingDetail(
            listing = listing,
            merchant = merchant,
            moreFromMerchant = visible().filter { it.merchantId == merchant.id && it.id != listingId },
        )
    }

    override fun addListing(listing: Listing) {
        _listings.value = listOf(listing) + _listings.value.filterNot { it.id == listing.id }
    }

    override fun deleteListing(listingId: String) {
        _listings.value = _listings.value.filterNot { it.id == listingId }
    }

    override fun reduceStock(listingId: String, by: Int) {
        _listings.value = _listings.value.map {
            if (it.id == listingId) it.copy(stock = (it.stock - by).coerceAtLeast(0)) else it
        }
    }
}
