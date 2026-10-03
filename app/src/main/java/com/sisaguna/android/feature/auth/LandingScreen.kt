package com.sisaguna.android.feature.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sisaguna.android.R
import com.sisaguna.android.data.repository.seedImage
import com.sisaguna.android.data.settings.AppLanguage
import com.sisaguna.android.feature.settings.SettingsViewModel
import com.sisaguna.android.ui.components.SgButton
import com.sisaguna.android.ui.components.SgButtonStyle
import com.sisaguna.android.ui.components.SgEaseOut
import com.sisaguna.android.ui.components.pressable
import com.sisaguna.android.ui.components.rememberReducedMotion
import com.sisaguna.android.ui.i18n.SgLocale
import com.sisaguna.android.ui.i18n.Text
import com.sisaguna.android.ui.i18n.l
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgFont
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgTextStyle
import com.sisaguna.android.ui.theme.SisaGunaTheme
import kotlinx.coroutines.delay
import androidx.compose.material3.Text as RawText

/**
 * Fades and lifts a block into place after [delayMs] — the sheet's copy assembles top to
 * bottom instead of popping in at once. Transform/alpha only; strong ease-out, 420ms (a
 * first-run screen, so it gets a little more time than in-app UI).
 */
private fun Modifier.enterUp(delayMs: Long, distance: Dp = 16.dp): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        progress.animateTo(1f, tween(420, easing = SgEaseOut))
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * distance.toPx()
    }
}

/**
 * First screen after the splash. The top half continues the Figma Landing frame — brand green
 * with soft light discs — and fans out real food photos from partner stores so people see
 * *what* they'll rescue before reading a word. The bottom sheet carries one headline, one
 * sentence, three proof chips, and the CTAs.
 *
 * Motion (first-run only, so it's allowed some delight): photo cards rise in with a 70ms
 * stagger from 0.92 scale, then drift ±5dp on one shared phase read at different offsets so
 * they never bob in sync; the sheet slides up underneath; copy staggers in after. The drift
 * loop is skipped when the system "remove animations" setting is on.
 */
@Composable
fun LandingScreen(
    onLoginClick: () -> Unit,
    onGetStartedClick: () -> Unit,
    onGuestClick: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    LandingContent(
        language = settings.language,
        onToggleLanguage = {
            settingsViewModel.setLanguage(if (settings.language == AppLanguage.ID) AppLanguage.EN else AppLanguage.ID)
        },
        onLoginClick = onLoginClick,
        onGetStartedClick = onGetStartedClick,
        onGuestClick = onGuestClick,
    )
}

