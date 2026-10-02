package com.sisaguna.android.feature.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sisaguna.android.R
import com.sisaguna.android.ui.components.SgButton
import com.sisaguna.android.ui.components.SgButtonStyle
import com.sisaguna.android.ui.components.SgEaseOut
import com.sisaguna.android.ui.components.pressable
import com.sisaguna.android.ui.theme.Inter
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle
import com.sisaguna.android.ui.theme.SisaGunaTheme
import kotlinx.coroutines.delay

/**
 * Fades and lifts a block into place [index] × 70ms after first composition — a short stagger
 * so the page assembles top to bottom instead of popping in at once. Transform/alpha only.
 */
private fun Modifier.enterUp(index: Int): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(80L + index * 70L)
        progress.animateTo(1f, tween(420, easing = SgEaseOut))
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 18.dp.toPx()
    }
}

/** Matches the Figma Landing frame (fileKey LUsLvGVUhvskfA6xrAiSrP) — hero, value props, and
 * CTAs — with an entrance sequence: hero scales in, copy and value props stagger up, CTAs last.
 * The hero then drifts gently (±4dp, 3.2s) so the page feels alive without demanding attention. */
@Composable
fun LandingScreen(
    onLoginClick: () -> Unit,
    onGetStartedClick: () -> Unit,
    onGuestClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(SgColor.Page)) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            HeroArea()
            Column(
                modifier = Modifier.padding(top = SgSpacing.Xl, start = 24.dp, end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Selamatkan Makanan, Lindungi Bumi Kita",
                    fontFamily = Inter,
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = SgColor.Ink,
                    modifier = Modifier.enterUp(1),
                )
                Text(
                    text = "Setiap tahun, berton-ton makanan layak makan terbuang sia-sia. Bersama SisaGuna, selamatkan surplus makanan lezat di sekitarmu dengan harga terjangkau atau bahkan gratis.",
                    style = SgTextStyle.TextSmRegular,
                    color = SgColor.InkMuted,
                    modifier = Modifier.enterUp(2),
                )
            }
            Column(
                modifier = Modifier.padding(top = 20.dp, start = 24.dp, end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FeatureRow("🌱", SgColor.Mint, "100% Eco-Friendly", "Mengurangi emisi karbon langsung dari limbah makanan organik.", Modifier.enterUp(3))
                FeatureRow("💰", SgColor.Farm, "Hemat & Berbagi", "Dapatkan surplus lezat dengan potongan harga s/d 70%.", Modifier.enterUp(4))
                FeatureRow("🐔", SgColor.Compost, "Tidak ada yang terbuang", "Sisa yang tak layak dimakan jadi pakan ternak dan kompos.", Modifier.enterUp(5))
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(24.dp).enterUp(6),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SgButton("Mulai Sekarang", onClick = onGetStartedClick, modifier = Modifier.fillMaxWidth())
            SgButton("Jelajahi tanpa akun", onClick = onGuestClick, style = SgButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Sudah punya akun? ", style = SgTextStyle.TextSmRegular, color = SgColor.InkMuted)
                Text(
                    text = "Masuk",
                    style = SgTextStyle.Label,
                    color = SgColor.Brand600,
                    modifier = Modifier
                        .clip(RoundedCornerShape(SgRadius.Pill))
                        .pressable(onLoginClick)
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun HeroArea(modifier: Modifier = Modifier) {
    val intro = remember { Animatable(0f) }
    LaunchedEffect(Unit) { intro.animateTo(1f, tween(600, easing = SgEaseOut)) }
    val drift by rememberInfiniteTransition(label = "heroDrift").animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3_200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(start = 24.dp, end = 24.dp, top = 24.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = 0.94f + 0.06f * intro.value
                    scaleX = s
                    scaleY = s
                    alpha = intro.value
                    translationY = drift * 4.dp.toPx()
                }
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp)),
        ) {
            Image(
                painter = painterResource(R.drawable.hero_area),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Soft gradient so the badge stays legible on any photo.
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.18f), Color.Transparent, Color.Transparent))))
        }
        // Frosted glass brand chip.
        Row(
            modifier = Modifier
                .padding(14.dp)
                .graphicsLayer { alpha = intro.value }
                .clip(RoundedCornerShape(SgRadius.Pill))
                .background(Color.White.copy(alpha = 0.78f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_logo_mark), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(16.dp))
            Text(
                "sisaguna",
                fontFamily = Inter,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = SgColor.Ink,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

@Composable
private fun FeatureRow(emoji: String, iconBg: Color, title: String, description: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier.size(40.dp).background(iconBg, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 18.sp)
        }
        Column {
            Text(text = title, style = SgTextStyle.Label)
            Text(text = description, style = SgTextStyle.Caption)
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun LandingScreenPreview() {
    SisaGunaTheme {
        LandingScreen(onLoginClick = {}, onGetStartedClick = {}, onGuestClick = {})
    }
}
