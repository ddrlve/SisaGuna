package com.sisaguna.android.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sisaguna.android.R
import com.sisaguna.android.ui.components.SgLogo
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgTextStyle
import com.sisaguna.android.ui.theme.SisaGunaTheme
import kotlinx.coroutines.delay

// Timing lifted from Figma's motion data on node 70:2314 ("Brand Opening Motion" +
// staggered section entrances), scaled down from its 4.2s loop to a one-shot splash.
private const val SplashCrossfadeStartMs = 1900
private const val SplashCrossfadeMs = 550
private val SectionEasing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
private const val SectionDurationMs = 460

@Composable
fun LandingScreen(
    onLoginClick: () -> Unit,
    onGetStartedClick: () -> Unit,
) {
    var showSplash by remember { mutableStateOf(true) }
    var contentRevealed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(SplashCrossfadeStartMs.toLong())
        contentRevealed = true
        delay(SplashCrossfadeMs.toLong())
        showSplash = false
    }

    Box(modifier = Modifier.fillMaxSize().background(SgColor.Neutral50)) {
        LandingContent(
            revealed = contentRevealed,
            onLoginClick = onLoginClick,
            onGetStartedClick = onGetStartedClick,
        )
        AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut(tween(SplashCrossfadeMs)),
        ) {
            SplashOverlay()
        }
    }
}

@Composable
private fun SplashOverlay(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(SgColor.SplashGreen)) {
        Box(Modifier.offset(x = 45.dp, y = 250.dp).size(92.dp).background(Color(0xFF73DB57).copy(alpha = 0.55f), CircleShape))
        Box(Modifier.offset(x = 265.dp, y = 180.dp).size(128.dp).background(Color(0xFF5CCF40).copy(alpha = 0.55f), CircleShape))
        Box(Modifier.offset(x = 230.dp, y = 540.dp).size(80.dp).background(Color(0xFF8CE66B).copy(alpha = 0.55f), CircleShape))
        SgLogo(
            modifier = Modifier.align(Alignment.Center),
            markTint = Color.Unspecified,
            textColor = SgColor.BaseWhite,
        )
    }
}

@Composable
private fun LandingContent(
    revealed: Boolean,
    onLoginClick: () -> Unit,
    onGetStartedClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(SgColor.Neutral50)) {
        Column(modifier = Modifier.weight(1f)) {
            Section(revealed, delayMs = 150) {
                HeroArea()
            }
            Section(revealed, delayMs = 440) {
                Column(
                    modifier = Modifier.padding(top = 28.dp, start = 24.dp, end = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Selamatkan Makanan, Lindungi Bumi Kita",
                        fontSize = 26.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = SgColor.Neutral800,
                    )
                    Text(
                        text = "Setiap tahun, berton-ton makanan layak makan terbuang sia-sia. Bersama SisaGuna, ambil bagian menyelamatkan surplus makanan lezat di sekitarmu dengan harga sangat terjangkau atau bahkan gratis!",
                        style = SgTextStyle.TextSmRegular,
                        color = SgColor.Neutral500,
                    )
                }
            }
            Section(revealed, delayMs = 720) {
                Column(
                    modifier = Modifier.padding(top = 20.dp, start = 24.dp, end = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FeatureRow(
                        emoji = "🌱",
                        iconBg = SgColor.Green50,
                        title = "100% Eco-Friendly",
                        description = "Mengurangi emisi karbon langsung dari limbah makanan organik.",
                    )
                    FeatureRow(
                        emoji = "💰",
                        iconBg = SgColor.Orange100,
                        title = "Hemat & Berbagi",
                        description = "Dapatkan surplus lezat dengan potongan harga s/d 70%.",
                    )
                }
            }
        }
        Section(revealed, delayMs = 1060) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(100.dp))
                        .background(SgColor.Brand500)
                        .clickableText(onGetStartedClick)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Mulai Sekarang",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SgColor.BaseWhite,
                    )
                }
                Row {
                    Text(text = "Sudah punya akun? ", style = SgTextStyle.TextSmRegular, color = SgColor.Neutral500)
                    Text(
                        text = "Masuk",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SgColor.Brand600,
                        modifier = Modifier.clickableText(onLoginClick),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroArea(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .padding(24.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.hero_area),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(SgColor.Brand500, RoundedCornerShape(100.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(text = "sisaguna", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SgColor.BaseWhite)
        }
    }
}

@Composable
private fun FeatureRow(emoji: String, iconBg: Color, title: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier.size(32.dp).background(iconBg, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 14.sp)
        }
        Column {
            Text(text = title, style = SgTextStyle.TextSmSemibold, color = SgColor.Neutral800)
            Text(text = description, fontSize = 12.sp, color = SgColor.Neutral500)
        }
    }
}

@Composable
private fun Section(
    revealed: Boolean,
    delayMs: Int,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    AnimatedVisibility(
        visible = revealed,
        enter = fadeIn(tween(SectionDurationMs, delayMillis = delayMs, easing = SectionEasing)) +
            slideInVertically(
                animationSpec = tween(SectionDurationMs, delayMillis = delayMs, easing = SectionEasing),
                initialOffsetY = { with(density) { 14.dp.roundToPx() } },
            ),
    ) {
        content()
    }
}

private fun Modifier.clickableText(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun LandingScreenPreview() {
    SisaGunaTheme {
        LandingScreen(onLoginClick = {}, onGetStartedClick = {})
    }
}
