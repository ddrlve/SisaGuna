package com.sisaguna.android.feature.home
import com.sisaguna.android.ui.components.CountBadge
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.foundation.layout.offset

import com.sisaguna.android.data.model.AppFees

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.DeliveryDining
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.sisaguna.android.data.model.FoodSafety
import com.sisaguna.android.data.model.Fulfillment
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.data.model.MerchantStatus
import com.sisaguna.android.data.model.QuantityUnit
import com.sisaguna.android.data.repository.ConfirmResult
import com.sisaguna.android.data.repository.IncomingOrder
import com.sisaguna.android.data.repository.IncomingOrderRepository
import com.sisaguna.android.core.ui.AppMessenger
import com.sisaguna.android.data.remote.AppException
import com.sisaguna.android.data.repository.IncomingStatus
import com.sisaguna.android.data.repository.MerchantDashboard
import com.sisaguna.android.data.repository.ListingRepository
import com.sisaguna.android.data.settings.UserMode
import com.sisaguna.android.ui.components.SgButton
import com.sisaguna.android.ui.components.SgEaseOut
import com.sisaguna.android.ui.components.pressable
import com.sisaguna.android.ui.domain.SafetyChip
import com.sisaguna.android.ui.domain.formatClock
import com.sisaguna.android.ui.domain.formatPrice
import com.sisaguna.android.ui.domain.formatRupiah
import com.sisaguna.android.ui.i18n.Text
import com.sisaguna.android.ui.i18n.l
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MerchantHomeViewModel @Inject constructor(
    private val listingRepository: ListingRepository,
    private val incomingRepository: IncomingOrderRepository,
    private val messenger: AppMessenger,
) : ViewModel() {
    val store: StateFlow<Merchant?> = listingRepository.myMerchant

    val myListings: StateFlow<List<Listing>> = combine(listingRepository.listings, listingRepository.myMerchant) { all, me ->
        all.filter { it.merchantId == me?.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val incoming: StateFlow<List<IncomingOrder>> = incomingRepository.orders

    val dashboard: StateFlow<MerchantDashboard> = incomingRepository.dashboard

    init {
        viewModelScope.launch {
            try {
                incomingRepository.refresh()
            } catch (e: AppException) {
                messenger.show(e)
            }
        }
    }

    suspend fun confirm(orderId: String, code: String): ConfirmResult = try {
        incomingRepository.confirmPickup(orderId, code)
    } catch (e: AppException) {
        ConfirmResult.Rejected(e.userMessage)
    }
}

/**
 * The seller's home (user testing: "perlu tampilan home aplikasi mitra dan user pribadi
 * dibedakan"). Built around the three seller jobs from the interview script: post leftovers
 * fast, see incoming orders, and confirm pickup with the buyer's code.
 */
@Composable
fun MerchantHome(
    onModeChange: (UserMode) -> Unit,
    onUploadTierClick: (ListingTier) -> Unit,
    onNotificationsClick: () -> Unit,
    unreadNotifications: Int,
    onMyCatalogClick: () -> Unit,
    onStoreSetupClick: () -> Unit = {},
    onChatsClick: () -> Unit = {},
    unreadChats: Int = 0,
    onListingClick: (Listing) -> Unit,
    viewModel: MerchantHomeViewModel = hiltViewModel(),
) {
    val listings by viewModel.myListings.collectAsStateWithLifecycle()
    val incoming by viewModel.incoming.collectAsStateWithLifecycle()
    val store by viewModel.store.collectAsStateWithLifecycle()
    val now = Instant.now()
    val waiting = incoming.filter { it.status == IncomingStatus.WAITING }
    val active = listings.filter { it.pickupEnd.isAfter(now) && it.stock > 0 }
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val todaySales = dashboard.salesToday
    val todayRevenue = dashboard.netToday

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(SgColor.Page),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(SgRadius.Pill)).background(SgColor.BaseWhite).padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Storefront, contentDescription = null, tint = SgColor.Brand600, modifier = Modifier.size(18.dp))
                    Text(store?.name ?: l("Toko saya", "My store"), style = SgTextStyle.Label, maxLines = 1, modifier = Modifier.padding(start = 6.dp))
                }
                Spacer(Modifier.size(8.dp))
                ModeSwitch(mode = UserMode.MERCHANT, onChange = onModeChange)
                Spacer(Modifier.size(8.dp))
                // Buyer questions land here; unanswered ones show as a count.
                ChatButton(unread = unreadChats, onClick = onChatsClick)
                Spacer(Modifier.size(8.dp))
                // Partners get order, pickup and payout alerts too, same bell as buyer Home.
                NotificationBell(unread = unreadNotifications, onClick = onNotificationsClick)
            }
        }
        // A partner account has no store until it is registered, and can't sell until an admin approves it.
        val storeStatus = store?.status
        if (store == null || storeStatus != MerchantStatus.APPROVED) {
            item { StoreStatusCard(status = storeStatus, onSetup = onStoreSetupClick) }
        }
        item {
            // Store hero: banner + today's numbers.
            Box(
                Modifier
                    .padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Lg)
                    .fillMaxWidth()
                    .height(172.dp)
                    .clip(RoundedCornerShape(24.dp)),
            ) {
                AsyncImage(store?.bannerUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xE61F3A14), Color(0x661F3A14)))))
                Column(Modifier.align(Alignment.CenterStart).padding(18.dp)) {
                    Text(l("Pendapatan bersih hari ini", "Today's net earnings"), style = SgTextStyle.Caption, color = Color.White.copy(alpha = 0.8f))
                    androidx.compose.material3.Text(formatRupiah(todayRevenue), style = SgTextStyle.Display, color = Color.White)
                    Text(
                        l("Penjualan ${formatRupiah(todaySales)} · komisi SisaGuna ${AppFees.MERCHANT_COMMISSION_PERCENT}%", "Sales ${formatRupiah(todaySales)} · SisaGuna fee ${AppFees.MERCHANT_COMMISSION_PERCENT}%"),
                        style = SgTextStyle.Caption.copy(fontSize = 11.sp),
                        color = Color.White.copy(alpha = 0.75f),
                    )
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        HeroStat("${waiting.size}", l("menunggu", "waiting"))
                        HeroStat("${active.size}", l("listing aktif", "live items"))
                        HeroStat("★ ${store?.rating ?: "-"}", "${store?.ratingCount ?: 0} " + l("ulasan", "reviews"))
                    }
                }
            }
        }
        item {
            SectionTitle(l("Posting sisa makanan", "Post leftovers"), l("Pilih jenisnya, satuannya beda", "Pick a type, they're counted differently"))
            Row(
                Modifier.padding(horizontal = SgSpacing.Gutter).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
            ) {
                UploadTile(
                    icon = Icons.Rounded.RestaurantMenu,
                    title = l("Siap santap", "Ready to eat"),
                    sub = l("Dihitung per porsi · wajib cek layak", "Per portion · safety check"),
                    tint = SgColor.Mint, fg = SgColor.Brand700,
                    modifier = Modifier.weight(1f),
                    onClick = { onUploadTierClick(ListingTier.HUMAN) },
                )
                UploadTile(
                    icon = Icons.Rounded.Agriculture,
                    title = l("Pakan & kompos", "Feed & compost"),
                    sub = l("Dihitung per gram / kg", "By gram / kg"),
                    tint = SgColor.Farm, fg = SgColor.FarmInk,
                    modifier = Modifier.weight(1f),
                    onClick = { onUploadTierClick(ListingTier.ANIMAL_FEED) },
                )
            }
        }
        item { SectionTitle(l("Pesanan masuk", "Incoming orders"), l("Minta kode dari pembeli, lalu konfirmasi", "Ask the buyer for their code, then confirm")) }
        if (incoming.isEmpty()) {
            item { Text(l("Belum ada pesanan.", "No orders yet."), style = SgTextStyle.Body, modifier = Modifier.padding(horizontal = SgSpacing.Gutter)) }
        }
        items(incoming.sortedBy { it.status != IncomingStatus.WAITING }, key = { it.id }) { order ->
            IncomingOrderCard(order, onConfirm = { code -> viewModel.confirm(order.id, code) })
        }
        item {
            Row(
                Modifier.padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Xl).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(l("Listing aktif", "Live listings"), style = SgTextStyle.Title)
                    Text(l("Status kelayakan dihitung otomatis", "Safety status is computed automatically"), style = SgTextStyle.Caption)
                }
                Row(
                    Modifier.clip(RoundedCornerShape(SgRadius.Pill)).background(SgColor.Mint).pressable(onMyCatalogClick).padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(l("Katalog", "Catalog"), style = SgTextStyle.Label, color = SgColor.Brand700)
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = SgColor.Brand700)
                }
            }
        }
        items(active, key = { "l-" + it.id }) { listing ->
            Row(
                Modifier
                    .padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Md)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SgColor.BaseWhite)
                    .pressable({ onListingClick(listing) })
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(listing.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(14.dp)))
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(listing.title, style = SgTextStyle.Label, maxLines = 1)
                    Text(
                        "${l("Sisa", "Left")} ${listing.stock} ${if (listing.unit == QuantityUnit.KILOGRAM) "kg" else l("porsi", "portions")} · ${l("s/d", "until")} ${formatClock(listing.pickupEnd)}",
                        style = SgTextStyle.Caption,
                    )
                    SafetyChip(FoodSafety.assess(listing), Modifier.padding(top = 4.dp))
                }
                Text(formatPrice(listing.unitPrice), style = SgTextStyle.Label)
            }
        }
    }
}

