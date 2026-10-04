package com.sisaguna.android.feature.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Recycling
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import com.sisaguna.android.data.model.RescueTier
import com.sisaguna.android.ui.components.SgEaseOut
import com.sisaguna.android.ui.domain.FruitAvatar
import com.sisaguna.android.ui.domain.FruitAvatarBadge
import com.sisaguna.android.ui.domain.UserAvatar

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
import com.sisaguna.android.ui.i18n.SgSnackbarHost
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
        snackbarHost = { SgSnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = SgSpacing.Xl),
        ) {
            item { ProfileHeader(state, onEditProfile, onPhoto = viewModel::setPhoto, onAvatar = viewModel::setAvatar) }
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
                            .padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Xl, bottom = SgSpacing.Xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.profile_catalog_title), style = SgTextStyle.Title)
                            Text(
                                l("${state.catalog.size} makanan sedang dijual", "${state.catalog.size} items on sale"),
                                style = SgTextStyle.Caption,
                            )
                        }
                        // A real button: 40dp tall pill, label + chevron, instead of a tiny text link.
                        Row(
                            Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(SgRadius.Pill))
                                .background(SgColor.Mint)
                                .pressable(onCatalog, pressedScale = 0.95f)
                                .padding(start = 16.dp, end = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(R.string.home_see_all), style = SgTextStyle.Label, color = SgColor.Brand700)
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = SgColor.Brand700, modifier = Modifier.size(20.dp))
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
                        SettingRow(Icons.Rounded.History, stringResource(R.string.profile_row_history), l("${state.rescueCount} selesai", "${state.rescueCount} done"), onHistory),
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
private fun ProfileHeader(
    state: ProfileUiState,
    onEditProfile: () -> Unit,
    onPhoto: (String?) -> Unit,
    onAvatar: (String) -> Unit,
) {
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
            .padding(start = SgSpacing.Gutter, end = SgSpacing.Sm, top = SgSpacing.Xl, bottom = SgSpacing.Lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Tap the avatar to pick a fruit, take a photo or choose one. The camera badge sits on
        // the outer box so it can hang over the circle's edge.
        Box(Modifier.pressable({ choosing = true }, pressedScale = 0.95f)) {
            UserAvatar(state.profile, size = 76.dp, ring = SgColor.Brand300)
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
                    .border(2.dp, SgColor.BaseWhite, CircleShape)
                    .padding(2.dp)
                    .background(SgColor.Brand500, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.PhotoCamera, contentDescription = l("Ganti foto profil", "Change profile photo"), tint = SgColor.OnBrand, modifier = Modifier.size(14.dp)) }
        }
        Column(
            modifier = Modifier.weight(1f).padding(start = SgSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(state.profile.name, style = SgTextStyle.Display.copy(fontSize = 22.sp, lineHeight = 28.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = SgColor.Brand500, modifier = Modifier.size(14.dp))
                Text(state.profile.location, style = SgTextStyle.Body, modifier = Modifier.padding(start = 3.dp), maxLines = 1)
            }
            Text(stringResource(R.string.profile_member_since, memberSince), style = SgTextStyle.Caption)
        }
        IconButton(onClick = onEditProfile) {
            Box(
                modifier = Modifier.size(40.dp).background(SgColor.Mint, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.profile_edit_cd), tint = SgColor.Brand600, modifier = Modifier.size(18.dp))
            }
        }
    }
    if (choosing) {
        AvatarSheet(
            profile = state.profile,
            onAvatar = { onAvatar(it); choosing = false },
            onTakePhoto = { choosing = false; media.takePhoto() },
            onPickPhoto = { choosing = false; media.pickPhotos() },
            onRemovePhoto = { choosing = false; onPhoto(null) },
            onDismiss = { choosing = false },
        )
    }
}

/** Fruit grid first (the fun, zero-effort option), then camera and gallery. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvatarSheet(
    profile: com.sisaguna.android.data.model.UserProfile,
    onAvatar: (String) -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onDismiss: () -> Unit,
) {
    val current = if (profile.photoUri == null) FruitAvatar.of(profile.avatar) ?: FruitAvatar.defaultFor(profile.name) else null
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SgColor.BaseWhite,
        scrimColor = SgColor.Ink.copy(alpha = 0.32f),
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = SgSpacing.Gutter).padding(bottom = SgSpacing.Lg)) {
            Text(l("Foto profil", "Profile photo"), style = SgTextStyle.Title)
            Text(
                l("Pilih avatar buah, atau pakai fotomu sendiri.", "Pick a fruit avatar, or use your own photo."),
                style = SgTextStyle.Body,
                modifier = Modifier.padding(top = 4.dp, bottom = SgSpacing.Lg),
            )
            FruitAvatar.entries.chunked(4).forEach { rowFruits ->
                Row(Modifier.fillMaxWidth().padding(bottom = SgSpacing.Md), horizontalArrangement = Arrangement.SpaceBetween) {
                    rowFruits.forEach { fruit ->
                        val selected = fruit == current
                        val ring by animateColorAsState(if (selected) SgColor.Brand500 else Color.Transparent, tween(150), label = "fruitRing")
                        val scale by animateFloatAsState(if (selected) 1f else 0.92f, spring(dampingRatio = 0.7f, stiffness = 500f), label = "fruitScale")
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(SgRadius.Tile))
                                .pressable({ onAvatar(fruit.key) }, pressedScale = 0.92f)
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier
                                    .graphicsLayer { scaleX = scale; scaleY = scale }
                                    .border(2.5.dp, ring, CircleShape)
                                    .padding(4.dp),
                            ) { FruitAvatarBadge(fruit, 56.dp) }
                            Text(
                                fruit.label,
                                style = SgTextStyle.Caption,
                                color = if (selected) SgColor.Brand700 else SgColor.InkMuted,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = SgColor.Hairline, modifier = Modifier.padding(vertical = SgSpacing.Sm))
            PhotoOption(Icons.Rounded.PhotoCamera, l("Ambil foto", "Take a photo"), onClick = onTakePhoto)
            PhotoOption(Icons.Rounded.PhotoLibrary, l("Pilih dari galeri", "Choose from gallery"), onClick = onPickPhoto)
            if (profile.photoUri != null) {
                PhotoOption(Icons.Rounded.DeleteOutline, l("Hapus foto", "Remove photo"), destructive = true, onClick = onRemovePhoto)
            }
        }
    }
}

/**
 * Hero card: portions rescued as the headline, the rescue level with progress to the next one,
 * and the three supporting numbers underneath. The count and the bar fill once when the
 * numbers arrive; they don't replay on recomposition.
 */
