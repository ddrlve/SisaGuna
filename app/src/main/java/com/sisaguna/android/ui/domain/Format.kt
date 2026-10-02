package com.sisaguna.android.ui.domain

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val IdLocale: Locale = Locale.forLanguageTag("id-ID")

/** "Rp 245.000" — Indonesian grouping, no decimals. */
fun formatRupiah(amount: Long): String = "Rp " + NumberFormat.getIntegerInstance(IdLocale).format(amount)

fun formatRupiah(amount: Int): String = formatRupiah(amount.toLong())

/** "Gratis" for 0, otherwise Rupiah. */
fun formatPrice(amount: Int): String = if (amount == 0) "Gratis" else formatRupiah(amount)

private val clockFormat = DateTimeFormatter.ofPattern("HH.mm", IdLocale)
private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", IdLocale)
private val dateTimeFormat = DateTimeFormatter.ofPattern("d MMM yyyy, HH.mm", IdLocale)

fun formatClock(at: Instant, zone: ZoneId = ZoneId.systemDefault()): String = clockFormat.format(at.atZone(zone))
fun formatDate(at: Instant, zone: ZoneId = ZoneId.systemDefault()): String = dateFormat.format(at.atZone(zone))
fun formatDateTime(at: Instant, zone: ZoneId = ZoneId.systemDefault()): String = dateTimeFormat.format(at.atZone(zone))

/** Percentage off the original price, or null when there's no discount to show. */
fun discountPercent(original: Int?, discounted: Int?): Int? {
    if (original == null || discounted == null || original <= 0 || discounted >= original) return null
    return ((original - discounted) * 100.0 / original).toInt()
}
