package com.sisaguna.android.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Which palette [SgColor] serves. It's snapshot state, so every composable that read a token
 * recomposes when the user flips the theme in Pengaturan — no screen has to thread a
 * `darkTheme` flag or switch to MaterialTheme.colorScheme.
 */
object SgPalette {
    var isDark by mutableStateOf(false)
}

private fun pick(light: Long, dark: Long): Color = Color(if (SgPalette.isDark) dark else light)

/**
 * Brand values are from Figma variable defs on the Home frame (fileKey LUsLvGVUhvskfA6xrAiSrP,
 * node 29:93). Neutral/semantic tokens are getters so they follow [SgPalette.isDark]; the dark
 * values are ours (Figma has no dark design) — warm green-greys so the app keeps its earthy
 * feel instead of going flat black.
 *
 * Rule of thumb: [BaseWhite] is a *surface* (cards, sheets) and goes dark in dark mode; use
 * [OnBrand] for text/icons sitting on a green or photo background, which stay white.
 */
object SgColor {
    val Brand100 get() = pick(0xFF99D583, 0xFF2F5A22)
    val Brand300 = Color(0xFF80CB65)
    val Brand400 = Color(0xFF66C046)
    val Brand500 = Color(0xFF55B931)
    val Brand600 get() = pick(0xFF4DA62C, 0xFF6CC84A)
    val Brand700 get() = pick(0xFF408B25, 0xFF8AD86C)

    val Green50 get() = pick(0xFFF0FDF4, 0xFF15251A)
    val Green100 get() = pick(0xFFDCFCE7, 0xFF1C3322)
    val Green600 get() = pick(0xFF16A34A, 0xFF4ADE80)

    val Neutral50 get() = pick(0xFFFAFAFA, 0xFF1A1D19)
    val Neutral100 get() = pick(0xFFF5F5F5, 0xFF20241F)
    val Neutral200 get() = pick(0xFFE5E5E5, 0xFF2E332C)
    val Neutral300 get() = pick(0xFFD4D4D4, 0xFF3D433A)
    val Neutral400 get() = pick(0xFFA3A3A3, 0xFF7C8578)
    val Neutral500 get() = pick(0xFF737373, 0xFF9DA699)
    val Neutral800 get() = pick(0xFF262626, 0xFFE9ECE6)

    val YellowStatus get() = pick(0xFFCA8A04, 0xFFFACC15)
    val RedStatus get() = pick(0xFFDC2626, 0xFFF87171)

    val Orange100 get() = pick(0xFFFFEDD5, 0xFF3A2A16)
    val Sky100 get() = pick(0xFFE0F2FE, 0xFF13293A)
    val Yellow300 = Color(0xFFFDE047)
    val Yellow50 get() = pick(0xFFFEFCE8, 0xFF2B2814)

    /** Card / sheet surface. */
    val BaseWhite get() = pick(0xFFFFFFFF, 0xFF1C211B)
    val LabelsPrimary get() = pick(0xFF000000, 0xFFFFFFFF)

    /** Foreground on brand-green, photos and the dark scrim — white in both themes. */
    val OnBrand = Color(0xFFFFFFFF)
    /** Fixed dark for overlays on photos (countdown pill, image scrims). */
    val Scrim = Color(0xFF141813)

    val Ink get() = pick(0xFF1F2A1C, 0xFFEEF2EB)
    val InkMuted get() = pick(0xFF6B7466, 0xFFA3AD9E)
    val Page get() = pick(0xFFF6F7F4, 0xFF111410)
    val Hairline get() = pick(0xFFE6E8E3, 0xFF2C3229)
    val Mint get() = pick(0xFFEAF7E4, 0xFF1D2E18)
    val Farm get() = pick(0xFFFFF1E2, 0xFF33251A)
    val FarmInk get() = pick(0xFFB4570B, 0xFFF5A35C)
    val Compost get() = pick(0xFFE6F2FB, 0xFF172833)
    val CompostInk get() = pick(0xFF1D6FA5, 0xFF7CC0EE)
    val PromoTint get() = pick(0xFFFCEFF3, 0xFF33192A)
    val PromoInk get() = pick(0xFFC0306A, 0xFFF27AAA)
}
