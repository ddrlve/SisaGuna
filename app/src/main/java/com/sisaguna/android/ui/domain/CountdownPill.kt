package com.sisaguna.android.ui.domain

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sisaguna.android.ui.theme.SgColor
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * "S/d {HH:mm}" pill overlaid on listing card images (see figma/HomePage.jpeg,
 * figma/products.jpeg) — pickup deadline as a fixed local time, not a live countdown.
 * Background goes urgent-red under an hour left; [Listing.pickupEnd] past listings should
 * already be filtered out by the repository (see FakeListingRepository), not hidden here.
 */
@Composable
fun CountdownPill(
    pickupEnd: Instant,
    now: Instant,
    modifier: Modifier = Modifier,
) {
    val remaining = Duration.between(now, pickupEnd)
    val urgent = remaining.toMinutes() in 0..60
    val label = "S/d " + timeFormatter.format(pickupEnd.atZone(ZoneId.systemDefault()))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                if (urgent) SgColor.Rose500 else SgColor.Neutral800.copy(alpha = 0.75f),
                RoundedCornerShape(50),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.AccessTime,
            contentDescription = null,
            tint = SgColor.Yellow500,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = label,
            color = SgColor.BaseWhite,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
