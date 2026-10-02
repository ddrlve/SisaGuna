package com.sisaguna.android.feature.activity

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sisaguna.android.data.model.Order
import com.sisaguna.android.data.model.OrderStatus
import com.sisaguna.android.ui.components.SgButton
import com.sisaguna.android.ui.components.SgButtonStyle
import com.sisaguna.android.ui.components.SgChip
import com.sisaguna.android.ui.components.SgEmptyState
import com.sisaguna.android.ui.components.pressable
import com.sisaguna.android.ui.domain.ListingImage
import com.sisaguna.android.ui.domain.formatClock
import com.sisaguna.android.ui.domain.formatDate
import com.sisaguna.android.ui.domain.formatPrice
import com.sisaguna.android.ui.domain.formatRupiah
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay

@Composable
fun ActivityScreen(
    onOrderClick: (String) -> Unit,
    onExplore: () -> Unit,
    viewModel: ActivityViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Ticks every 30s so countdowns stay honest without recomposing every frame.
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = Instant.now()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(SgColor.Page),
        contentPadding = PaddingValues(bottom = SgSpacing.Xl),
    ) {
        item {
            Text(
                "Aktivitas",
                style = SgTextStyle.Display,
                modifier = Modifier.padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Lg),
            )
        }
        item { ImpactStrip(state.impact) }
        item {
            ActivitySwitch(
                selected = state.tab,
                ongoingCount = state.ongoing.size,
                onSelect = viewModel::onTab,
                modifier = Modifier.padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Xl),
            )
        }
        item(key = "content") {
            AnimatedContent(
                targetState = state.tab,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "activityTab",
            ) { tab ->
                Column(
                    modifier = Modifier.padding(top = SgSpacing.Lg),
                    verticalArrangement = Arrangement.spacedBy(SgSpacing.Md),
                ) {
                    when (tab) {
                        ActivityTab.ONGOING -> {
                            if (state.ongoing.isEmpty()) {
                                SgEmptyState(
                                    icon = Icons.Rounded.ShoppingBag,
                                    title = "Belum ada pesanan berlangsung",
                                    body = "Pesanan yang siap diambil akan muncul di sini, lengkap dengan kode pickup.",
                                    action = { SgButton("Cari makanan", onClick = onExplore) },
                                )
                            }
                            state.ongoing.forEach { order ->
                                OngoingCard(order, now, onClick = { onOrderClick(order.id) })
                            }
                        }
                        ActivityTab.HISTORY -> {
                            if (state.toRateCount > 0) RateNudge(state.toRateCount)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = SgSpacing.Gutter),
                                horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm),
                            ) {
                                listOf(HistoryFilter.ALL to "Semua", HistoryFilter.COMPLETED to "Selesai", HistoryFilter.CANCELLED to "Dibatalkan")
                                    .forEach { (f, label) -> SgChip(label, state.filter == f, { viewModel.onFilter(f) }) }
                            }
                            if (state.history.isEmpty()) {
                                SgEmptyState(Icons.Rounded.History, "Belum ada riwayat", "Pesanan yang sudah selesai atau dibatalkan tersimpan di sini.")
                            }
                            state.history.forEach { order ->
                                HistoryCard(order, onClick = { onOrderClick(order.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImpactStrip(impact: ActivityImpact) {
    Row(
        modifier = Modifier
            .padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Lg)
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Card))
            .background(Brush.linearGradient(listOf(SgColor.Brand500, SgColor.Brand700)))
            .padding(SgSpacing.Lg),
        horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm),
    ) {
        ImpactStat(Icons.Rounded.Eco, impact.portions.toString(), "porsi\ndiselamatkan", Modifier.weight(1f))
        ImpactStat(Icons.Rounded.Savings, formatRupiah(impact.savedRupiah), "kamu\nhemat", Modifier.weight(1.3f))
        ImpactStat(Icons.Rounded.ReceiptLong, impact.completedOrders.toString(), "pesanan\nselesai", Modifier.weight(1f))
    }
}

@Composable
private fun ImpactStat(icon: ImageVector, value: String, label: String, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(
            Modifier.size(28.dp).background(Color.White.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp)) }
        Text(value, style = SgTextStyle.Title, color = Color.White, maxLines = 1)
        Text(label, style = SgTextStyle.Caption, color = Color.White.copy(alpha = 0.85f))
    }
}

@Composable
private fun ActivitySwitch(selected: ActivityTab, ongoingCount: Int, onSelect: (ActivityTab) -> Unit, modifier: Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(SgRadius.Pill))
            .background(SgColor.Hairline)
            .padding(4.dp),
    ) {
        val w = maxWidth / 2
        val x by animateDpAsState(w * selected.ordinal, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow), label = "actThumb")
        Box(
            Modifier.offset(x = x).width(w).fillMaxHeight()
                .shadow(2.dp, RoundedCornerShape(SgRadius.Pill))
                .background(SgColor.BaseWhite, RoundedCornerShape(SgRadius.Pill)),
        )
        Row(Modifier.fillMaxSize().selectableGroup()) {
            listOf(ActivityTab.ONGOING to "Berlangsung", ActivityTab.HISTORY to "Riwayat").forEach { (tab, label) ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(SgRadius.Pill))
                        .selectable(selected == tab, role = Role.Tab, onClick = { onSelect(tab) }),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, style = SgTextStyle.Label, color = if (selected == tab) SgColor.Ink else SgColor.InkMuted)
                    if (tab == ActivityTab.ONGOING && ongoingCount > 0) {
                        Text(
                            ongoingCount.toString(),
                            style = SgTextStyle.TextXsMedium,
                            color = Color.White,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .background(SgColor.Brand500, CircleShape)
                                .padding(horizontal = 7.dp, vertical = 1.dp),
                        )
                    }
                }
            }
        }
    }
}

