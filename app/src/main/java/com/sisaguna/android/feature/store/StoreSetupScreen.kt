package com.sisaguna.android.feature.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sisaguna.android.data.model.Allergen
import com.sisaguna.android.data.model.HalalStatus
import com.sisaguna.android.feature.address.MapPickerDialog
import com.sisaguna.android.ui.components.SgButton
import com.sisaguna.android.ui.components.SgButtonStyle
import com.sisaguna.android.ui.components.SgChip
import com.sisaguna.android.ui.components.SgTopBar
import com.sisaguna.android.ui.i18n.Text
import com.sisaguna.android.ui.i18n.l
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle

// Centre of Jakarta: where the map opens until the partner moves the pin (or uses "my location").
private const val DEFAULT_LAT = -6.2088
private const val DEFAULT_LNG = 106.8456

/**
 * "Daftarkan toko": the data the backend needs before a partner can sell. The store is created as
 * `pending`; an admin approves it, after which uploads and orders open up.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoreSetupScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: StoreSetupViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    var showMap by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = SgColor.Page,
        topBar = { SgTopBar(title = l("Daftarkan toko", "Open your store"), onBack = onBack) },
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            Column(Modifier.background(SgColor.Page).navigationBarsPadding().imePadding()) {
                SgButton(
                    if (form.saving) l("Mengirim…", "Sending…") else l("Kirim untuk disetujui", "Submit for approval"),
                    onClick = { viewModel.submit(onDone) },
                    enabled = !form.saving,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = SgSpacing.Gutter, vertical = SgSpacing.Md),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SgSpacing.Gutter),
            verticalArrangement = Arrangement.spacedBy(SgSpacing.Md),
        ) {
            Text(
                l(
                    "Tim SisaGuna memeriksa tokomu dulu sebelum bisa berjualan. Biasanya tidak lama.",
                    "The SisaGuna team reviews every store before it can sell. This is usually quick.",
                ),
                style = SgTextStyle.Body,
                color = SgColor.InkMuted,
            )

            Label(l("Nama toko", "Store name"))
            Field(form.name, { v -> viewModel.update { copy(name = v.take(60)) } }, l("Contoh: Warung Bu Sari", "e.g. Bu Sari's Warung"), form.visible(form.nameError))

            Label(l("Lokasi toko", "Store location"))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(SgRadius.Tile)).background(SgColor.BaseWhite).padding(SgSpacing.Md),
                horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(36.dp).background(SgColor.Mint, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = SgColor.Brand600)
                }
                Column(Modifier.weight(1f)) {
                    val place = form.place
                    Text(place?.shortLabel ?: l("Belum dipilih", "Not set"), style = SgTextStyle.Label)
                    if (place != null) Text(place.fullAddress, style = SgTextStyle.Caption, maxLines = 2)
                }
                SgButton(
                    if (form.place == null) l("Pilih", "Pick") else l("Ubah", "Change"),
                    onClick = { showMap = true },
                    style = SgButtonStyle.Ghost,
                    height = 36.dp,
                )
            }
            form.visible(form.placeError)?.let { Text(it, style = SgTextStyle.Caption, color = SgColor.RedStatus) }

            Label(l("Tentang toko (opsional)", "About (optional)"))
            Field(form.about, { v -> viewModel.update { copy(about = v.take(500)) } }, l("Ceritakan singkat tokomu", "A short description"))

            Label(l("Jam buka (opsional)", "Opening hours (optional)"))
            Field(form.openHours, { v -> viewModel.update { copy(openHours = v.take(60)) } }, "07.00-21.00")

            Label(l("Siap diambil dalam", "Ready to collect in"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                prepMinuteOptions.forEach { m ->
                    SgChip("$m " + l("menit", "min"), form.prepMinutes == m, { viewModel.update { copy(prepMinutes = m) } })
                }
            }

            Label(l("Status halal", "Halal status"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                HalalStatus.entries.forEach { h -> SgChip(h.label, form.halal == h, { viewModel.update { copy(halal = h) } }) }
            }

            Label(l("Alergen di dapurmu", "Allergens in your kitchen"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                Allergen.entries.forEach { a -> SgChip(a.emoji + " " + a.label, a in form.allergens, { viewModel.toggleAllergen(a) }) }
            }
            Box(Modifier.size(SgSpacing.Lg))
        }
    }

    if (showMap) {
        MapPickerDialog(
            initialLatitude = form.place?.latitude ?: DEFAULT_LAT,
            initialLongitude = form.place?.longitude ?: DEFAULT_LNG,
            onPicked = { place ->
                viewModel.update { copy(place = place) }
                showMap = false
            },
            onDismiss = { showMap = false },
        )
    }
}

@Composable
private fun Label(text: String) = Text(text, style = SgTextStyle.Label)

@Composable
private fun Field(value: String, onChange: (String) -> Unit, placeholder: String, error: String? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        isError = error != null,
        placeholder = { Text(placeholder, style = SgTextStyle.Body) },
        supportingText = error?.let { message -> { Text(message, style = SgTextStyle.Caption, color = SgColor.RedStatus) } },
        textStyle = SgTextStyle.TextSmRegular.copy(color = SgColor.Ink),
        shape = RoundedCornerShape(SgRadius.Thumb),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SgColor.Brand500, unfocusedBorderColor = SgColor.Hairline),
        modifier = Modifier.fillMaxWidth(),
    )
}
