package com.sisaguna.android.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.sisaguna.android.ui.i18n.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sisaguna.android.R
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle

/** Strong ease-out: starts fast so taps feel answered immediately. Used for every 3a UI
 * transition instead of Compose's default FastOutSlowIn, which feels soft on entry. */
val SgEaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

/** Strong ease-in-out for things moving *across* the screen (segmented indicator, carousels). */
val SgEaseInOut = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)

/**
 * True when the user turned animations off (Developer options / Accessibility "Remove
 * animations" sets the animator scale to 0). Compose already zeroes finite tweens then, but
 * infinite decorative loops (floating hero cards) should stop entirely, so check this first.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember {
        android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * Clickable that shrinks to 97% while pressed — pressed feedback for cards and tiles where a
 * full-bleed ripple would look heavy. Scale is transform-only (graphicsLayer), so it never
 * triggers relayout.
 */
fun Modifier.pressable(
    onClick: () -> Unit,
    role: Role = Role.Button,
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(durationMillis = 140, easing = SgEaseOut),
        label = "pressScale",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interaction,
            indication = ripple(bounded = true, color = SgColor.Ink),
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
}

/** 3a sub-page top bar: 48dp back target on the left, title, optional trailing slot. */
@Composable
fun SgTopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(start = if (onBack != null) 4.dp else SgSpacing.Gutter, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.common_back_cd),
                    tint = SgColor.Ink,
                )
            }
        }
        Text(
            text = title,
            style = SgTextStyle.Title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(contentAlignment = Alignment.CenterEnd) { trailing() }
    }
}

/** Empty / not-found state shared by Saved, merchant detail, and Notifications. */
@Composable
fun SgEmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SgSpacing.Sm),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(SgColor.Mint, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = SgColor.Brand600, modifier = Modifier.size(32.dp))
        }
        Text(
            text = title,
            style = SgTextStyle.Title,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = SgSpacing.Sm),
        )
        Text(text = body, style = SgTextStyle.Body, textAlign = TextAlign.Center)
        if (action != null) {
            Box(modifier = Modifier.padding(top = SgSpacing.Md)) { action() }
        }
    }
}
