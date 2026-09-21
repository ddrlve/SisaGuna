package com.sisaguna.android.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sisaguna.android.R
import com.sisaguna.android.ui.theme.SgColor

private data class BottomNavItem(val screen: Screen, val label: String, val icon: Int)

private val items = listOf(
    BottomNavItem(Screen.Home, "Home", R.drawable.ic_nav_home),
    BottomNavItem(Screen.Activity, "Activity", R.drawable.ic_nav_activity),
    BottomNavItem(Screen.Saved, "Saved", R.drawable.ic_nav_saved),
    BottomNavItem(Screen.Profile, "Profile", R.drawable.ic_nav_profile),
)

/** Matches Figma node 31:1520 "Bottom Navigation Bar" on the Home frame (29:93). */
@Composable
fun SgBottomNav(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SgColor.BaseWhite)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.screen.route
            val tint = if (selected) SgColor.Brand600 else SgColor.Neutral500
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(43.dp)
                    .selectable(
                        selected = selected,
                        onClick = { onNavigate(item.screen) },
                        role = Role.Tab,
                    ),
            ) {
                Icon(
                    painter = painterResource(item.icon),
                    contentDescription = item.label,
                    tint = tint,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(text = item.label, fontSize = 12.sp, color = tint)
            }
        }
    }
}