/** "1j 35m lagi" / "12m lagi" / "Lewat waktu". */
fun remainingLabel(until: Instant, now: Instant): String {
    val d = Duration.between(now, until)
    if (d.isNegative || d.isZero) return "Lewat waktu"
    val h = d.toHours()
    val m = d.toMinutes() % 60
    return if (h > 0) "${h}j ${m}m lagi" else "${m}m lagi"
}

@Composable
private fun OngoingCard(order: Order, now: Instant, onClick: () -> Unit) {
    val urgent = Duration.between(now, order.pickupEnd).toMinutes() < 30
    Column(
        modifier = Modifier
            .padding(horizontal = SgSpacing.Gutter)
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Card))
            .background(SgColor.BaseWhite)
            .border(1.dp, SgColor.Hairline, RoundedCornerShape(SgRadius.Card))
            .pressable(onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (urgent) Color(0xFFFDECEC) else SgColor.Mint)
                .padding(horizontal = SgSpacing.Lg, vertical = SgSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = if (urgent) SgColor.RedStatus else SgColor.Brand700, modifier = Modifier.size(16.dp))
            Text(
                "Ambil sebelum ${formatClock(order.pickupEnd)}",
                style = SgTextStyle.Label,
                color = if (urgent) SgColor.RedStatus else SgColor.Brand700,
                modifier = Modifier.padding(start = 6.dp).weight(1f),
            )
            Text(remainingLabel(order.pickupEnd, now), style = SgTextStyle.Label, color = if (urgent) SgColor.RedStatus else SgColor.Brand700)
        }
        Row(
            modifier = Modifier.padding(SgSpacing.Lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
        ) {
            val first = order.lines.first()
            ListingImage(first.imageUrl, first.tier, null, Modifier.size(56.dp).clip(RoundedCornerShape(SgRadius.Thumb)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(order.merchant.name, style = SgTextStyle.Label)
                Text(order.lines.joinToString { "${it.quantity}× ${it.title}" }, style = SgTextStyle.Body, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(
            modifier = Modifier.padding(start = SgSpacing.Lg, end = SgSpacing.Lg, bottom = SgSpacing.Lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Kode pickup", style = SgTextStyle.Caption)
                Text(order.pickupCode, style = SgTextStyle.Title, color = SgColor.Ink)
            }
            SgButton("Lihat detail", onClick = onClick, style = SgButtonStyle.Secondary, height = 40.dp)
        }
    }
}

@Composable
private fun RateNudge(count: Int) {
    Row(
        modifier = Modifier
            .padding(horizontal = SgSpacing.Gutter)
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Tile))
            .background(Color(0xFFFFF8E1))
            .padding(SgSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xFFF5B301))
        Text(
            "$count pesanan menunggu rating darimu. Bantu penyedia makin baik!",
            style = SgTextStyle.Body.copy(color = SgColor.Ink),
            modifier = Modifier.padding(start = SgSpacing.Sm),
        )
    }
}

@Composable
private fun HistoryCard(order: Order, onClick: () -> Unit) {
    val (statusText, statusFg, statusBg) = when (order.status) {
        OrderStatus.COMPLETED -> Triple("Selesai", SgColor.Brand700, SgColor.Mint)
        OrderStatus.CANCELLED -> Triple("Dibatalkan", SgColor.RedStatus, Color(0xFFFDECEC))
        OrderStatus.READY -> Triple("Siap diambil", SgColor.Brand700, SgColor.Mint)
    }
    Column(
        modifier = Modifier
            .padding(horizontal = SgSpacing.Gutter)
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Card))
            .background(SgColor.BaseWhite)
            .border(1.dp, SgColor.Hairline, RoundedCornerShape(SgRadius.Card))
            .pressable(onClick)
            .padding(SgSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(SgSpacing.Md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatDate(order.createdAt), style = SgTextStyle.Caption, modifier = Modifier.weight(1f))
            Text(
                statusText,
                style = SgTextStyle.TextXsMedium,
                color = statusFg,
                modifier = Modifier.background(statusBg, RoundedCornerShape(SgRadius.Pill)).padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md)) {
            val first = order.lines.first()
            ListingImage(first.imageUrl, first.tier, null, Modifier.size(48.dp).clip(RoundedCornerShape(SgRadius.Thumb)))
            Column(Modifier.weight(1f)) {
                Text(order.merchant.name, style = SgTextStyle.Label)
                Text("${order.itemCount} item · ${formatPrice(order.total)}", style = SgTextStyle.Body)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = SgColor.InkMuted)
        }
        when {
            order.canRate -> SgButton(
                "Beri rating",
                onClick = onClick,
                style = SgButtonStyle.Secondary,
                height = 40.dp,
                modifier = Modifier.fillMaxWidth(),
                leading = { Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xFFF5B301), modifier = Modifier.size(18.dp)) },
            )
            order.rating != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(5) { i ->
                    Icon(
                        Icons.Rounded.Star,
                        contentDescription = null,
                        tint = if (i < order.rating.stars) Color(0xFFF5B301) else SgColor.Hairline,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    order.rating.comment.ifBlank { order.rating.tags.joinToString() },
                    style = SgTextStyle.Caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = SgSpacing.Sm),
                )
            }
        }
    }
}
