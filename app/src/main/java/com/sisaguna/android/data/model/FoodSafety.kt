package com.sisaguna.android.data.model

import java.time.Duration
import java.time.Instant

/**
 * "Gimana caranya kita tau makanan ini layak konsumsi?" — the question every merchant and
 * buyer in user testing asked. SisaGuna answers it with three layers that are all visible to
 * the buyer:
 *
 * 1. **Merchant self-check** at upload ([SafetyCheck]) — a short checklist; the required
 *    items can't be skipped for food meant for people.
 * 2. **Time × storage rule** ([StorageMethod]) — how long since it was cooked, against how
 *    long that storage keeps it safe. Past the window, the app refuses to list it as human
 *    food and suggests Pakan Ternak / Kompos instead.
 * 3. **Buyer complaints** after pickup ([Complaint]) feed back into the merchant's record.
 *
 * The windows are conservative defaults based on the common food-safety "danger zone" rule
 * (cooked food shouldn't sit at room temperature for more than ~4 hours) — they are not a lab
 * test and the UI says so.
 */
enum class StorageMethod(val label: String, val labelEn: String, val safeHours: Long) {
    ROOM_TEMP("Suhu ruang (masakan)", "Room temp (cooked)", 4),
    SHELF("Suhu ruang, kering (roti/kue)", "Room temp, dry (bread/cake)", 72),
    CHILLED("Kulkas (1–5°C)", "Fridge (1–5°C)", 48),
    FROZEN("Freezer", "Freezer", 24 * 7),
}

enum class SafetyCheck(val label: String, val detail: String, val requiredForHumans: Boolean) {
    UNTOUCHED("Bukan sisa piring", "Belum pernah disajikan atau disentuh konsumen.", true),
    SENSORY_OK("Bau, warna & tekstur normal", "Tidak asam, berlendir, berjamur, atau berubah tekstur.", true),
    STORED_RIGHT("Disimpan di suhu yang benar", "Sesuai cara simpan yang dipilih, tertutup rapat.", true),
    PACKED_CLEAN("Dikemas bersih & food-grade", "Wadah tertutup, bukan plastik bekas.", true),
    HYGIENE("Diolah dengan tangan & alat bersih", "Pakai sarung tangan atau cuci tangan sebelum mengemas.", false),
}

enum class SafetyLevel { SAFE, CAUTION, UNSAFE, NOT_FOR_HUMANS }

data class SafetyAssessment(
    val level: SafetyLevel,
    /** Null when the listing has no cooked/made time. */
    val hoursSinceMade: Long?,
    val safeUntil: Instant?,
    val passedChecks: Set<SafetyCheck>,
    val missingRequired: List<SafetyCheck>,
    val headline: String,
    val advice: String,
    /** Set when the food is past its human-safe window but still useful as feed/compost. */
    val suggestTier: ListingTier? = null,
) {
    val score: Int get() = SafetyCheck.entries.count { it in passedChecks }
}

object FoodSafety {

    fun safeUntil(madeAt: Instant, storage: StorageMethod): Instant = madeAt.plus(Duration.ofHours(storage.safeHours))

    fun assess(
        tier: ListingTier,
        madeAt: Instant?,
        storage: StorageMethod,
        checks: Set<SafetyCheck>,
        now: Instant = Instant.now(),
    ): SafetyAssessment {
        if (tier != ListingTier.HUMAN) {
            return SafetyAssessment(
                level = SafetyLevel.NOT_FOR_HUMANS,
                hoursSinceMade = madeAt?.let { Duration.between(it, now).toHours() },
                safeUntil = null,
                passedChecks = checks,
                missingRequired = emptyList(),
                headline = if (tier == ListingTier.ANIMAL_FEED) "Khusus pakan ternak" else "Khusus bahan kompos",
                advice = "Bukan untuk dikonsumsi manusia. Pastikan bebas plastik dan benda tajam.",
            )
        }
        val missing = SafetyCheck.entries.filter { it.requiredForHumans && it !in checks }
        val until = madeAt?.let { safeUntil(it, storage) }
        val hours = madeAt?.let { Duration.between(it, now).toHours() }
        val windowUsed = if (madeAt != null && until != null) {
            Duration.between(madeAt, now).toMinutes().toDouble() / Duration.between(madeAt, until).toMinutes().coerceAtLeast(1)
        } else null

        return when {
            missing.isNotEmpty() -> SafetyAssessment(
                SafetyLevel.UNSAFE, hours, until, checks, missing,
                headline = "Checklist kelayakan belum lengkap",
                advice = "Lengkapi ${missing.size} poin wajib sebelum makanan bisa dijual untuk dikonsumsi.",
            )
            windowUsed != null && windowUsed >= 1.0 -> SafetyAssessment(
                SafetyLevel.UNSAFE, hours, until, checks, missing,
                headline = "Lewat batas aman konsumsi",
                advice = "Sudah ${hours} jam sejak dimasak (${storage.label.lowercase()} maks. ${storage.safeHours} jam). Alihkan jadi pakan ternak atau kompos.",
                suggestTier = ListingTier.ANIMAL_FEED,
            )
            windowUsed != null && windowUsed >= 0.75 -> SafetyAssessment(
                SafetyLevel.CAUTION, hours, until, checks, missing,
                headline = "Layak, segera dikonsumsi",
                advice = "Mendekati batas aman. Sarankan pembeli langsung makan atau simpan di kulkas.",
            )
            else -> SafetyAssessment(
                SafetyLevel.SAFE, hours, until, checks, missing,
                headline = "Lolos cek kelayakan",
                advice = "Checklist wajib lengkap dan masih dalam batas waktu aman.",
            )
        }
    }

    fun assess(listing: Listing, now: Instant = Instant.now()): SafetyAssessment =
        assess(listing.tier, listing.madeAt, listing.storage, listing.safetyChecks, now)
}

/** Halal labelling the merchant declares; "Lainnya" covers e.g. vegetarian kitchens that
 * haven't certified. */
enum class HalalStatus(val label: String) {
    HALAL_CERTIFIED("Halal"),
    NON_HALAL("Non-halal"),
    UNVERIFIED("Belum terverifikasi halal"),
    OTHER("Lainnya"),
}

enum class Allergen(val label: String, val emoji: String) {
    SEAFOOD("Seafood", "🦐"),
    PEANUT("Kacang tanah", "🥜"),
    TREE_NUT("Kacang pohon", "🌰"),
    MILK("Susu", "🥛"),
    EGG("Telur", "🥚"),
    GLUTEN("Gluten/gandum", "🌾"),
    SOY("Kedelai", "🫘"),
    SESAME("Wijen", "⚪"),
}

/** How a listing's stock and price are counted. Ready-to-eat food is sold per portion;
 * feed and compost by weight. */
enum class QuantityUnit(val short: String, val shortEn: String) {
    PORTION("porsi", "portion"),
    KILOGRAM("kg", "kg"),
}
