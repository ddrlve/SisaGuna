package com.sisaguna.android.feature.checkout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.rounded.DeliveryDining
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sisaguna.android.data.model.Courier
import com.sisaguna.android.data.model.DeliveryQuote
import com.sisaguna.android.data.model.DeliverySpeed
import com.sisaguna.android.data.model.Fulfillment
import com.sisaguna.android.ui.components.SgEaseOut
import com.sisaguna.android.ui.domain.unitSuffix
import com.sisaguna.android.ui.i18n.l
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.sisaguna.android.ui.i18n.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sisaguna.android.data.model.PaymentKind
import com.sisaguna.android.data.model.PaymentMethod
import com.sisaguna.android.ui.components.QuantityStepper
import com.sisaguna.android.ui.components.SgButton
import com.sisaguna.android.ui.components.SgButtonStyle
import com.sisaguna.android.ui.components.SgEmptyState
import com.sisaguna.android.ui.components.SgTopBar
import com.sisaguna.android.ui.domain.FakeQr
import com.sisaguna.android.ui.domain.VoucherTicket
import com.sisaguna.android.ui.components.pressable
import com.sisaguna.android.ui.domain.ListingImage
import com.sisaguna.android.ui.domain.formatClock
import com.sisaguna.android.ui.domain.formatPrice
import com.sisaguna.android.ui.domain.formatRupiah
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle

fun paymentIcon(kind: PaymentKind): ImageVector = when (kind) {
    PaymentKind.QRIS -> Icons.Rounded.QrCode2
    PaymentKind.GOPAY, PaymentKind.OVO, PaymentKind.DANA -> Icons.Rounded.AccountBalanceWallet
    PaymentKind.CASH -> Icons.Rounded.Payments
}

fun paymentLabel(kind: PaymentKind): String = when (kind) {
    PaymentKind.QRIS -> "QRIS"
    PaymentKind.GOPAY -> "GoPay"
    PaymentKind.OVO -> "OVO"
    PaymentKind.DANA -> "DANA"
    PaymentKind.CASH -> "Bayar saat ambil"
}

