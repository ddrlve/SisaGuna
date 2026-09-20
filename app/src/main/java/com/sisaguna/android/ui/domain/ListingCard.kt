package com.sisaguna.android.ui.domain

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.ui.theme.SgColor
import java.time.Instant

/**
 * The card used across the Home rails and category grid (see figma/HomePage.jpeg and
 * figma/products.jpeg). Width is fixed for horizontal rails — a vertical grid caller should
 * override with [Modifier.fillMaxWidth] instead.
 */
@Composable
fun ListingCard(
    listing: Listing,
    merchant: Merchant,
    now: Instant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.width(168.dp),
        colors = CardDefaults.cardColors(containerColor = SgColor.BaseWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(112.dp)) {
            if (listing.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = listing.imageUrl,
                    contentDescription = listing.title,
                    modifier = Modifier.fillMaxWidth().height(112.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                        .background(SgColor.Brand100),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Restaurant,
                        contentDescription = null,
                        tint = SgColor.Brand700,
                    )
                }
            }
            CountdownPill(
                pickupEnd = listing.pickupEnd,
                now = now,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
            )
        }

        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = listing.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SgColor.Neutral800,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = merchant.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = SgColor.Neutral500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (merchant.isVerified) {
                    Text(
                        text = " · Verified",
                        style = MaterialTheme.typography.bodySmall,
                        color = SgColor.Brand700,
                        maxLines = 1,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = SgColor.Neutral500,
                    modifier = Modifier.height(12.dp),
                )
                Text(
                    text = buildString {
                        append(merchant.location)
                        listing.distanceKm?.let { append(" · ${formatDistance(it)}") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = SgColor.Neutral500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            TierBadge(tier = listing.tier)

            Spacer(modifier = Modifier.height(6.dp))

            PriceLabel(listing = listing)
        }
    }
}

@Composable
private fun PriceLabel(listing: Listing) {
    when {
        listing.isFree -> Text(
            text = "Gratis",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = SgColor.Brand700,
        )
        listing.priceDiscounted != null && listing.priceOriginal != null -> Column {
            Text(
                text = formatRupiah(listing.priceDiscounted),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = SgColor.Brand700,
            )
            Text(
                text = formatRupiah(listing.priceOriginal),
                style = MaterialTheme.typography.bodySmall,
                color = SgColor.Neutral500,
                textDecoration = TextDecoration.LineThrough,
            )
        }
        listing.priceOriginal != null -> Text(
            text = formatRupiah(listing.priceOriginal),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = SgColor.Brand700,
        )
    }
}

private fun formatRupiah(amount: Int): String {
    val grouped = amount.toString().reversed().chunked(3).joinToString(".").reversed()
    return "Rp. $grouped"
}

private fun formatDistance(km: Double): String =
    if (km < 1.0) "${(km * 1000).toInt()} m" else "%.1f km".format(km)
