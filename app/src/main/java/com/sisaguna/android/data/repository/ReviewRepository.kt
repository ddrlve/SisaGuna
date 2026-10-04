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
            Review("r3", "m1", "Sekar W.", 4, "Enak, cuma antrinya agak lama pas jam pulang kantor.", daysAgo(6), emptyList(), "Pisang Goreng Tepung"),
            Review("r4", "m2", "Andi K.", 5, "Donatnya masih empuk, beda tipis sama yang di etalase. Mantap buat sarapan anak-anak.", daysAgo(2), listOf(seedImage("donat")), "Donat Glaze Sisa Etalase", listOf("Makanan enak", "Hemat")),
            Review("r5", "m2", "Maya L.", 4, "Roti gandumnya masih oke diiris lalu dipanggang. Croissant agak lembek tapi wajar.", daysAgo(5), listOf(seedImage("roti_tawar"), seedImage("croissant")), "Roti Gandum Lewat Best Before"),
            Review("r6", "m3", "Fajar H.", 4, "Nasi box gratis dan masih layak, terima kasih kantin!", daysAgo(2), listOf(seedImage("nasi_box")), "Nasi Box Rapat Berlebih"),
            Review("r7", "m3", "Nadia S.", 3, "Ayamnya enak tapi agak dingin. Lebih baik dihangatkan dulu.", daysAgo(8), emptyList(), "Ayam Goreng Crispy"),
            Review("r8", "m4", "Pak Darto", 5, "Sayurnya sudah bersih dari plastik, kambing saya lahap. Langganan tiap minggu.", daysAgo(1), listOf(seedImage("sayur_pakan")), "Sayur Layu Pasar untuk Pakan", listOf("Bersih")),
            Review("r9", "m4", "Bu Wati", 5, "Ampas tahu masih segar, nggak bau asam.", daysAgo(4), listOf(seedImage("ampas_tahu")), "Ampas Tahu Segar"),
            Review("r10", "m5", "Komunitas Kebun Kita", 5, "Ampas kopinya banyak dan kering. Cocok buat campuran kompos.", daysAgo(3), listOf(seedImage("ampas_kopi")), "Ampas Kopi untuk Kompos"),
            Review("r11", "m6", "Clara T.", 5, "Pudingnya lembut banget, saus stroberinya seger. Masih dingin pas diambil.", daysAgo(1), listOf(seedImage("puding_cheesecake")), "Puding Cheesecake Sisa PO", listOf("Makanan enak", "Masih fresh")),
            Review("r12", "m6", "Bayu R.", 4, "Yang H+1 teksturnya memang lebih padat, tapi rasanya tetap enak. Jujur di deskripsi, jadi nggak kaget.", daysAgo(2), emptyList(), "Puding Cheesecake H+1 (Mini)"),
            Review("r13", "m7", "Kevin S.", 5, "Mie gorengnya porsi kuli, harga cuma 7 ribu.", daysAgo(2), listOf(seedImage("mie_goreng")), "Mie Goreng Spesial"),
            Review("r14", ListingRepository.MY_MERCHANT_ID, "Tante Lina", 5, "Kue lapisnya legit, persis buatan rumah.", daysAgo(9), listOf(seedImage("kue_lapis")), "Kue Lapis Sisa Arisan"),

            // More per store, so every store page has a believable spread of stars.
            Review("r15", ListingRepository.MY_MERCHANT_ID, "Rizky A.", 5, "Potongannya rapi dan masih wangi mentega. Murah banget untuk lapis legit.", daysAgo(3), emptyList(), "Kue Lapis Sisa Arisan", listOf("Makanan enak", "Hemat")),
            Review("r16", ListingRepository.MY_MERCHANT_ID, "Kebun Warga RW 04", 5, "Kulit buahnya sudah dipilah, tinggal masuk komposter.", daysAgo(5), listOf(seedImage("kulit_buah")), "Kulit Buah untuk Kompos", listOf("Bersih")),
            Review("r17", ListingRepository.MY_MERCHANT_ID, "Dinda P.", 4, "Enak, cuma saya datang agak telat jadi tinggal sedikit.", daysAgo(12), emptyList(), "Kue Lapis Sisa Arisan"),

            Review("r18", "m1", "Hendra G.", 5, "Gado-gadonya bumbu kacangnya kental, porsinya kenyang.", daysAgo(2), listOf(seedImage("gado_gado")), "Gado-gado Sisa Katering", listOf("Porsi banyak")),
            Review("r19", "m1", "Lestari M.", 5, "Pisang gorengnya masih renyah walau sudah sore.", daysAgo(4), listOf(seedImage("pisang_goreng")), "Pisang Goreng Tepung", listOf("Makanan enak")),
            Review("r20", "m1", "Yoga P.", 4, "Nasi kuningnya enak, lauknya lengkap. Sambalnya kurang pedas buat saya.", daysAgo(7), emptyList(), "Nasi Kuning Sisa Katering"),
            Review("r21", "m1", "Ani S.", 5, "Langganan tiap sore. Bu Sari selalu ingat nama saya.", daysAgo(10), emptyList(), "Sayur Sop Sisa Hari Ini", listOf("Ramah")),

            Review("r22", "m2", "Putri N.", 5, "Brownies pinggirannya justru favorit saya, fudgy banget.", daysAgo(1), listOf(seedImage("brownies")), "Brownies Panggang Potong", listOf("Makanan enak")),
            Review("r23", "m2", "Arif B.", 4, "Croissant sandwich-nya masih dingin dari chiller, enak dipanaskan sebentar.", daysAgo(3), listOf(seedImage("croissant")), "Croissant Sandwich Smoked Beef"),
            Review("r24", "m2", "Pak Slamet", 5, "Remah rotinya kering dan bersih, ayam saya suka.", daysAgo(6), listOf(seedImage("roti_kering_pakan")), "Remah Roti Kering untuk Pakan", listOf("Bersih")),
            Review("r25", "m2", "Wulan K.", 3, "Donatnya enak tapi glaze-nya agak meleleh di perjalanan.", daysAgo(9), emptyList(), "Donat Glaze Sisa Etalase", listOf("Kemasan rapi")),

            Review("r26", "m3", "Galih R.", 5, "Nasi gorengnya masih hangat, telurnya setengah matang pas.", daysAgo(1), listOf(seedImage("nasi_goreng")), "Nasi Goreng Telur Mata Sapi", listOf("Makanan enak")),
            Review("r27", "m3", "Siti A.", 4, "Bubur ayamnya enak, kerupuk dipisah jadi tetap renyah.", daysAgo(4), listOf(seedImage("bubur_ayam")), "Bubur Ayam Komplit", listOf("Kemasan rapi")),
            Review("r28", "m3", "Bima T.", 4, "Bakso tahunya mantap. Kuah dipisah, aman dibawa pulang.", daysAgo(5), emptyList(), "Bakso Tahu Kuah"),

            Review("r29", "m4", "Kelompok Ternak Sawangan", 5, "Dedaknya kering, tidak apek. Harga jauh di bawah pasar.", daysAgo(2), listOf(seedImage("dedak")), "Dedak Padi Sisa Penggilingan", listOf("Hemat")),
            Review("r30", "m4", "Mas Joko", 4, "Sayurnya banyak, tapi tetap harus dipilah lagi sedikit.", daysAgo(6), emptyList(), "Sayur Layu Pasar untuk Pakan"),
            Review("r31", "m4", "Bu Endang", 5, "Ampas tahunya gratis dan masih segar. Terima kasih!", daysAgo(9), emptyList(), "Ampas Tahu Segar", listOf("Ramah")),

            Review("r32", "m5", "Rina Urban Farm", 5, "Daun keringnya pas untuk menyeimbangkan sisa dapur.", daysAgo(2), listOf(seedImage("daun_kering")), "Daun Kering untuk Kompos", listOf("Bersih")),
            Review("r33", "m5", "Dimas H.", 4, "Bahan kompos bebas plastik, tapi lokasi agak susah dicari.", daysAgo(5), emptyList(), "Sisa Sayur & Buah untuk Kompos"),
            Review("r34", "m5", "Ibu PKK Cilandak", 5, "Ampas kopinya banyak, tanaman cabai kami subur.", daysAgo(11), listOf(seedImage("ampas_kopi")), "Ampas Kopi untuk Kompos", listOf("Ramah")),

            Review("r35", "m6", "Nabila F.", 5, "Peach-nya segar, whipped cream-nya tidak terlalu manis.", daysAgo(3), listOf(seedImage("puding_cheesecake")), "Puding Cheesecake Sisa PO", listOf("Masih fresh")),
            Review("r36", "m6", "Teddy W.", 5, "Kak Rara ramah, pudingnya dikemas rapi pakai ice gel.", daysAgo(6), emptyList(), "Puding Cheesecake Sisa PO", listOf("Ramah", "Kemasan rapi")),
            Review("r37", "m6", "Grace L.", 4, "Yang mini cocok buat camilan, sesuai deskripsi.", daysAgo(10), emptyList(), "Puding Cheesecake H+1 (Mini)"),

            Review("r38", "m7", "Hans W.", 5, "Bumbu mie gorengnya khas rumahan, porsinya besar.", daysAgo(4), emptyList(), "Mie Goreng Spesial", listOf("Porsi banyak")),
            Review("r39", "m7", "Melisa C.", 4, "Enak, tinggal dipanaskan sebentar di wajan.", daysAgo(7), emptyList(), "Mie Goreng Spesial"),
            Review("r40", "m7", "Andre S.", 4, "Label non-halal jelas di halaman toko, jadi tidak salah beli. Rasanya mantap.", daysAgo(13), emptyList(), "Mie Goreng Spesial"),

            Review("r41", "m8", "Fikri Z.", 5, "Ayam bakarnya wangi arang, bumbunya meresap sampai dalam.", daysAgo(1), listOf(seedImage("martabak")), "Ayam Bakar Jepit (4 potong)", listOf("Makanan enak")),
            Review("r42", "m8", "Rosa D.", 5, "Empat potong cuma 18 ribu. Sambal dan lalapannya dipisah, rapi.", daysAgo(3), emptyList(), "Ayam Bakar Jepit (4 potong)", listOf("Hemat", "Kemasan rapi")),
            Review("r43", "m8", "Bang Ucok", 4, "Enak, tapi harus antre sebentar karena warung lagi ramai.", daysAgo(6), emptyList(), "Ayam Bakar Jepit (4 potong)"),
            Review("r44", "m8", "Lia K.", 5, "Pesan lewat GoSend, sampai masih hangat.", daysAgo(9), emptyList(), "Ayam Bakar Jepit (4 potong)", listOf("Tepat waktu")),

            Review("r45", "m9", "Kevin S.", 5, "Pizzanya tinggal dipanaskan di oven, rasanya hampir seperti baru.", daysAgo(2), listOf(seedImage("pizza")), "Pizza Pepperoni Jamur (2 slice)", listOf("Makanan enak")),
            Review("r46", "m9", "Tasya R.", 4, "Topping jamurnya banyak. Pinggirannya sedikit keras.", daysAgo(5), emptyList(), "Pizza Pepperoni Jamur (2 slice)"),
            Review("r47", "m9", "Dewa A.", 4, "Harga separuh, worth it untuk makan malam.", daysAgo(8), emptyList(), "Pizza Pepperoni Jamur (2 slice)", listOf("Hemat")),

            Review("r48", "m10", "Pak Harun", 5, "Rendangnya empuk, bumbu keringnya mantap.", daysAgo(1), listOf(seedImage("rendang")), "Rendang Daging (2 potong)", listOf("Makanan enak")),
            Review("r49", "m10", "Sari W.", 5, "Soto santannya gurih, kuah dan isi dipisah jadi tidak lembek.", daysAgo(3), listOf(seedImage("soto_ayam")), "Soto Ayam Kuah Santan", listOf("Kemasan rapi")),
            Review("r50", "m10", "Eko P.", 4, "Sate 10 tusuk dengan bumbu kacang yang banyak. Lontongnya beli terpisah.", daysAgo(5), emptyList(), "Sate Ayam 10 Tusuk"),
            Review("r51", "m10", "Mira H.", 5, "Pak Gino ramah, selalu tambah bawang goreng.", daysAgo(10), emptyList(), "Soto Ayam Kuah Santan", listOf("Ramah")),

            Review("r52", "m11", "Ratna S.", 5, "Klepon gula merahnya lumer, masih kenyal.", daysAgo(1), listOf(seedImage("klepon")), "Klepon Gula Merah (isi 10)", listOf("Makanan enak", "Masih fresh")),
            Review("r53", "m11", "Ahmad F.", 5, "Bolu pandannya lembut, setengah loyang cuma 12 ribu.", daysAgo(2), listOf(seedImage("bolu_pandan")), "Bolu Pandan Setengah Loyang", listOf("Hemat")),
            Review("r54", "m11", "Yuni L.", 4, "Risolesnya enak, sebaiknya dihangatkan dulu.", daysAgo(4), listOf(seedImage("risoles")), "Risoles Ragout (isi 5)"),
            Review("r55", "m11", "Pak Rahmat", 5, "Ampas kelapanya masih segar, bebek saya lahap.", daysAgo(6), emptyList(), "Ampas Kelapa Parut", listOf("Bersih")),
            Review("r56", "m11", "Citra D.", 5, "Onde-ondenya renyah dan isinya penuh.", daysAgo(8), listOf(seedImage("onde_onde")), "Onde-onde Wijen (isi 6)", listOf("Makanan enak")),

            Review("r57", "m12", "Laras P.", 4, "Salad buahnya segar dan dingin. Porsinya pas untuk satu orang.", daysAgo(2), listOf(seedImage("salad_buah")), "Salad Buah Segar", listOf("Porsi pas")),
            Review("r58", "m12", "Kebun Atap Kuningan", 5, "Kulit pisang dan ampas tehnya sudah dipisah rapi. Mantap untuk kompos.", daysAgo(5), emptyList(), "Kulit Pisang untuk Kompos", listOf("Bersih")),
            Review("r59", "m12", "Bayu R.", 3, "Saladnya enak, tapi saya harus menunggu karena kedai sedang ramai.", daysAgo(9), emptyList(), "Salad Buah Segar", listOf("Harus menunggu lama")),
        ),
    )
    override val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

    override fun add(review: Review) {
        _reviews.value = listOf(review) + _reviews.value
    }
}