@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onExplore: () -> Unit,
    onPlaced: (String) -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showVouchers by remember { mutableStateOf(false) }

    LaunchedEffect(state.step) {
        when (val step = state.step) {
            is PaymentStep.Done -> onPlaced(step.orderId)
            is PaymentStep.Failed -> {
                snackbar.showSnackbar(step.message)
                viewModel.dismissPayment()
            }
            else -> Unit
        }
    }

    Scaffold(
        containerColor = SgColor.Page,
        // Renamed from "Keranjang" after user testing: by the time people land here they're
        // reviewing an order, not browsing a basket.
        topBar = { SgTopBar(title = "Order Summary", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (!state.isEmpty) {
                Column(Modifier.background(SgColor.BaseWhite).navigationBarsPadding()) {
                    HorizontalDivider(color = SgColor.Hairline)
                    Row(
                        modifier = Modifier.padding(horizontal = SgSpacing.Gutter, vertical = SgSpacing.Md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SgSpacing.Lg),
                    ) {
                        Column {
                            Text(if (state.selectedQuote != null) l("Total + ongkir", "Total incl. delivery") else "Total", style = SgTextStyle.Caption)
                            Text(formatPrice(state.total), style = SgTextStyle.Title, color = SgColor.Brand700)
                        }
                        val cta = when {
                            state.effectivePayment == PaymentKind.CASH -> "Pesan sekarang"
                            else -> "Bayar dengan ${paymentLabel(state.effectivePayment)}"
                        }
                        SgButton(text = cta, onClick = viewModel::pay, modifier = Modifier.weight(1f))
                    }
                }
            }
        },
    ) { padding ->
        if (state.isEmpty && state.step !is PaymentStep.Done) {
            SgEmptyState(
                icon = Icons.Rounded.ShoppingBag,
                title = "Keranjang masih kosong",
                body = "Yuk selamatkan makanan di sekitarmu sebelum waktunya habis.",
                modifier = Modifier.padding(padding),
                action = { SgButton("Cari makanan", onClick = onExplore) },
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = SgSpacing.Gutter, end = SgSpacing.Gutter, bottom = SgSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(SgSpacing.Md),
        ) {
            item {
                FulfillmentSection(
                    state = state,
                    onFulfillment = viewModel::setFulfillment,
                    onCourier = viewModel::setCourier,
                    onSpeed = viewModel::setSpeed,
                )
            }
            item { SectionTitle(l("Pesanan dari ", "Items from ") + state.merchant?.name.orEmpty()) }
            items(state.lines, key = { it.listing.id }) { line ->
                Card(modifier = Modifier.animateItem()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md)) {
                        ListingImage(
                            imageUrl = line.listing.imageUrl,
                            tier = line.listing.tier,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(SgRadius.Thumb)),
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(line.listing.title, style = SgTextStyle.Label, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(formatPrice(line.listing.unitPrice) + if (line.listing.isFree) "" else unitSuffix(line.listing), style = SgTextStyle.Label, color = SgColor.Brand600)
                                if (line.listing.unitOriginalPrice > line.listing.unitPrice) {
                                    Text(formatRupiah(line.listing.unitOriginalPrice), style = SgTextStyle.Caption.copy(textDecoration = TextDecoration.LineThrough))
                                }
                            }
                        }
                        QuantityStepper(
                            quantity = line.quantity,
                            onChange = { viewModel.setQuantity(line.listing.id, it) },
                            max = line.listing.stock,
                            min = 0,
                            compact = true,
                        )
                    }
                }
            }
            item {
                SectionTitle("Catatan untuk penyedia")
                OutlinedTextField(
                    value = state.note,
                    onValueChange = viewModel::onNoteChange,
                    placeholder = { Text("Contoh: saya ambil jam 6 sore", style = SgTextStyle.Body) },
                    textStyle = SgTextStyle.TextSmRegular.copy(color = SgColor.Ink),
                    shape = RoundedCornerShape(SgRadius.Thumb),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SgColor.Brand500,
                        unfocusedBorderColor = SgColor.Hairline,
                        focusedContainerColor = SgColor.BaseWhite,
                        unfocusedContainerColor = SgColor.BaseWhite,
                    ),
                    supportingText = { Text("${state.note.length}/140", style = SgTextStyle.Caption) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (state.total > 0) {
                item { SectionTitle("Metode pembayaran") }
                items(state.methods, key = { it.id }) { method ->
                    PaymentOption(method, selected = state.selected == method.kind, onSelect = { viewModel.select(method.kind) })
                }
            }
            if (state.subtotal > 0) {
                item {
                    SectionTitle("Voucher")
                    VoucherRow(state, onClick = { showVouchers = true })
                }
            }
            item {
                SectionTitle("Ringkasan")
                Card {
                    Column(verticalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                        SummaryRow("Harga normal (${state.itemCount} item)", formatRupiah(state.itemsTotal + state.savings))
                        SummaryRow("Diskon surplus", "−" + formatRupiah(state.savings - state.voucherDiscount), valueColor = SgColor.Brand600)
                        if (state.voucherDiscount > 0) {
                            SummaryRow("Voucher ${state.voucher?.code.orEmpty()}", "−" + formatRupiah(state.voucherDiscount), valueColor = SgColor.Brand600)
                        }
                        state.selectedQuote?.let { q ->
                            SummaryRow(l("Ongkir ", "Delivery ") + "${q.courier.label} · ${l(q.speed.label, q.speed.labelEn)}", formatRupiah(q.fee))
                            if (q.discount > 0) SummaryRow(l("Diskon ongkir", "Delivery discount"), "−" + formatRupiah(q.discount), valueColor = SgColor.Brand600)
                        }
                        HorizontalDivider(color = SgColor.Hairline)
                        SummaryRow("Total bayar", formatPrice(state.total), bold = true)
                    }
                }
            }
        }
    }

    if (showVouchers) {
        VoucherSheet(
            state = state,
            onSelect = {
                viewModel.selectVoucher(it)
                showVouchers = false
            },
            onDismiss = { showVouchers = false },
        )
    }

    when (val step = state.step) {
        PaymentStep.AwaitingQris -> QrisSheet(
            amount = state.total,
            onPaid = viewModel::confirmQrisPaid,
            onDismiss = viewModel::dismissPayment,
        )
        is PaymentStep.Processing -> ProcessingDialog(step.kind)
        else -> Unit
    }
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Tile))
            .background(SgColor.BaseWhite)
            .border(1.dp, SgColor.Hairline, RoundedCornerShape(SgRadius.Tile))
            .padding(SgSpacing.Md),
    ) { content() }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = SgTextStyle.Title, modifier = Modifier.padding(top = SgSpacing.Md, bottom = SgSpacing.Xs))
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color = SgColor.Ink, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = if (bold) SgTextStyle.Label else SgTextStyle.Body, modifier = Modifier.weight(1f))
        Text(value, style = if (bold) SgTextStyle.Title else SgTextStyle.Label, color = valueColor)
    }
}

