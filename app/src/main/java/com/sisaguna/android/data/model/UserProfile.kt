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
    /** Fruit avatar key (see FruitAvatar) shown when there's no photo; null picks one from the name. */
    val avatar: String? = null,
) {
    val initial: Char get() = name.trim().firstOrNull()?.uppercaseChar() ?: '?'
}

/**
 * Rescue level shown on Profile, by portions rescued. Gives the impact numbers a goal: the card
 * shows the current level and how many portions until the next one.
 */
enum class RescueTier(val minPortions: Int, val emoji: String, val label: String, val labelEn: String) {
    STARTER(0, "🌱", "Pemula", "Starter"),
    RESCUER(10, "🌿", "Penyelamat", "Rescuer"),
    HERO(30, "🌳", "Pahlawan Pangan", "Food Hero"),
    LEGEND(60, "🏆", "Legenda", "Legend");

    val next: RescueTier? get() = entries.getOrNull(ordinal + 1)

    /** 0..1 progress from this tier's floor to the next one; 1 at the top tier. */
    fun progress(portions: Int): Float {
        val n = next ?: return 1f
        return ((portions - minPortions).toFloat() / (n.minPortions - minPortions)).coerceIn(0f, 1f)
    }

    companion object {
        fun of(portions: Int): RescueTier = entries.last { portions >= it.minPortions }
    }
}

data class ImpactStats(
    val portions: Int,
    val compostKg: Int,
    val carbonKg: Int,
    val savedRupiah: Long,
)