@Composable
private fun ChatButton(unread: Int, onClick: () -> Unit) {
    Box(Modifier.size(40.dp)) {
        Box(
            Modifier.fillMaxSize().clip(CircleShape).background(SgColor.BaseWhite).pressable(onClick, pressedScale = 0.92f),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.ChatBubbleOutline,
                contentDescription = if (unread > 0) l("Chat pembeli, $unread belum dibaca", "Buyer chats, $unread unread") else l("Chat pembeli", "Buyer chats"),
                tint = SgColor.Ink,
                modifier = Modifier.size(20.dp),
            )
        }
        if (unread > 0) {
            // Same placement as the bell's badge next to it, so the two line up.
            CountBadge(
                unread,
                ring = SgColor.Page,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-2).dp),
            )
        }
    }
}

@Composable
private fun HeroStat(value: String, label: String) {
    Column {
        androidx.compose.material3.Text(value, style = SgTextStyle.Label, color = Color.White)
        androidx.compose.material3.Text(label, style = SgTextStyle.Caption, color = Color.White.copy(alpha = 0.75f))
    }
}

@Composable
private fun SectionTitle(title: String, sub: String) {
    Column(Modifier.padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Xl, bottom = SgSpacing.Md)) {
        Text(title, style = SgTextStyle.Title)
        Text(sub, style = SgTextStyle.Caption)
    }
}

