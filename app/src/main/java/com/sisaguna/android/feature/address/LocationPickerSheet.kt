package com.sisaguna.android.feature.address

import com.sisaguna.android.ui.i18n.l

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import com.sisaguna.android.ui.i18n.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sisaguna.android.data.model.Address
import com.sisaguna.android.ui.components.SgButton
import com.sisaguna.android.ui.components.SgButtonStyle
import com.sisaguna.android.ui.components.SgSearchField
import com.sisaguna.android.ui.components.pressable
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Fallback map centre (Tangerang) when there's no address or GPS fix yet. */
private const val DEFAULT_LAT = -6.2275
private const val DEFAULT_LNG = 106.6544

/**
 * Home's location sheet (Figma 43:6960, rebuilt after device feedback). Dimmed backdrop like
 * the app's other sheets, real address search, a GPS
 * action that reports progress and errors inline, an in-app map picker, and saved addresses
 * with a clear selected state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerSheet(
    state: AddressUiState,
    onSelectAddress: (String) -> Unit,
    onUseCurrentLocation: (String) -> Unit,
    onSaveAddress: (existingId: String?, label: String, place: Place, note: String) -> Unit,
    onDeleteAddress: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var gpsBusy by remember { mutableStateOf(false) }
    var gpsError by remember { mutableStateOf<String?>(null) }
    var mapFor by remember { mutableStateOf<Address?>(null) }
    var showMap by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Pair<Address?, Place>?>(null) }

    LaunchedEffect(query) {
        if (query.length < 3) {
            results = emptyList()
            return@LaunchedEffect
        }
        searching = true
        delay(500)
        results = searchPlaces(context, query)
        searching = false
    }

    fun fetchGps() {
        gpsBusy = true
        gpsError = null
        scope.launch {
            runCatching {
                val loc = fetchCurrentLocation(context)
                reverseGeocode(context, loc.latitude, loc.longitude)
            }.onSuccess { onUseCurrentLocation(it.shortLabel) }
                .onFailure { gpsError = it.message ?: "Gagal mengambil lokasi." }
            gpsBusy = false
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) fetchGps() else gpsError = "Izin lokasi ditolak. Kamu tetap bisa pilih lewat peta."
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SgColor.BaseWhite,
        // Same dim scrim as the other sheets (avatar, voucher). The old blurred backdrop was
        // costly to render and read as smudged on device.
        scrimColor = SgColor.Ink.copy(alpha = 0.32f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = SgSpacing.Gutter)
                .padding(bottom = SgSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(SgSpacing.Md),
        ) {
            Column {
                Text("Pilih lokasi", style = SgTextStyle.Title)
                Text("Makanan di sekitar lokasi ini yang akan ditampilkan.", style = SgTextStyle.Body)
            }
            SgSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Cari jalan, kelurahan, atau gedung",
                containerColor = SgColor.Page,
                modifier = Modifier.fillMaxWidth(),
            )
            AnimatedVisibility(
                visible = query.length >= 3,
                enter = fadeIn(tween(150)) + expandVertically(tween(180)),
                exit = fadeOut(tween(100)) + shrinkVertically(tween(150)),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(SgSpacing.Xs)) {
                    when {
                        searching -> Text("Mencari…", style = SgTextStyle.Caption)
                        results.isEmpty() -> Text("Alamat tidak ditemukan. Coba kata lain atau pilih lewat peta.", style = SgTextStyle.Caption)
                    }
                    results.forEach { place ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(SgRadius.Thumb))
                                .pressable({ onUseCurrentLocation(place.shortLabel) })
                                .padding(vertical = SgSpacing.Sm, horizontal = SgSpacing.Xs),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
                        ) {
                            Icon(Icons.Rounded.Place, contentDescription = null, tint = SgColor.InkMuted)
                            Column(Modifier.weight(1f)) {
                                Text(place.shortLabel, style = SgTextStyle.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(place.fullAddress, style = SgTextStyle.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md)) {
                ActionTile(
                    icon = Icons.Rounded.MyLocation,
                    title = "Lokasi saat ini",
                    subtitle = when {
                        gpsBusy -> "Mencari sinyal…"
                        state.currentLocationLabel != null -> state.currentLocationLabel
                        else -> "Pakai GPS"
                    },
                    busy = gpsBusy,
                    selected = state.currentLocationLabel != null,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (gpsBusy) return@ActionTile
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (granted) fetchGps() else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    },
                )
                ActionTile(
                    icon = Icons.Rounded.Map,
                    title = "Pilih di peta",
                    subtitle = "Geser titik sendiri",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        mapFor = null
                        showMap = true
                    },
                )
            }
            gpsError?.let { Text(it, style = SgTextStyle.Caption, color = SgColor.RedStatus) }

            Text("Alamat tersimpan", style = SgTextStyle.Label, modifier = Modifier.padding(top = SgSpacing.Sm))
            state.addresses.forEach { address ->
                SavedAddressRow(
                    address = address,
                    selected = state.currentLocationLabel == null && address.id == state.selectedId,
                    onSelect = { onSelectAddress(address.id) },
                    onEdit = { editing = address to Place(address.label, address.fullAddress, address.latitude, address.longitude) },
                )
            }
            SgButton(
                "Tambah alamat baru",
                onClick = {
                    mapFor = null
                    showMap = true
                },
                style = SgButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
                leading = { Icon(Icons.Rounded.Add, contentDescription = null, tint = SgColor.Brand600, modifier = Modifier.size(20.dp)) },
            )
        }
    }

    if (showMap) {
        val start = mapFor ?: state.selected
        MapPickerDialog(
            initialLatitude = start?.latitude ?: DEFAULT_LAT,
            initialLongitude = start?.longitude ?: DEFAULT_LNG,
            onPicked = { place ->
                showMap = false
                editing = mapFor to place
            },
            onDismiss = { showMap = false },
        )
    }

    editing?.let { (existing, place) ->
        AddressEditorSheet(
            place = place,
            initialLabel = existing?.label ?: "",
            initialNote = existing?.note ?: "",
            onChangeLocation = {
                mapFor = existing ?: Address("tmp", "", place.fullAddress, place.latitude, place.longitude)
                editing = null
                showMap = true
            },
            onSave = { label, note ->
                onSaveAddress(existing?.id, label, place, note)
                editing = null
            },
            onDismiss = { editing = null },
            onDelete = existing?.let { e -> { onDeleteAddress(e.id); editing = null } },
        )
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
    selected: Boolean = false,
) {
    val border by animateColorAsState(if (selected) SgColor.Brand500 else SgColor.Hairline, tween(150), label = "tileBorder")
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(SgRadius.Tile))
            .background(if (selected) SgColor.Mint else SgColor.Page)
            .border(1.dp, border, RoundedCornerShape(SgRadius.Tile))
            .pressable(onClick)
            .padding(SgSpacing.Md),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(36.dp).background(SgColor.BaseWhite, CircleShape), contentAlignment = Alignment.Center) {
            if (busy) CircularProgressIndicator(strokeWidth = 2.dp, color = SgColor.Brand500, modifier = Modifier.size(18.dp))
            else Icon(icon, contentDescription = null, tint = SgColor.Brand600, modifier = Modifier.size(20.dp))
        }
        Text(title, style = SgTextStyle.Label)
        Text(subtitle, style = SgTextStyle.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SavedAddressRow(address: Address, selected: Boolean, onSelect: () -> Unit, onEdit: () -> Unit) {
    val border by animateColorAsState(if (selected) SgColor.Brand500 else SgColor.Hairline, tween(150), label = "addrBorder")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Tile))
            .background(if (selected) SgColor.Mint else SgColor.BaseWhite)
            .border(if (selected) 1.5.dp else 1.dp, border, RoundedCornerShape(SgRadius.Tile))
            .pressable(onSelect)
            .padding(start = SgSpacing.Md, top = SgSpacing.Md, bottom = SgSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
    ) {
        Icon(
            if (selected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = if (selected) "Dipilih" else null,
            tint = if (selected) SgColor.Brand500 else SgColor.InkMuted,
        )
        Column(Modifier.weight(1f)) {
            Text(address.label, style = SgTextStyle.Label)
            Text(address.fullAddress, style = SgTextStyle.Caption, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (address.note.isNotBlank()) Text(address.note, style = SgTextStyle.Caption, color = SgColor.Brand700, maxLines = 1)
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Rounded.Edit, contentDescription = l("Ubah ", "Edit ") + address.label, tint = SgColor.InkMuted, modifier = Modifier.size(20.dp))
        }
    }
}
