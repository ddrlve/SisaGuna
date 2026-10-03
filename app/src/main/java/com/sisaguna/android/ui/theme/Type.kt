package com.sisaguna.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sisaguna.android.R

// Plus Jakarta Sans (Tokotype, Jakarta) replaced Inter after user testing: Inter read as
// "template/AI" and a bit cold. Jakarta's rounder terminals and open apertures feel friendlier
// at small sizes and fit a local food app. Static instances per weight — FontVariation on the
// variable file was ignored on the Redmi test device.
val SgFont = FontFamily(
    Font(R.font.jakarta_regular, FontWeight.Normal),
    Font(R.font.jakarta_medium, FontWeight.Medium),
    Font(R.font.jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.jakarta_bold, FontWeight.Bold),
    Font(R.font.jakarta_extrabold, FontWeight.ExtraBold),
    Font(R.font.jakarta_extrabold, FontWeight.Black),
)

/**
 * Named text styles. Getters (not vals) because the colour-carrying ones read [SgColor], which
 * changes with the light/dark setting — a val would freeze whichever palette was active when
 * the object first loaded.
 */
object SgTextStyle {
    val TextXsRegular get() = TextStyle(fontFamily = SgFont, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp)
    val TextXsMedium get() = TextStyle(fontFamily = SgFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 18.sp)
    val TextSmRegular get() = TextStyle(fontFamily = SgFont, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp)
    val TextSmMedium get() = TextStyle(fontFamily = SgFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 21.sp)
    val TextSmSemibold get() = TextStyle(fontFamily = SgFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 21.sp)
    val TextLgSemibold get() = TextStyle(fontFamily = SgFont, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 26.sp)

    val Hero get() = TextStyle(fontFamily = SgFont, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.6).sp, color = SgColor.Ink)
    val Display get() = TextStyle(fontFamily = SgFont, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.3).sp, color = SgColor.Ink)
    val Title get() = TextStyle(fontFamily = SgFont, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 24.sp, color = SgColor.Ink)
    val Body get() = TextStyle(fontFamily = SgFont, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp, color = SgColor.InkMuted)
    val Label get() = TextStyle(fontFamily = SgFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, color = SgColor.Ink)
    val Caption get() = TextStyle(fontFamily = SgFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 17.sp, color = SgColor.InkMuted)
    val Overline get() = TextStyle(fontFamily = SgFont, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.6.sp, color = SgColor.InkMuted)
}

val SgTypography get() = Typography(
    titleLarge = SgTextStyle.TextLgSemibold,
    titleMedium = SgTextStyle.TextLgSemibold,
    titleSmall = SgTextStyle.TextSmSemibold,
    bodyLarge = SgTextStyle.TextSmRegular,
    bodyMedium = SgTextStyle.TextSmRegular,
    bodySmall = SgTextStyle.TextXsRegular,
    labelLarge = SgTextStyle.TextSmMedium,
    labelMedium = SgTextStyle.TextXsMedium,
    labelSmall = SgTextStyle.TextXsRegular,
)