@Composable
private fun UploadTile(icon: ImageVector, title: String, sub: String, tint: Color, fg: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SgColor.BaseWhite)
            .border(1.dp, SgColor.Hairline, RoundedCornerShape(20.dp))
            .pressable(onClick)
            .padding(14.dp),
    ) {
        Box(Modifier.size(44.dp).background(tint, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = fg)
        }
        Text(title, style = SgTextStyle.Label, modifier = Modifier.padding(top = 10.dp))
        Text(sub, style = SgTextStyle.Caption)
        Text("+ " + l("Upload", "Upload"), style = SgTextStyle.Label, color = fg, modifier = Modifier.padding(top = 8.dp))
    }
}

/** Shown while the partner has no approved store: register it, wait for approval, or learn why not. */
@Composable
private fun StoreStatusCard(status: MerchantStatus?, onSetup: () -> Unit) {
    val (title, body) = when (status) {
        null -> l("Daftarkan tokomu", "Open your store") to l(
            "Lengkapi data toko supaya kamu bisa mulai berjualan sisa makanan.",
            "Add your store details to start selling surplus food.",
        )
        MerchantStatus.PENDING -> l("Toko menunggu persetujuan", "Store awaiting approval") to l(
            "Tim SisaGuna sedang memeriksa tokomu. Kamu bisa berjualan setelah disetujui.",
            "The SisaGuna team is reviewing your store. You can sell once it's approved.",
        )
        MerchantStatus.REJECTED -> l("Toko belum disetujui", "Store not approved") to l(
            "Pendaftaran tokomu ditolak. Hubungi bantuan untuk info lebih lanjut.",
            "Your store registration was declined. Contact support for details.",
        )
        else -> l("Toko dinonaktifkan", "Store suspended") to l(
            "Tokomu sementara dinonaktifkan. Hubungi bantuan untuk info lebih lanjut.",
            "Your store is suspended. Contact support for details.",
        )
    }
    Column(
        Modifier
            .padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Lg)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SgColor.Yellow50)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = SgTextStyle.Label)
        Text(body, style = SgTextStyle.Body)
        if (status == null) {
            SgButton(l("Daftarkan toko", "Open store"), onClick = onSetup, height = 44.dp, modifier = Modifier.padding(top = 6.dp).fillMaxWidth())
        }
    }
}

