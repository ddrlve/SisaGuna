package com.sisaguna.android.feature.profile

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.foundation.layout.fillMaxSize
import com.sisaguna.android.ui.i18n.l
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.sisaguna.android.ui.i18n.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sisaguna.android.R
import com.sisaguna.android.data.model.ImpactStats
import com.sisaguna.android.ui.components.pressable
import com.sisaguna.android.ui.domain.IdLocale
import com.sisaguna.android.ui.domain.InitialAvatar
import com.sisaguna.android.ui.domain.formatRupiah
import com.sisaguna.android.ui.domain.ListingCard
import com.sisaguna.android.ui.theme.SgFont
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch


/** Figma 273:12725. */
@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onNotificationSettings: () -> Unit,
    onAddresses: () -> Unit,
    onHistory: () -> Unit,
    onPayments: () -> Unit,
    onPassword: () -> Unit,
    onHelp: () -> Unit,
    onPrivacy: () -> Unit,
    onCatalog: () -> Unit,
    onSettings: () -> Unit = {},
    onListingClick: (String) -> Unit,
    onLogout: () -> Unit,
    profileUpdated: Boolean,
    onProfileUpdatedShown: () -> Unit,
    message: String? = null,
    onMessageShown: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showLogout by remember { mutableStateOf(false) }
    val updatedMessage = stringResource(R.string.profile_updated)

    LaunchedEffect(message) {
        if (message != null) {
            onMessageShown()
            snackbar.showSnackbar(message)
        }
    }

    LaunchedEffect(profileUpdated) {
        if (profileUpdated) {
            onProfileUpdatedShown()
            snackbar.showSnackbar(updatedMessage)
        }
    }

    Scaffold(
        containerColor = SgColor.Page,
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = SgSpacing.Xl),
        ) {
            item { ProfileHeader(state, onEditProfile, onPhoto = viewModel::setPhoto) }
            item {
                ImpactCard(
                    impact = state.impact,
                    modifier = Modifier.padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Lg),
                )
            }
            if (state.catalog.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = SgSpacing.Gutter, end = SgSpacing.Sm, top = SgSpacing.Xl),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.profile_catalog_title), style = SgTextStyle.Title, modifier = Modifier.weight(1f))
                        TextButton(onClick = onCatalog) {
                            Text(stringResource(R.string.home_see_all), style = SgTextStyle.TextXsMedium, color = SgColor.Brand600)
                        }
                    }
                }
                item {
                    val now = remember { Instant.now() }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = SgSpacing.Gutter, vertical = SgSpacing.Xs),
                        horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
                    ) {
                        items(state.catalog, key = { it.first.id }) { (listing, merchant) ->
                            ListingCard(listing = listing, merchant = merchant, now = now, onClick = { onListingClick(listing.id) })
                        }
                    }
                }
            }
            item {
                SettingsSection(
                    title = stringResource(R.string.profile_section_account),
                    rows = listOf(
                        SettingRow(Icons.Rounded.Place, stringResource(R.string.profile_row_address), "${state.addressCount} tersimpan", onAddresses),
                        SettingRow(Icons.Rounded.History, stringResource(R.string.profile_row_history), "${state.rescueCount} selesai", onHistory),
                        SettingRow(Icons.Rounded.CreditCard, stringResource(R.string.profile_row_payment), "${state.paymentCount} aktif", onPayments),
                        SettingRow(Icons.Rounded.Lock, stringResource(R.string.profile_row_password), stringResource(R.string.profile_row_password_hint), onPassword),
                        SettingRow(
                            Icons.Rounded.NotificationsNone,
                            stringResource(R.string.profile_row_notifications),
                            stringResource(if (state.notificationsEnabled) R.string.profile_row_on else R.string.profile_row_off),
                            onNotificationSettings,
                            hintColor = if (state.notificationsEnabled) SgColor.Brand600 else SgColor.InkMuted,
                        ),
                    ),
                )
            }
            item {
                SettingsSection(
                    title = stringResource(R.string.profile_section_support),
                    rows = listOf(
                        SettingRow(Icons.Rounded.Tune, l("Pengaturan", "Settings"), l("Bahasa, tema, mode", "Language, theme, mode"), onSettings),
                        SettingRow(Icons.AutoMirrored.Rounded.HelpOutline, stringResource(R.string.profile_row_help), stringResource(R.string.profile_row_help_hint), onHelp),
                        SettingRow(Icons.Rounded.Shield, stringResource(R.string.profile_row_privacy), stringResource(R.string.profile_row_privacy_hint), onPrivacy),
                    ),
                )
            }
            item {
                TextButton(
                    onClick = { showLogout = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Xl)
                        .height(52.dp),
                    shape = RoundedCornerShape(SgRadius.Tile),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = SgColor.RedStatus, modifier = Modifier.size(20.dp))
                    Text(
                        stringResource(R.string.profile_logout),
                        style = SgTextStyle.Label,
                        color = SgColor.RedStatus,
                        modifier = Modifier.padding(start = SgSpacing.Sm),
                    )
                }
            }
        }
    }

    if (showLogout) {
        AlertDialog(
            onDismissRequest = { showLogout = false },
            title = { Text(stringResource(R.string.profile_logout_title), style = SgTextStyle.Title) },
            text = { Text(stringResource(R.string.profile_logout_body), style = SgTextStyle.Body) },
            confirmButton = {
                TextButton(onClick = {
                    showLogout = false
                    onLogout()
                }) { Text(stringResource(R.string.profile_logout_confirm), style = SgTextStyle.Label, color = SgColor.RedStatus) }
            },
            dismissButton = {
                TextButton(onClick = { showLogout = false }) {
                    Text(stringResource(R.string.common_cancel), style = SgTextStyle.Label, color = SgColor.Ink)
                }
            },
            containerColor = SgColor.BaseWhite,
        )
    }
}

