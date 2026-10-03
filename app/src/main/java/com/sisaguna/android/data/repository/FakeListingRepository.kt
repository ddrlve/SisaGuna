package com.sisaguna.android.data.repository

import com.sisaguna.android.data.model.Allergen
import com.sisaguna.android.data.model.Allergen.EGG
import com.sisaguna.android.data.model.Allergen.GLUTEN
import com.sisaguna.android.data.model.Allergen.MILK
import com.sisaguna.android.data.model.Allergen.PEANUT
import com.sisaguna.android.data.model.Allergen.SEAFOOD
import com.sisaguna.android.data.model.Allergen.SOY
import com.sisaguna.android.data.model.HalalStatus
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.ListingTier.ANIMAL_FEED
import com.sisaguna.android.data.model.ListingTier.COMPOST
import com.sisaguna.android.data.model.ListingTier.HUMAN
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.data.model.MerchantStatus
import com.sisaguna.android.data.model.SafetyCheck
import com.sisaguna.android.data.model.StorageMethod
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Bundled seed photo (assets/img). Coil loads `file:///android_asset/…` directly. */
fun seedImage(name: String) = "file:///android_asset/img/$name.jpg"

/**
 * Mock data standing in for supabase-kt until the Supabase project decision in
 * ANDROID_CLAUDE.md #1 is confirmed. Swap the Hilt binding in di/RepositoryModule.kt for a
 * real SupabaseListingRepository once that lands — HomeViewModel doesn't need to change.
 *
 * Listings live in a StateFlow so uploads ([addListing]) show up on Home, the store page, and
 * Katalog saya without a restart. Every photo matches its title (see assets/img/CREDITS.txt).
 */
@Singleton
class FakeListingRepository @Inject constructor() : ListingRepository {

    private val now = Instant.now()
    private fun hours(h: Double) = now.plus((h * 60).toLong(), ChronoUnit.MINUTES)
    private fun hours(h: Long) = now.plus(h, ChronoUnit.HOURS)

    private val allChecks = SafetyCheck.entries.toSet()
    private val requiredChecks = SafetyCheck.entries.filter { it.requiredForHumans }.toSet()

