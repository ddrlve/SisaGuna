package com.sisaguna.android.feature.activity

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.ReportProblem
import com.sisaguna.android.data.model.Complaint
import com.sisaguna.android.data.model.ComplaintReason
import com.sisaguna.android.data.model.ComplaintStatus
import com.sisaguna.android.data.model.Fulfillment
import com.sisaguna.android.ui.i18n.l
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
import com.sisaguna.android.ui.i18n.Text
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
    var showComplaint by remember { mutableStateOf(false) }
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
            o.complaint?.let { c -> item { ComplaintStatusCard(c) } }
            if (o.canComplain) item { ComplaintPrompt(onClick = { showComplaint = true }) }
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
    if (showComplaint) {
        ComplaintSheet(
            onSubmit = { reason, detail, photos ->
                viewModel.complain(reason, detail, photos)
                showComplaint = false
                scope.launch { snackbar.showSnackbar(l("Komplain terkirim. Kami tinjau dalam 1×24 jam.", "Complaint sent. We'll review it within 24h.")) }
            },
            onDismiss = { showComplaint = false },
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
        OrderStatus.READY -> if (o.fulfillment == Fulfillment.DELIVERY && o.delivery != null) {
            l("Sedang disiapkan", "Being prepared") to l(
                "${o.delivery.courier.label} ${o.delivery.speed.label.lowercase()} · tiba ±${o.delivery.etaMinMinutes}–${o.delivery.etaMaxMinutes} menit ke ${o.deliveryAddress?.label ?: "alamatmu"}.",
                "${o.delivery.courier.label} ${o.delivery.speed.labelEn.lowercase()} · arrives in ~${o.delivery.etaMinMinutes}–${o.delivery.etaMaxMinutes} min.",
            )
        } else {
            "Siap diambil" to l(
                "Ambil maks. ${formatClock(o.pickupBy ?: o.pickupEnd)} di ${o.merchant.name}.",
                "Collect by ${formatClock(o.pickupBy ?: o.pickupEnd)} at ${o.merchant.name}.",
            )
        }
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
        // QR always sits on white so scanners read it in dark mode too.
        Box(Modifier.size(160.dp).background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(12.dp)).padding(SgSpacing.Sm)) { FakeQr(seed = o.pickupCode.hashCode()) }
        Text(o.pickupCode, style = SgTextStyle.Display.copy(letterSpacing = 2.sp))
        Text("Sisa waktu " + remainingLabel(o.pickupBy ?: o.pickupEnd, Instant.now()), style = SgTextStyle.Caption, color = SgColor.Brand700)
        if (o.fulfillment == Fulfillment.DELIVERY) {
            Text(l("Kurir akan menunjukkan kode ini ke penyedia.", "The courier shows this code to the store."), style = SgTextStyle.Caption)
        }
    }
}

@Composable
private fun RatePrompt(onStar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Card))
            .background(SgColor.Yellow50)
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


/** Entry to the complaint flow, under the rating. Quiet (outlined), because most orders are fine. */
@Composable
private fun ComplaintPrompt(onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Card))
            .background(SgColor.BaseWhite)
            .border(1.dp, SgColor.Hairline, RoundedCornerShape(SgRadius.Card))
            .pressable(onClick)
            .padding(SgSpacing.Lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).background(SgColor.RedStatus.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.ReportProblem, contentDescription = null, tint = SgColor.RedStatus, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(l("Ada masalah dengan makanannya?", "Something wrong with the food?"), style = SgTextStyle.Label)
            Text(l("Laporkan dalam 24 jam — basi, tidak sesuai, atau kurang. Dana kembali kalau terbukti.", "Report within 24h — spoiled, wrong or missing. Refund if confirmed."), style = SgTextStyle.Caption)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = SgColor.InkMuted)
    }
}

