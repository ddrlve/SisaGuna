package com.sisaguna.android.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Exact values pulled from Figma variable defs on the Home frame (fileKey LUsLvGVUhvskfA6xrAiSrP,
 * node 29:93) via get_variable_defs — not guesses. If a screen introduces a token not listed
 * here, pull it the same way rather than eyeballing the swatch.
 */
object SgColor {
    val Brand100 = Color(0xFF99D583)
    val Brand300 = Color(0xFF80CB65)
    val Brand400 = Color(0xFF66C046)
    val Brand500 = Color(0xFF55B931)
    val Brand600 = Color(0xFF4DA62C)
    val Brand700 = Color(0xFF408B25)

    val Green50 = Color(0xFFF0FDF4)
    val Green100 = Color(0xFFDCFCE7)
    val Green600 = Color(0xFF16A34A)

    val Neutral50 = Color(0xFFFAFAFA)
    val Neutral100 = Color(0xFFF5F5F5)
    val Neutral200 = Color(0xFFE5E5E5) // seen on Landing/Login/Register borders, not on the Home variable list
    val Neutral300 = Color(0xFFD4D4D4)
    val Neutral400 = Color(0xFFA3A3A3)
    val Neutral500 = Color(0xFF737373)
    val Neutral800 = Color(0xFF262626)

    // Activity status colors (Activity-ambil, node 78:13801 variable defs).
    val YellowStatus = Color(0xFFCA8A04)
    val RedStatus = Color(0xFFDC2626)

    val Orange100 = Color(0xFFFFEDD5)
    val Sky100 = Color(0xFFE0F2FE)
    val Yellow300 = Color(0xFFFDE047)

    val BaseWhite = Color(0xFFFFFFFF)
    val LabelsPrimary = Color(0xFF000000)

    // 3a tokens (spec §1.2) — no Figma variable source; chosen for the Home/Saved/Profile
    // polish pass and approved in the spec.
    val Ink = Color(0xFF1F2A1C)
    val InkMuted = Color(0xFF6B7466)
    val Page = Color(0xFFF6F7F4)
    val Hairline = Color(0xFFE6E8E3)
    val Mint = Color(0xFFEAF7E4)
    val Farm = Color(0xFFFFF1E2)
    val FarmInk = Color(0xFFB4570B)
    val Compost = Color(0xFFE6F2FB)
    val CompostInk = Color(0xFF1D6FA5)
    val PromoTint = Color(0xFFFCEFF3)
    val PromoInk = Color(0xFFC0306A)
}
