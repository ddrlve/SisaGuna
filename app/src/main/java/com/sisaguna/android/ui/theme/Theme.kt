package com.sisaguna.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// The Figma file only designs a single light theme. Dark scheme below reuses the same brand
// tokens purely so the app doesn't look broken on a dark-mode device — it is not a designed
// dark mode and has no Figma source.
private val UndesignedError = Color(0xFFDC2626) // No error/destructive state exists in Figma yet.

private val SgLightColorScheme = lightColorScheme(
    primary = SgColor.Brand500,
    onPrimary = SgColor.BaseWhite,
    primaryContainer = SgColor.Brand100,
    onPrimaryContainer = SgColor.Brand700,
    secondary = SgColor.Green600,
    onSecondary = SgColor.BaseWhite,
    error = UndesignedError,
    onError = SgColor.BaseWhite,
    background = SgColor.Neutral100,
    onBackground = SgColor.Neutral800,
    surface = SgColor.BaseWhite,
    onSurface = SgColor.Neutral800,
    surfaceVariant = SgColor.Neutral100,
    onSurfaceVariant = SgColor.Neutral500,
    outline = SgColor.Neutral300,
)

private val SgDarkColorScheme = darkColorScheme(
    primary = SgColor.Brand500,
    onPrimary = SgColor.BaseWhite,
    primaryContainer = SgColor.Brand700,
    onPrimaryContainer = SgColor.Brand100,
    secondary = SgColor.Green600,
    onSecondary = SgColor.Neutral800,
    error = UndesignedError,
    onError = SgColor.BaseWhite,
    background = SgColor.Neutral800,
    onBackground = SgColor.Neutral50,
    surface = SgColor.Neutral800,
    onSurface = SgColor.Neutral50,
    surfaceVariant = SgColor.Neutral400,
    onSurfaceVariant = SgColor.Neutral100,
    outline = SgColor.Neutral500,
)

/** Light by default (Figma only designs light). Dark is driven by [SgPalette.isDark], which
 * MainActivity sets from the user's Pengaturan choice; [SgColor] tokens follow it directly and
 * the Material scheme here keeps M3 internals (sheets, dialogs, ripples) in step. */
@Composable
fun SisaGunaTheme(
    darkTheme: Boolean = SgPalette.isDark,
    content: @Composable () -> Unit,
) {
    val base = if (darkTheme) SgDarkColorScheme else SgLightColorScheme
    val colorScheme = base.copy(
        background = SgColor.Page,
        surface = SgColor.BaseWhite,
        surfaceContainerLow = SgColor.BaseWhite,
        surfaceContainer = SgColor.BaseWhite,
        surfaceContainerHigh = SgColor.BaseWhite,
        surfaceContainerHighest = SgColor.Neutral100,
        onSurface = SgColor.Ink,
        onSurfaceVariant = SgColor.InkMuted,
        outline = SgColor.Neutral300,
        outlineVariant = SgColor.Hairline,
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SgTypography,
        shapes = SgShapes,
        content = content,
    )
}
