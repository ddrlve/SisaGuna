package com.sisaguna.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sisaguna.android.R

// Confirmed from Figma variable defs on the Home frame (29:93): every text style in the
// design is Inter. R.font.inter is a single variable-weight-axis file (Thin..Black) — just
// declaring Font(resId, weight=X) does NOT select that instance on Android; it silently
// renders every weight as the font's default (Regular), which is why headings looked too
// thin. FontVariation.Settings pins the actual "wght" axis value per entry.
@OptIn(ExperimentalTextApi::class)
val Inter = FontFamily(
    Font(R.font.inter, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.inter, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.inter, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.inter, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.inter, weight = FontWeight.Black, variationSettings = FontVariation.Settings(FontVariation.weight(900))),
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
