package com.sisaguna.android.feature.activity

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sisaguna.android.data.model.Order
import com.sisaguna.android.data.model.OrderStatus
import com.sisaguna.android.feature.checkout.paymentLabel
import com.sisaguna.android.ui.components.SgButton
import com.sisaguna.android.ui.components.SgButtonStyle
import com.sisaguna.android.ui.components.SgChip
import com.sisaguna.android.ui.components.SgEmptyState
import com.sisaguna.android.ui.components.SgTopBar
import com.sisaguna.android.ui.components.pressable
import com.sisaguna.android.ui.domain.FakeQr
import com.sisaguna.android.ui.domain.ListingImage
import com.sisaguna.android.ui.domain.formatClock
import com.sisaguna.android.ui.domain.formatDateTime
import com.sisaguna.android.ui.domain.formatPrice
import com.sisaguna.android.ui.domain.formatRupiah
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle
import java.time.Instant
import kotlinx.coroutines.launch

private val Gold = Color(0xFFF5B301)

@Composable
fun OrderDetailScreen(
    onBack: () -> Unit,
    viewModel: OrderDetailViewModel = hiltViewModel(),
) {
    val order by viewModel.order.collectAsStateWithLifecycle()
    var showCancel by remember { mutableStateOf(false) }
    var showRating by remember { mutableStateOf(false) }
    var showPickedUp by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = SgColor.Page,
        topBar = { SgTopBar(title = "Detail pesanan", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        val o = order
        if (o == null) {
            SgEmptyState(Icons.Rounded.Cancel, "Pesanan tidak ditemukan", "Mungkin sudah dihapus.", Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = SgSpacing.Gutter, end = SgSpacing.Gutter, bottom = SgSpacing.Xxl),
            verticalArrangement = Arrangement.spacedBy(SgSpacing.Md),
        ) {
            if (viewModel.justPlaced) item { SuccessBanner() }
            item { StatusHeader(o) }
            if (o.status == OrderStatus.READY) {
                item { PickupCodeCard(o) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                        SgButton("Sudah saya ambil", onClick = { showPickedUp = true }, modifier = Modifier.fillMaxWidth())
                        SgButton("Batalkan pesanan", onClick = { showCancel = true }, style = SgButtonStyle.Ghost, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            if (o.canRate) item { RatePrompt(onStar = { showRating = true }) }
            o.rating?.let { r -> item { RatedCard(r.stars, r.tags, r.comment) } }
            item { MerchantCard(o) }
            item { ItemsCard(o) }
            item { Timeline(o) }
        }
    }

    if (showPickedUp) {
        ConfirmDialog(
            title = "Sudah ambil pesananmu?",
            body = "Tandai selesai setelah makanan ada di tanganmu. Setelah ini kamu bisa memberi rating.",
            confirm = "Ya, sudah",
            onConfirm = {
                showPickedUp = false
                viewModel.markPickedUp()
                showRating = true
            },
            onDismiss = { showPickedUp = false },
        )
    }
    if (showCancel) {
        ConfirmDialog(
            title = "Batalkan pesanan?",
            body = "Makanan akan ditawarkan lagi ke orang lain. Pembayaran online dikembalikan dalam 1×24 jam.",
            confirm = "Batalkan",
            destructive = true,
            onConfirm = {
                showCancel = false
                viewModel.cancel()
            },
            onDismiss = { showCancel = false },
        )
    }
    if (showRating) {
        order?.let { o ->
            RatingSheet(
                merchantName = o.merchant.name,
                onSubmit = { stars, tags, comment ->
                    viewModel.rate(stars, tags, comment)
                    showRating = false
                    scope.launch { snackbar.showSnackbar("Terima kasih atas ratingnya!") }
                },
                onDismiss = { showRating = false },
            )
        }
    }
}

@Composable
private fun SuccessBanner() {
    val scale = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow)) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = SgSpacing.Sm)
            .clip(RoundedCornerShape(SgRadius.Card))
            .background(SgColor.Mint)
            .padding(SgSpacing.Lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SgSpacing.Xs),
    ) {
        Icon(
            Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = SgColor.Brand500,
            modifier = Modifier.size(56.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        )
        Text("Pesanan berhasil!", style = SgTextStyle.Title)
        Text("Kamu baru saja menyelamatkan makanan dari terbuang. 🌱", style = SgTextStyle.Body, textAlign = TextAlign.Center)
    }
}

@Composable
private fun StatusHeader(o: Order) {
    val (title, body) = when (o.status) {
        OrderStatus.READY -> "Siap diambil" to "Ambil sebelum ${formatClock(o.pickupEnd)} di ${o.merchant.name}."
        OrderStatus.COMPLETED -> "Pesanan selesai" to "Diambil ${o.completedAt?.let { formatDateTime(it) } ?: ""}."
        OrderStatus.CANCELLED -> "Pesanan dibatalkan" to "Dibatalkan. Pembayaran online sudah dikembalikan."
    }
    Column(Modifier.padding(top = SgSpacing.Sm)) {
        Text(title, style = SgTextStyle.Display, color = if (o.status == OrderStatus.CANCELLED) SgColor.RedStatus else SgColor.Ink)
        Text(body, style = SgTextStyle.Body)
    }
}

@Composable
private fun PickupCodeCard(o: Order) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Card))
            .background(SgColor.BaseWhite)
            .border(1.dp, SgColor.Hairline, RoundedCornerShape(SgRadius.Card))
            .padding(SgSpacing.Lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SgSpacing.Sm),
    ) {
        Text("Tunjukkan ke penyedia", style = SgTextStyle.Caption)
        Box(Modifier.size(160.dp).padding(SgSpacing.Sm)) { FakeQr(seed = o.pickupCode.hashCode()) }
        Text(o.pickupCode, style = SgTextStyle.Display.copy(letterSpacing = 2.sp))
        Text("Sisa waktu " + remainingLabel(o.pickupEnd, Instant.now()), style = SgTextStyle.Caption, color = SgColor.Brand700)
    }
}

