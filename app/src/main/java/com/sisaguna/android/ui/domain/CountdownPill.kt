package com.sisaguna.android.ui.domain

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sisaguna.android.R
import com.sisaguna.android.ui.theme.SgColor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * "S/d {HH:mm}" pill overlaid on the listing image (Figma node 39:5964 on Home 29:93) — a
 * fixed pickup deadline, not a live countdown. [Listing.pickupEnd] past listings are filtered
 * out by the repository, not hidden here.
 */
@Composable
fun CountdownPill(
    pickupEnd: Instant,
    now: Instant,
    modifier: Modifier = Modifier,
) {
    val label = "S/d " + timeFormatter.format(pickupEnd.atZone(ZoneId.systemDefault()))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(SgColor.Neutral800.copy(alpha = 0.8f), RoundedCornerShape(30.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_clock),
            contentDescription = null,
            tint = androidx.compose.ui.graphics.Color.Unspecified,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = label,
            color = SgColor.Neutral50,
            fontSize = 10.sp,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
