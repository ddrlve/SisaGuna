package com.sisaguna.android.feature.upload

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.ui.components.QuantityStepper
import com.sisaguna.android.ui.components.SgButton
import com.sisaguna.android.ui.components.SgChip
import com.sisaguna.android.ui.components.SgTopBar
import com.sisaguna.android.ui.components.pressable
import com.sisaguna.android.ui.domain.ListingImage
import com.sisaguna.android.ui.domain.discountPercent
import com.sisaguna.android.ui.domain.formatClock
import com.sisaguna.android.ui.domain.formatRupiah
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle
import java.time.Instant
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UploadScreen(
    onBack: () -> Unit,
    onPublished: (String) -> Unit,
    viewModel: UploadViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    var confirmLeave by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.update { copy(photoUri = uri.toString()) }
    }
    val leave = { if (viewModel.isDirty) confirmLeave = true else onBack() }
    BackHandler(enabled = form.let { viewModel.isDirty }) { confirmLeave = true }

    Scaffold(
        containerColor = SgColor.Page,
        topBar = { SgTopBar(title = "Upload makanan", onBack = leave) },
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            Column(Modifier.background(SgColor.BaseWhite).navigationBarsPadding().imePadding()) {
                HorizontalDivider(color = SgColor.Hairline)
                SgButton(
                    "Publikasikan",
                    onClick = { viewModel.publish()?.let(onPublished) },
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
                .padding(horizontal = SgSpacing.Gutter, vertical = SgSpacing.Md),
            verticalArrangement = Arrangement.spacedBy(SgSpacing.Lg),
        ) {
            PhotoPicker(form.photoUri, form.tier) { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

            Section("Untuk siapa makanan ini?") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                    listOf(ListingTier.HUMAN to "Siap Santap", ListingTier.ANIMAL_FEED to "Pakan Ternak", ListingTier.COMPOST to "Kompos").forEach { (tier, label) ->
                        SgChip(label, form.tier == tier, { viewModel.update { copy(tier = tier) } })
                    }
                }
                Text(
                    when (form.tier) {
                        ListingTier.HUMAN -> "Masih layak dan aman dimakan manusia."
                        ListingTier.ANIMAL_FEED -> "Tidak untuk manusia, tapi aman untuk ternak."
                        ListingTier.COMPOST -> "Sisa organik untuk dijadikan kompos."
                    },
                    style = SgTextStyle.Caption,
                )
            }

            Section("Detail") {
                Field(
                    label = "Nama makanan",
                    value = form.title,
                    onChange = { v -> viewModel.update { copy(title = v.take(60)) } },
                    placeholder = "Contoh: Nasi kotak sisa acara",
                    error = form.visibleError(UploadField.TITLE),
                    onBlur = { viewModel.touch(UploadField.TITLE) },
                    capitalization = KeyboardCapitalization.Sentences,
                )
                Field(
                    label = "Deskripsi",
                    value = form.description,
                    onChange = { v -> viewModel.update { copy(description = v.take(300)) } },
                    placeholder = "Kondisi, kapan dibuat, isi per porsi, alergen…",
                    error = form.visibleError(UploadField.DESCRIPTION),
                    onBlur = { viewModel.touch(UploadField.DESCRIPTION) },
                    singleLine = false,
                    capitalization = KeyboardCapitalization.Sentences,
                    counter = "${form.description.length}/300",
                )
            }

            Section("Harga") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(form.isFree, role = Role.Switch) { v -> viewModel.update { copy(isFree = v) } },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Bagikan gratis", style = SgTextStyle.Label)
                        Text("Gratis lebih cepat diambil", style = SgTextStyle.Caption)
                    }
                    Switch(
                        checked = form.isFree,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(checkedTrackColor = SgColor.Brand500, uncheckedTrackColor = SgColor.Hairline, uncheckedBorderColor = SgColor.Hairline, uncheckedThumbColor = SgColor.BaseWhite),
                    )
                }
                AnimatedVisibility(
                    visible = !form.isFree,
                    enter = fadeIn(tween(150)) + expandVertically(tween(180)),
                    exit = fadeOut(tween(100)) + shrinkVertically(tween(150)),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(SgSpacing.Md)) {
                        Field(
                            label = "Harga normal",
                            value = form.originalPrice,
                            onChange = { v -> viewModel.update { copy(originalPrice = v.filter(Char::isDigit).take(7)) } },
                            placeholder = "40000",
                            error = form.visibleError(UploadField.ORIGINAL_PRICE),
                            onBlur = { viewModel.touch(UploadField.ORIGINAL_PRICE) },
                            prefix = "Rp ",
                            keyboardType = KeyboardType.Number,
                        )
                        Field(
                            label = "Harga jual",
                            value = form.price,
                            onChange = { v -> viewModel.update { copy(price = v.filter(Char::isDigit).take(7)) } },
                            placeholder = form.suggestedPrice?.toString() ?: "15000",
                            error = form.visibleError(UploadField.PRICE),
                            onBlur = { viewModel.touch(UploadField.PRICE) },
                            prefix = "Rp ",
                            keyboardType = KeyboardType.Number,
                        )
                        form.suggestedPrice?.let { suggested ->
                            val pct = discountPercent(form.originalPriceValue, form.priceValue)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (pct != null) "Diskon $pct% dari harga normal" else "Saran: ${formatRupiah(suggested)} (diskon 60%)",
                                    style = SgTextStyle.Caption,
                                    color = SgColor.Brand700,
                                    modifier = Modifier.weight(1f),
                                )
                                if (form.priceValue == null) {
                                    TextButton(onClick = { viewModel.update { copy(price = suggested.toString()) } }) {
                                        Text("Pakai saran", style = SgTextStyle.TextXsMedium, color = SgColor.Brand600)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Section("Stok & waktu ambil") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Jumlah porsi", style = SgTextStyle.Label, modifier = Modifier.weight(1f))
                    QuantityStepper(quantity = form.stock, onChange = { v -> viewModel.update { copy(stock = v) } }, max = 50)
                }
                Text("Bisa diambil sampai", style = SgTextStyle.Label)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm), verticalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                    pickupWindowOptions.forEach { h ->
                        val until = formatClock(Instant.now().plus(h, ChronoUnit.HOURS))
                        SgChip("$h jam · $until", form.pickupHours == h, { viewModel.update { copy(pickupHours = h) } })
                    }
                }
            }
        }
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Buang draf?", style = SgTextStyle.Title) },
            text = { Text("Isian yang belum dipublikasikan akan hilang.", style = SgTextStyle.Body) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLeave = false
                    onBack()
                }) { Text("Buang", style = SgTextStyle.Label, color = SgColor.RedStatus) }
            },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Lanjut isi", style = SgTextStyle.Label, color = SgColor.Ink) } },
            containerColor = SgColor.BaseWhite,
        )
    }
}

