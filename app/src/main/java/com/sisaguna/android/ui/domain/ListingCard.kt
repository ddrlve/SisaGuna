package com.sisaguna.android.ui.domain

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
 * The listing card used on Home and (once built) the category grid. Matches Figma node
 * 43:7015 "card-home" on the Home frame (29:93) — 200dp wide, 25dp corners, dark countdown
 * pill over the image, outlined tier badge, price in neutral text ("Gratis" is the only case
 * styled green).
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
        modifier = modifier.width(200.dp),
        shape = RoundedCornerShape(25.dp),
        colors = CardDefaults.cardColors(containerColor = SgColor.BaseWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp)
                    .clip(RoundedCornerShape(17.dp)),
            ) {
                if (listing.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = listing.imageUrl,
                        contentDescription = listing.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(108.dp),
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.listing_ayam_olie),
                        contentDescription = listing.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(108.dp)
                            .background(SgColor.Neutral300),
                    )
                }
                CountdownPill(
                    pickupEnd = listing.pickupEnd,
                    now = now,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = listing.title,
                style = SgTextStyle.TextSmSemibold,
                color = SgColor.Neutral800,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            SellerLine(name = merchant.name, verified = merchant.isVerified)

            Spacer(modifier = Modifier.height(2.dp))

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

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PriceLabel(listing = listing)
                TierBadge(tier = listing.tier)
            }
        }
    }
}

@Composable
private fun SellerLine(name: String, verified: Boolean) {
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = SgColor.Neutral800)) { append(name) }
        if (verified) {
            append(" • ")
            withStyle(SpanStyle(color = SgColor.Brand600, fontWeight = FontWeight.Medium)) {
                append("Verified")
            }
        }
    }
    Text(text = text, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun PriceLabel(listing: Listing) {
    when {
        listing.isFree -> Text(
            text = "Gratis",
            style = SgTextStyle.TextSmSemibold,
            color = SgColor.Brand500,
        )
        listing.priceDiscounted != null && listing.priceOriginal != null -> Column {
            Text(
                text = formatRupiah(listing.priceDiscounted),
                style = SgTextStyle.TextSmSemibold,
                color = SgColor.Neutral800,
            )
            Text(
                text = formatRupiah(listing.priceOriginal),
                fontSize = 10.sp,
                color = SgColor.Neutral400,
                textDecoration = TextDecoration.LineThrough,
            )
        }
        listing.priceOriginal != null -> Text(
            text = formatRupiah(listing.priceOriginal),
            style = SgTextStyle.TextSmSemibold,
            color = SgColor.Neutral800,
        )
    }
}

private fun formatRupiah(amount: Int): String {
    val grouped = amount.toString().reversed().chunked(3).joinToString(".").reversed()
    return "Rp. $grouped"
}

private fun formatDistance(km: Double): String =
    if (km < 1.0) "${(km * 1000).toInt()} m" else "%.1f km".format(km)
