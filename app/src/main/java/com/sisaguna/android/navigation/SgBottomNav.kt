package com.sisaguna.android.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import com.sisaguna.android.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sisaguna.android.R
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgTextStyle

private data class BottomNavItem(val screen: Screen, val label: String, val icon: Int)

private val items = listOf(
    BottomNavItem(Screen.Home, "Beranda", R.drawable.ic_nav_home),
    BottomNavItem(Screen.Activity, "Aktivitas", R.drawable.ic_nav_activity),
    BottomNavItem(Screen.Saved, "Disimpan", R.drawable.ic_nav_saved),
    BottomNavItem(Screen.Profile, "Profil", R.drawable.ic_nav_profile),
)

/**
 * Figma node 31:1520, restyled: a frosted bar (near-opaque white over the page + hairline top
 * edge) where the active tab grows a mint pill behind its icon. The pill width springs so quick
 * taps between tabs retarget smoothly.
 */
@Composable
fun SgBottomNav(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().background(SgColor.BaseWhite.copy(alpha = 0.97f))) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(SgColor.Hairline))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .selectableGroup(),
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.screen.route
                val tint by animateColorAsState(if (selected) SgColor.Brand700 else SgColor.InkMuted, tween(160), label = "navTint")
                val pillWidth by animateDpAsState(
                    if (selected) 56.dp else 32.dp,
                    spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow), // critically damped: a tap has no momentum to overshoot with
                    label = "navPill",
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(SgRadius.Tile))
                        .selectable(selected = selected, onClick = { onNavigate(item.screen) }, role = Role.Tab)
                        .padding(vertical = 4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .width(pillWidth)
                            .clip(RoundedCornerShape(SgRadius.Pill))
                            .background(if (selected) SgColor.Mint else androidx.compose.ui.graphics.Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painter = painterResource(item.icon), contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                    }
                    Text(text = item.label, style = SgTextStyle.TextXsMedium, color = tint, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}