@Composable
private fun ProfileHeader(state: ProfileUiState, onEditProfile: () -> Unit, onPhoto: (String?) -> Unit) {
    var choosing by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val media = com.sisaguna.android.ui.components.rememberMediaCapture(
        onPhotos = { uris -> uris.firstOrNull()?.let { onPhoto(persistProfilePhoto(context, it)) } },
        maxPick = 2,
    )
    val memberSince = remember(state.profile.memberSince) {
        state.profile.memberSince.format(DateTimeFormatter.ofPattern("MMMM yyyy", IdLocale))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SgColor.BaseWhite)
            .padding(start = SgSpacing.Gutter, end = SgSpacing.Sm, top = SgSpacing.Xl, bottom = SgSpacing.Xl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SgSpacing.Lg),
    ) {
        // Tap the avatar to take or pick a profile photo; a small camera badge says so.
        // Only the photo is clipped to a circle; the camera badge sits on the outer box so it can
        // hang over the edge instead of being cut off.
        Box(Modifier.size(76.dp).pressable({ choosing = true }, pressedScale = 0.95f)) {
            if (state.profile.photoUri != null) {
                coil.compose.AsyncImage(
                    state.profile.photoUri,
                    contentDescription = l("Foto profil", "Profile photo"),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(CircleShape).border(2.dp, SgColor.Brand300, CircleShape),
                )
            } else {
                InitialAvatar(
                    initial = state.profile.initial,
                    size = 72.dp,
                    verified = false,
                    fill = SgColor.Mint,
                    ring = SgColor.Brand300,
                    textColor = SgColor.Brand700,
                )
            }
            Box(
                Modifier.align(Alignment.BottomEnd).size(26.dp).border(2.dp, SgColor.BaseWhite, CircleShape).padding(2.dp).background(SgColor.Brand500, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.PhotoCamera, contentDescription = null, tint = SgColor.OnBrand, modifier = Modifier.size(13.dp)) }
        }
        if (choosing) {
            AlertDialog(
                onDismissRequest = { choosing = false },
                title = { Text(l("Foto profil", "Profile photo"), style = SgTextStyle.Title) },
                text = {
                    Column {
                        PhotoOption(Icons.Rounded.PhotoCamera, l("Ambil foto", "Take a photo")) { choosing = false; media.takePhoto() }
                        PhotoOption(Icons.Rounded.PhotoLibrary, l("Pilih dari galeri", "Choose from gallery")) { choosing = false; media.pickPhotos() }
                        if (state.profile.photoUri != null) {
                            PhotoOption(Icons.Rounded.DeleteOutline, l("Hapus foto", "Remove photo"), destructive = true) { choosing = false; onPhoto(null) }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { choosing = false }) { Text(stringResource(R.string.common_cancel), style = SgTextStyle.Label, color = SgColor.Ink) } },
                containerColor = SgColor.BaseWhite,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(state.profile.name, style = SgTextStyle.Display, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = SgColor.Brand500, modifier = Modifier.size(14.dp))
                Text(state.profile.location, style = SgTextStyle.Body, modifier = Modifier.padding(start = 2.dp))
            }
            Text(stringResource(R.string.profile_member_since, memberSince), style = SgTextStyle.Caption)
        }
        IconButton(onClick = onEditProfile) {
            Box(
                modifier = Modifier.size(36.dp).background(SgColor.Mint, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.profile_edit_cd), tint = SgColor.Brand600, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ImpactCard(impact: ImpactStats?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Card))
            .background(SgColor.BaseWhite)
            .border(1.dp, SgColor.Hairline, RoundedCornerShape(SgRadius.Card))
            .padding(SgSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(SgSpacing.Md),
    ) {
        Text(stringResource(R.string.profile_impact_title), style = SgTextStyle.Title)
        // IntrinsicSize.Min + fillMaxHeight: all three tiles take the tallest one's height, so
        // "porsi" and "kg" tiles line up whatever their label length.
        Row(Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
            StatTile("${impact?.portions ?: "…"}", stringResource(R.string.profile_impact_portions_unit), stringResource(R.string.profile_impact_portions_label), Modifier.weight(1f))
            StatTile("${impact?.compostKg ?: "…"}", "kg", stringResource(R.string.profile_impact_compost_label), Modifier.weight(1f))
            StatTile("${impact?.carbonKg ?: "…"}", "kg", stringResource(R.string.profile_impact_carbon_label), Modifier.weight(1f))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(SgRadius.Tile))
                .background(SgColor.Yellow50)
                .padding(SgSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(SgColor.Yellow300, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("Rp", fontFamily = SgFont, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF854D0E))
            }
            Column {
                Text(
                    stringResource(R.string.profile_saved_money, impact?.let { formatRupiah(it.savedRupiah) } ?: "-"),
                    style = SgTextStyle.Label,
                )
                Text(stringResource(R.string.profile_saved_money_body), style = SgTextStyle.Caption)
            }
        }
    }
}

@Composable
private fun StatTile(value: String, unit: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(SgRadius.Thumb))
            .background(SgColor.Mint)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = SgTextStyle.Display, color = SgColor.Brand600, modifier = Modifier.alignByBaseline())
            Text(unit, style = SgTextStyle.Caption.copy(fontWeight = FontWeight.Bold), color = SgColor.Brand600, modifier = Modifier.alignByBaseline().padding(start = 3.dp))
        }
        Text(label, style = SgTextStyle.Caption, minLines = 2, maxLines = 2, modifier = Modifier.padding(top = 2.dp))
    }
}

