package com.sisaguna.android.feature.saved

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.layout
import coil.compose.AsyncImage
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.data.model.Review
import com.sisaguna.android.ui.components.SgButton
import com.sisaguna.android.ui.components.SgButtonStyle
import com.sisaguna.android.ui.components.SgEaseOut
import com.sisaguna.android.ui.components.pressable
import com.sisaguna.android.ui.components.rememberMediaCapture
import com.sisaguna.android.ui.domain.AllergenChips
import com.sisaguna.android.ui.domain.HalalBadge
import com.sisaguna.android.ui.domain.StarGold
import com.sisaguna.android.ui.domain.formatDistance
import com.sisaguna.android.ui.i18n.Text
import com.sisaguna.android.ui.i18n.l
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle
import com.sisaguna.android.data.model.HalalStatus

/** Store banner with the avatar overlapping its bottom edge, name, rating, hours, halal. */
@Composable
fun StoreHero(
    merchant: Merchant,
    distanceKm: Double?,
    listingCount: Int,
    saved: Boolean,
    onToggleSave: () -> Unit,
    modifier: Modifier = Modifier,
    onChat: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().background(SgColor.BaseWhite)) {
        Box(Modifier.fillMaxWidth().height(190.dp)) {
            AsyncImage(merchant.bannerUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)))))
            if (onChat != null) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 62.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .pressable(onChat, pressedScale = 0.88f),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = l("Chat toko", "Chat store"), tint = Color(0xFF1F2A1C), modifier = Modifier.size(21.dp))
                }
            }
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .pressable(onToggleSave, pressedScale = 0.88f),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (saved) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = l("Simpan toko", "Save store"),
                    tint = if (saved) Color(0xFFE5484D) else Color(0xFF1F2A1C),
                )
            }
        }
        // Avatar (72dp) overlaps the banner by 30dp. The text block (42dp) fills exactly the part
        // below the banner and is centred in it, so the name never
        // touches the photo and the row reads balanced against the avatar.
        Row(
            Modifier
                .padding(horizontal = SgSpacing.Gutter)
                .layout { measurable, constraints ->
                    val overlap = 30.dp.roundToPx()
                    val p = measurable.measure(constraints)
                    layout(p.width, p.height - overlap) { p.place(0, -overlap) }
                },
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .shadow(6.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .background(SgColor.BaseWhite)
                    .padding(3.dp),
            ) {
                AsyncImage(
                    merchant.photos.firstOrNull() ?: merchant.bannerUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(19.dp)),
                )
            }
            Column(
                Modifier.padding(start = 14.dp).weight(1f).height(42.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(merchant.name, style = SgTextStyle.Title.copy(fontSize = 19.sp, lineHeight = 24.sp), maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                    if (merchant.isVerified) Icon(Icons.Rounded.Verified, contentDescription = "Verified", tint = SgColor.Brand500, modifier = Modifier.padding(start = 4.dp).size(18.dp))
                }
                Text(
                    merchant.location + (distanceKm?.let { " · " + formatDistance(it) } ?: "") + " · $listingCount " + l("menu aktif", "live items"),
                    style = SgTextStyle.Caption,
                    maxLines = 1,
                )
            }
        }
        Row(
            Modifier.padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = 14.dp, bottom = SgSpacing.Lg),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            merchant.rating?.let {
                StatPill(Icons.Rounded.Star, StarGold, "%.1f".format(it), "(${merchant.ratingCount})")
            }
            if (merchant.openHours.isNotBlank()) StatPill(Icons.Rounded.Schedule, SgColor.InkMuted, merchant.openHours, null)
            if (merchant.halal != HalalStatus.OTHER) HalalBadge(merchant.halal)
        }
    }
}

@Composable
private fun StatPill(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, value: String, sub: String?) {
    Row(
        Modifier.background(SgColor.Page, RoundedCornerShape(SgRadius.Pill)).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Text(" $value", style = SgTextStyle.TextXsMedium.copy(fontWeight = FontWeight.Bold), color = SgColor.Ink)
        if (sub != null) Text(" $sub", style = SgTextStyle.TextXsMedium, color = SgColor.InkMuted)
    }
}

/** "Today's offer" strip in promo pink. */
@Composable
fun TodaysOfferBanner(offer: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(SgColor.PromoTint, SgColor.Farm)))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).background(SgColor.BaseWhite, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.LocalOffer, contentDescription = null, tint = SgColor.PromoInk, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text(l("Penawaran hari ini", "Today's offer"), style = SgTextStyle.Overline.copy(color = SgColor.PromoInk))
            Text(offer, style = SgTextStyle.Label)
        }
    }
}

