package com.sisaguna.android.feature.upload

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sisaguna.android.data.model.Allergen
import com.sisaguna.android.data.model.HalalStatus
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.SafetyCheck
import com.sisaguna.android.data.model.SafetyLevel
import com.sisaguna.android.data.model.StorageMethod
import com.sisaguna.android.ui.components.MAX_VIDEO_SECONDS
import com.sisaguna.android.ui.components.QuantityStepper
import com.sisaguna.android.ui.components.SgButton
import com.sisaguna.android.ui.components.SgButtonStyle
import com.sisaguna.android.ui.components.SgChip
import com.sisaguna.android.ui.components.SgEaseOut
import com.sisaguna.android.ui.components.SgTopBar
import com.sisaguna.android.ui.components.pressable
import com.sisaguna.android.ui.components.rememberMediaCapture
import com.sisaguna.android.ui.domain.SafetyCard
import com.sisaguna.android.ui.domain.discountPercent
import com.sisaguna.android.ui.domain.formatClock
import com.sisaguna.android.ui.domain.formatRupiah
import com.sisaguna.android.ui.i18n.Text
import com.sisaguna.android.ui.i18n.l
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle
import java.time.temporal.ChronoUnit

/**
 * Upload, split by what's being posted (user feedback + interview):
 * - **Siap santap** is counted per portion and must pass the food-safety self-check — the
 *   form computes the verdict live from "dimasak berapa jam lalu" × storage, and blocks
 *   publishing (with a one-tap "jadikan pakan ternak") once it's past the safe window.
 * - **Pakan ternak / kompos** is weighed in grams or kilos.
 * Photos come from the camera directly (or gallery), plus an optional ≤30s video.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UploadScreen(
    onBack: () -> Unit,
    onPublished: (String) -> Unit,
    viewModel: UploadViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    var confirmLeave by remember { mutableStateOf(false) }
    val media = rememberMediaCapture(
        onPhotos = { uris -> viewModel.addPhotos(uris.map { it.toString() }) },
        onVideo = { uri -> viewModel.update { copy(videoUri = uri.toString()) } },
        maxPick = MAX_PHOTOS,
    )
    val leave = { if (viewModel.isDirty) confirmLeave = true else onBack() }
    BackHandler(enabled = viewModel.isDirty) { confirmLeave = true }
    val now = viewModel.now
    val assessment = form.assessment(now)

    Scaffold(
        containerColor = SgColor.Page,
        topBar = { SgTopBar(title = if (form.isFood) l("Upload siap santap", "Post ready-to-eat food") else l("Upload pakan & kompos", "Post feed & compost"), onBack = leave) },
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            Column(Modifier.background(SgColor.BaseWhite).navigationBarsPadding().imePadding()) {
                HorizontalDivider(color = SgColor.Hairline)
                SgButton(
                    l("Publikasikan", "Publish"),
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
            KindSwitch(isFood = form.isFood, onChange = { food ->
                viewModel.update { copy(tier = if (food) ListingTier.HUMAN else ListingTier.ANIMAL_FEED) }
            })

            MediaSection(
                photos = form.photos,
                videoUri = form.videoUri,
                tier = form.tier,
                onCamera = media.takePhoto,
                onGallery = media.pickPhotos,
                onVideo = media.recordVideo,
                onRemove = viewModel::removePhoto,
                onCover = viewModel::makeCover,
                onRemoveVideo = { viewModel.update { copy(videoUri = null) } },
            )

            AnimatedVisibility(
                visible = !form.isFood,
                enter = fadeIn(tween(180, easing = SgEaseOut)) + expandVertically(tween(220, easing = SgEaseOut)),
                exit = fadeOut(tween(90)) + shrinkVertically(tween(160)),
            ) {
                Section(l("Jenisnya", "Type")) {
                    Row(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                        SgChip(l("Pakan ternak", "Animal feed"), form.tier == ListingTier.ANIMAL_FEED, { viewModel.update { copy(tier = ListingTier.ANIMAL_FEED) } })
                        SgChip(l("Kompos", "Compost"), form.tier == ListingTier.COMPOST, { viewModel.update { copy(tier = ListingTier.COMPOST) } })
                    }
                    Text(
                        if (form.tier == ListingTier.ANIMAL_FEED) l("Aman untuk ternak: bebas plastik, tidak berjamur.", "Safe for livestock: no plastic, no mould.")
                        else l("Sisa organik untuk komposter, sudah dipilah dari plastik.", "Organic scraps for composting, sorted from plastic."),
                        style = SgTextStyle.Caption,
                    )
                }
            }

            Section(l("Detail", "Details")) {
                Field(
                    label = if (form.isFood) l("Nama makanan", "Food name") else l("Nama bahan", "Material"),
                    value = form.title,
                    onChange = { v -> viewModel.update { copy(title = v.take(60)) } },
                    placeholder = if (form.isFood) l("Contoh: Puding cheesecake sisa PO", "e.g. Leftover cheesecake pudding") else l("Contoh: Sayur layu pasar pagi", "e.g. Wilted market vegetables"),
                    error = form.visibleError(UploadField.TITLE),
                    onBlur = { viewModel.touch(UploadField.TITLE) },
                    capitalization = KeyboardCapitalization.Sentences,
                )
                Field(
                    label = l("Deskripsi", "Description"),
                    value = form.description,
                    onChange = { v -> viewModel.update { copy(description = v.take(300)) } },
                    placeholder = if (form.isFood) l("Kondisi, isi per porsi, cara simpan…", "Condition, portion contents, storage…") else l("Isi, kondisi, sudah dipilah atau belum…", "Contents, condition, sorted or not…"),
                    error = form.visibleError(UploadField.DESCRIPTION),
                    onBlur = { viewModel.touch(UploadField.DESCRIPTION) },
                    singleLine = false,
                    capitalization = KeyboardCapitalization.Sentences,
                    counter = "${form.description.length}/300",
                )
            }

            if (form.isFood) {
                Section(l("Cek kelayakan konsumsi", "Food safety check")) {
                    Text(l("Dimasak / dibuat berapa jam lalu?", "Cooked / made how long ago?"), style = SgTextStyle.Label)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm), verticalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                        madeHoursOptions.forEach { h ->
                            SgChip(if (h == 0) l("Baru saja", "Just now") else "$h " + l("jam", "h"), form.madeHoursAgo == h, { viewModel.update { copy(madeHoursAgo = h) } })
                        }
                    }
                    Text(l("Disimpan di mana?", "How is it stored?"), style = SgTextStyle.Label)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm), verticalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                        StorageMethod.entries.forEach { st ->
                            SgChip(l(st.label, st.labelEn), form.storage == st, { viewModel.update { copy(storage = st) } })
                        }
                    }
                    Text(l("Centang yang benar (wajib ditandai *)", "Tick what's true (* required)"), style = SgTextStyle.Label)
                    SafetyCheck.entries.forEach { check ->
                        CheckRow(check, checked = check in form.checks, onToggle = { viewModel.toggleCheck(check) })
                    }
                    SafetyCard(assessment = assessment, madeAt = now.minus(form.madeHoursAgo.toLong(), ChronoUnit.HOURS), storage = form.storage, now = now)
                    if (assessment.suggestTier != null) {
                        SgButton(
                            l("Jadikan pakan ternak", "List as animal feed instead"),
                            onClick = viewModel::switchToFeed,
                            style = SgButtonStyle.Secondary,
                            modifier = Modifier.fillMaxWidth(),
                            leading = { Icon(Icons.Rounded.Agriculture, contentDescription = null, tint = SgColor.FarmInk, modifier = Modifier.size(18.dp)) },
                        )
                    }
                    form.visibleError(UploadField.SAFETY)?.let { Text(it, style = SgTextStyle.Caption, color = SgColor.RedStatus) }
                }

                Section(l("Halal & alergen", "Halal & allergens")) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm), verticalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                        HalalStatus.entries.forEach { h -> SgChip(h.label, form.halal == h, { viewModel.update { copy(halal = h) } }) }
                    }
                    Text(l("Mengandung alergen (pilih semua yang ada)", "Contains allergens (select all)"), style = SgTextStyle.Label)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm), verticalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                        Allergen.entries.forEach { a ->
                            SgChip("${a.emoji} ${a.label}", a in form.allergens, {
                                viewModel.update { copy(allergens = if (a in allergens) allergens - a else allergens + a) }
                            })
                        }
                    }
                }
            }

            Section(if (form.isFood) l("Harga per porsi", "Price per portion") else l("Harga per kg", "Price per kg")) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(form.isFree, role = Role.Switch) { v -> viewModel.update { copy(isFree = v) } },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(l("Bagikan gratis", "Give away free"), style = SgTextStyle.Label)
                        Text(l("Gratis lebih cepat diambil", "Free goes faster"), style = SgTextStyle.Caption)
                    }
                    Switch(
                        checked = form.isFree,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(checkedTrackColor = SgColor.Brand500, uncheckedTrackColor = SgColor.Hairline, uncheckedBorderColor = SgColor.Hairline, uncheckedThumbColor = SgColor.OnBrand),
                    )
                }
                AnimatedVisibility(
                    visible = !form.isFree,
                    enter = fadeIn(tween(150)) + expandVertically(tween(180, easing = SgEaseOut)),
                    exit = fadeOut(tween(100)) + shrinkVertically(tween(150)),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(SgSpacing.Md)) {
                        Field(
                            label = l("Harga normal", "Regular price"),
                            value = form.originalPrice,
                            onChange = { v -> viewModel.update { copy(originalPrice = v.filter(Char::isDigit).take(7)) } },
                            placeholder = if (form.isFood) "25000" else "5000",
                            error = form.visibleError(UploadField.ORIGINAL_PRICE),
                            onBlur = { viewModel.touch(UploadField.ORIGINAL_PRICE) },
                            prefix = "Rp ",
                            suffix = if (form.isFood) null else "/kg",
                            keyboardType = KeyboardType.Number,
                        )
                        Field(
                            label = l("Harga jual", "Selling price"),
                            value = form.price,
                            onChange = { v -> viewModel.update { copy(price = v.filter(Char::isDigit).take(7)) } },
                            placeholder = form.suggestedPrice?.toString() ?: "10000",
                            error = form.visibleError(UploadField.PRICE),
                            onBlur = { viewModel.touch(UploadField.PRICE) },
                            prefix = "Rp ",
                            suffix = if (form.isFood) null else "/kg",
                            keyboardType = KeyboardType.Number,
                        )
                        form.suggestedPrice?.let { suggested ->
                            val pct = discountPercent(form.originalPriceValue, form.priceValue)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (pct != null) l("Diskon $pct% dari harga normal", "$pct% off the regular price") else l("Saran: ${formatRupiah(suggested)} (diskon 60%)", "Suggested: ${formatRupiah(suggested)} (60% off)"),
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

            Section(if (form.isFood) l("Jumlah & waktu ambil", "Quantity & pickup window") else l("Berat & waktu ambil", "Weight & pickup window")) {
                AnimatedContent(
                    targetState = form.isFood,
                    transitionSpec = { fadeIn(tween(180, easing = SgEaseOut)) togetherWith fadeOut(tween(90)) using SizeTransform(clip = false) },
                    label = "qtyKind",
                ) { food ->
                    if (food) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(l("Jumlah porsi", "Portions"), style = SgTextStyle.Label)
                                Text(l("Dihitung per porsi", "Counted per portion"), style = SgTextStyle.Caption)
                            }
                            QuantityStepper(quantity = form.stock, onChange = { v -> viewModel.update { copy(stock = v) } }, max = 50)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                                Box(Modifier.weight(1f)) {
                                    Field(
                                        label = l("Berat total", "Total weight"),
                                        value = form.weight,
                                        onChange = { v -> viewModel.update { copy(weight = v.filter { it.isDigit() || it == ',' || it == '.' }.take(7)) } },
                                        placeholder = if (form.weightUnit == WeightUnit.GRAM) "1500" else "5",
                                        error = form.visibleError(UploadField.STOCK),
                                        onBlur = { viewModel.touch(UploadField.STOCK) },
                                        keyboardType = KeyboardType.Decimal,
                                    )
                                }
                                UnitToggle(form.weightUnit, onChange = { u -> viewModel.update { copy(weightUnit = u) } }, modifier = Modifier.padding(top = 28.dp))
                            }
                            form.weightKg?.takeIf { it > 0 }?.let { kg ->
                                Text(
                                    l("≈ ${"%.1f".format(kg)} kg · dijual per kg (${form.stockUnits} slot)", "≈ ${"%.1f".format(kg)} kg · sold per kg (${form.stockUnits} slots)"),
                                    style = SgTextStyle.Caption,
                                    color = SgColor.Brand700,
                                )
                            }
                        }
                    }
                }
                Text(l("Bisa diambil sampai", "Available until"), style = SgTextStyle.Label)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm), verticalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                    pickupWindowOptions.forEach { h ->
                        val until = formatClock(now.plus(h, ChronoUnit.HOURS))
                        SgChip("$h " + l("jam", "h") + " · $until", form.pickupHours == h, { viewModel.update { copy(pickupHours = h) } })
                    }
                }
                if (form.isFood && assessment.safeUntil != null && assessment.level != SafetyLevel.UNSAFE &&
                    now.plus(form.pickupHours, ChronoUnit.HOURS).isAfter(assessment.safeUntil)
                ) {
                    Text(
                        l("Listing otomatis ditutup ${formatClock(assessment.safeUntil)}, batas aman konsumsi.", "Listing closes automatically at ${formatClock(assessment.safeUntil)}, the safe-to-eat limit."),
                        style = SgTextStyle.Caption,
                        color = SgColor.YellowStatus,
                    )
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

/** Siap santap | Pakan & kompos — two big options so the seller picks the right form first. */
@Composable
private fun KindSwitch(isFood: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md)) {
        KindCard(Icons.Rounded.RestaurantMenu, l("Siap santap", "Ready to eat"), l("per porsi", "per portion"), isFood, SgColor.Mint, SgColor.Brand700, Modifier.weight(1f)) { onChange(true) }
        KindCard(Icons.Rounded.Agriculture, l("Pakan & kompos", "Feed & compost"), l("per gram / kg", "per gram / kg"), !isFood, SgColor.Farm, SgColor.FarmInk, Modifier.weight(1f)) { onChange(false) }
    }
}

