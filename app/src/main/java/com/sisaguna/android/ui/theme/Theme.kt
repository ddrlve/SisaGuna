package com.sisaguna.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Figma design shows a single light theme (see figma/figma export). Dark scheme reuses
// the same brand tokens so it doesn't crash on dark-mode devices, not a designed dark mode.
private val SgLightColorScheme = lightColorScheme(
    primary = SgColor.Brand500,
    onPrimary = SgColor.BaseWhite,
    primaryContainer = SgColor.Brand200,
    onPrimaryContainer = SgColor.Brand800,
    secondary = SgColor.Yellow500,
    onSecondary = SgColor.BaseWhite,
    error = SgColor.Red500,
    onError = SgColor.BaseWhite,
    background = SgColor.Neutral50,
    onBackground = SgColor.Neutral800,
    surface = SgColor.BaseWhite,
    onSurface = SgColor.Neutral800,
    surfaceVariant = SgColor.Neutral100,
    onSurfaceVariant = SgColor.Neutral500,
    outline = SgColor.Neutral400,
)

private val SgDarkColorScheme = darkColorScheme(
    primary = SgColor.Brand500,
    onPrimary = SgColor.BaseWhite,
    primaryContainer = SgColor.Brand800,
    onPrimaryContainer = SgColor.Brand200,
    secondary = SgColor.Yellow500,
    onSecondary = SgColor.Neutral800,
    error = SgColor.Red500,
    onError = SgColor.BaseWhite,
    background = SgColor.Neutral800,
    onBackground = SgColor.Neutral50,
    surface = SgColor.Neutral800,
    onSurface = SgColor.Neutral50,
    surfaceVariant = SgColor.Neutral400,
    onSurfaceVariant = SgColor.Neutral100,
    outline = SgColor.Neutral500,
)

@Composable
fun SisaGunaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) SgDarkColorScheme else SgLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SgTypography,
        shapes = SgShapes,
        content = content,
    )
}
