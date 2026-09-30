package com.sisaguna.android.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sisaguna.android.R
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgTextStyle
import com.sisaguna.android.ui.theme.SisaGunaTheme

/** Matches the Figma Landing frame (fileKey LUsLvGVUhvskfA6xrAiSrP) — hero, value props, and
 * CTAs. The brand-opening splash motion now lives in its own route
 * ([com.sisaguna.android.feature.splash.SplashScreen]), shown before this screen, rather than
 * as a timer-driven overlay baked into this composable. */
@Composable
fun LandingScreen(
    onLoginClick: () -> Unit,
    onGetStartedClick: () -> Unit,
    onGuestClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(SgColor.Neutral50)) {
        Column(modifier = Modifier.weight(1f)) {
            HeroArea()
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
                    .clickable(onClick = onGetStartedClick)
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Mulai Sekarang", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SgColor.BaseWhite)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(100.dp))
                    .border(BorderStroke(1.dp, SgColor.Neutral200), RoundedCornerShape(100.dp))
                    .clickable(onClick = onGuestClick)
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Jelajahi tanpa akun", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SgColor.Neutral800)
            }
            Row {
                Text(text = "Sudah punya akun? ", style = SgTextStyle.TextSmRegular, color = SgColor.Neutral500)
                Text(
                    text = "Masuk",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SgColor.Brand600,
                    modifier = Modifier.clickable(onClick = onLoginClick),
                )
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

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun LandingScreenPreview() {
    SisaGunaTheme {
        LandingScreen(onLoginClick = {}, onGetStartedClick = {}, onGuestClick = {})
    }
}