    // location is the short area name shown on cards ("Alam Sutera · 0.4 km" in the Figma
    // design), not a full address — that's what Merchant.location means on Home/Category cards.
    private val merchants = listOf(
        Merchant(
            ListingRepository.MY_MERCHANT_ID, "Dapur Budi", isVerified = true, status = MerchantStatus.APPROVED, location = "Tangerang",
            rating = 4.8, ratingCount = 23, bannerUrl = seedImage("banner_dapur"), photos = listOf(seedImage("kue_lapis"), seedImage("banner_dapur")),
            about = "Dapur rumahan yang sering kebanjiran pesanan arisan. Sisa yang masih layak dijual murah, sisanya jadi kompos.",
            address = "Jl. Sutera Onyx XII No.30, Tangerang", openHours = "08.00–20.00", halal = HalalStatus.UNVERIFIED,
            allergens = setOf(EGG, MILK, GLUTEN), prepMinutes = 10, distanceKm = 0.3, totalRescued = 41,
        ),
        Merchant(
            "m1", "Warung Bu Sari", isVerified = true, status = MerchantStatus.APPROVED, location = "Kemang",
            rating = 4.9, ratingCount = 212, bannerUrl = seedImage("banner_warung2"),
            photos = listOf(seedImage("banner_warung2"), seedImage("nasi_kuning"), seedImage("sayur_sop"), seedImage("pisang_goreng"), seedImage("banner_warung")),
            about = "Warung masakan rumahan sejak 2009. Tiap sore sisa lauk katering kantor kami bagikan murah lewat SisaGuna.",
            address = "Jl. Kemang Raya No. 12, Jakarta Selatan", openHours = "07.00–21.00", halal = HalalStatus.HALAL_CERTIFIED,
            allergens = setOf(EGG, PEANUT, SEAFOOD), todaysOffer = "Nasi kuning + sayur sop mulai Rp 4.000 sampai jam 21.00",
            prepMinutes = 10, distanceKm = 0.8, totalRescued = 1_284,
        ),
        Merchant(
            "m2", "Roti Segar Bakery", isVerified = true, status = MerchantStatus.APPROVED, location = "Blok M",
            rating = 4.7, ratingCount = 158, bannerUrl = seedImage("banner_bakery"),
            photos = listOf(seedImage("banner_bakery"), seedImage("donat"), seedImage("croissant"), seedImage("roti_tawar")),
            about = "Bakery kecil di Blok M. Roti yang lewat best before tapi masih lembut dijual 70% lebih murah.",
            address = "Jl. Melawai VI No. 3, Blok M", openHours = "06.00–20.00", halal = HalalStatus.HALAL_CERTIFIED,
            allergens = setOf(GLUTEN, MILK, EGG), todaysOffer = "Semua roti & donat diskon 70% setelah jam 18.00",
            prepMinutes = 5, distanceKm = 1.2, totalRescued = 932,
        ),
        Merchant(
            "m3", "Kantin Pak Budi", isVerified = false, status = MerchantStatus.APPROVED, location = "Cipete",
            rating = 4.5, ratingCount = 37, bannerUrl = seedImage("banner_kantin"),
            photos = listOf(seedImage("banner_kantin"), seedImage("nasi_box"), seedImage("ayam_goreng"), seedImage("bubur_ayam")),
            about = "Kantin kantor yang sering kelebihan nasi box rapat.",
            address = "Jl. Cipete Raya No. 8", openHours = "06.30–16.00", halal = HalalStatus.UNVERIFIED,
            allergens = setOf(EGG, SOY), prepMinutes = 15, distanceKm = 0.5, totalRescued = 210,
        ),
        Merchant(
            "m4", "Peternakan Hijau", isVerified = true, status = MerchantStatus.APPROVED, location = "Depok",
            rating = 4.8, ratingCount = 64, bannerUrl = seedImage("banner_farm"),
            photos = listOf(seedImage("banner_farm"), seedImage("sayur_pakan"), seedImage("ampas_tahu"), seedImage("dedak")),
            about = "Kami kumpulkan sisa sayur pasar dan ampas tahu untuk peternak kecil di Depok.",
            address = "Jl. Raya Sawangan No. 51, Depok", openHours = "05.00–17.00", halal = HalalStatus.OTHER,
            allergens = setOf(SOY), deliveryAvailable = true, prepMinutes = 20, distanceKm = 5.4, totalRescued = 3_400,
        ),
        Merchant(
            "m5", "Kompos Kita", isVerified = true, status = MerchantStatus.APPROVED, location = "Cilandak",
            rating = 4.6, ratingCount = 19, bannerUrl = seedImage("banner_kompos"),
            photos = listOf(seedImage("banner_kompos"), seedImage("kompos_sayur"), seedImage("ampas_kopi")),
            about = "Bank sampah organik. Bahan kompos sudah dipilah bebas plastik.",
            address = "Jl. Cilandak KKO No. 2", openHours = "08.00–16.00", halal = HalalStatus.OTHER,
            deliveryAvailable = false, prepMinutes = 15, distanceKm = 3.1, totalRescued = 5_120,
        ),
        Merchant(
            "m6", "Dessert Rara", isVerified = true, status = MerchantStatus.APPROVED, location = "Alam Sutera",
            rating = 4.9, ratingCount = 88, bannerUrl = seedImage("puding_cheesecake"),
            photos = listOf(seedImage("puding_cheesecake"), seedImage("kue_lapis")),
            about = "Puding cheesecake sistem PO. Bahan dibeli H-1 jadi selalu fresh; sisa PO dijual di sini sebelum lewat 2 hari.",
            address = "Jl. Alam Sutera Boulevard No. 21", openHours = "10.00–21.00", halal = HalalStatus.UNVERIFIED,
            allergens = setOf(MILK, EGG, GLUTEN), todaysOffer = "Sisa PO hari ini: beli 2 cup puding, hemat Rp 10.000",
            prepMinutes = 5, distanceKm = 0.4, totalRescued = 310,
        ),
        Merchant(
            "m7", "Bakmi Ahong", isVerified = true, status = MerchantStatus.APPROVED, location = "Kebon Jeruk",
            rating = 4.6, ratingCount = 120, bannerUrl = seedImage("mie_goreng"),
            photos = listOf(seedImage("mie_goreng")),
            about = "Bakmi dan mie goreng rumahan. Dapur kami non-halal.",
            address = "Jl. Kebon Jeruk Raya No. 77", openHours = "09.00–21.00", halal = HalalStatus.NON_HALAL,
            allergens = setOf(SOY, EGG, GLUTEN), prepMinutes = 10, distanceKm = 2.2, totalRescued = 540,
        ),
    )

