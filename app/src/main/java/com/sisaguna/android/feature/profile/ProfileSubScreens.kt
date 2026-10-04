package com.sisaguna.android.feature.profile

import com.sisaguna.android.ui.i18n.l

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sisaguna.android.data.model.Address
import com.sisaguna.android.data.model.PaymentKind
import com.sisaguna.android.feature.address.AddressEditorSheet
import com.sisaguna.android.feature.address.AddressViewModel
import com.sisaguna.android.feature.address.MapPickerDialog
import com.sisaguna.android.feature.address.Place
import com.sisaguna.android.feature.checkout.paymentIcon
import com.sisaguna.android.feature.checkout.paymentLabel
import com.sisaguna.android.ui.components.SgButton
import com.sisaguna.android.ui.components.SgButtonStyle
import com.sisaguna.android.ui.components.SgEmptyState
import com.sisaguna.android.ui.components.SgTopBar
import com.sisaguna.android.ui.components.pressable
import com.sisaguna.android.ui.domain.ListingImage
import com.sisaguna.android.ui.domain.formatClock
import com.sisaguna.android.ui.domain.formatDate
import com.sisaguna.android.ui.domain.formatPrice
import com.sisaguna.android.ui.domain.formatRupiah
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle
import kotlinx.coroutines.launch

// ---------------------------------------------------------------- shared bits

@Composable
private fun SubPage(
    title: String,
    onBack: () -> Unit,
    snackbar: SnackbarHostState? = null,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = SgColor.Page,
        topBar = { SgTopBar(title = title, onBack = onBack) },
        snackbarHost = { if (snackbar != null) SgSnackbarHost(snackbar) },
        bottomBar = bottomBar,
        contentWindowInsets = WindowInsets(0),
        content = content,
    )
}

@Composable
private fun CardBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Tile))
            .background(SgColor.BaseWhite)
            .border(1.dp, SgColor.Hairline, RoundedCornerShape(SgRadius.Tile)),
    ) { content() }
}

// ---------------------------------------------------------------- Alamat

private const val DEFAULT_LAT = -6.2275
private const val DEFAULT_LNG = 106.6544

