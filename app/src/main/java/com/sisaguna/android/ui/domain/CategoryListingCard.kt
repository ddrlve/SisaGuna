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
import androidx.compose.material3.Text
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
        modifier = modifier.width(168.dp),
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
                    if (listing.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = listing.imageUrl,
                            contentDescription = listing.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(146.dp),
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.listing_ayam_olie),
                            contentDescription = listing.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(146.dp)
                                .background(SgColor.Neutral300),
                        )
                    }
                    CountdownPill(
                        pickupEnd = listing.pickupEnd,
                        now = now,
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = listing.title,
                        style = SgTextStyle.TextSmSemibold,
                        color = SgColor.Neutral800,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    SellerLine(name = merchant.name, verified = merchant.isVerified)
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
                TierBadge(tier = listing.tier)
                PriceLabel(listing = listing)
            }
        }
    }
}
