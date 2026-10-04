package com.sisaguna.android.ui.domain

import com.sisaguna.android.ui.i18n.l

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import com.sisaguna.android.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.ui.theme.SgColor

/** Tier pill on listing cards (Figma node 36:5541 on Home 29:93): light-green fill + border. */
@Composable
fun TierBadge(tier: ListingTier, modifier: Modifier = Modifier) {
    val label = when (tier) {
        ListingTier.HUMAN -> l("siap santap", "ready to eat")
        ListingTier.ANIMAL_FEED -> l("untuk ternak", "animal feed")
        ListingTier.COMPOST -> l("untuk kompos", "for compost")
    }
    Text(
        text = label,
        color = SgColor.Green600,
        fontSize = 10.sp,
        modifier = modifier
            .background(SgColor.Green50, RoundedCornerShape(30.dp))
            .border(BorderStroke(1.dp, SgColor.Green600), RoundedCornerShape(30.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
