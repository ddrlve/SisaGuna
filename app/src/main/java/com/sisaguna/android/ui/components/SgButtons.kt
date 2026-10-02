package com.sisaguna.android.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgTextStyle

enum class SgButtonStyle { Primary, Secondary, Destructive, Ghost }

/**
 * One button for the whole app so hierarchy is consistent: Primary (brand fill) is the single
 * main action on a screen; Secondary (white + hairline) is the alternative; Destructive is red
 * text on a red tint; Ghost is text only. Disabled keeps the shape and drops to 40% alpha so
 * users can still read what it would do.
 */
@Composable
fun SgButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: SgButtonStyle = SgButtonStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    height: Dp = 52.dp,
    leading: (@Composable RowScope.() -> Unit)? = null,
) {
    val (bg, fg, border) = when (style) {
        SgButtonStyle.Primary -> Triple(SgColor.Brand500, SgColor.BaseWhite, null)
        SgButtonStyle.Secondary -> Triple(SgColor.BaseWhite, SgColor.Ink, SgColor.Hairline)
        SgButtonStyle.Destructive -> Triple(Color(0xFFFDECEC), SgColor.RedStatus, null)
        SgButtonStyle.Ghost -> Triple(Color.Transparent, SgColor.Brand600, null)
    }
    val shape = RoundedCornerShape(SgRadius.Tile)
    Row(
        modifier = modifier
            .height(height)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(bg)
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
            .pressable(onClick = { if (!loading) onClick() }, enabled = enabled)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(color = fg, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            if (leading != null) {
                leading()
                Box(Modifier.width(8.dp))
            }
            Text(text, style = SgTextStyle.Label, color = fg, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

/** −  n  + stepper. The number slides up/down so the change reads as a count, not a swap. */
@Composable
fun QuantityStepper(
    quantity: Int,
    onChange: (Int) -> Unit,
    max: Int,
    modifier: Modifier = Modifier,
    min: Int = 1,
    compact: Boolean = false,
) {
    val buttonSize = if (compact) 30.dp else 40.dp
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(SgRadius.Pill))
            .background(SgColor.BaseWhite)
            .border(1.dp, SgColor.Hairline, RoundedCornerShape(SgRadius.Pill))
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(Icons.Rounded.Remove, "Kurangi", enabled = quantity > min, size = buttonSize) { onChange(quantity - 1) }
        AnimatedContent(
            targetState = quantity,
            transitionSpec = {
                val up = targetState > initialState
                (slideInVertically(tween(160, easing = SgEaseOut)) { if (up) it else -it } + fadeIn(tween(160))) togetherWith
                    (slideOutVertically(tween(120)) { if (up) -it else it } + fadeOut(tween(120)))
            },
            label = "qty",
            modifier = Modifier.width(if (compact) 28.dp else 36.dp),
        ) { q ->
            Text(q.toString(), style = SgTextStyle.Label, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        StepButton(Icons.Rounded.Add, "Tambah", enabled = quantity < max, size = buttonSize) { onChange(quantity + 1) }
    }
}

@Composable
private fun StepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    size: Dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (enabled) SgColor.Mint else SgColor.Page)
            .pressable(onClick, enabled = enabled, role = Role.Button, pressedScale = 0.9f)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) SgColor.Brand700 else SgColor.Hairline, modifier = Modifier.size(18.dp))
    }
}