@Composable
fun AddressesScreen(onBack: () -> Unit, viewModel: AddressViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var mapStart by remember { mutableStateOf<Address?>(null) }
    var showMap by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Pair<Address?, Place>?>(null) }
    var confirmDelete by remember { mutableStateOf<Address?>(null) }

    SubPage(
        title = "Alamat",
        onBack = onBack,
        bottomBar = {
            SgButton(
                "Tambah alamat",
                onClick = {
                    mapStart = null
                    showMap = true
                },
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = SgSpacing.Gutter, vertical = SgSpacing.Md),
                leading = { Icon(Icons.Rounded.Add, contentDescription = null, tint = SgColor.OnBrand, modifier = Modifier.size(20.dp)) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(SgSpacing.Gutter),
            verticalArrangement = Arrangement.spacedBy(SgSpacing.Md),
        ) {
            if (state.addresses.isEmpty()) {
                item { SgEmptyState(Icons.Rounded.Place, "Belum ada alamat", "Simpan alamat biar nggak perlu pilih lokasi tiap buka aplikasi.") }
            }
            items(state.addresses, key = { it.id }) { address ->
                val primary = address.id == state.selectedId
                CardBox(Modifier.animateItem()) {
                    Row(
                        modifier = Modifier.padding(start = SgSpacing.Lg, top = SgSpacing.Md, bottom = SgSpacing.Md, end = SgSpacing.Xs),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                                Text(address.label, style = SgTextStyle.Label)
                                if (primary) Text(
                                    "Utama",
                                    style = SgTextStyle.TextXsMedium,
                                    color = SgColor.Brand700,
                                    modifier = Modifier.background(SgColor.Mint, RoundedCornerShape(SgRadius.Pill)).padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                            Text(address.fullAddress, style = SgTextStyle.Body)
                            if (address.note.isNotBlank()) Text(address.note, style = SgTextStyle.Caption, color = SgColor.Brand700)
                            if (!primary) {
                                Text(
                                    "Jadikan utama",
                                    style = SgTextStyle.TextXsMedium,
                                    color = SgColor.Brand600,
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .clip(RoundedCornerShape(SgRadius.Pill))
                                        .pressable({ viewModel.select(address.id) })
                                        .padding(vertical = 6.dp),
                                )
                            }
                        }
                        IconButton(onClick = { editing = address to Place(address.label, address.fullAddress, address.latitude, address.longitude) }) {
                            Icon(Icons.Rounded.Edit, contentDescription = l("Ubah ", "Edit ") + address.label, tint = SgColor.InkMuted, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { confirmDelete = address }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = l("Hapus ", "Delete ") + address.label, tint = SgColor.RedStatus, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }

    if (showMap) {
        val start = mapStart ?: state.selected
        MapPickerDialog(
            initialLatitude = start?.latitude ?: DEFAULT_LAT,
            initialLongitude = start?.longitude ?: DEFAULT_LNG,
            onPicked = {
                showMap = false
                editing = mapStart to it
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
                mapStart = existing ?: Address("tmp", "", place.fullAddress, place.latitude, place.longitude)
                editing = null
                showMap = true
            },
            onSave = { label, note ->
                viewModel.save(existing?.id, label, place, note)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
    confirmDelete?.let { a ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(l("Hapus ${a.label}?", "Delete ${a.label}?"), style = SgTextStyle.Title) },
            text = { Text(a.fullAddress, style = SgTextStyle.Body) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(a.id)
                    confirmDelete = null
                }) { Text("Hapus", style = SgTextStyle.Label, color = SgColor.RedStatus) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Batal", style = SgTextStyle.Label, color = SgColor.Ink) } },
            containerColor = SgColor.BaseWhite,
        )
    }
}

// ---------------------------------------------------------------- Riwayat Penyelamatan

@Composable
fun RescueHistoryScreen(
    onBack: () -> Unit,
    onOrderClick: (String) -> Unit,
    viewModel: RescueHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SubPage(title = "Riwayat Penyelamatan", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(SgSpacing.Gutter),
            verticalArrangement = Arrangement.spacedBy(SgSpacing.Md),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(SgRadius.Card))
                        .background(Brush.linearGradient(listOf(SgColor.Brand500, SgColor.Brand700)))
                        .padding(SgSpacing.Lg),
                ) {
                    listOf(
                        state.portions.toString() to "porsi",
                        formatRupiah(state.saved) to "dihemat",
                        "${state.carbonKg} kg" to "CO₂ dicegah",
                    ).forEach { (value, label) ->
                        Column(Modifier.weight(1f)) {
                            Text(value, style = SgTextStyle.Title, color = Color.White, maxLines = 1)
                            Text(label, style = SgTextStyle.Caption, color = Color.White.copy(alpha = 0.85f))
                        }
                    }
                }
            }
            if (state.orders.isEmpty()) {
                item { SgEmptyState(Icons.Rounded.History, "Belum ada penyelamatan", "Setiap pesanan yang kamu ambil tercatat di sini.") }
            }
            items(state.orders, key = { it.id }) { order ->
                CardBox(Modifier.pressable({ onOrderClick(order.id) })) {
                    Row(
                        modifier = Modifier.padding(SgSpacing.Md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
                    ) {
                        val first = order.lines.first()
                        ListingImage(first.imageUrl, first.tier, null, Modifier.size(52.dp).clip(RoundedCornerShape(SgRadius.Thumb)))
                        Column(Modifier.weight(1f)) {
                            Text(order.merchant.name, style = SgTextStyle.Label)
                            Text(l("${order.itemCount} porsi · hemat ", "${order.itemCount} portions · saved ") + formatRupiah(order.savings), style = SgTextStyle.Body)
                            Text(order.completedAt?.let { formatDate(it) } ?: formatDate(order.createdAt), style = SgTextStyle.Caption)
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = SgColor.InkMuted)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Metode Pembayaran

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodsScreen(onBack: () -> Unit, viewModel: PaymentMethodsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var linking by remember { mutableStateOf<PaymentKind?>(null) }
    var pickKind by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    SubPage(title = "Metode Pembayaran", onBack = onBack, snackbar = snackbar) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(SgSpacing.Gutter),
            verticalArrangement = Arrangement.spacedBy(SgSpacing.Md),
        ) {
            item { Text("Pilih metode utama untuk checkout.", style = SgTextStyle.Body) }
            items(state.methods, key = { it.id }) { m ->
                val selected = state.defaultKind == m.kind
                Row(
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(SgRadius.Tile))
                        .background(if (selected) SgColor.Mint else SgColor.BaseWhite)
                        .border(if (selected) 1.5.dp else 1.dp, if (selected) SgColor.Brand500 else SgColor.Hairline, RoundedCornerShape(SgRadius.Tile))
                        .selectable(selected, role = Role.RadioButton) { viewModel.setDefault(m.kind) }
                        .padding(start = SgSpacing.Md, top = SgSpacing.Sm, bottom = SgSpacing.Sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
                ) {
                    Box(Modifier.size(36.dp).background(SgColor.Page, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(paymentIcon(m.kind), contentDescription = null, tint = SgColor.Ink, modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(m.label, style = SgTextStyle.Label)
                        Text(if (selected) "Utama · ${m.detail}" else m.detail, style = SgTextStyle.Caption, maxLines = 1)
                    }
                    RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = SgColor.Brand500))
                    if (m.kind != PaymentKind.QRIS && m.kind != PaymentKind.CASH) {
                        IconButton(onClick = {
                            viewModel.unlink(m.kind)
                            scope.launch { snackbar.showSnackbar("${m.label} dilepas") }
                        }) { Icon(Icons.Rounded.Close, contentDescription = "Lepas ${m.label}", tint = SgColor.InkMuted, modifier = Modifier.size(18.dp)) }
                    } else {
                        Box(Modifier.size(SgSpacing.Sm))
                    }
                }
            }
            if (state.linkable.isNotEmpty()) {
                item {
                    SgButton(
                        "Hubungkan e-wallet",
                        onClick = { pickKind = true },
                        style = SgButtonStyle.Secondary,
                        modifier = Modifier.fillMaxWidth(),
                        leading = { Icon(Icons.Rounded.Add, contentDescription = null, tint = SgColor.Brand600, modifier = Modifier.size(20.dp)) },
                    )
                }
            }
        }
    }

    if (pickKind) {
        ModalBottomSheet(onDismissRequest = { pickKind = false }, containerColor = SgColor.BaseWhite, scrimColor = SgColor.Ink.copy(alpha = 0.25f)) {
            Column(Modifier.padding(horizontal = SgSpacing.Gutter).padding(bottom = SgSpacing.Xl), verticalArrangement = Arrangement.spacedBy(SgSpacing.Md)) {
                Text("Pilih e-wallet", style = SgTextStyle.Title)
                state.linkable.forEach { kind ->
                    SgButton(paymentLabel(kind), onClick = {
                        pickKind = false
                        linking = kind
                    }, style = SgButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
    linking?.let { kind ->
        var phone by remember(kind) { mutableStateOf("") }
        var error by remember(kind) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { linking = null },
            title = { Text("Hubungkan ${paymentLabel(kind)}", style = SgTextStyle.Title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                    Text(l("Masukkan nomor HP yang terdaftar di ", "Enter the phone number registered with ") + paymentLabel(kind) + ".", style = SgTextStyle.Body)
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' }.take(15); error = null },
                        singleLine = true,
                        isError = error != null,
                        placeholder = { Text("0812…", style = SgTextStyle.Body) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        supportingText = error?.let { { Text(it, color = SgColor.RedStatus, style = SgTextStyle.Caption) } },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SgColor.Brand500, unfocusedBorderColor = SgColor.Hairline),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (viewModel.link(kind, phone)) {
                        linking = null
                        scope.launch { snackbar.showSnackbar("${paymentLabel(kind)} terhubung") }
                    } else {
                        error = "Nomor HP tidak valid"
                    }
                }) { Text("Hubungkan", style = SgTextStyle.Label, color = SgColor.Brand600) }
            },
            dismissButton = { TextButton(onClick = { linking = null }) { Text("Batal", style = SgTextStyle.Label, color = SgColor.Ink) } },
            containerColor = SgColor.BaseWhite,
        )
    }
}

// ---------------------------------------------------------------- Ganti Password

@Composable
fun ChangePasswordScreen(onBack: () -> Unit, onDone: () -> Unit, viewModel: ChangePasswordViewModel = hiltViewModel()) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val done by viewModel.done.collectAsStateWithLifecycle()
    LaunchedEffect(done) { if (done) onDone() }

    SubPage(
        title = "Ganti Password",
        onBack = onBack,
        bottomBar = {
            SgButton(
                "Simpan password",
                onClick = viewModel::submit,
                loading = form.busy,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = SgSpacing.Gutter, vertical = SgSpacing.Md),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(SgSpacing.Gutter),
            verticalArrangement = Arrangement.spacedBy(SgSpacing.Md),
        ) {
            PasswordInput("Password saat ini", form.current, { v -> viewModel.update { copy(current = v) } }, form.visibleError(PasswordField.CURRENT)) { viewModel.touch(PasswordField.CURRENT) }
            PasswordInput("Password baru", form.new, { v -> viewModel.update { copy(new = v) } }, null) { viewModel.touch(PasswordField.NEW) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Rule("Minimal 8 karakter", form.hasLength)
                Rule("Gabungan huruf dan angka", form.hasLetterAndDigit)
                Rule("Beda dari password lama", form.differsFromCurrent)
            }
            PasswordInput("Ulangi password baru", form.confirm, { v -> viewModel.update { copy(confirm = v) } }, form.visibleError(PasswordField.CONFIRM)) { viewModel.touch(PasswordField.CONFIRM) }
            Text("Lupa password? Hubungi Pusat Bantuan.", style = SgTextStyle.Caption)
        }
    }
}

@Composable
private fun Rule(text: String, ok: Boolean) {
    val alpha by animateFloatAsState(if (ok) 1f else 0.5f, tween(150), label = "rule")
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.graphicsLayer { this.alpha = alpha }) {
        Icon(
            if (ok) Icons.Rounded.Check else Icons.Rounded.Close,
            contentDescription = null,
            tint = if (ok) SgColor.Brand600 else SgColor.InkMuted,
            modifier = Modifier.size(16.dp),
        )
        Text(text, style = SgTextStyle.Caption, color = if (ok) SgColor.Brand700 else SgColor.InkMuted, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun PasswordInput(label: String, value: String, onChange: (String) -> Unit, error: String?, onBlur: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    var hadFocus by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = SgTextStyle.Label)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            isError = error != null,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (visible) "Sembunyikan" else "Tampilkan",
                        tint = SgColor.InkMuted,
                    )
                }
            },
            supportingText = error?.let { { Text(it, color = SgColor.RedStatus, style = SgTextStyle.Caption) } },
            textStyle = SgTextStyle.TextSmRegular.copy(color = SgColor.Ink),
            shape = RoundedCornerShape(SgRadius.Thumb),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SgColor.Brand500,
                unfocusedBorderColor = SgColor.Hairline,
                errorBorderColor = SgColor.RedStatus,
                focusedContainerColor = SgColor.BaseWhite,
                unfocusedContainerColor = SgColor.BaseWhite,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (it.isFocused) hadFocus = true else if (hadFocus) onBlur() },
        )
    }
}

// ---------------------------------------------------------------- Pusat Bantuan

private val faqs = listOf(
    "Apa itu SisaGuna?" to "SisaGuna menghubungkan kamu dengan makanan berlebih dari warung, toko roti, katering, dan rumah tangga di sekitarmu. Makanan layak makan dijual murah atau gratis, sisanya disalurkan untuk pakan ternak dan kompos.",
    "Apakah makanannya aman?" to "Penyedia wajib mendeskripsikan kondisi makanan dengan jujur. Cek tampilan dan bau saat ambil, dan konsumsi di hari yang sama. Laporkan lewat rating kalau ada yang tidak sesuai.",
    "Bagaimana cara mengambil pesanan?" to "Setelah checkout, buka Aktivitas lalu tunjukkan kode pickup (contoh SG-4821) ke penyedia sebelum waktu ambil habis. Tandai \"Sudah saya ambil\" setelah makanan di tanganmu.",
    "Bisa batal pesanan?" to "Bisa, selama pesanan belum diambil. Pembayaran online dikembalikan dalam 1×24 jam.",
    "Bagaimana cara jual atau bagikan makanan?" to "Ketuk Upload di Beranda, isi foto, jenis (Siap Santap, Pakan Ternak, atau Kompos), harga atau gratis, stok, dan batas waktu ambil.",
    "Kenapa ada kategori pakan ternak dan kompos?" to "Supaya tidak ada sisa yang terbuang ke TPA. Yang tidak layak untuk manusia masih berguna untuk peternak dan pembuat kompos.",
)

@Composable
fun HelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var open by remember { mutableStateOf<Int?>(0) }
    SubPage(title = "Pusat Bantuan", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(SgSpacing.Gutter),
            verticalArrangement = Arrangement.spacedBy(SgSpacing.Sm),
        ) {
            item { Text("Pertanyaan umum", style = SgTextStyle.Title, modifier = Modifier.padding(bottom = SgSpacing.Xs)) }
            items(faqs.size) { i ->
                val (q, a) = faqs[i]
                val expanded = open == i
                val rotation by animateFloatAsState(if (expanded) 180f else 0f, tween(200), label = "faq$i")
                CardBox(Modifier.pressable({ open = if (expanded) null else i }, pressedScale = 0.99f)) {
                    Column(Modifier.padding(SgSpacing.Lg)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(q, style = SgTextStyle.Label, modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = SgColor.InkMuted, modifier = Modifier.graphicsLayer { rotationZ = rotation })
                        }
                        AnimatedVisibility(expanded, enter = fadeIn(tween(160)) + expandVertically(tween(200)), exit = fadeOut(tween(100)) + shrinkVertically(tween(160))) {
                            Text(a, style = SgTextStyle.Body, modifier = Modifier.padding(top = SgSpacing.Sm))
                        }
                    }
                }
            }
            item {
                Text("Masih butuh bantuan?", style = SgTextStyle.Title, modifier = Modifier.padding(top = SgSpacing.Lg, bottom = SgSpacing.Xs))
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md)) {
                    SgButton(
                        "Chat CS",
                        onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/6281200000000?text=Halo%20SisaGuna"))) } },
                        modifier = Modifier.weight(1f),
                        leading = { Icon(Icons.Rounded.SupportAgent, contentDescription = null, tint = SgColor.OnBrand, modifier = Modifier.size(20.dp)) },
                    )
                    SgButton(
                        "Email",
                        onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:halo@sisaguna.id"))) } },
                        style = SgButtonStyle.Secondary,
                        modifier = Modifier.weight(1f),
                        leading = { Icon(Icons.Rounded.Email, contentDescription = null, tint = SgColor.Brand600, modifier = Modifier.size(20.dp)) },
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Kebijakan Privasi

private val privacySections = listOf(
    "Data yang kami kumpulkan" to "Nama, email, nomor HP, alamat yang kamu simpan, lokasi saat kamu memilih \"Lokasi saat ini\", serta riwayat pesanan dan rating.",
    "Untuk apa data dipakai" to "Menampilkan makanan di sekitarmu, memproses pesanan, menghubungkan kamu dengan penyedia, dan menghitung dampak penyelamatanmu.",
    "Lokasi" to "Lokasi hanya diambil saat kamu memintanya dan tidak dilacak di latar belakang.",
    "Berbagi dengan pihak lain" to "Penyedia hanya melihat nama dan kode pickup pesananmu. Kami tidak menjual data pribadimu.",
    "Hak kamu" to "Kamu bisa mengubah profil, menghapus alamat, dan meminta penghapusan akun lewat Pusat Bantuan.",
)

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    SubPage(title = "Kebijakan Privasi", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(SgSpacing.Gutter),
            verticalArrangement = Arrangement.spacedBy(SgSpacing.Lg),
        ) {
            item { Text("Terakhir diperbarui 1 Oktober 2026", style = SgTextStyle.Caption) }
            items(privacySections.size) { i ->
                val (title, body) = privacySections[i]
                Column(verticalArrangement = Arrangement.spacedBy(SgSpacing.Xs)) {
                    Text("${i + 1}. $title", style = SgTextStyle.Label)
                    Text(body, style = SgTextStyle.Body.copy(color = SgColor.Ink))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Katalog saya

@Composable
fun MyCatalogScreen(
    onBack: () -> Unit,
    onUpload: () -> Unit,
    onListingClick: (String) -> Unit,
    viewModel: MyCatalogViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf<CatalogItem?>(null) }
    SubPage(
        title = "Katalog saya",
        onBack = onBack,
        bottomBar = {
            SgButton(
                "Upload makanan",
                onClick = onUpload,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = SgSpacing.Gutter, vertical = SgSpacing.Md),
                leading = { Icon(Icons.Rounded.Add, contentDescription = null, tint = SgColor.OnBrand, modifier = Modifier.size(20.dp)) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(SgSpacing.Gutter),
            verticalArrangement = Arrangement.spacedBy(SgSpacing.Md),
        ) {
            item { Text(l("${state.activeCount} aktif dari ${state.items.size} listing", "${state.activeCount} of ${state.items.size} listings live"), style = SgTextStyle.Body) }
            if (state.items.isEmpty()) {
                item { SgEmptyState(Icons.Rounded.Inventory2, "Katalog masih kosong", "Punya makanan berlebih? Upload supaya bisa diselamatkan orang lain.") }
            }
            items(state.items, key = { it.listing.id }) { item ->
                val l = item.listing
                val (label, fg, bg) = when (item.status) {
                    CatalogStatus.ACTIVE -> Triple(com.sisaguna.android.ui.i18n.l("Aktif · sampai ", "Live · until ") + formatClock(l.pickupEnd), SgColor.Brand700, SgColor.Mint)
                    CatalogStatus.SOLD_OUT -> Triple("Habis", SgColor.FarmInk, SgColor.Farm)
                    CatalogStatus.EXPIRED -> Triple("Berakhir", SgColor.InkMuted, SgColor.Page)
                }
                CardBox(Modifier.animateItem().pressable({ onListingClick(l.id) })) {
                    Row(
                        modifier = Modifier.padding(start = SgSpacing.Md, top = SgSpacing.Md, bottom = SgSpacing.Md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
                    ) {
                        ListingImage(l.imageUrl, l.tier, null, Modifier.size(64.dp).clip(RoundedCornerShape(SgRadius.Thumb)))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(l.title, style = SgTextStyle.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${formatPrice(l.unitPrice)} · stok ${l.stock}", style = SgTextStyle.Body)
                            Text(label, style = SgTextStyle.TextXsMedium, color = fg, modifier = Modifier.background(bg, RoundedCornerShape(SgRadius.Pill)).padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                        IconButton(onClick = { confirm = item }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = com.sisaguna.android.ui.i18n.l("Hapus ", "Delete ") + l.title, tint = SgColor.RedStatus, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
    confirm?.let { item ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Hapus listing?", style = SgTextStyle.Title) },
            text = { Text(l("\"${item.listing.title}\" akan hilang dari Beranda dan tokomu.", "\"${item.listing.title}\" will be removed from Home and your store."), style = SgTextStyle.Body) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(item.listing.id)
                    confirm = null
                }) { Text("Hapus", style = SgTextStyle.Label, color = SgColor.RedStatus) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Batal", style = SgTextStyle.Label, color = SgColor.Ink) } },
            containerColor = SgColor.BaseWhite,
        )
    }
}