/** One incoming order. "Konfirmasi ambil" expands an inline code field (the buyer shows
 * SG-XXXX); a wrong code shakes nothing — it just says so under the field. */
@Composable
private fun IncomingOrderCard(order: IncomingOrder, onConfirm: suspend (String) -> ConfirmResult) {
    var expanded by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val done = order.status == IncomingStatus.HANDED_OVER
    Column(
        Modifier
            .padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, bottom = SgSpacing.Md)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SgColor.BaseWhite)
            .padding(14.dp)
            .animateContentSize(tween(220, easing = SgEaseOut)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(if (done) SgColor.Page else SgColor.Mint, CircleShape), contentAlignment = Alignment.Center) {
                Icon(
                    if (order.fulfillment == Fulfillment.DELIVERY) Icons.Rounded.DeliveryDining else Icons.Rounded.Inventory2,
                    contentDescription = null,
                    tint = if (done) SgColor.InkMuted else SgColor.Brand600,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                Text("${order.buyerName} · ${order.quantity} ${if (order.unit == QuantityUnit.KILOGRAM) "kg" else l("porsi", "pcs")}", style = SgTextStyle.Label, maxLines = 1)
                Text(order.itemTitle, style = SgTextStyle.Caption, maxLines = 1)
                Text(
                    when {
                        done -> l("✓ Sudah diserahkan", "✓ Handed over")
                        order.fulfillment == Fulfillment.DELIVERY -> (order.courierLabel ?: l("Kurir", "Courier")) + l(" · siapkan sebelum ", " · ready by ") + formatClock(order.pickupBy)
                        else -> l("Diambil maks. ", "Pickup by ") + formatClock(order.pickupBy)
                    },
                    style = SgTextStyle.Caption.copy(fontWeight = FontWeight.SemiBold),
                    color = if (done) SgColor.Brand600 else SgColor.YellowStatus,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatPrice(order.total), style = SgTextStyle.Label)
                if (order.total > 0) {
                    Text(
                        l("Kamu terima ", "You get ") + formatRupiah(AppFees.merchantPayout(order.total)),
                        style = SgTextStyle.Caption.copy(fontSize = 11.sp),
                        color = SgColor.Brand700,
                    )
                }
            }
        }
        if (!done) {
            AnimatedVisibility(visible = !expanded, enter = fadeIn(tween(150)), exit = fadeOut(tween(90))) {
                SgButton(
                    text = l("Konfirmasi pengambilan", "Confirm pickup"),
                    onClick = { expanded = true },
                    height = 44.dp,
                    modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                    leading = { Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, tint = SgColor.OnBrand, modifier = Modifier.size(18.dp)) },
                )
            }
            AnimatedVisibility(visible = expanded, enter = fadeIn(tween(180, easing = SgEaseOut)), exit = fadeOut(tween(90))) {
                Column(Modifier.padding(top = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it.take(8); error = null },
                            singleLine = true,
                            placeholder = { androidx.compose.material3.Text("SG-0000", style = SgTextStyle.TextSmRegular, color = SgColor.Neutral400) },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            textStyle = SgTextStyle.Label.copy(letterSpacing = 1.sp),
                            isError = error != null,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SgColor.Brand500, unfocusedBorderColor = SgColor.Hairline),
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.size(8.dp))
                        SgButton(
                            text = if (checking) "…" else l("Cek", "Check"),
                            onClick = {
                                checking = true
                                scope.launch {
                                    error = when (val result = onConfirm(code)) {
                                        ConfirmResult.Confirmed -> null
                                        ConfirmResult.WrongCode -> l("Kode tidak cocok. Cek lagi layar pembeli.", "Code doesn't match. Check the buyer's screen.")
                                        is ConfirmResult.Rejected -> result.message
                                    }
                                    checking = false
                                }
                            },
                            enabled = code.isNotBlank() && !checking,
                            height = 52.dp,
                        )
                    }
                    Text(
                        error ?: l("Minta kode dari layar pembeli (mis. SG-1234).", "Ask for the code on the buyer's screen (e.g. SG-1234)."),
                        style = SgTextStyle.Caption,
                        color = if (error != null) SgColor.RedStatus else SgColor.InkMuted,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}