@Composable
private fun KindCard(icon: ImageVector, title: String, sub: String, selected: Boolean, tint: Color, fg: Color, modifier: Modifier, onClick: () -> Unit) {
    val border by animateColorAsState(if (selected) fg else SgColor.Hairline, tween(160, easing = SgEaseOut), label = "kindBorder")
    val bg by animateColorAsState(if (selected) tint else SgColor.BaseWhite, tween(160, easing = SgEaseOut), label = "kindBg")
    Row(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(if (selected) 1.5.dp else 1.dp, border, RoundedCornerShape(18.dp))
            .pressable(onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) fg else SgColor.InkMuted)
        Column(Modifier.padding(start = 10.dp)) {
            Text(title, style = SgTextStyle.Label)
            Text(sub, style = SgTextStyle.Caption)
        }
    }
}

@Composable
private fun UnitToggle(unit: WeightUnit, onChange: (WeightUnit) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.clip(RoundedCornerShape(SgRadius.Thumb)).background(SgColor.Page).padding(3.dp)) {
        WeightUnit.entries.forEach { u ->
            val selected = u == unit
            Text(
                u.label,
                style = SgTextStyle.Label.copy(fontSize = 13.sp),
                color = if (selected) SgColor.OnBrand else SgColor.InkMuted,
                modifier = Modifier
                    .clip(RoundedCornerShape(SgRadius.Thumb))
                    .background(if (selected) SgColor.Brand500 else Color.Transparent)
                    .pressable({ onChange(u) }, pressedScale = 0.95f)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
            )
        }
    }
}

