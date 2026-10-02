package com.sisaguna.android.feature.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.sisaguna.android.R
import com.sisaguna.android.ui.components.SgEaseOut
import com.sisaguna.android.ui.components.pressable
import com.sisaguna.android.ui.theme.Inter
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle
import kotlin.math.absoluteValue
import kotlinx.coroutines.delay

enum class BannerAction { BROWSE_FREE, CLAIM_FIRST_ORDER, OPEN_FARM_TAB, UPLOAD }

private data class Banner(
    val eyebrow: String,
    val title: String,
    val cta: String,
    val image: Int,
    val colors: List<Color>,
    val ink: Color,
    val action: BannerAction,
)

private val banners = listOf(
    Banner("Nikmati makanan", "GRATIS hari ini", "Lihat yang gratis", R.drawable.banner_food, listOf(Color(0xFF99D583), Color(0xFF55B931)), Color.White, BannerAction.BROWSE_FREE),
    Banner("Pengguna baru", "Diskon 50% pesanan pertama", "Klaim voucher", R.drawable.category_human, listOf(Color(0xFFFFD6E4), Color(0xFFF48FB1)), Color(0xFF6B1238), BannerAction.CLAIM_FIRST_ORDER),
    Banner("Peternak & petani", "Pakan ternak mulai Rp 0", "Lihat pakan", R.drawable.category_animal, listOf(Color(0xFFFFE2BF), Color(0xFFF6A55A)), Color(0xFF5C2A04), BannerAction.OPEN_FARM_TAB),
    Banner("Punya makanan berlebih?", "Bagikan, jangan buang", "Upload sekarang", R.drawable.category_compost, listOf(Color(0xFFCFE8FA), Color(0xFF5BA8E0)), Color(0xFF0B3A5C), BannerAction.UPLOAD),
)

/**
 * Auto-advancing promo carousel. Neighbours peek on both sides and shrink slightly so the row
 * reads as swipeable; auto-advance pauses while the user is dragging and restarts after.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BannerCarousel(onAction: (BannerAction) -> Unit, modifier: Modifier = Modifier) {
    val pager = rememberPagerState { banners.size }
    val dragged by pager.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(dragged, pager.settledPage) {
        if (dragged) return@LaunchedEffect
        delay(4_500)
        pager.animateScrollToPage((pager.currentPage + 1) % banners.size, animationSpec = tween(520, easing = SgEaseOut))
    }
    Column(modifier) {
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = SgSpacing.Gutter),
            pageSpacing = SgSpacing.Md,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            BannerCard(banners[page], pager, page, onAction)
        }
        PagerDots(pager, Modifier.align(Alignment.CenterHorizontally).padding(top = SgSpacing.Sm))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BannerCard(banner: Banner, pager: PagerState, page: Int, onAction: (BannerAction) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp)
            .graphicsLayer {
                val offset = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                val s = lerp(1f, 0.92f, offset)
                scaleX = s
                scaleY = s
                alpha = lerp(1f, 0.7f, offset)
            }
            .clip(RoundedCornerShape(SgRadius.Card))
            .background(Brush.linearGradient(banner.colors))
            .pressable({ onAction(banner.action) }, pressedScale = 0.98f),
    ) {
        // Soft glass circles for depth behind the illustration.
        Box(Modifier.size(180.dp).offset(x = 200.dp, y = (-50).dp).background(Color.White.copy(alpha = 0.18f), CircleShape))
        Box(Modifier.size(90.dp).offset(x = 150.dp, y = 100.dp).background(Color.White.copy(alpha = 0.12f), CircleShape))
        Image(
            painter = painterResource(banner.image),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.align(Alignment.CenterEnd).size(140.dp).padding(end = SgSpacing.Sm),
        )
        Column(
            modifier = Modifier.align(Alignment.CenterStart).padding(start = SgSpacing.Lg).fillMaxWidth(0.6f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(banner.eyebrow, style = SgTextStyle.Label, color = banner.ink.copy(alpha = 0.85f))
            Text(
                banner.title,
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                fontStyle = if (banner.action == BannerAction.BROWSE_FREE) FontStyle.Italic else FontStyle.Normal,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                color = banner.ink,
            )
            // Frosted CTA chip: translucent white over the gradient.
            Row(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(SgRadius.Pill))
                    .background(Color.White.copy(alpha = 0.32f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(banner.cta, style = SgTextStyle.TextXsMedium, color = banner.ink)
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = banner.ink, modifier = Modifier.padding(start = 4.dp).size(14.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PagerDots(pager: PagerState, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(pager.pageCount) { i ->
            val selected = pager.currentPage == i
            val width by animateDpAsState(if (selected) 18.dp else 6.dp, tween(220, easing = SgEaseOut), label = "dot$i")
            Box(
                Modifier
                    .height(6.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(if (selected) SgColor.Brand500 else SgColor.Hairline),
            )
        }
    }
}
