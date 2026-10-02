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
            Voucher("HEMAT5K", "Potongan Rp 5.000", "Min. belanja Rp 15.000", flatOff = 5_000, minSpend = 15_000, expiresAt = now.plus(3, ChronoUnit.DAYS)),
            Voucher("SELAMAT20", "Diskon 20%", "Maks. Rp 8.000 · tanpa minimum", percentOff = 20, maxDiscount = 8_000, expiresAt = now.plus(7, ChronoUnit.DAYS)),
            Voucher("PERTAMA", "Pesanan pertama 50%", "Maks. Rp 10.000 · min. Rp 10.000", percentOff = 50, maxDiscount = 10_000, minSpend = 10_000, expiresAt = now.plus(14, ChronoUnit.DAYS)),
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
