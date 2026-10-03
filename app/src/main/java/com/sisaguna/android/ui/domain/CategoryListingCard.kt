package com.sisaguna.android.ui.domain

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import com.sisaguna.android.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sisaguna.android.R
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgTextStyle
import java.time.Instant

/**
 * Compact listing card for Category List (Figma node 45:10286 "card-home-mini" on frame
 * 45:9805) — same content as [ListingCard] but narrower (168dp), taller image (146dp), and the
 * tier badge stacked above the price instead of beside it.
 */
@Composable
fun CategoryListingCard(
    listing: Listing,
    merchant: Merchant,
    now: Instant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(25.dp),
        colors = CardDefaults.cardColors(containerColor = SgColor.BaseWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(146.dp)
                        .clip(RoundedCornerShape(17.dp)),
                ) {
                    ListingImage(
                        imageUrl = listing.imageUrl,
                        tier = listing.tier,
                        contentDescription = listing.title,
                        modifier = Modifier.fillMaxWidth().height(146.dp),
                    )
                    CountdownPill(
                        pickupEnd = listing.pickupEnd,
                        now = now,
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                    )
                    DiscountBadge(listing, Modifier.align(Alignment.TopEnd).padding(8.dp))
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = listing.title,
                        style = SgTextStyle.TextSmSemibold,
                        color = SgColor.Neutral800,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { SellerLine(name = merchant.name, verified = merchant.isVerified) }
                        merchant.rating?.let { Text("★ %.1f".format(it), fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Color(0xFFE59E0B)) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_location_card),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.height(14.dp),
                        )
                        Text(
                            text = buildString {
                                append(merchant.location)
                                listing.distanceKm?.let { append(" • ${formatDistance(it)}") }
                            },
                            fontSize = 10.sp,
                            color = SgColor.Neutral500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                if (merchant.deliveryAvailable) {
                    // Cheapest standard courier, same quote the "Ongkir termurah" sort uses.
                    val q = com.sisaguna.android.data.model.DeliveryPricing.quote(
                        com.sisaguna.android.data.model.Courier.GOSEND, com.sisaguna.android.data.model.DeliverySpeed.STANDARD,
                        listing.distanceKm ?: 5.0, listing.unitPrice, merchant.prepMinutes,
                    )
                    Text("🛵 ${formatRupiah(q.payable)} · ${q.etaMinMinutes}–${q.etaMaxMinutes} mnt", fontSize = 10.sp, color = SgColor.Neutral500, maxLines = 1)
                }
                TierBadge(tier = listing.tier)
                PriceLabel(listing = listing)
            }
        }
    }
}