@Composable
private fun PaymentOption(method: PaymentMethod, selected: Boolean, onSelect: () -> Unit) {
    val border by animateColorAsState(if (selected) SgColor.Brand500 else SgColor.Hairline, tween(150), label = "payBorder")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Tile))
            .background(if (selected) SgColor.Mint else SgColor.BaseWhite)
            .border(if (selected) 1.5.dp else 1.dp, border, RoundedCornerShape(SgRadius.Tile))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = SgSpacing.Md, vertical = SgSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
    ) {
        Box(Modifier.size(36.dp).background(SgColor.Page, CircleShape), contentAlignment = Alignment.Center) {
            Icon(paymentIcon(method.kind), contentDescription = null, tint = SgColor.Ink, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(method.label, style = SgTextStyle.Label)
            Text(method.detail, style = SgTextStyle.Caption, maxLines = 1)
        }
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = SgColor.Brand500, unselectedColor = SgColor.InkMuted),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QrisSheet(amount: Int, onPaid: () -> Unit, onDismiss: () -> Unit) {
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
            Text("Scan untuk bayar", style = SgTextStyle.Title)
            Text("Buka aplikasi bank atau e-wallet apa pun, lalu scan kode QRIS ini.", style = SgTextStyle.Body, textAlign = TextAlign.Center)
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .border(1.dp, SgColor.Hairline, RoundedCornerShape(SgRadius.Tile))
                    .padding(SgSpacing.Lg),
            ) { FakeQr(seed = amount) }
            Text(formatRupiah(amount), style = SgTextStyle.Display, color = SgColor.Brand700)
            Text("Ini simulasi. Tidak ada uang yang ditarik.", style = SgTextStyle.Caption)
            SgButton("Saya sudah bayar", onClick = onPaid, modifier = Modifier.fillMaxWidth())
            SgButton("Ganti metode", onClick = onDismiss, style = SgButtonStyle.Ghost, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ProcessingDialog(kind: PaymentKind) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(SgRadius.Card))
                .background(SgColor.BaseWhite)
                .padding(SgSpacing.Xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SgSpacing.Md),
        ) {
            CircularProgressIndicator(color = SgColor.Brand500)
            Text(
                when (kind) {
                    PaymentKind.QRIS -> "Mengecek pembayaran…"
                    PaymentKind.CASH -> "Membuat pesanan…"
                    else -> "Menghubungkan ke ${paymentLabel(kind)}…"
                },
                style = SgTextStyle.Label,
            )
        }
    }
}

