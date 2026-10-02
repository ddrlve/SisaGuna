package com.sisaguna.android.ui.domain

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.sisaguna.android.ui.components.SgEaseOut
import com.sisaguna.android.ui.components.pressable
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle

/** Floating "n item · Rp x · Lihat keranjang" bar. Slides up from the bottom edge when the
 * cart gets its first item and back down when it empties. */
@Composable
fun CartBar(
    visible: Boolean,
    itemCount: Int,
    total: Int,
    merchantName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(260, easing = SgEaseOut)) { it } + fadeIn(tween(180)),
        exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(120)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SgSpacing.Gutter, vertical = SgSpacing.Sm)
                .shadow(8.dp, RoundedCornerShape(SgRadius.Tile))
                .clip(RoundedCornerShape(SgRadius.Tile))
                .background(SgColor.Brand600)
                .pressable(onClick)
                .padding(horizontal = SgSpacing.Lg, vertical = SgSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(SgColor.Brand500, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.ShoppingBag, contentDescription = null, tint = SgColor.BaseWhite, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("$itemCount item · ${formatPrice(total)}", style = SgTextStyle.Label, color = SgColor.BaseWhite)
                if (merchantName != null) {
                    Text("dari $merchantName", style = SgTextStyle.Caption, color = SgColor.Brand100, maxLines = 1)
                }
            }
            Text("Keranjang", style = SgTextStyle.Label, color = SgColor.BaseWhite)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = SgColor.BaseWhite)
        }
    }
}
