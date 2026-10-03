package com.sisaguna.android.data.repository

import com.sisaguna.android.data.model.Review
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ReviewRepository {
    /** Newest first, across all stores. */
    val reviews: StateFlow<List<Review>>
    fun forMerchant(merchantId: String): List<Review> = reviews.value.filter { it.merchantId == merchantId }
    fun add(review: Review)
}

@Singleton
class FakeReviewRepository @Inject constructor() : ReviewRepository {
    private val now = Instant.now()
    private fun daysAgo(d: Long) = now.minus(d, ChronoUnit.DAYS)

    private val _reviews = MutableStateFlow(
        listOf(
            Review("r1", "m1", "Dinda P.", 5, "Nasi kuningnya masih hangat pas diambil, porsinya banyak banget. Bu Sari ramah!", daysAgo(1), listOf(seedImage("nasi_kuning")), "Nasi Kuning Sisa Katering", listOf("Makanan enak", "Porsi pas")),
            Review("r2", "m1", "Rizky A.", 5, "Sayur sopnya seger, kuahnya dipisah jadi nggak tumpah.", daysAgo(3), listOf(seedImage("sayur_sop")), "Sayur Sop Sisa Hari Ini", listOf("Kemasan rapi")),
            Review("r3", "m1", "Sekar W.", 4, "Enak, cuma antrinya agak lama pas jam pulang kantor.", daysAgo(6), emptyList(), "Pisang Goreng Kipas"),
            Review("r4", "m2", "Andi K.", 5, "Donatnya masih empuk, beda tipis sama yang di etalase. Mantap buat sarapan anak-anak.", daysAgo(2), listOf(seedImage("donat")), "Donat Reject Bentuk", listOf("Makanan enak", "Hemat")),
            Review("r5", "m2", "Maya L.", 4, "Roti tawarnya masih oke dipanggang. Croissant agak lembek tapi wajar.", daysAgo(5), listOf(seedImage("roti_tawar"), seedImage("croissant")), "Roti Tawar Lewat Best Before"),
            Review("r6", "m3", "Fajar H.", 4, "Nasi box gratis dan masih layak, terima kasih kantin!", daysAgo(2), listOf(seedImage("nasi_box")), "Nasi Box Rapat Berlebih"),
            Review("r7", "m3", "Nadia S.", 3, "Ayamnya enak tapi agak dingin. Lebih baik dihangatkan dulu.", daysAgo(8), emptyList(), "Ayam Goreng Crispy"),
            Review("r8", "m4", "Pak Darto", 5, "Sayurnya sudah bersih dari plastik, kambing saya lahap. Langganan tiap minggu.", daysAgo(1), listOf(seedImage("sayur_pakan")), "Sayur Layu Pasar untuk Pakan", listOf("Bersih")),
            Review("r9", "m4", "Bu Wati", 5, "Ampas tahu masih segar, nggak bau asam.", daysAgo(4), listOf(seedImage("ampas_tahu")), "Ampas Tahu Segar"),
            Review("r10", "m5", "Komunitas Kebun Kita", 5, "Ampas kopinya banyak dan kering. Cocok buat campuran kompos.", daysAgo(3), listOf(seedImage("ampas_kopi")), "Ampas Kopi untuk Kompos"),
            Review("r11", "m6", "Clara T.", 5, "Pudingnya lembut banget, saus stroberinya seger. Masih dingin pas diambil.", daysAgo(1), listOf(seedImage("puding_cheesecake")), "Puding Cheesecake Sisa PO", listOf("Makanan enak", "Masih fresh")),
            Review("r12", "m6", "Bayu R.", 4, "Yang H+1 teksturnya memang lebih padat, tapi rasanya tetap enak. Jujur di deskripsi, jadi nggak kaget.", daysAgo(2), emptyList(), "Puding Cheesecake H+1 (Mini)"),
            Review("r13", "m7", "Kevin S.", 5, "Mie gorengnya porsi kuli, harga cuma 7 ribu.", daysAgo(2), listOf(seedImage("mie_goreng")), "Mie Goreng Spesial"),
            Review("r14", ListingRepository.MY_MERCHANT_ID, "Tante Lina", 5, "Kue lapisnya legit, persis buatan rumah.", daysAgo(9), listOf(seedImage("kue_lapis")), "Kue Lapis Sisa Arisan"),
        ),
    )
    override val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

    override fun add(review: Review) {
        _reviews.value = listOf(review) + _reviews.value
    }
}
