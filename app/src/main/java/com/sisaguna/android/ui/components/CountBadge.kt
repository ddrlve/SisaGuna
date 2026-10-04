package com.sisaguna.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgFont

/**
 * Unread count pill. The digits are optically centred: font padding is off and the line box
 * is trimmed to the glyphs, otherwise Jakarta's ascender space pushes the number low. Grows
 * into a pill for two digits; "9+" caps it.
 *
 * [ring] draws a cut-out border in the surface colour when the badge sits on an icon.
 */
@Composable
fun CountBadge(
    count: Int,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    color: Color = SgColor.Brand500,
    ring: Color? = null,
) {
    val label = if (count > 9) "9+" else count.toString()
    val ringed = if (ring != null) modifier.border(2.dp, ring, CircleShape).padding(2.dp) else modifier
    Box(
        ringed
            .defaultMinSize(minWidth = size, minHeight = size)
            .background(color, CircleShape)
            .padding(horizontal = if (label.length > 1) 5.dp else 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = SgColor.OnBrand,
            style = TextStyle(
                fontFamily = SgFont,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.6f).sp,
                lineHeight = (size.value * 0.6f).sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
            ),
        )
    }
}
