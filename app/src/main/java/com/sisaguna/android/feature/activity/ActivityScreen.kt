package com.sisaguna.android.feature.activity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sisaguna.android.R
import com.sisaguna.android.ui.domain.TierBadge
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgTextStyle
import com.sisaguna.android.ui.theme.SisaGunaTheme

sealed interface ActivityStatus {
    data class Waiting(val deadlineLabel: String) : ActivityStatus
    data object Done : ActivityStatus
    data object Cancelled : ActivityStatus
}

data class ActivityEntry(
    val id: String,
    val date: String,
    val title: String,
    val sellerName: String,
    val sellerVerified: Boolean,
    val location: String,
    val distanceKm: Double,
    val tier: ListingTier,
    val price: String,
    val status: ActivityStatus,
)

private enum class ActivityTab { AMBIL, JUAL }

private val mockAmbil = listOf(
    ActivityEntry("a1", "13 September 2026", "Ayam olie", "fadlhan", true, "Alam Sutera", 0.4, ListingTier.HUMAN, "Rp. 11.000", ActivityStatus.Cancelled),
    ActivityEntry("a2", "13 September 2026", "Ayam olie", "fadlhan", true, "Alam Sutera", 0.4, ListingTier.HUMAN, "Rp. 11.000", ActivityStatus.Waiting("Menunggu diambil, maks s/d 21:21")),
    ActivityEntry("a3", "13 September 2026", "Ayam olie", "fadlhan", true, "Alam Sutera", 0.4, ListingTier.HUMAN, "Rp. 11.000", ActivityStatus.Done),
    ActivityEntry("a4", "12 September 2026", "Ayam olie", "fadlhan", true, "Alam Sutera", 0.4, ListingTier.HUMAN, "Rp. 11.000", ActivityStatus.Done),
)

/** Matches Figma node 78:13801 "Activity-ambil". The "Jual" tab (85:3051/85:3327, merchant
 * side) isn't built this session — it's a plain placeholder here rather than left dead. */
@Composable
fun ActivityScreen(modifier: Modifier = Modifier) {
    var tab by remember { mutableStateOf(ActivityTab.AMBIL) }
    var selectedFilter by remember { mutableStateOf(0) }
    val filters = listOf("Untuk manusia", "Untuk Ternak", "Untuk kompos", "Terdekat", "Gratis")

    Column(modifier = modifier.fillMaxSize().background(SgColor.Neutral100)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SgColor.BaseWhite)
                .border(BorderStroke(1.dp, SgColor.Neutral200))
                .padding(top = 20.dp, start = 24.dp, end = 24.dp),
            verticalArrangement = Arrangement.spacedBy(19.dp),
        ) {
            Text(text = "Aktifitas", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SgColor.Neutral800)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filters.size) { i ->
                    FilterChip(label = filters[i], selected = i == selectedFilter, onClick = { selectedFilter = i })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(17.dp)) {
                TabItem(label = "Ambil", selected = tab == ActivityTab.AMBIL, onClick = { tab = ActivityTab.AMBIL })
                TabItem(label = "Jual", selected = tab == ActivityTab.JUAL, onClick = { tab = ActivityTab.JUAL })
            }
        }

        if (tab == ActivityTab.JUAL) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Aktivitas jual belum tersedia di sesi ini",
                    style = SgTextStyle.TextSmRegular,
                    color = SgColor.Neutral500,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(mockAmbil, key = { it.id }) { entry -> ActivityCard(entry) }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = if (selected) SgColor.Brand500 else SgColor.Neutral500,
        modifier = Modifier
            .background(if (selected) SgColor.Green50 else SgColor.BaseWhite, RoundedCornerShape(30.dp))
            .border(BorderStroke(1.dp, if (selected) SgColor.Brand500 else SgColor.Neutral200), RoundedCornerShape(30.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 4.dp),
    )
}

@Composable
private fun TabItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
    ) {
        Column {
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) SgColor.Neutral800 else SgColor.Neutral500,
            )
            if (selected) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(Modifier.fillMaxWidth().height(2.dp).background(SgColor.Brand500))
            }
        }
    }
}

@Composable
private fun ActivityCard(entry: ActivityEntry, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SgColor.BaseWhite, RoundedCornerShape(25.dp))
            .border(BorderStroke(1.dp, SgColor.Neutral200), RoundedCornerShape(25.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = entry.date, fontSize = 10.sp, color = SgColor.Neutral500)
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(
                modifier = Modifier
                    .width(118.dp)
                    .height(107.dp)
                    .clip(RoundedCornerShape(17.dp)),
            ) {
                Image(
                    painter = painterResource(R.drawable.listing_ayam_olie),
                    contentDescription = entry.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().background(SgColor.Neutral300),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = entry.title, style = SgTextStyle.TextSmSemibold, color = SgColor.Neutral800)
                SellerLocationLines(entry)
                StatusLine(entry.status)
                Spacer(modifier = Modifier.height(if (entry.status is ActivityStatus.Done) 12.dp else 4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TierBadge(tier = entry.tier)
                    Text(text = entry.price, style = SgTextStyle.TextSmSemibold, color = SgColor.Neutral800)
                }
            }
        }
        if (entry.status is ActivityStatus.Done) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Beri penilaian", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = SgColor.Neutral800)
                Row {
                    repeat(5) {
                        Icon(
                            painter = painterResource(R.drawable.ic_star),
                            contentDescription = null,
                            tint = SgColor.Neutral300,
                            modifier = Modifier.width(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SellerLocationLines(entry: ActivityEntry) {
    Row {
        Text(text = entry.sellerName, fontSize = 10.sp, color = SgColor.Neutral800)
        if (entry.sellerVerified) {
            Text(text = " • Verified", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = SgColor.Brand600)
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_location_card),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.height(14.dp),
        )
        Text(
            text = "${entry.location} • ${entry.distanceKm} km",
            fontSize = 10.sp,
            color = SgColor.Neutral500,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun StatusLine(status: ActivityStatus) {
    val (icon, label, color) = when (status) {
        is ActivityStatus.Waiting -> Triple(R.drawable.ic_clock, status.deadlineLabel, SgColor.YellowStatus)
        ActivityStatus.Done -> Triple(R.drawable.ic_check_circle, "Makanan sudah diambil", SgColor.Brand600)
        ActivityStatus.Cancelled -> Triple(R.drawable.ic_error, "Pesanan dibatalkan", SgColor.RedStatus)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.width(12.dp))
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = color, modifier = Modifier.padding(start = 4.dp))
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun ActivityScreenPreview() {
    SisaGunaTheme { ActivityScreen() }
}