@Composable
private fun CheckRow(check: SafetyCheck, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (checked) SgColor.Mint else SgColor.Page)
            .toggleable(checked, role = Role.Checkbox) { onToggle() }
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            if (checked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (checked) SgColor.Brand500 else SgColor.Neutral400,
            modifier = Modifier.size(22.dp),
        )
        Column(Modifier.padding(start = 10.dp)) {
            Text(check.label + if (check.requiredForHumans) " *" else "", style = SgTextStyle.Label.copy(fontSize = 13.sp))
            Text(check.detail, style = SgTextStyle.Caption)
        }
    }
}

/**
 * Cover photo + strip of extra photos, with three capture buttons: camera (opens the phone's
 * camera straight away), gallery, and a ≤30s video. Tapping a thumbnail makes it the cover.
 */
@Composable
private fun MediaSection(
    photos: List<String>,
    videoUri: String?,
    tier: ListingTier,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onVideo: () -> Unit,
    onRemove: (String) -> Unit,
    onCover: (String) -> Unit,
    onRemoveVideo: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(SgRadius.Card))
                .background(SgColor.BaseWhite)
                .border(1.5.dp, if (photos.isEmpty()) SgColor.Brand300 else SgColor.Hairline, RoundedCornerShape(SgRadius.Card)),
            contentAlignment = Alignment.Center,
        ) {
            if (photos.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(l("Foto langsung dari kamera", "Shoot it right now"), style = SgTextStyle.Title)
                    Text(l("Foto yang jelas bikin lebih cepat diambil", "Clear photos get picked up faster"), style = SgTextStyle.Caption)
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CaptureButton(Icons.Rounded.PhotoCamera, l("Kamera", "Camera"), primary = true, onClick = onCamera)
                        CaptureButton(Icons.Rounded.PhotoLibrary, l("Galeri", "Gallery"), onClick = onGallery)
                    }
                }
            } else {
                AsyncImage(photos.first(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Text(
                    l("Sampul", "Cover"),
                    style = SgTextStyle.TextXsMedium,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(SgRadius.Pill)).padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }
        if (photos.isNotEmpty() || videoUri != null) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(photos, key = { it }) { p ->
                    Box(Modifier.size(72.dp)) {
                        AsyncImage(
                            p,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp))
                                .border(if (p == photos.first()) 2.dp else 0.dp, SgColor.Brand500, RoundedCornerShape(14.dp))
                                .pressable({ onCover(p) }),
                        )
                        RemoveDot(Modifier.align(Alignment.TopEnd)) { onRemove(p) }
                    }
                }
                if (videoUri != null) {
                    item {
                        Box(Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)).background(SgColor.Scrim), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.PlayCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                            Text("≤${MAX_VIDEO_SECONDS}s", style = SgTextStyle.Caption.copy(fontSize = 10.sp), color = Color.White, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp))
                            RemoveDot(Modifier.align(Alignment.TopEnd), onRemoveVideo)
                        }
                    }
                }
                if (photos.size < MAX_PHOTOS) {
                    item { SmallAdd(Icons.Rounded.PhotoCamera, l("Kamera", "Camera"), onCamera) }
                    item { SmallAdd(Icons.Rounded.PhotoLibrary, l("Galeri", "Gallery"), onGallery) }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SgColor.BaseWhite)
                .border(1.dp, SgColor.Hairline, RoundedCornerShape(14.dp))
                .pressable(onVideo)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Videocam, contentDescription = null, tint = SgColor.PromoInk)
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                Text(if (videoUri == null) l("Rekam video (opsional)", "Record a video (optional)") else l("Rekam ulang video", "Re-record video"), style = SgTextStyle.Label)
                Text(l("Maks. $MAX_VIDEO_SECONDS detik, tunjukkan kondisi makanan dari dekat", "Max $MAX_VIDEO_SECONDS seconds, show the food up close"), style = SgTextStyle.Caption)
            }
            if (videoUri != null) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = SgColor.Brand500)
        }
        Text("${photos.size}/$MAX_PHOTOS " + l("foto · ketuk foto untuk jadikan sampul", "photos · tap one to make it the cover"), style = SgTextStyle.Caption)
    }
}