    private fun km(merchantId: String) = merchants.first { it.id == merchantId }.distanceKm

    private fun human(
        id: String, merchantId: String, title: String, original: Int?, discounted: Int?, image: String,
        madeHoursAgo: Double, storage: StorageMethod, pickupEndIn: Double, description: String,
        stock: Int, allergens: Set<Allergen>, gallery: List<String> = emptyList(), sold: Int = 0,
        halal: HalalStatus = merchants.first { it.id == merchantId }.halal,
    ) = Listing(
        id, merchantId, title, HUMAN, original, discounted, isFree = discounted == null,
        pickupEnd = hours(pickupEndIn), imageUrl = seedImage(image), distanceKm = km(merchantId),
        description = description, stock = stock, pickupStart = hours(-0.5),
        gallery = gallery.map(::seedImage), halal = halal, allergens = allergens,
        madeAt = hours(-madeHoursAgo), storage = storage, safetyChecks = allChecks, soldCount = sold,
    )

    private fun byWeight(
        id: String, merchantId: String, title: String, tier: ListingTier, perKgOriginal: Int?, perKgPrice: Int?,
        image: String, pickupEndIn: Long, description: String, stockKg: Int, sold: Int = 0, gallery: List<String> = emptyList(),
        allergens: Set<Allergen> = emptySet(),
    ) = Listing(
        id, merchantId, title, tier, perKgOriginal, perKgPrice, isFree = perKgPrice == null,
        pickupEnd = hours(pickupEndIn), imageUrl = seedImage(image), distanceKm = km(merchantId),
        description = description, stock = stockKg, pickupStart = hours(-3L), gallery = gallery.map(::seedImage),
        halal = HalalStatus.OTHER, allergens = allergens, madeAt = hours(-5L), storage = StorageMethod.ROOM_TEMP,
        safetyChecks = requiredChecks, soldCount = sold,
    )

