package com.sisaguna.android.data.repository

import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Voucher(
    val code: String,
    val title: String,
    val description: String,
    /** Either a percentage (capped by [maxDiscount]) or a flat Rupiah amount. */
    val percentOff: Int? = null,
    val flatOff: Int? = null,
    val maxDiscount: Int? = null,
    val minSpend: Int = 0,
    val expiresAt: Instant,
    /** Illustration shown on the voucher card and detail sheet. */
    val imageUrl: String = "",
    /** Syarat & ketentuan, one condition per line. */
    val terms: List<String> = emptyList(),
    /** null = any store. */
    val merchantName: String? = null,
    val appliesTo: String = "Semua makanan siap santap",
    val accent: Long = 0xFF55B931,
) {
    /** Discount for a [subtotal], or 0 when the voucher doesn't apply. Never exceeds the subtotal. */
    fun discountFor(subtotal: Int): Int {
        if (subtotal < minSpend || subtotal <= 0) return 0
        val raw = when {
            percentOff != null -> subtotal * percentOff / 100
            flatOff != null -> flatOff
            else -> 0
        }
        return raw.coerceAtMost(maxDiscount ?: Int.MAX_VALUE).coerceAtMost(subtotal)
    }
}

interface VoucherRepository {
    val vouchers: StateFlow<List<Voucher>>
    val claimed: StateFlow<Set<String>>
    fun claim(code: String)

    /** Marks a voucher used after an order; it disappears from the list. */
    fun consume(code: String)
}

@Singleton
class FakeVoucherRepository(now: Instant) : VoucherRepository {

    @Inject constructor() : this(Instant.now())

    private val _vouchers = MutableStateFlow(
        listOf(
            Voucher(
                "HEMAT5K", "Potongan Rp 5.000", "Min. belanja Rp 15.000", flatOff = 5_000, minSpend = 15_000,
                expiresAt = now.plus(3, ChronoUnit.DAYS), imageUrl = seedImage("nasi_box"), accent = 0xFF4DA62C,
                terms = listOf(
                    "Minimum belanja Rp 15.000 (sebelum ongkir).",
                    "Berlaku untuk pickup maupun delivery.",
                    "Satu voucher per pesanan, tidak bisa digabung voucher lain.",
                    "Tidak berlaku untuk listing gratis.",
                    "Berlaku sampai tanggal kedaluwarsa pukul 23.59 WIB.",
                ),
            ),
            Voucher(
                "SELAMAT20", "Diskon 20%", "Maks. Rp 8.000 · tanpa minimum", percentOff = 20, maxDiscount = 8_000,
                expiresAt = now.plus(7, ChronoUnit.DAYS), imageUrl = seedImage("donat"), accent = 0xFFC0306A,
                appliesTo = "Roti, kue & dessert",
                terms = listOf(
                    "Diskon 20% dari subtotal, maksimal Rp 8.000.",
                    "Tanpa minimum belanja.",
                    "Khusus kategori roti, kue, dan dessert dari mitra terverifikasi.",
                    "Bisa dipakai 2× per akun selama periode promo.",
                ),
            ),
            Voucher(
                "PERTAMA", "Pesanan pertama 50%", "Maks. Rp 10.000 · min. Rp 10.000", percentOff = 50, maxDiscount = 10_000, minSpend = 10_000,
                expiresAt = now.plus(14, ChronoUnit.DAYS), imageUrl = seedImage("nasi_kuning"), accent = 0xFFB4570B,
                terms = listOf(
                    "Khusus pengguna yang belum pernah menyelesaikan pesanan.",
                    "Diskon 50%, maksimal Rp 10.000; minimum belanja Rp 10.000.",
                    "Hanya untuk pembayaran QRIS atau e-wallet.",
                    "Pesanan yang dibatalkan mengembalikan kuota voucher.",
                ),
            ),
            Voucher(
                "PAKAN10", "Pakan ternak hemat 10%", "Min. 5 kg · maks. Rp 5.000", percentOff = 10, maxDiscount = 5_000, minSpend = 10_000,
                expiresAt = now.plus(10, ChronoUnit.DAYS), imageUrl = seedImage("sayur_pakan"), accent = 0xFF1D6FA5,
                appliesTo = "Pakan ternak & kompos",
                terms = listOf(
                    "Berlaku untuk kategori pakan ternak dan kompos.",
                    "Minimum pembelian setara 5 kg atau Rp 10.000.",
                    "Diskon maksimal Rp 5.000 per pesanan.",
                    "Khusus pickup mandiri.",
                ),
            ),
        ),
    )
    override val vouchers: StateFlow<List<Voucher>> = _vouchers.asStateFlow()

    private val _claimed = MutableStateFlow(setOf("HEMAT5K"))
    override val claimed: StateFlow<Set<String>> = _claimed.asStateFlow()

    override fun claim(code: String) {
        if (_vouchers.value.any { it.code == code }) _claimed.value = _claimed.value + code
    }

    override fun consume(code: String) {
        _vouchers.value = _vouchers.value.filterNot { it.code == code }
        _claimed.value = _claimed.value - code
    }
}
