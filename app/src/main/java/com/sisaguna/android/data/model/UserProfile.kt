package com.sisaguna.android.data.model

import java.time.YearMonth

/** [phone] holds digits only, without the +62 prefix — the input field draws that prefix. */
data class UserProfile(
    val name: String,
    val email: String,
    val phone: String,
    val location: String,
    val memberSince: YearMonth,
    /** Local file/content URI of the profile photo; null shows the initial. */
    val photoUri: String? = null,
) {
    val initial: Char get() = name.trim().firstOrNull()?.uppercaseChar() ?: '?'
}

data class ImpactStats(
    val portions: Int,
    val compostKg: Int,
    val carbonKg: Int,
    val savedRupiah: Long,
)