@Composable
private fun RatePrompt(onStar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Card))
            .background(Color(0xFFFFF8E1))
            .pressable(onStar)
            .padding(SgSpacing.Lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SgSpacing.Sm),
    ) {
        Text("Bagaimana pengalamanmu?", style = SgTextStyle.Title)
        Row(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Xs)) {
            repeat(5) { Icon(Icons.Rounded.StarOutline, contentDescription = null, tint = Gold, modifier = Modifier.size(36.dp)) }
        }
        Text("Ketuk untuk memberi rating", style = SgTextStyle.Caption)
    }
}

@Composable
private fun RatedCard(stars: Int, tags: List<String>, comment: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Card))
            .background(SgColor.BaseWhite)
            .border(1.dp, SgColor.Hairline, RoundedCornerShape(SgRadius.Card))
            .padding(SgSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(SgSpacing.Sm),
    ) {
        Text("Rating kamu", style = SgTextStyle.Label)
        Row {
            repeat(5) { i -> Icon(Icons.Rounded.Star, contentDescription = null, tint = if (i < stars) Gold else SgColor.Hairline, modifier = Modifier.size(22.dp)) }
            Text(ratingLabel(stars), style = SgTextStyle.Label, modifier = Modifier.padding(start = SgSpacing.Sm))
        }
        if (tags.isNotEmpty()) Text(tags.joinToString(" · "), style = SgTextStyle.Body)
        if (comment.isNotBlank()) Text("“$comment”", style = SgTextStyle.Body.copy(color = SgColor.Ink))
    }
}

@Composable
private fun MerchantCard(o: Order) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Tile))
            .background(SgColor.BaseWhite)
            .border(1.dp, SgColor.Hairline, RoundedCornerShape(SgRadius.Tile))
            .padding(SgSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(o.merchant.name, style = SgTextStyle.Label)
            Text(o.merchant.location, style = SgTextStyle.Body)
        }
        SgButton(
            "Rute",
            onClick = {
                val uri = Uri.parse("geo:0,0?q=" + Uri.encode("${o.merchant.name}, ${o.merchant.location}"))
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            },
            style = SgButtonStyle.Secondary,
            height = 40.dp,
            leading = { Icon(Icons.Rounded.Directions, contentDescription = null, tint = SgColor.Brand600, modifier = Modifier.size(18.dp)) },
        )
    }
}

@Composable
private fun ItemsCard(o: Order) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Tile))
            .background(SgColor.BaseWhite)
            .border(1.dp, SgColor.Hairline, RoundedCornerShape(SgRadius.Tile))
            .padding(SgSpacing.Md),
        verticalArrangement = Arrangement.spacedBy(SgSpacing.Md),
    ) {
        o.lines.forEach { line ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md)) {
                ListingImage(line.imageUrl, line.tier, null, Modifier.size(44.dp).clip(RoundedCornerShape(SgRadius.Thumb)))
                Text("${line.quantity}× ${line.title}", style = SgTextStyle.Label, modifier = Modifier.weight(1f))
                Text(formatPrice(line.total), style = SgTextStyle.Label)
            }
        }
        HorizontalDivider(color = SgColor.Hairline)
        Row { Text("Pembayaran", style = SgTextStyle.Body, modifier = Modifier.weight(1f)); Text(paymentLabel(o.payment), style = SgTextStyle.Label) }
        if (o.savings > 0) Row { Text("Kamu hemat", style = SgTextStyle.Body, modifier = Modifier.weight(1f)); Text(formatRupiah(o.savings), style = SgTextStyle.Label, color = SgColor.Brand600) }
        Row { Text("Total", style = SgTextStyle.Label, modifier = Modifier.weight(1f)); Text(formatPrice(o.total), style = SgTextStyle.Title) }
        if (o.note.isNotBlank()) Text("Catatan: ${o.note}", style = SgTextStyle.Body)
    }
}