private data class SettingRow(
    val icon: ImageVector,
    val label: String,
    val hint: String,
    val onClick: () -> Unit,
    val hintColor: Color = SgColor.InkMuted,
)

@Composable
private fun SettingsSection(title: String, rows: List<SettingRow>) {
    Column(modifier = Modifier.padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Xl)) {
        Text(title, style = SgTextStyle.Title, modifier = Modifier.padding(bottom = SgSpacing.Md))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(SgRadius.Card))
                .background(SgColor.BaseWhite)
                .border(1.dp, SgColor.Hairline, RoundedCornerShape(SgRadius.Card)),
        ) {
            rows.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp)
                        .pressable(row.onClick, pressedScale = 0.99f)
                        .padding(start = SgSpacing.Lg, end = SgSpacing.Md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(32.dp).background(SgColor.Page, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(row.icon, contentDescription = null, tint = SgColor.Ink, modifier = Modifier.size(18.dp))
                    }
                    Text(row.label, style = SgTextStyle.Label, modifier = Modifier.weight(1f).padding(start = SgSpacing.Md))
                    Text(row.hint, style = SgTextStyle.Caption, color = row.hintColor)
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = SgColor.InkMuted)
                }
                if (index < rows.lastIndex) {
                    HorizontalDivider(color = SgColor.Hairline, modifier = Modifier.padding(start = 64.dp))
                }
            }
        }
    }
}


@Composable
private fun PhotoOption(icon: ImageVector, label: String, destructive: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(SgRadius.Thumb)).pressable(onClick).padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (destructive) SgColor.RedStatus else SgColor.Ink)
        Text(label, style = SgTextStyle.Label, color = if (destructive) SgColor.RedStatus else SgColor.Ink, modifier = Modifier.padding(start = 12.dp))
    }
}

/** Gallery URIs lose their read grant after a restart, so the chosen photo is copied into app
 * storage and that file is what the profile points at. */
private fun persistProfilePhoto(context: android.content.Context, uri: android.net.Uri): String? = runCatching {
    val out = java.io.File(context.filesDir, "profile_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { input -> out.outputStream().use { input.copyTo(it) } }
    android.net.Uri.fromFile(out).toString()
}.getOrNull()