    private val _listings = MutableStateFlow(
        listOf(
            human(
                "l1", "m1", "Nasi Kuning Sisa Katering", 25000, 8000, "nasi_kuning", 1.5, StorageMethod.ROOM_TEMP, 2.0,
                "Nasi kuning lengkap dengan ayam suwir, telur balado, dan sambal dari pesanan katering kantor siang ini. Dimasak jam 10 pagi, disimpan tertutup. Paling enak dihangatkan sebentar sebelum dimakan.",
                6, setOf(EGG, PEANUT), gallery = listOf("banner_warung2"), sold = 48,
            ),
            human(
                "l2", "m2", "Roti Tawar Lewat Best Before", 18000, 5000, "roti_tawar", 26.0, StorageMethod.SHELF, 3.0,
                "Roti tawar gandum lewat tanggal best before 1 hari. Masih lembut dan aman dikonsumsi, cocok dipanggang untuk sarapan besok.",
                8, setOf(GLUTEN, MILK), gallery = listOf("banner_bakery"), sold = 61,
            ),
            human(
                "l3", "m3", "Nasi Box Rapat Berlebih", 20000, null, "nasi_box", 2.0, StorageMethod.ROOM_TEMP, 1.0,
                "Nasi box sisa rapat: nasi putih, ayam bakar, telur pindang, dan sambal. Gratis untuk yang bisa ambil sebelum kantin tutup.",
                4, setOf(EGG, SOY), sold = 22,
            ),
            human(
                "l4", "m1", "Sayur Sop Sisa Hari Ini", 15000, 4000, "sayur_sop", 1.0, StorageMethod.ROOM_TEMP, 2.5,
                "Sayur sop wortel, kentang, dan kol dengan kaldu ayam. Bawa wadah sendiri ya biar lebih ramah lingkungan.",
                5, emptySet(), sold = 30,
            ),
            human(
                "l5", "m2", "Donat Reject Bentuk", 12000, 3000, "donat", 6.0, StorageMethod.SHELF, 5.0,
                "Donat gula dan cokelat yang bentuknya kurang rapi. Rasa tetap sama dengan donat etalase. Per porsi isi 3 donat.",
                10, setOf(GLUTEN, MILK, EGG), gallery = listOf("banner_bakery"), sold = 95,
            ),
            human(
                "l10", ListingRepository.MY_MERCHANT_ID, "Kue Lapis Sisa Arisan", 30000, 10000, "kue_lapis", 10.0, StorageMethod.SHELF, 5.0,
                "Kue lapis legit sisa arisan keluarga, masih utuh setengah loyang. Dipotong rapi per porsi.",
                4, setOf(EGG, MILK, GLUTEN), sold = 12,
            ),
            human(
                "l12", "m6", "Puding Cheesecake Sisa PO", 25000, 12000, "puding_cheesecake", 20.0, StorageMethod.CHILLED, 6.0,
                "Puding cheesecake lembut dengan saus stroberi, sisa PO hari ini. Disimpan di kulkas sejak dibuat. Tekstur paling enak dalam 2 hari — habiskan hari ini.",
                6, setOf(MILK, EGG, GLUTEN), gallery = listOf("kue_lapis"), sold = 74,
            ),
            human(
                "l13", "m6", "Puding Cheesecake H+1 (Mini)", 15000, 5000, "puding_cheesecake", 38.0, StorageMethod.CHILLED, 4.0,
                "Cup mini dari PO kemarin. Masih aman karena disimpan di kulkas, tapi teksturnya sedikit lebih padat. Langsung dimakan ya.",
                4, setOf(MILK, EGG, GLUTEN), sold = 18,
            ),
            human(
                "l14", "m3", "Ayam Goreng Crispy", 22000, 9000, "ayam_goreng", 2.0, StorageMethod.ROOM_TEMP, 1.5,
                "Ayam goreng tepung renyah sisa makan siang kantin, dikemas paper wrap. Cocok dipanaskan pakai air fryer.",
                5, setOf(GLUTEN, EGG), gallery = listOf("banner_kantin"), sold = 57,
            ),
            human(
                "l15", "m2", "Croissant Isi Sayur", 28000, 9000, "croissant", 8.0, StorageMethod.SHELF, 4.0,
                "Croissant butter isi selada dan tomat dari etalase pagi. Masih renyah di luar.",
                6, setOf(GLUTEN, MILK, EGG), sold = 33,
            ),
            human(
                "l16", "m7", "Mie Goreng Spesial", 20000, 7000, "mie_goreng", 1.0, StorageMethod.ROOM_TEMP, 2.0,
                "Mie goreng kecap porsi besar sisa pesanan online yang dibatalkan. Dapur non-halal.",
                5, setOf(SOY, EGG, GLUTEN), sold = 40,
            ),
            human(
                "l17", "m3", "Bubur Ayam Komplit", 15000, 4000, "bubur_ayam", 1.0, StorageMethod.ROOM_TEMP, 2.0,
                "Bubur ayam dengan cakwe, daun bawang, dan kerupuk dipisah biar tetap renyah.",
                7, setOf(GLUTEN, SOY), sold = 26,
            ),
            human(
                "l18", "m1", "Pisang Goreng Kipas", 10000, 3000, "pisang_goreng", 2.0, StorageMethod.ROOM_TEMP, 1.5,
                "Pisang goreng tepung dari etalase sore, isi 4 potong per porsi.",
                8, setOf(GLUTEN), sold = 39,
            ),
            byWeight(
                "l6", "m4", "Sayur Layu Pasar untuk Pakan", ANIMAL_FEED, 5000, 2000, "sayur_pakan", 6,
                "Wortel, daun bawang, dan sayuran layu dari pasar pagi. Sudah dipilah dari plastik, cocok untuk pakan kambing, ayam, atau bebek.",
                25, sold = 210, gallery = listOf("banner_farm"),
            ),
            byWeight(
                "l7", "m4", "Ampas Tahu Segar", ANIMAL_FEED, 3000, null, "ampas_tahu", 2,
                "Ampas tahu hasil produksi pagi ini, masih segar. Bagus untuk campuran pakan sapi dan ayam. Bawa karung sendiri.",
                15, sold = 160, allergens = setOf(SOY),
            ),
            byWeight(
                "l19", "m2", "Remah Roti Kering untuk Pakan", ANIMAL_FEED, 4000, 1500, "roti_kering_pakan", 8,
                "Remah dan pinggiran roti yang sudah dikeringkan. Campuran pakan ayam dan ikan, bebas jamur.",
                6, sold = 44, allergens = setOf(GLUTEN),
            ),
            byWeight(
                "l20", "m4", "Dedak Padi Sisa Penggilingan", ANIMAL_FEED, 6000, 3000, "dedak", 10,
                "Dedak halus sisa penggilingan beras, kering dan bersih. Sumber energi untuk unggas dan sapi.",
                40, sold = 300,
            ),
            byWeight(
                "l8", "m5", "Sisa Sayur & Buah untuk Kompos", COMPOST, null, null, "kompos_sayur", 8,
                "Sisa kupasan dan sayur busuk yang tidak layak pakan. Langsung masuk komposter, sudah bebas plastik.",
                30, sold = 120,
            ),
            byWeight(
                "l9", "m5", "Ampas Kopi untuk Kompos", COMPOST, null, null, "ampas_kopi", 10,
                "Ampas kopi dari kedai mitra, kaya nitrogen untuk kompos dan media tanam.",
                12, sold = 85,
            ),
            byWeight(
                "l11", ListingRepository.MY_MERCHANT_ID, "Kulit Buah untuk Kompos", COMPOST, null, null, "kulit_buah", 9,
                "Kulit pisang, semangka, dan jeruk dari dapur rumah. Gratis untuk yang punya komposter.",
                6,
            ),
            byWeight(
                "l21", "m2", "Cangkang Telur Bakery", COMPOST, null, null, "cangkang_telur", 9,
                "Cangkang telur dari dapur bakery, sudah dibilas. Sumber kalsium untuk kompos dan tanaman.",
                3, sold = 20,
            ),
        ),
    )
    override val listings: StateFlow<List<Listing>> = _listings.asStateFlow()

