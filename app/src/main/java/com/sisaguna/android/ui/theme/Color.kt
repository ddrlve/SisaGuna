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

    // Splash-only fill from Landing Page's "Brand Opening Motion" node — distinct from Brand500.
    val SplashGreen = Color(0xFF49C22E)

    // Activity status colors (Activity-ambil, node 78:13801 variable defs).
    val YellowStatus = Color(0xFFCA8A04)
    val RedStatus = Color(0xFFDC2626)

    val Orange100 = Color(0xFFFFEDD5)
    val Sky100 = Color(0xFFE0F2FE)
    val Yellow300 = Color(0xFFFDE047)

    val BaseWhite = Color(0xFFFFFFFF)
    val LabelsPrimary = Color(0xFF000000)
}
