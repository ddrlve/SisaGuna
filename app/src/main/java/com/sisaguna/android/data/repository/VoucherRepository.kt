package com.sisaguna.android.data.repository

import java.time.Instant
import kotlinx.coroutines.flow.StateFlow

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

    /** Reloads from the source: placing an order uses up a voucher on the server. */
    suspend fun refresh() {}
}
