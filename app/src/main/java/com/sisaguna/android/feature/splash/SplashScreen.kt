package com.sisaguna.android.feature.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.sisaguna.android.ui.components.SgLogo
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SisaGunaTheme
import kotlinx.coroutines.delay

private const val SplashDurationMs = 1900L

/** Matches Figma "Splash screen" (nodes 255:5689-255:5722, fileKey LUsLvGVUhvskfA6xrAiSrP) — a
 * brand-opening motion sequence. Background is Brand/500 per the frame's own variable defs. The
 * raw keyframes are mid-animation (the wordmark is still brand-green on brand-green background,
 * i.e. invisible) so white logo/wordmark here is a deliberate finished-state choice, not a
 * literal trace of one keyframe. */
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(SplashDurationMs)
        onTimeout()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(SgColor.Brand500),
        contentAlignment = Alignment.Center,
    ) {
        SgLogo(
            markSize = 40,
            textSize = 28,
            markTint = Color.Unspecified,
            textColor = SgColor.BaseWhite,
        )
    }
}

@Preview(showBackground = true, heightDp = 852)
@Composable
private fun SplashScreenPreview() {
    SisaGunaTheme {
        SplashScreen(onTimeout = {})
    }
}
