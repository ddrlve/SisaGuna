package com.sisaguna.android.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sisaguna.android.data.settings.AppLanguage
import com.sisaguna.android.data.settings.ThemeMode
import com.sisaguna.android.data.settings.UserMode
import com.sisaguna.android.ui.components.SgEaseOut
import com.sisaguna.android.ui.components.SgTopBar
import com.sisaguna.android.ui.components.pressable
import com.sisaguna.android.ui.i18n.Text
import com.sisaguna.android.ui.i18n.l
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle

/**
 * Language, appearance and buyer/seller mode. Every choice applies instantly — no "save"
 * button — and the whole app re-renders in place, so the user sees the result of the tap.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(SgColor.Page)) {
        SgTopBar(title = l("Pengaturan", "Settings"), onBack = onBack)
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = SgSpacing.Gutter, vertical = SgSpacing.Sm),
            verticalArrangement = Arrangement.spacedBy(SgSpacing.Xl),
        ) {
            Group(l("Bahasa", "Language"), Icons.Rounded.Translate) {
                AppLanguage.entries.forEach { lang ->
                    OptionRow(
                        title = lang.nativeName,
                        sub = if (lang == AppLanguage.ID) "Indonesian" else "Bahasa Inggris",
                        icon = null,
                        selected = settings.language == lang,
                        onClick = { viewModel.setLanguage(lang) },
                    )
                }
            }
            Group(l("Tampilan", "Appearance"), Icons.Rounded.LightMode) {
                OptionRow(l("Terang", "Light"), l("Default, sesuai desain", "Default, as designed"), Icons.Rounded.LightMode, settings.themeMode == ThemeMode.LIGHT) { viewModel.setThemeMode(ThemeMode.LIGHT) }
                OptionRow(l("Gelap", "Dark"), l("Nyaman di malam hari, hemat baterai OLED", "Easier at night, saves OLED battery"), Icons.Rounded.DarkMode, settings.themeMode == ThemeMode.DARK) { viewModel.setThemeMode(ThemeMode.DARK) }
                OptionRow(l("Ikuti sistem", "Follow system"), l("Ikut pengaturan HP", "Match the phone setting"), Icons.Rounded.BrightnessAuto, settings.themeMode == ThemeMode.SYSTEM) { viewModel.setThemeMode(ThemeMode.SYSTEM) }
            }
            Group(l("Mode aplikasi", "App mode"), Icons.Rounded.Storefront) {
                OptionRow(l("Pembeli", "Buyer"), l("Cari & selamatkan makanan di sekitar", "Find and rescue food nearby"), Icons.Rounded.ShoppingBag, settings.mode == UserMode.BUYER) { viewModel.setMode(UserMode.BUYER) }
                OptionRow(l("Mitra / penjual", "Seller"), l("Posting sisa, kelola pesanan masuk", "Post leftovers, manage incoming orders"), Icons.Rounded.Storefront, settings.mode == UserMode.MERCHANT) { viewModel.setMode(UserMode.MERCHANT) }
            }
            Box(Modifier.height(SgSpacing.Xl))
        }
    }
}

@Composable
private fun Group(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = SgColor.InkMuted, modifier = Modifier.size(18.dp))
            Text(title, style = SgTextStyle.Title, modifier = Modifier.padding(start = 8.dp))
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(SgRadius.Card)).background(SgColor.BaseWhite).padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) { content() }
    }
}

@Composable
private fun OptionRow(title: String, sub: String, icon: ImageVector?, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) SgColor.Mint else Color.Transparent, tween(160, easing = SgEaseOut), label = "optBg")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .pressable(onClick, pressedScale = 0.98f)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(Modifier.size(36.dp).background(SgColor.Page, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = SgColor.Ink, modifier = Modifier.size(18.dp))
            }
        }
        Column(Modifier.padding(start = if (icon != null) 12.dp else 4.dp).weight(1f)) {
            Text(title, style = SgTextStyle.Label)
            Text(sub, style = SgTextStyle.Caption)
        }
        if (selected) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = SgColor.Brand500)
        } else {
            Box(Modifier.size(22.dp).border(1.5.dp, SgColor.Neutral300, CircleShape))
        }
    }
}
