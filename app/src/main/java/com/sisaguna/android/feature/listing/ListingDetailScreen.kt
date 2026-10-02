package com.sisaguna.android.feature.listing

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.ui.components.QuantityStepper
import com.sisaguna.android.ui.components.SgButton
import com.sisaguna.android.ui.components.SgEmptyState
import com.sisaguna.android.ui.components.pressable
import com.sisaguna.android.ui.domain.CartBar
import com.sisaguna.android.ui.domain.CountdownPill
import com.sisaguna.android.ui.domain.InitialAvatar
import com.sisaguna.android.ui.domain.ListingCard
import com.sisaguna.android.ui.domain.ListingImage
import com.sisaguna.android.ui.domain.MerchantSummary
import com.sisaguna.android.ui.domain.TierBadge
import com.sisaguna.android.ui.domain.discountPercent
import com.sisaguna.android.ui.domain.formatClock
import com.sisaguna.android.ui.domain.formatPrice
import com.sisaguna.android.ui.domain.formatRupiah
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle
import kotlinx.coroutines.launch

private val HeroHeight = 300.dp

@Composable
fun ListingDetailScreen(
    onBack: () -> Unit,
    onMerchantClick: (String) -> Unit,
    onListingClick: (String) -> Unit,
    onCartClick: () -> Unit,
    viewModel: ListingDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var replaceFrom by remember { mutableStateOf<String?>(null) }

    fun confirmAdded(qty: Int) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar("$qty item masuk keranjang", "Lihat", duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) onCartClick()
        }
    }

    Scaffold(
        containerColor = SgColor.Page,
        snackbarHost = { SnackbarHost(snackbar, modifier = Modifier.padding(bottom = 72.dp)) },
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            val s = state as? ListingDetailUiState.Success
            if (s != null) Column(modifier = Modifier.background(SgColor.Page).navigationBarsPadding()) {
                CartBar(
                    visible = !s.cart.isEmpty,
                    itemCount = s.cart.itemCount,
                    total = s.cartTotal,
                    merchantName = null,
                    onClick = onCartClick,
                )
                PurchaseBar(
                    state = s,
                    onQuantity = viewModel::setQuantity,
                    onAdd = {
                        when (val outcome = viewModel.addToCart()) {
                            is AddOutcome.Added -> confirmAdded(outcome.quantity)
                            is AddOutcome.NeedsReplace -> replaceFrom = outcome.currentMerchantName
                            null -> Unit
                        }
                    },
                )
            }
        },
    ) { padding ->
        when (val s = state) {
            ListingDetailUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SgColor.Brand500)
            }
            ListingDetailUiState.NotFound -> Column(Modifier.fillMaxSize()) {
                FloatingBack(onBack, Modifier.padding(SgSpacing.Lg))
                SgEmptyState(Icons.Rounded.SearchOff, "Makanan tidak ditemukan", "Mungkin sudah habis atau dihapus penyedianya.")
            }
            is ListingDetailUiState.Success -> DetailBody(s, padding, onBack, onMerchantClick, onListingClick)
        }
    }

    replaceFrom?.let { other ->
        AlertDialog(
            onDismissRequest = { replaceFrom = null },
            title = { Text("Ganti isi keranjang?", style = SgTextStyle.Title) },
            text = {
                Text(
                    "Keranjangmu berisi makanan dari $other. Satu pesanan hanya bisa diambil di satu tempat, jadi keranjang lama akan dikosongkan.",
                    style = SgTextStyle.Body,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.replaceCart()
                    replaceFrom = null
                    confirmAdded(1)
                }) { Text("Ganti keranjang", style = SgTextStyle.Label, color = SgColor.Brand600) }
            },
            dismissButton = {
                TextButton(onClick = { replaceFrom = null }) { Text("Batal", style = SgTextStyle.Label, color = SgColor.Ink) }
            },
            containerColor = SgColor.BaseWhite,
        )
    }
}

