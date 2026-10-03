package com.sisaguna.android.ui.domain

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import com.sisaguna.android.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.ui.theme.SgFont
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgTextStyle

/** Initial-letter avatar with a ring and an optional verified badge (Saved, Profile). */
@Composable
fun InitialAvatar(
    initial: Char,
    size: Dp,
    verified: Boolean,
    modifier: Modifier = Modifier,
    fill: Color = SgColor.Mint,
    ring: Color = SgColor.Brand300,
    textColor: Color = SgColor.Brand700,
) {
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .size(size)
                .border(2.dp, ring, CircleShape)
                .background(fill, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial.toString(),
                fontFamily = SgFont,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.4f).sp,
                color = textColor,
            )
        }
        if (verified) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(size * 0.38f)
                    .background(SgColor.BaseWhite, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Verified,
                    contentDescription = null,
                    tint = SgColor.Brand500,
                    modifier = Modifier.size(size * 0.32f),
                )
            }
        }
    }
}

/** Name / Verified pill / rating / area line shared by the Saved card and merchant header. */
@Composable
fun MerchantSummary(
    merchant: Merchant,
    distanceKm: Double?,
    verifiedLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = merchant.name,
                style = SgTextStyle.Label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (merchant.isVerified) {
                Text(
                    text = verifiedLabel,
                    fontFamily = SgFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    color = SgColor.Brand700,
                    modifier = Modifier
                        .background(SgColor.Mint, CircleShape)
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
        }
        val meta = buildList {
            merchant.rating?.let { add("★ %.1f".format(it)) }
            add(if (distanceKm != null) "${merchant.location} (${formatDistance(distanceKm)})" else merchant.location)
        }.joinToString("  •  ")
        Text(text = meta, style = SgTextStyle.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