    private fun visible() = _listings.value.filter { it.pickupEnd.isAfter(Instant.now()) && it.stock > 0 }

    override suspend fun getHomeFeed(focusTier: ListingTier): HomeFeed {
        delay(600) // simulate network round trip so the loading state is visible in preview

        val visible = visible()

        return HomeFeed(
            nearby = visible.filter { it.tier == HUMAN }.sortedBy { it.distanceKm ?: Double.MAX_VALUE }.take(8),
            deals = visible
                .filter { it.tier == HUMAN }
                .sortedByDescending { l -> if (l.isFree) 1.0 else 1.0 - (l.unitPrice.toDouble() / (l.priceOriginal ?: 1)) }
                .take(8),
            animalFeed = visible.filter { it.tier == ANIMAL_FEED }.take(8),
            compost = visible.filter { it.tier == COMPOST }.take(8),
            merchantsById = merchants.associateBy { it.id },
            popular = visible.filter { it.tier == HUMAN }.sortedByDescending { it.soldCount }.take(8),
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
            // "Orang juga memesan" — the store's other listings, best sellers first.
            moreFromMerchant = visible().filter { it.merchantId == merchant.id && it.id != listingId }.sortedByDescending { it.soldCount },
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
            if (it.id == listingId) it.copy(stock = (it.stock - by).coerceAtLeast(0), soldCount = it.soldCount + by) else it
        }
    }
}