@Composable
private fun PhotoPicker(uri: String, tier: ListingTier, onPick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp)
            .clip(RoundedCornerShape(SgRadius.Card))
            .background(SgColor.BaseWhite)
            .border(1.5.dp, if (uri.isBlank()) SgColor.Brand300 else SgColor.Hairline, RoundedCornerShape(SgRadius.Card))
            .pressable(onPick, pressedScale = 0.98f),
        contentAlignment = Alignment.Center,
    ) {
        if (uri.isBlank()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                Box(Modifier.size(56.dp).background(SgColor.Mint, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.AddAPhoto, contentDescription = null, tint = SgColor.Brand600)
                }
                Text("Tambah foto makanan", style = SgTextStyle.Label)
                Text("Foto yang jelas bikin lebih cepat diambil", style = SgTextStyle.Caption)
            }
        } else {
            ListingImage(uri, tier, "Foto makanan", Modifier.fillMaxSize())
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(SgSpacing.Md)
                    .background(SgColor.BaseWhite.copy(alpha = 0.9f), RoundedCornerShape(SgRadius.Pill))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = null, tint = SgColor.Ink, modifier = Modifier.size(16.dp))
                Text("Ganti foto", style = SgTextStyle.TextXsMedium, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Card))
            .background(SgColor.BaseWhite)
            .border(1.dp, SgColor.Hairline, RoundedCornerShape(SgRadius.Card))
            .padding(SgSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(SgSpacing.Md),
    ) {
        Text(title, style = SgTextStyle.Title)
        content()
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    error: String?,
    onBlur: () -> Unit,
    singleLine: Boolean = true,
    prefix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    counter: String? = null,
) {
    var hadFocus by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = SgTextStyle.Label)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            isError = error != null,
            placeholder = { Text(placeholder, style = SgTextStyle.Body) },
            prefix = prefix?.let { { Text(it, style = SgTextStyle.TextSmRegular, color = SgColor.InkMuted) } },
            textStyle = SgTextStyle.TextSmRegular.copy(color = SgColor.Ink),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization),
            shape = RoundedCornerShape(SgRadius.Thumb),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SgColor.Brand500,
                unfocusedBorderColor = SgColor.Hairline,
                errorBorderColor = SgColor.RedStatus,
                cursorColor = SgColor.Brand500,
            ),
            supportingText = when {
                error != null -> { { Text(error, style = SgTextStyle.Caption, color = SgColor.RedStatus) } }
                counter != null -> { { Text(counter, style = SgTextStyle.Caption) } }
                else -> null
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (it.isFocused) hadFocus = true else if (hadFocus) onBlur() },
        )
    }
}
