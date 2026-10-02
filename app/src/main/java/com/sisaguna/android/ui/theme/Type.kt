package com.sisaguna.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sisaguna.android.R

// Confirmed from Figma variable defs on the Home frame (29:93): every text style in the
// design is Inter. These are static instances cut from the variable font with fontTools
// (opsz=14) — FontVariation.Settings on the variable file was ignored on the Redmi test
// device, so every weight rendered as Regular. Black (promo banner) maps to Bold; a 900
// instance isn't worth another ~340KB.
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_bold, FontWeight.Black),
)

// Maps 1:1 to the named text styles Figma returns (Text xs/Regular, Text sm/Medium, etc.).
// Only the slots Material3's fixed scale can express are wired into SgTypography below;
// one-off micro text (10sp badges, strikethrough price) is styled inline at the call site
// instead of forced into a Material slot that doesn't fit.
object SgTextStyle {
    val TextXsRegular = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp)
    val TextXsMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 18.sp)
    val TextSmRegular = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
    val TextSmMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp)
    val TextSmSemibold = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp)
    val TextLgSemibold = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 28.sp)

    // 3a styles (spec §1.2). Color is baked in so 3a screens don't repeat Ink/InkMuted.
    val Display = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.3).sp, color = SgColor.Ink)
    val Title = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp, letterSpacing = (-0.1).sp, color = SgColor.Ink)
    val Body = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, color = SgColor.InkMuted)
    val Label = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, color = SgColor.Ink)
    val Caption = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, color = SgColor.InkMuted)
}

val SgTypography = Typography(
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