@Composable
private fun LandingContent(
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onLoginClick: () -> Unit,
    onGetStartedClick: () -> Unit,
    onGuestClick: () -> Unit,
) {
    val sheet = remember { Animatable(0f) }
    LaunchedEffect(Unit) { sheet.animateTo(1f, tween(520, easing = SgEaseOut)) }

    Box(Modifier.fillMaxSize().background(SgColor.Brand500)) {
        HeroCollage(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 40.dp).height(400.dp))

        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_logo_mark), contentDescription = null, tint = SgColor.OnBrand, modifier = Modifier.size(22.dp))
            RawText(
                "sisaguna",
                fontFamily = SgFont,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = SgColor.OnBrand,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
            LanguagePill(language, onToggleLanguage)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = (1f - sheet.value) * 120.dp.toPx()
                    alpha = sheet.value
                }
                .shadow(24.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(SgColor.BaseWhite)
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 12.dp),
        ) {
            // One colour, no accented word: the photo collage above is the memorable element,
            // so the copy stays quiet.
            Text(
                text = l("Makanan enak dari warung sekitar, hemat sampai 70%", "Good food from shops nearby, up to 70% off"),
                style = SgTextStyle.Hero,
                modifier = Modifier.enterUp(160),
            )
            Text(
                l(
                    "Warung, bakery, dan katering menjual sisa hari ini sebelum terbuang. Pesan, lalu ambil sendiri atau kirim pakai ojek online.",
                    "Shops sell today's surplus before it's thrown away. Order, then pick it up or have a courier bring it.",
                ),
                style = SgTextStyle.Body,
                modifier = Modifier.padding(top = 10.dp).enterUp(230),
            )
            Column(
                modifier = Modifier.padding(top = 16.dp).enterUp(300),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProofLine(Icons.Rounded.VerifiedUser, l("Tiap makanan dicek layak konsumsi sebelum tayang", "Every listing passes a food-safety check"))
                ProofLine(Icons.Rounded.Eco, l("Yang tak layak dimakan jadi pakan ternak & kompos", "What can't be eaten becomes feed & compost"))
            }
            Spacer(Modifier.height(22.dp))
            Column(Modifier.enterUp(380), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SgButton(l("Mulai Sekarang", "Get Started"), onClick = onGetStartedClick, modifier = Modifier.fillMaxWidth(), height = 54.dp)
                SgButton(l("Jelajahi tanpa akun", "Browse as guest"), onClick = onGuestClick, style = SgButtonStyle.Secondary, modifier = Modifier.fillMaxWidth(), height = 54.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(l("Sudah punya akun?", "Already have an account?"), style = SgTextStyle.Body)
                    Text(
                        text = l("Masuk", "Log in"),
                        style = SgTextStyle.Label,
                        color = SgColor.Brand600,
                        modifier = Modifier
                            .clip(RoundedCornerShape(SgRadius.Pill))
                            .pressable(onLoginClick)
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

private data class HeroCard(val image: String, val label: String, val price: String, val x: Dp, val y: Dp, val w: Dp, val rotation: Float)

private val heroCards = listOf(
    HeroCard("nasi_kuning", "Nasi Kuning", "Rp 8rb", (-104).dp, 10.dp, 132.dp, -8f),
    HeroCard("puding_cheesecake", "Puding Cheesecake", "Rp 12rb", 104.dp, 0.dp, 128.dp, 7f),
    HeroCard("donat", "Donat", "Rp 3rb", 0.dp, 78.dp, 146.dp, -2f),
    HeroCard("sayur_pakan", "Pakan Ternak", "Rp 2rb/kg", 112.dp, 150.dp, 112.dp, 9f),
    HeroCard("kompos_sayur", "Kompos", "Gratis", (-116).dp, 166.dp, 108.dp, -9f),
)

@Composable
private fun HeroCollage(modifier: Modifier = Modifier) {
    val reduceMotion = rememberReducedMotion()
    // One linear phase drives every card; each reads it at its own offset. Linear because it's
    // constant motion — the sine supplies the easing.
    val still = remember { mutableFloatStateOf(0f) }
    val phase by if (reduceMotion) still else rememberInfiniteTransition(label = "heroDrift").animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(6_000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Box(modifier) {
        // Figma Landing: soft light discs on brand green.
        Disc(220.dp, (-70).dp, (-20).dp, 0.12f)
        Disc(150.dp, 270.dp, 40.dp, 0.10f)
        Disc(96.dp, 40.dp, 280.dp, 0.14f)
        heroCards.forEachIndexed { i, card ->
            val enter = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                delay(80L + i * 70L)
                enter.animateTo(1f, tween(560, easing = SgEaseOut))
            }
            FoodPhotoCard(
                card = card,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = card.x, y = card.y)
                    .graphicsLayer {
                        val s = 0.92f + 0.08f * enter.value
                        scaleX = s
                        scaleY = s
                        alpha = enter.value
                        rotationZ = card.rotation
                        translationY = (1f - enter.value) * 40.dp.toPx() +
                            kotlin.math.sin(phase + i * 1.3f) * 5.dp.toPx()
                    },
            )
        }
    }
}

@Composable
private fun BoxScope.Disc(size: Dp, x: Dp, y: Dp, alpha: Float) {
    Box(
        Modifier
            .offset(x = x, y = y)
            .size(size)
            .background(Color.White.copy(alpha = alpha), CircleShape),
    )
}

/** Always-light polaroid card: it sits on brand green in both themes. */
@Composable
private fun FoodPhotoCard(card: HeroCard, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(card.w)
            .shadow(14.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(6.dp),
    ) {
        AsyncImage(
            model = seedImage(card.image),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(card.w * 0.66f).clip(RoundedCornerShape(15.dp)),
        )
        Row(Modifier.padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RawText(
                card.label,
                fontFamily = SgFont,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color(0xFF1F2A1C),
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            RawText(card.price, fontFamily = SgFont, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = Color(0xFF408B25), maxLines = 1)
        }
    }
}

@Composable
private fun ProofLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = SgColor.Brand600, modifier = Modifier.size(18.dp))
        Text(text, style = SgTextStyle.Caption.copy(color = SgColor.Ink, fontSize = 13.sp), modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun LanguagePill(language: AppLanguage, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(SgRadius.Pill))
            .background(Color.White.copy(alpha = 0.18f))
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(SgRadius.Pill))
            .pressable(onToggle)
            .padding(3.dp),
    ) {
        listOf(AppLanguage.ID to "ID", AppLanguage.EN to "EN").forEach { (lang, code) ->
            val selected = lang == language
            RawText(
                code,
                fontFamily = SgFont,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (selected) Color(0xFF408B25) else Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(SgRadius.Pill))
                    .background(if (selected) Color.White else Color.Transparent)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 860)
@Composable
private fun LandingPreview() {
    SisaGunaTheme {
        LandingContent(language = SgLocale.language, onToggleLanguage = {}, onLoginClick = {}, onGetStartedClick = {}, onGuestClick = {})
    }
}
