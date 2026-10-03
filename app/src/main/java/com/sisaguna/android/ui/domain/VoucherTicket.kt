package com.sisaguna.android.ui.domain

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material3.Icon
import com.sisaguna.android.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.sisaguna.android.ui.components.pressable
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle

/** Rounded rectangle with two half-circle notches cut at [notchX] — the classic ticket. */
class TicketShape(private val notchX: Dp, private val notchRadius: Dp = 8.dp, private val corner: Dp = 14.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { notchRadius.toPx() }
        val x = with(density) { notchX.toPx() }
        val c = with(density) { corner.toPx() }
        val body = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(c, c))) }
        val notches = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(Offset(x, 0f), r))
            addOval(androidx.compose.ui.geometry.Rect(Offset(x, size.height), r))
        }
        return Outline.Generic(Path.combine(PathOperation.Difference, body, notches))
    }
}

@Composable
fun VoucherTicket(
    title: String,
    subtitle: String,
    trailing: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingColor: Color = SgColor.Brand700,
    selected: Boolean = false,
) {
    val shape = TicketShape(notchX = 64.dp)
    val bg by animateColorAsState(if (selected) SgColor.Mint else SgColor.BaseWhite, tween(150), label = "ticketBg")
    Row(
        modifier = modifier
            .height(76.dp)
            .clip(shape)
            .background(bg)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) SgColor.Brand500 else SgColor.Hairline, shape)
            .pressable(onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(64.dp).fillMaxHeight().background(SgColor.PromoTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.ConfirmationNumber, contentDescription = null, tint = SgColor.PromoInk, modifier = Modifier.size(26.dp))
        }
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = SgSpacing.Md),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, style = SgTextStyle.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = SgTextStyle.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(trailing, style = SgTextStyle.TextXsMedium, color = trailingColor, maxLines = 1)
        }
        if (selected) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = "Dipilih", tint = SgColor.Brand500, modifier = Modifier.padding(end = SgSpacing.Md))
        }
    }
}
