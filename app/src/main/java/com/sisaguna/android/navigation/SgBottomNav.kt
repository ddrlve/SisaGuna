package com.sisaguna.android.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import com.sisaguna.android.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sisaguna.android.R
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgTextStyle
import kotlinx.coroutines.delay

private data class BottomNavItem(val screen: Screen, val label: String, val icon: Int)

private val items = listOf(
    BottomNavItem(Screen.Home, "Beranda", R.drawable.ic_nav_home),
    BottomNavItem(Screen.Activity, "Aktivitas", R.drawable.ic_nav_activity),
    BottomNavItem(Screen.Saved, "Disimpan", R.drawable.ic_nav_saved),
    BottomNavItem(Screen.Profile, "Profil", R.drawable.ic_nav_profile),
)

private val PillWidth = 56.dp
private val PillHeight = 30.dp

/**
 * Figma node 31:1520, restyled: a frosted bar (near-opaque white over the page + hairline top
 * edge) with one mint pill that slides to the active tab.
 *
 * Smoothness notes (device feedback: tab taps felt sticky):
 * - The pill is a single element moved with translationX (draw phase only). The old version
 *   animated each tab's pill width, which re-laid-out the row every frame and made the old pill
 *   vanish while the new one grew from nothing.
 * - Selection is optimistic: the pill and tint move on the tap itself instead of waiting for
 *   the NavController to commit the new route a frame or two later.
 * - Spring, not tween, so rapid taps across tabs retarget from the current position.
 */
@Composable
fun SgBottomNav(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tapped by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(currentRoute) { tapped = null }
    // A guest tapping a gated tab gets a sheet instead of a route change; slide the pill back.
    LaunchedEffect(tapped) {
        if (tapped != null) {
            delay(400)
            tapped = null
        }
    }
    val activeRoute = tapped ?: currentRoute
    val activeIndex = items.indexOfFirst { it.screen.route == activeRoute }

    Column(modifier.fillMaxWidth().background(SgColor.BaseWhite.copy(alpha = 0.97f))) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(SgColor.Hairline))
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            val density = LocalDensity.current
            val slotPx = with(density) { maxWidth.toPx() } / items.size
            val pillPx = with(density) { PillWidth.toPx() }
            val pillX by animateFloatAsState(
                targetValue = slotPx * activeIndex.coerceAtLeast(0) + (slotPx - pillPx) / 2f,
                // Slightly under-damped: settles in ~250ms with a hint of follow-through, no visible bounce.
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 600f),
                label = "navPillX",
            )
            val pillAlpha by animateFloatAsState(if (activeIndex >= 0) 1f else 0f, tween(150), label = "navPillAlpha")

            Box(
                Modifier
                    .padding(top = 4.dp)
                    .size(PillWidth, PillHeight)
                    .graphicsLayer {
                        translationX = pillX
                        alpha = pillAlpha
                    }
                    .background(SgColor.Mint, RoundedCornerShape(SgRadius.Pill)),
            )

            Row(Modifier.fillMaxWidth().selectableGroup()) {
                items.forEachIndexed { index, item ->
                    NavTab(
                        item = item,
                        selected = index == activeIndex,
                        onClick = {
                            if (item.screen.route != activeRoute) tapped = item.screen.route
                            onNavigate(item.screen)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavTab(item: BottomNavItem, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // Press feedback in place of the ripple: the pill is already the selection signal, a
    // ripple on top of it read as noise.
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, tween(if (pressed) 100 else 160), label = "navPress")
    val tint by animateColorAsState(if (selected) SgColor.Brand700 else SgColor.InkMuted, tween(150), label = "navTint")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interaction,
                indication = null,
            )
            .padding(vertical = 4.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        Box(Modifier.size(PillWidth, PillHeight), contentAlignment = Alignment.Center) {
            Icon(painter = painterResource(item.icon), contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Text(text = item.label, style = SgTextStyle.TextXsMedium, color = tint, modifier = Modifier.padding(top = 2.dp))
    }
}