@Composable
private fun CaptureButton(icon: ImageVector, label: String, onClick: () -> Unit, primary: Boolean = false) {
    Row(
        Modifier
            .clip(RoundedCornerShape(SgRadius.Pill))
            .background(if (primary) SgColor.Brand500 else SgColor.Page)
            .pressable(onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (primary) SgColor.OnBrand else SgColor.Ink, modifier = Modifier.size(18.dp))
        Text(label, style = SgTextStyle.Label, color = if (primary) SgColor.OnBrand else SgColor.Ink, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun SmallAdd(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)).border(1.5.dp, SgColor.Neutral300, RoundedCornerShape(14.dp)).pressable(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = SgColor.Brand600, modifier = Modifier.size(20.dp))
        Text(label, style = SgTextStyle.Caption.copy(fontSize = 11.sp))
    }
}

@Composable
private fun RemoveDot(modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.padding(4.dp).size(22.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).pressable(onClick, pressedScale = 0.9f),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Rounded.Close, contentDescription = l("Hapus", "Remove"), tint = Color.White, modifier = Modifier.size(14.dp)) }
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
    suffix: String? = null,
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
            placeholder = { Text(placeholder, style = SgTextStyle.Body.copy(color = SgColor.Neutral400)) },
            prefix = prefix?.let { { Text(it, style = SgTextStyle.TextSmRegular, color = SgColor.InkMuted) } },
            suffix = suffix?.let { { Text(it, style = SgTextStyle.TextSmRegular, color = SgColor.InkMuted) } },
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
