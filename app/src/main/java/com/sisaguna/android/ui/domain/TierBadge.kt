package com.sisaguna.android.ui.domain

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.ui.theme.SgColor

/** Outlined tier pill on listing cards (see figma/HomePage.jpeg, figma/products.jpeg). */
@Composable
fun TierBadge(tier: ListingTier, modifier: Modifier = Modifier) {
    val (label, color) = when (tier) {
        ListingTier.HUMAN -> "Untuk manusia" to SgColor.Brand700
        ListingTier.ANIMAL_FEED -> "Untuk ternak" to SgColor.Yellow500
        ListingTier.COMPOST -> "Untuk kompos" to SgColor.Brand700
    }
    Text(
        text = label,
        color = color,
        fontSize = 11.sp,
        modifier = modifier
            .border(BorderStroke(1.dp, color), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