/** Horizontal store photo gallery. */
@Composable
fun StorePhotos(photos: List<String>, modifier: Modifier = Modifier) {
    if (photos.isEmpty()) return
    Column(modifier) {
        Text(l("Foto toko & menu", "Store & menu photos"), style = SgTextStyle.Title, modifier = Modifier.padding(horizontal = SgSpacing.Gutter))
        LazyRow(
            contentPadding = PaddingValues(horizontal = SgSpacing.Gutter, vertical = SgSpacing.Md),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(photos) { i, p ->
                AsyncImage(
                    p,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(width = if (i == 0) 200.dp else 130.dp, height = 130.dp).clip(RoundedCornerShape(18.dp)),
                )
            }
        }
    }
}

/**
 * About, address and the allergen declaration. The buyer acknowledges the allergens once per
 * store ("consent") before checkout; the box here records that and the sheet in checkout
 * reuses the same declaration.
 */
@Composable
fun StoreInfoCard(
    merchant: Merchant,
    allergenAcknowledged: Boolean,
    onAcknowledge: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SgColor.BaseWhite)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (merchant.about.isNotBlank()) Text(merchant.about, style = SgTextStyle.Body.copy(color = SgColor.Ink))
        if (merchant.address.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = SgColor.RedStatus, modifier = Modifier.size(18.dp))
                Text(merchant.address, style = SgTextStyle.Caption.copy(color = SgColor.Ink), modifier = Modifier.padding(start = 6.dp))
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(SgColor.Hairline))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = SgColor.FarmInk, modifier = Modifier.size(18.dp))
            Text(l("Info alergen dari toko", "Allergen info from the store"), style = SgTextStyle.Label, modifier = Modifier.padding(start = 6.dp))
        }
        AllergenChips(merchant.allergens)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).pressable({ onAcknowledge(!allergenAcknowledged) }).padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = allergenAcknowledged,
                onCheckedChange = onAcknowledge,
                colors = CheckboxDefaults.colors(checkedColor = SgColor.Brand500, uncheckedColor = SgColor.Neutral400),
            )
            Text(
                l("Saya sudah membaca info alergen toko ini", "I've read this store's allergen info"),
                style = SgTextStyle.Caption.copy(color = SgColor.Ink),
            )
        }
    }
}

/** Average, count, and a 5→1 star distribution with bars. */
@Composable
fun ReviewSummary(merchant: Merchant, reviews: List<Review>, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(SgColor.BaseWhite).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(92.dp)) {
                androidx.compose.material3.Text("%.1f".format(merchant.rating ?: 0.0), style = SgTextStyle.Hero)
                Row { repeat(5) { i -> Icon(if (i < Math.round(merchant.rating ?: 0.0)) Icons.Rounded.Star else Icons.Rounded.StarBorder, null, tint = StarGold, modifier = Modifier.size(14.dp)) } }
                Text("${merchant.ratingCount} " + l("ulasan", "reviews"), style = SgTextStyle.Caption)
            }
            Column(Modifier.padding(start = 12.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // The store's own histogram covers every review; the loaded list is only the newest page.
                val histogram = merchant.ratingBreakdown.ifEmpty { reviews.groupingBy { it.stars }.eachCount() }
                val total = histogram.values.sum().coerceAtLeast(1)
                (5 downTo 1).forEach { star ->
                    val n = histogram[star] ?: 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$star", style = SgTextStyle.Caption, modifier = Modifier.width(12.dp))
                        LinearProgressIndicator(
                            progress = { n.toFloat() / total },
                            color = StarGold,
                            trackColor = SgColor.Hairline,
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier.padding(start = 6.dp).weight(1f).height(6.dp),
                        )
                    }
                }
            }
        }
        Text(
            l("Ulasan hanya bisa ditulis dari pesanan yang sudah selesai.", "Reviews can only be written from a completed order."),
            style = SgTextStyle.Caption,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
fun MediaButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, SgColor.Neutral300, RoundedCornerShape(14.dp))
            .pressable(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = SgColor.Brand600, modifier = Modifier.size(22.dp))
        Text(label, style = SgTextStyle.Caption.copy(fontSize = 11.sp))
    }
}
