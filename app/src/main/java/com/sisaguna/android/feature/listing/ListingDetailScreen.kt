package com.sisaguna.android.feature.listing

import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.sisaguna.android.data.model.FoodSafety
import com.sisaguna.android.data.model.HalalStatus
import com.sisaguna.android.data.model.QuantityUnit
import com.sisaguna.android.ui.components.SgButtonStyle
import com.sisaguna.android.ui.domain.AllergenChips
import com.sisaguna.android.ui.domain.HalalBadge
import com.sisaguna.android.ui.domain.ReviewCard
import com.sisaguna.android.ui.domain.SafetyCard
import com.sisaguna.android.ui.domain.SafetyChip
import com.sisaguna.android.ui.domain.unitSuffix
import com.sisaguna.android.ui.i18n.l
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
import com.sisaguna.android.ui.i18n.SgSnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.sisaguna.android.ui.i18n.Text
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
            val r = snackbar.showSnackbar(l("$qty item masuk keranjang", "$qty item(s) added to your order"), l("Lihat", "View"), duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) onCartClick()
        }
    }

    Scaffold(
        containerColor = SgColor.Page,
        snackbarHost = { SgSnackbarHost(snackbar) },
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
                    l(
                        "Keranjangmu berisi makanan dari $other. Satu pesanan hanya bisa diambil di satu tempat, jadi keranjang lama akan dikosongkan.",
                        "Your order has food from $other. One order can only be collected from one place, so the old one will be cleared.",
                    ),
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
                PhotoPager(
                    photos = listing.photos.ifEmpty { listOf("") },
                    tier = listing.tier,
                    title = listing.title,
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
                    Section(l("Cek kelayakan", "Is it safe to eat?")) {
                        SafetyCard(
                            assessment = FoodSafety.assess(listing, s.now),
                            madeAt = listing.madeAt,
                            storage = listing.storage,
                            now = s.now,
                        )
                    }
                    // Halal only means something for food people eat; feed/compost shows allergens only.
                    Section(if (listing.tier == ListingTier.HUMAN) l("Halal & alergen", "Halal & allergens") else l("Alergen", "Allergens")) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (listing.tier == ListingTier.HUMAN) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    HalalBadge(listing.halal)
                                    Text(
                                        when (listing.halal) {
                                            HalalStatus.HALAL_CERTIFIED -> l("Bersertifikat halal", "Halal certified")
                                            HalalStatus.NON_HALAL -> l("Mengandung bahan non-halal", "Contains non-halal ingredients")
                                            HalalStatus.UNVERIFIED -> l("Penyedia belum punya sertifikat", "Seller isn't certified yet")
                                            HalalStatus.OTHER -> l("Lainnya, tanya penyedia", "Other, ask the seller")
                                        },
                                        style = SgTextStyle.Caption,
                                        modifier = Modifier.padding(start = 8.dp),
                                    )
                                }
                            }
                            Text(l("Mengandung", "Contains"), style = SgTextStyle.Caption)
                            AllergenChips(listing.allergens)
                            val kitchen = s.detail.merchant.allergens - listing.allergens
                            if (kitchen.isNotEmpty()) {
                                Text(
                                    l("Dapur ini juga mengolah: ", "This kitchen also handles: ") + kitchen.joinToString { it.label } +
                                        l(". Kemungkinan kontaminasi silang.", ". Cross-contact is possible."),
                                    style = SgTextStyle.Caption,
                                )
                            }
                        }
                    }
                    Section("Deskripsi") {
                        Text(
                            listing.description.ifBlank { "Penyedia belum menambahkan deskripsi." },
                            style = SgTextStyle.TextSmRegular.copy(color = SgColor.Ink),
                        )
                    }
                    Section("Detail pengambilan") {
                        Column(verticalArrangement = Arrangement.spacedBy(SgSpacing.Md)) {
                            val start = listing.pickupStart?.let { formatClock(it) }
                            InfoLine(Icons.Rounded.Schedule, "Waktu ambil", if (start != null) "Hari ini, $start sampai ${formatClock(listing.pickupEnd)}" else "Sampai ${formatClock(listing.pickupEnd)}")
                            InfoLine(Icons.Rounded.LocationOn, "Lokasi", s.detail.merchant.location + (listing.distanceKm?.let { " · %.1f km dari kamu".format(it) } ?: ""))
                            InfoLine(Icons.Rounded.Inventory2, l("Stok", "Stock"), l("Sisa ", "") + "${listing.stock} ${unitWord(listing.unit)}" + l("", " left") + if (s.inCart > 0) l(" · ${s.inCart} di keranjangmu", " · ${s.inCart} in your order") else "")
                        }
                    }
                    TipsCard(listing.tier)
                    if (s.reviews.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Xl),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(l("Ulasan toko", "Store reviews"), style = SgTextStyle.Title)
                                Text("★ ${s.detail.merchant.rating ?: "-"} · ${s.detail.merchant.ratingCount} " + l("ulasan", "reviews"), style = SgTextStyle.Caption)
                            }
                            SeeStorePill(l("Semua ulasan", "All reviews")) { onMerchantClick(s.detail.merchant.id) }
                        }
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = SgSpacing.Gutter, vertical = SgSpacing.Sm),
                            horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
                        ) {
                            items(s.reviews.take(5), key = { it.id }) { r -> ReviewCard(r, Modifier.width(280.dp)) }
                        }
                    }
                    if (s.detail.moreFromMerchant.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Xl),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(l("Orang juga memesan", "People also ordered"), style = SgTextStyle.Title)
                                Text(l("dari ", "from ") + s.detail.merchant.name + l(" · ambil sekalian", " · same pickup"), style = SgTextStyle.Caption)
                            }
                            SeeStorePill(l("Lihat toko", "Visit store")) { onMerchantClick(s.detail.merchant.id) }
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
            SafetyChip(FoodSafety.assess(listing, s.now))
        }
        Text(listing.title, style = SgTextStyle.Display)
        // Price, struck-through original and savings sit on one centre line. The savings badge
        // is a solid fill (red for a discount, green when free) so it's the second thing the
        // eye lands on after the price.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(formatPrice(listing.unitPrice) + if (listing.isFree) "" else unitSuffix(listing), style = SgTextStyle.Display, color = SgColor.Brand600)
            if (!listing.isFree && listing.priceOriginal != null && listing.priceOriginal > listing.unitPrice) {
                Text(formatRupiah(listing.priceOriginal), style = SgTextStyle.Body.copy(textDecoration = TextDecoration.LineThrough))
            }
            val pct = if (listing.isFree) 100 else discountPercent(listing.priceOriginal, listing.priceDiscounted)
            if (pct != null) {
                Text(
                    l("Hemat $pct%", "Save $pct%"),
                    style = SgTextStyle.Label.copy(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold),
                    color = SgColor.OnBrand,
                    modifier = Modifier
                        .background(if (listing.isFree) SgColor.Brand500 else Color(0xFFE5484D), RoundedCornerShape(SgRadius.Pill))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
/**
 * Store card. User testing: the old "Lihat toko" was a tiny text link nobody saw. Now the
 * store banner, name, rating and halal status sit in a card with a full-width outlined
 * "Lihat Toko" button (48dp tall), unmistakably a button.
 */
private fun MerchantRow(s: ListingDetailUiState.Success, onMerchantClick: (String) -> Unit) {
    val m = s.detail.merchant
    Column(
        modifier = Modifier
            .padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Xl)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SgColor.BaseWhite)
            .border(1.dp, SgColor.Hairline, RoundedCornerShape(20.dp))
            .padding(SgSpacing.Md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md)) {
            if (m.bannerUrl.isNotBlank()) {
                AsyncImage(m.bannerUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)))
            } else {
                InitialAvatar(m.name.first().uppercaseChar(), 52.dp, m.isVerified)
            }
            Column(modifier = Modifier.weight(1f)) {
                MerchantSummary(merchant = m, distanceKm = s.listing.distanceKm, verifiedLabel = "Verified")
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (s.listing.tier == ListingTier.HUMAN) HalalBadge(m.halal)
                    if (m.ratingCount > 0) Text("${m.ratingCount} " + l("ulasan", "reviews"), style = SgTextStyle.Caption)
                }
            }
        }
        SgButton(
            text = l("Lihat Toko", "Visit Store"),
            onClick = { onMerchantClick(m.id) },
            style = SgButtonStyle.Secondary,
            height = 48.dp,
            modifier = Modifier.padding(top = SgSpacing.Md).fillMaxWidth(),
            leading = { Icon(Icons.Rounded.Storefront, contentDescription = null, tint = SgColor.Brand600, modifier = Modifier.size(20.dp)) },
        )
    }
}