@Composable
private fun Timeline(o: Order) {
    val steps = buildList {
        add("Pesanan dibuat" to formatDateTime(o.createdAt))
        add("Siap diambil" to "Sampai ${formatClock(o.pickupEnd)}")
        when (o.status) {
            OrderStatus.COMPLETED -> add("Sudah diambil" to (o.completedAt?.let { formatDateTime(it) } ?: ""))
            OrderStatus.CANCELLED -> add("Dibatalkan" to "")
            OrderStatus.READY -> add("Menunggu diambil" to "")
        }
    }
    val doneCount = when (o.status) {
        OrderStatus.READY -> 2
        else -> 3
    }
    Column(Modifier.padding(top = SgSpacing.Sm)) {
        Text("Status", style = SgTextStyle.Title, modifier = Modifier.padding(bottom = SgSpacing.Sm))
        steps.forEachIndexed { i, (label, time) ->
            val done = i < doneCount
            val color = when {
                o.status == OrderStatus.CANCELLED && i == steps.lastIndex -> SgColor.RedStatus
                done -> SgColor.Brand500
                else -> SgColor.Hairline
            }
            Row {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(14.dp).background(color, CircleShape))
                    if (i < steps.lastIndex) Box(Modifier.width(2.dp).height(36.dp).background(if (i + 1 < doneCount) SgColor.Brand500 else SgColor.Hairline))
                }
                Column(Modifier.padding(start = SgSpacing.Md)) {
                    Text(label, style = SgTextStyle.Label, color = if (done) SgColor.Ink else SgColor.InkMuted)
                    if (time.isNotBlank()) Text(time, style = SgTextStyle.Caption)
                }
            }
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = SgTextStyle.Title) },
        text = { Text(body, style = SgTextStyle.Body) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirm, style = SgTextStyle.Label, color = if (destructive) SgColor.RedStatus else SgColor.Brand600)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Kembali", style = SgTextStyle.Label, color = SgColor.Ink) } },
        containerColor = SgColor.BaseWhite,
    )
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun RatingSheet(
    merchantName: String,
    onSubmit: (Int, List<String>, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var stars by remember { mutableIntStateOf(0) }
    val tags = remember { mutableStateListOf<String>() }
    var comment by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SgColor.BaseWhite,
        scrimColor = SgColor.Ink.copy(alpha = 0.32f),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = SgSpacing.Gutter).padding(bottom = SgSpacing.Xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SgSpacing.Md),
        ) {
            Text("Beri rating untuk $merchantName", style = SgTextStyle.Title, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Xs)) {
                (1..5).forEach { n ->
                    val scale by animateFloatAsState(
                        if (n <= stars) 1.15f else 1f,
                        spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium),
                        label = "star$n",
                    )
                    Icon(
                        if (n <= stars) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                        contentDescription = "$n bintang",
                        tint = Gold,
                        modifier = Modifier
                            .size(44.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .clip(CircleShape)
                            .pressable({
                                if ((stars >= 4) != (n >= 4)) tags.clear()
                                stars = n
                            }, pressedScale = 0.85f),
                    )
                }
            }
            Text(ratingLabel(stars), style = SgTextStyle.Label, color = if (stars == 0) SgColor.InkMuted else SgColor.Ink)
            AnimatedVisibility(visible = stars > 0, enter = fadeIn(tween(180)) + expandVertically(tween(200))) {
                Column(verticalArrangement = Arrangement.spacedBy(SgSpacing.Md)) {
                    Text(if (stars >= 4) "Apa yang kamu suka?" else "Apa yang perlu diperbaiki?", style = SgTextStyle.Label)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm), verticalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                        ratingTags(stars).forEach { tag ->
                            SgChip(tag, tag in tags, { if (tag in tags) tags.remove(tag) else tags.add(tag) })
                        }
                    }
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it.take(200) },
                        placeholder = { Text("Ceritakan pengalamanmu (opsional)", style = SgTextStyle.Body) },
                        textStyle = SgTextStyle.TextSmRegular.copy(color = SgColor.Ink),
                        minLines = 3,
                        shape = RoundedCornerShape(SgRadius.Thumb),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SgColor.Brand500, unfocusedBorderColor = SgColor.Hairline),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            SgButton(
                "Kirim rating",
                onClick = { onSubmit(stars, tags.toList(), comment) },
                enabled = stars > 0,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