@Composable
private fun ImpactCard(impact: ImpactStats?, modifier: Modifier = Modifier) {
    val portions = impact?.portions ?: 0
    val tier = RescueTier.of(portions)
    val count = remember { Animatable(0f) }
    val fill = remember { Animatable(0f) }
    LaunchedEffect(impact) {
        if (impact != null) {
            launch { count.animateTo(portions.toFloat(), tween(700, easing = SgEaseOut)) }
            fill.animateTo(tier.progress(portions), tween(800, delayMillis = 120, easing = SgEaseOut))
        }
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(SgRadius.Card))
                .background(Brush.linearGradient(listOf(SgColor.Brand600, SgColor.Brand700)))
                .padding(SgSpacing.Lg),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.profile_impact_title), style = SgTextStyle.Caption, color = Color.White.copy(alpha = 0.8f))
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                        androidx.compose.material3.Text(
                            if (impact == null) "…" else count.value.toInt().toString(),
                            style = SgTextStyle.Display.copy(fontSize = 40.sp, lineHeight = 44.sp),
                            color = Color.White,
                            modifier = Modifier.alignByBaseline(),
                        )
                        Text(
                            l("porsi diselamatkan", "portions rescued"),
                            style = SgTextStyle.Label,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.alignByBaseline().padding(start = 6.dp),
                        )
                    }
                }
                Row(
                    Modifier.background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(SgRadius.Pill)).padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.Text(tier.emoji, fontSize = 13.sp)
                    Text(l(tier.label, tier.labelEn), style = SgTextStyle.TextXsMedium, color = Color.White, modifier = Modifier.padding(start = 4.dp))
                }
            }
            // Progress to the next level.
            Box(
                Modifier
                    .padding(top = SgSpacing.Md)
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(SgRadius.Pill))
                    .background(Color.White.copy(alpha = 0.2f)),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = fill.value
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                        }
                        .background(SgColor.Brand300, RoundedCornerShape(SgRadius.Pill)),
                )
            }
            Text(
                tier.next?.let { n ->
                    val left = (n.minPortions - portions).coerceAtLeast(0)
                    l("$left porsi lagi menuju ${n.emoji} ${n.label}", "$left more portions to ${n.emoji} ${n.labelEn}")
                } ?: l("Level tertinggi. Terima kasih sudah ikut menyelamatkan makanan!", "Top level. Thanks for rescuing food!"),
                style = SgTextStyle.Caption,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Row(
            Modifier.padding(top = SgSpacing.Sm).height(androidx.compose.foundation.layout.IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm),
        ) {
            StatTile(Icons.Rounded.Cloud, "${impact?.carbonKg ?: "…"} kg", stringResource(R.string.profile_impact_carbon_label), SgColor.Mint, SgColor.Brand700, Modifier.weight(1f))
            StatTile(Icons.Rounded.Recycling, "${impact?.compostKg ?: "…"} kg", stringResource(R.string.profile_impact_compost_label), SgColor.Farm, SgColor.FarmInk, Modifier.weight(1f))
            StatTile(Icons.Rounded.Savings, impact?.let { formatRupiah(it.savedRupiah) } ?: "…", l("uang dihemat", "money saved"), SgColor.Yellow50, Color(0xFF854D0E), Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatTile(icon: ImageVector, value: String, label: String, tint: Color, ink: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(SgRadius.Tile))
            .background(SgColor.BaseWhite)
            .border(1.dp, SgColor.Hairline, RoundedCornerShape(SgRadius.Tile))
            .padding(12.dp),
    ) {
        Box(Modifier.size(30.dp).background(tint, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(16.dp))
        }
        androidx.compose.material3.Text(
            value,
            style = SgTextStyle.Label.copy(fontSize = 15.sp),
            color = SgColor.Ink,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(label, style = SgTextStyle.Caption, minLines = 2, maxLines = 2)
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