@Composable
private fun DetailBody(
    s: ListingDetailUiState.Success,
    padding: PaddingValues,
    onBack: () -> Unit,
    onMerchantClick: (String) -> Unit,
    onListingClick: (String) -> Unit,
) {
    val listing = s.listing
    val listState = rememberLazyListState()
    val heroPx = with(LocalDensity.current) { HeroHeight.toPx() }
    // Parallax: hero scrolls at half speed and fades as the sheet covers it.
    val heroOffset by remember {
        derivedStateOf { if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat() else heroPx }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + SgSpacing.Xl),
        ) {
            item(key = "hero") {
                ListingImage(
                    imageUrl = listing.imageUrl,
                    tier = listing.tier,
                    contentDescription = listing.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HeroHeight)
                        .graphicsLayer {
                            translationY = heroOffset * 0.5f
                            alpha = 1f - (heroOffset / heroPx).coerceIn(0f, 0.6f)
                        },
                )
            }
            item(key = "sheet") {
                Column(
                    modifier = Modifier
                        .offset(y = (-24).dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(SgColor.Page)
                        .padding(top = SgSpacing.Xl),
                ) {
                    HeaderBlock(s)
                    MerchantRow(s, onMerchantClick)
                    Section("Deskripsi") {
                        Text(
                            listing.description.ifBlank { "Penyedia belum menambahkan deskripsi." },
                            style = SgTextStyle.TextSmRegular.copy(color = SgColor.Ink),
                        )
                    }
                    Section("Detail pengambilan") {
                        Column(verticalArrangement = Arrangement.spacedBy(SgSpacing.Md)) {
                            val start = listing.pickupStart?.let { formatClock(it) }
                            InfoLine(Icons.Rounded.Schedule, "Waktu ambil", if (start != null) "Hari ini, $start – ${formatClock(listing.pickupEnd)}" else "Sampai ${formatClock(listing.pickupEnd)}")
                            InfoLine(Icons.Rounded.LocationOn, "Lokasi", s.detail.merchant.location + (listing.distanceKm?.let { " · %.1f km dari kamu".format(it) } ?: ""))
                            InfoLine(Icons.Rounded.Inventory2, "Stok", "Sisa ${listing.stock} porsi" + if (s.inCart > 0) " · ${s.inCart} di keranjangmu" else "")
                        }
                    }
                    TipsCard(listing.tier)
                    if (s.detail.moreFromMerchant.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = SgSpacing.Gutter, end = SgSpacing.Sm, top = SgSpacing.Xl),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Lainnya dari ${s.detail.merchant.name}", style = SgTextStyle.Title, modifier = Modifier.weight(1f))
                            TextButton(onClick = { onMerchantClick(s.detail.merchant.id) }) {
                                Text("Lihat toko", style = SgTextStyle.TextXsMedium, color = SgColor.Brand600)
                            }
                        }
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = SgSpacing.Gutter, vertical = SgSpacing.Sm),
                            horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
                        ) {
                            items(s.detail.moreFromMerchant, key = { it.id }) { other ->
                                ListingCard(listing = other, merchant = s.detail.merchant, now = s.now, onClick = { onListingClick(other.id) })
                            }
                        }
                    }
                }
            }
        }
        FloatingBack(onBack, Modifier.padding(SgSpacing.Lg))
    }
}

@Composable
private fun FloatingBack(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(SgColor.BaseWhite)
            .pressable(onBack, pressedScale = 0.9f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali", tint = SgColor.Ink)
    }
}