@Composable
private fun VoucherRow(state: CheckoutUiState, onClick: () -> Unit) {
    val v = state.voucher
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Tile))
            .background(if (state.voucherDiscount > 0) SgColor.Mint else SgColor.BaseWhite)
            .border(1.dp, if (state.voucherDiscount > 0) SgColor.Brand300 else SgColor.Hairline, RoundedCornerShape(SgRadius.Tile))
            .pressable(onClick)
            .padding(SgSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
    ) {
        Box(Modifier.size(36.dp).background(SgColor.PromoTint, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.ConfirmationNumber, contentDescription = null, tint = SgColor.PromoInk, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            when {
                v == null -> {
                    Text("Pakai voucher", style = SgTextStyle.Label)
                    Text("${state.vouchers.size} voucher tersedia", style = SgTextStyle.Caption)
                }
                state.voucherDiscount > 0 -> {
                    Text(v.title, style = SgTextStyle.Label)
                    Text("Hemat ${formatRupiah(state.voucherDiscount)}", style = SgTextStyle.Caption, color = SgColor.Brand700)
                }
                else -> {
                    Text(v.title, style = SgTextStyle.Label)
                    Text("Belum memenuhi ${v.description.lowercase()}", style = SgTextStyle.Caption, color = SgColor.RedStatus)
                }
            }
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = SgColor.InkMuted)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoucherSheet(state: CheckoutUiState, onSelect: (String?) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SgColor.BaseWhite,
        scrimColor = SgColor.Ink.copy(alpha = 0.32f),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = SgSpacing.Gutter).padding(bottom = SgSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(SgSpacing.Md),
        ) {
            Text("Voucher kamu", style = SgTextStyle.Title)
            if (state.vouchers.isEmpty()) {
                Text("Belum ada voucher. Klaim voucher dari banner di Beranda.", style = SgTextStyle.Body)
            }
            state.vouchers.forEach { v ->
                val discount = v.discountFor(state.subtotal)
                val selected = state.voucher?.code == v.code
                VoucherTicket(
                    title = v.title,
                    subtitle = v.description,
                    trailing = if (discount > 0) "Hemat ${formatRupiah(discount)}" else "Belum memenuhi syarat",
                    trailingColor = if (discount > 0) SgColor.Brand700 else SgColor.InkMuted,
                    selected = selected,
                    onClick = { onSelect(if (selected) null else v.code) },
                )
            }
            if (state.voucher != null) {
                SgButton("Jangan pakai voucher", onClick = { onSelect(null) }, style = SgButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}


/**
 * Pickup or courier. Pickup states the latest arrival time ("ambil maks. 18.45, 45 menit
 * setelah pesan"); delivery shows Prioritas / Standar / Hemat with each one's fee and ETA,
 * then every courier's quote (cheapest first) with its promo and the discount struck through.
 */
@Composable
private fun FulfillmentSection(
    state: CheckoutUiState,
    onFulfillment: (Fulfillment) -> Unit,
    onCourier: (Courier) -> Unit,
    onSpeed: (DeliverySpeed) -> Unit,
) {
    val delivery = state.delivery.fulfillment == Fulfillment.DELIVERY && state.deliveryAvailable
    Column(verticalArrangement = Arrangement.spacedBy(SgSpacing.Md)) {
        SectionTitle(l("Cara terima pesanan", "How you'll get it"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md)) {
            ModeCard(
                icon = Icons.Rounded.Storefront,
                title = l("Ambil sendiri", "Self pickup"),
                sub = l("Gratis", "Free"),
                selected = !delivery,
                onClick = { onFulfillment(Fulfillment.PICKUP) },
                modifier = Modifier.weight(1f),
            )
            ModeCard(
                icon = Icons.Rounded.DeliveryDining,
                title = l("Kirim kurir", "Courier"),
                sub = if (state.deliveryAvailable) l("mulai ", "from ") + formatRupiah(state.quotes.minOfOrNull { it.payable } ?: 0) else l("Tidak tersedia", "Unavailable"),
                selected = delivery,
                enabled = state.deliveryAvailable,
                onClick = { onFulfillment(Fulfillment.DELIVERY) },
                modifier = Modifier.weight(1f),
            )
        }
        AnimatedContent(
            targetState = delivery,
            transitionSpec = { fadeIn(tween(200, easing = SgEaseOut)) togetherWith fadeOut(tween(90)) using SizeTransform(clip = false) },
            label = "fulfillment",
        ) { isDelivery ->
            if (!isDelivery) {
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md)) {
                        Box(Modifier.size(40.dp).background(SgColor.Mint, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = SgColor.Brand600, modifier = Modifier.size(20.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(l("Ambil di ", "Pick up at ") + state.merchant?.name.orEmpty(), style = SgTextStyle.Label)
                            Text(state.merchant?.address?.ifBlank { null } ?: state.merchant?.location.orEmpty(), style = SgTextStyle.Caption, maxLines = 1)
                            state.pickupBy?.let { by ->
                                val mins = java.time.Duration.between(state.now, by).toMinutes()
                                Text(
                                    l("Ambil maks. ${formatClock(by)} · $mins menit setelah pesan", "Collect by ${formatClock(by)} · $mins min after ordering"),
                                    style = SgTextStyle.Caption.copy(fontWeight = FontWeight.SemiBold),
                                    color = SgColor.YellowStatus,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            Text(
                                l("Siap diambil ±${state.merchant?.prepMinutes ?: 15} menit", "Ready in ~${state.merchant?.prepMinutes ?: 15} min"),
                                style = SgTextStyle.Caption,
                            )
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(SgSpacing.Md)) {
                    Card {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md)) {
                            Box(Modifier.size(40.dp).background(SgColor.RedStatus.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = SgColor.RedStatus, modifier = Modifier.size(20.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(l("Kirim ke ", "Deliver to ") + (state.address?.label ?: l("lokasi kamu", "your location")), style = SgTextStyle.Label)
                                Text(state.address?.fullAddress.orEmpty(), style = SgTextStyle.Caption, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("%.1f km ".format(state.distanceKm) + l("dari toko", "from the store"), style = SgTextStyle.Caption)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                        state.speedQuotes.forEach { q ->
                            SpeedCard(q, selected = q.speed == state.delivery.speed, onClick = { onSpeed(q.speed) }, modifier = Modifier.weight(1f))
                        }
                    }
                    Text(l(state.delivery.speed.blurb, state.delivery.speed.blurb), style = SgTextStyle.Caption)
                    state.quotes.forEach { q ->
                        CourierRow(q, selected = q.courier == state.delivery.courier, onClick = { onCourier(q.courier) })
                    }
                    Text(
                        l("Ongkir adalah estimasi dari mitra kurir dan bisa berubah saat pesanan dijemput.", "Fees are courier estimates and may change at pickup."),
                        style = SgTextStyle.Caption.copy(fontSize = 11.sp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeCard(
    icon: ImageVector,
    title: String,
    sub: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val border by animateColorAsState(if (selected) SgColor.Brand500 else SgColor.Hairline, tween(150), label = "modeBorder")
    val bg by animateColorAsState(if (selected) SgColor.Mint else SgColor.BaseWhite, tween(150), label = "modeBg")
    Column(
        modifier
            .clip(RoundedCornerShape(SgRadius.Tile))
            .background(bg)
            .border(if (selected) 1.5.dp else 1.dp, border, RoundedCornerShape(SgRadius.Tile))
            .alpha(if (enabled) 1f else 0.45f)
            .pressable(onClick, enabled = enabled)
            .padding(SgSpacing.Md),
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) SgColor.Brand600 else SgColor.InkMuted)
        Text(title, style = SgTextStyle.Label, modifier = Modifier.padding(top = 6.dp))
        Text(sub, style = SgTextStyle.Caption)
    }
}

@Composable
private fun SpeedCard(q: DeliveryQuote, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val border by animateColorAsState(if (selected) SgColor.Ink else SgColor.Hairline, tween(150), label = "speedBorder")
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SgColor.BaseWhite)
            .border(if (selected) 1.5.dp else 1.dp, border, RoundedCornerShape(14.dp))
            .pressable(onClick, pressedScale = 0.96f)
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Text(l(q.speed.label, q.speed.labelEn), style = SgTextStyle.Label.copy(fontSize = 13.sp))
        Text("${q.etaMinMinutes}-${q.etaMaxMinutes} " + l("mnt", "min"), style = SgTextStyle.Caption)
        Text(formatRupiah(q.payable), style = SgTextStyle.Label.copy(fontSize = 13.sp), color = SgColor.Brand700, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun CourierRow(q: DeliveryQuote, selected: Boolean, onClick: () -> Unit) {
    val border by animateColorAsState(if (selected) SgColor.Brand500 else SgColor.Hairline, tween(150), label = "courierBorder")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Tile))
            .background(SgColor.BaseWhite)
            .border(if (selected) 1.5.dp else 1.dp, border, RoundedCornerShape(SgRadius.Tile))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = SgSpacing.Md, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).background(Color(q.courier.brandColor), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.DeliveryDining, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(q.courier.label, style = SgTextStyle.Label)
            Text(
                l("Tiba ", "Arrives in ") + "${q.etaMinMinutes}-${q.etaMaxMinutes} " + l("mnt", "min") + (q.promoLabel?.let { " · $it" } ?: ""),
                style = SgTextStyle.Caption,
                color = if (q.promoLabel != null) SgColor.Brand700 else SgColor.InkMuted,
                maxLines = 1,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatRupiah(q.payable), style = SgTextStyle.Label)
            if (q.discount > 0) {
                Text(formatRupiah(q.fee), style = SgTextStyle.Caption.copy(textDecoration = TextDecoration.LineThrough))
            }
        }
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = SgColor.Brand500, unselectedColor = SgColor.InkMuted),
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