@Composable
private fun ComplaintStatusCard(c: Complaint) {
    val steps = ComplaintStatus.entries
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Card))
            .background(SgColor.BaseWhite)
            .border(1.dp, SgColor.RedStatus.copy(alpha = 0.3f), RoundedCornerShape(SgRadius.Card))
            .padding(SgSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(l("Komplain kamu", "Your complaint"), style = SgTextStyle.Caption)
        Text(c.reason.label, style = SgTextStyle.Title)
        if (c.detail.isNotBlank()) Text(c.detail, style = SgTextStyle.Body)
        if (c.photos.isNotEmpty()) {
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                c.photos.take(3).forEach { p ->
                    coil.compose.AsyncImage(p, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)))
                }
            }
        }
        steps.forEachIndexed { i, st ->
            val reached = i <= steps.indexOf(c.status)
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(if (reached) SgColor.Brand500 else SgColor.Neutral300, CircleShape))
                Text(st.label, style = SgTextStyle.Caption.copy(color = if (reached) SgColor.Ink else SgColor.InkMuted), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/**
 * Complaint form (user testing: "tambah fitur komplain"). Pick a reason, describe it, add
 * proof photos (camera or gallery). "Makanan basi" is first because it's the one that matters
 * most for food safety and feeds back into the store's safety record.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ComplaintSheet(
    onSubmit: (ComplaintReason, String, List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var reason by remember { mutableStateOf<ComplaintReason?>(null) }
    var detail by remember { mutableStateOf("") }
    val photos = remember { androidx.compose.runtime.mutableStateListOf<android.net.Uri>() }
    val media = com.sisaguna.android.ui.components.rememberMediaCapture(onPhotos = { uris -> uris.forEach { if (photos.size < 3) photos.add(it) } }, maxPick = 3)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SgColor.BaseWhite,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = SgSpacing.Gutter),
            verticalArrangement = Arrangement.spacedBy(SgSpacing.Md),
        ) {
            Text(l("Laporkan masalah", "Report a problem"), style = SgTextStyle.Title)
            Text(l("Apa yang terjadi?", "What happened?"), style = SgTextStyle.Caption)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ComplaintReason.entries.forEach { r ->
                    val on = r == reason
                    Text(
                        r.label,
                        style = SgTextStyle.TextXsMedium,
                        color = if (on) SgColor.RedStatus else SgColor.Ink,
                        modifier = Modifier
                            .clip(RoundedCornerShape(SgRadius.Pill))
                            .background(if (on) SgColor.RedStatus.copy(alpha = 0.1f) else SgColor.Page)
                            .border(1.dp, if (on) SgColor.RedStatus else SgColor.Hairline, RoundedCornerShape(SgRadius.Pill))
                            .pressable({ reason = r })
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
            OutlinedTextField(
                value = detail,
                onValueChange = { detail = it.take(400) },
                placeholder = { androidx.compose.material3.Text(l("Ceritakan detailnya, misal: bau asam saat dibuka jam 19.10", "Details, e.g. smelled sour when opened at 7.10pm"), color = SgColor.Neutral400, style = SgTextStyle.TextSmRegular) },
                minLines = 3,
                textStyle = SgTextStyle.TextSmRegular.copy(color = SgColor.Ink),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SgColor.Brand500, unfocusedBorderColor = SgColor.Hairline),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(l("Foto bukti (maks. 3)", "Photo proof (max 3)"), style = SgTextStyle.Caption)
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                photos.forEach { uri ->
                    coil.compose.AsyncImage(uri, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)).pressable({ photos.remove(uri) }))
                }
                if (photos.size < 3) {
                    com.sisaguna.android.feature.saved.MediaButton(Icons.Rounded.PhotoCamera, l("Kamera", "Camera"), media.takePhoto)
                    com.sisaguna.android.feature.saved.MediaButton(Icons.Rounded.PhotoLibrary, l("Galeri", "Gallery"), media.pickPhotos)
                }
            }
            SgButton(
                text = l("Kirim komplain", "Send complaint"),
                enabled = reason != null,
                onClick = { reason?.let { onSubmit(it, detail, photos.map(android.net.Uri::toString)) } },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                l("Tim SisaGuna meninjau dalam 1×24 jam. Laporan makanan basi juga menurunkan skor kelayakan toko.", "We review within 24h. Spoiled-food reports also lower the store's safety score."),
                style = SgTextStyle.Caption,
                modifier = Modifier.padding(bottom = SgSpacing.Lg),
            )
        }
    }
}