@Composable
private fun HeaderBlock(s: ListingDetailUiState.Success) {
    val listing = s.listing
    Column(
        modifier = Modifier.padding(horizontal = SgSpacing.Gutter),
        verticalArrangement = Arrangement.spacedBy(SgSpacing.Sm),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm), verticalAlignment = Alignment.CenterVertically) {
            TierBadge(tier = listing.tier)
            CountdownPill(pickupEnd = listing.pickupEnd, now = s.now)
        }
        Text(listing.title, style = SgTextStyle.Display)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
            Text(formatPrice(listing.unitPrice), style = SgTextStyle.Display, color = SgColor.Brand600)
            if (!listing.isFree && listing.priceOriginal != null && listing.priceOriginal > listing.unitPrice) {
                Text(
                    formatRupiah(listing.priceOriginal),
                    style = SgTextStyle.Body.copy(textDecoration = TextDecoration.LineThrough),
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            val pct = if (listing.isFree) 100 else discountPercent(listing.priceOriginal, listing.priceDiscounted)
            if (pct != null) {
                Text(
                    "Hemat $pct%",
                    style = SgTextStyle.TextXsMedium,
                    color = SgColor.RedStatus,
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .background(Color(0xFFFDECEC), RoundedCornerShape(SgRadius.Pill))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun MerchantRow(s: ListingDetailUiState.Success, onMerchantClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Xl)
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Tile))
            .background(SgColor.BaseWhite)
            .border(1.dp, SgColor.Hairline, RoundedCornerShape(SgRadius.Tile))
            .pressable({ onMerchantClick(s.detail.merchant.id) })
            .padding(SgSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
    ) {
        InitialAvatar(s.detail.merchant.name.first().uppercaseChar(), 44.dp, s.detail.merchant.isVerified)
        Column(modifier = Modifier.weight(1f)) {
            MerchantSummary(merchant = s.detail.merchant, distanceKm = s.listing.distanceKm, verifiedLabel = "Verified")
            Text("Lihat semua makanan di toko ini", style = SgTextStyle.Caption, color = SgColor.Brand600)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = SgColor.InkMuted)
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(SgSpacing.Sm),
    ) {
        Text(title, style = SgTextStyle.Title)
        content()
    }
}

@Composable
private fun InfoLine(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md)) {
        Box(Modifier.size(36.dp).background(SgColor.Mint, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = SgColor.Brand600, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(label, style = SgTextStyle.Caption)
            Text(value, style = SgTextStyle.Label)
        }
    }
}

@Composable
private fun TipsCard(tier: ListingTier) {
    val tips = when (tier) {
        ListingTier.HUMAN -> listOf("Bawa wadah atau tas sendiri", "Cek kondisi makanan saat ambil", "Sebaiknya dikonsumsi hari ini")
        ListingTier.ANIMAL_FEED -> listOf("Bawa karung atau wadah besar", "Pilah lagi sebelum diberikan ke ternak", "Bukan untuk konsumsi manusia")
        ListingTier.COMPOST -> listOf("Bawa wadah tertutup", "Campur dengan bahan cokelat (daun kering)", "Bukan untuk konsumsi")
    }
    Column(
        modifier = Modifier
            .padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Xl)
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Tile))
            .background(Color(0xFFFFF8E1))
            .padding(SgSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = Color(0xFFB7791F), modifier = Modifier.size(18.dp))
            Text("Yang perlu kamu tahu", style = SgTextStyle.Label, modifier = Modifier.padding(start = 6.dp))
        }
        tips.forEach { Text("•  $it", style = SgTextStyle.Body.copy(color = SgColor.Ink)) }
    }
}

@Composable
private fun PurchaseBar(
    state: ListingDetailUiState.Success,
    onQuantity: (Int) -> Unit,
    onAdd: () -> Unit,
) {
    Column {
        HorizontalDivider(color = SgColor.Hairline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SgColor.BaseWhite)
                .padding(horizontal = SgSpacing.Gutter, vertical = SgSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
        ) {
            if (state.canAdd) {
                QuantityStepper(quantity = state.quantity, onChange = onQuantity, max = state.maxQuantity)
            }
            val label = when {
                state.isExpired -> "Waktu ambil sudah lewat"
                state.isSoldOut -> if (state.inCart > 0) "Semua stok ada di keranjang" else "Habis"
                else -> "Tambah · ${formatPrice(state.listing.unitPrice * state.quantity)}"
            }
            SgButton(text = label, onClick = onAdd, enabled = state.canAdd, modifier = Modifier.weight(1f))
        }
    }
}
