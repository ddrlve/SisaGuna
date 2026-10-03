package com.sisaguna.android.ui.domain

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sisaguna.android.data.model.Review
import com.sisaguna.android.ui.i18n.Text
import com.sisaguna.android.ui.i18n.l
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgTextStyle
import java.time.Duration
import java.time.Instant

val StarGold = Color(0xFFF5A524)

@Composable
fun StarRow(stars: Int, size: Int = 14) {
    Row {
        repeat(5) { i ->
            Icon(Icons.Rounded.Star, contentDescription = null, tint = if (i < stars) StarGold else SgColor.Neutral300, modifier = Modifier.size(size.dp))
        }
    }
}

fun relativeDay(at: Instant, now: Instant = Instant.now()): String {
    val d = Duration.between(at, now).toDays()
    return when {
        d <= 0 -> l("Hari ini", "Today")
        d == 1L -> l("Kemarin", "Yesterday")
        d < 7 -> l("$d hari lalu", "$d days ago")
        else -> l("${d / 7} minggu lalu", "${d / 7} weeks ago")
    }
}

/** A buyer review: who, stars, when, what they bought, comment, and their photos. */
@Composable
fun ReviewCard(review: Review, modifier: Modifier = Modifier, maxLines: Int = 3) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(SgColor.BaseWhite)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).background(SgColor.Mint, CircleShape), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Text(review.author.first().uppercase(), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SgColor.Brand700)
            }
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                Text(review.author, style = SgTextStyle.Label, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StarRow(review.stars, 12)
                    Text("  " + relativeDay(review.createdAt), style = SgTextStyle.Caption)
                }
            }
        }
        review.itemTitle?.let {
            Text(
                it,
                style = SgTextStyle.TextXsMedium,
                color = SgColor.InkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.background(SgColor.Page, RoundedCornerShape(SgRadius.Pill)).padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Text(review.comment, style = SgTextStyle.Body.copy(color = SgColor.Ink), maxLines = maxLines, overflow = TextOverflow.Ellipsis)
        if (review.photos.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                review.photos.take(3).forEach { p ->
                    AsyncImage(p, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)))
                }
            }
        }
    }
}