@Composable
private fun SeeStorePill(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(SgRadius.Pill))
            .background(SgColor.Mint)
            .pressable(onClick)
            .padding(start = 14.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = SgTextStyle.Label, color = SgColor.Brand700)
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = SgColor.Brand700, modifier = Modifier.size(18.dp))
    }
}

private fun unitWord(unit: QuantityUnit) = if (unit == QuantityUnit.KILOGRAM) "kg" else "porsi"

/**
 * Swipeable photo carousel for the hero. Dots track the page; the active dot stretches into a
 * pill (width on a spring-free tween, 200ms ease-out) so position reads at a glance.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PhotoPager(photos: List<String>, tier: ListingTier, title: String, modifier: Modifier = Modifier) {
    val pager = androidx.compose.foundation.pager.rememberPagerState { photos.size }
    Box(modifier) {
        androidx.compose.foundation.pager.HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            ListingImage(imageUrl = photos[page], tier = tier, contentDescription = title, modifier = Modifier.fillMaxSize())
        }
        if (photos.size > 1) {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp)
                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(SgRadius.Pill))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                repeat(photos.size) { i ->
                    val active = pager.currentPage == i
                    val w by androidx.compose.animation.core.animateDpAsState(if (active) 16.dp else 6.dp, androidx.compose.animation.core.tween(200, easing = com.sisaguna.android.ui.components.SgEaseOut), label = "dot")
                    Box(Modifier.height(6.dp).width(w).background(Color.White.copy(alpha = if (active) 1f else 0.6f), RoundedCornerShape(SgRadius.Pill)))
                }
            }
            Text(
                "${pager.currentPage + 1}/${photos.size}",
                style = SgTextStyle.TextXsMedium,
                color = Color.White,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(SgRadius.Pill)).padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
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
            .background(SgColor.Yellow50)
            .padding(SgSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = SgColor.YellowStatus, modifier = Modifier.size(18.dp))
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
