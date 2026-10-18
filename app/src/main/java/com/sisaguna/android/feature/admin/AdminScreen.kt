package com.sisaguna.android.feature.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sisaguna.android.data.model.AdminComplaint
import com.sisaguna.android.data.model.AdminMerchant
import com.sisaguna.android.data.model.AdminVoucher
import com.sisaguna.android.data.model.ComplaintStatus
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.MerchantStatus
import com.sisaguna.android.ui.components.SgButton
import com.sisaguna.android.ui.components.SgButtonStyle
import com.sisaguna.android.ui.components.SgChip
import com.sisaguna.android.ui.components.SgEmptyState
import com.sisaguna.android.ui.components.SgTopBar
import com.sisaguna.android.ui.domain.formatDateTime
import com.sisaguna.android.ui.domain.formatRupiah
import com.sisaguna.android.ui.i18n.Text
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle

private val tabs = listOf("Toko", "Komplain", "Voucher")

/** Admin panel: store approvals, buyer complaints and vouchers. The server refuses every call from a non-admin. */
@Composable
fun AdminScreen(onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(SgColor.Page)) {
        Column(Modifier.background(SgColor.BaseWhite)) {
            SgTopBar(title = "Panel admin", onBack = onBack)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = SgSpacing.Gutter).padding(bottom = SgSpacing.Sm),
                horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm),
            ) {
                tabs.forEachIndexed { i, label -> SgChip(label, tab == i, { tab = i }) }
            }
        }
        when (tab) {
            0 -> MerchantsTab()
            1 -> ComplaintsTab()
            else -> VouchersTab()
        }
    }
}

// ---------------------------------------------------------------- shared bits

@Composable
private fun <T> PagedColumn(
    state: PagedState<T>,
    emptyTitle: String,
    emptyBody: String,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    header: @Composable () -> Unit = {},
    row: @Composable (T) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        header()
        when {
            state.loading -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator(color = SgColor.Brand500)
            }
            state.error != null && state.items.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(SgSpacing.Gutter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(state.error, style = SgTextStyle.Body, color = SgColor.RedStatus)
                SgButton("Coba lagi", onClick = onRetry, modifier = Modifier.padding(top = SgSpacing.Md))
            }
            state.items.isEmpty() -> SgEmptyState(Icons.Rounded.Inbox, emptyTitle, emptyBody)
            else -> LazyColumn(
                contentPadding = PaddingValues(SgSpacing.Gutter),
                verticalArrangement = Arrangement.spacedBy(SgSpacing.Md),
            ) {
                items(state.items) { row(it) }
                if (state.hasMore || state.loadingMore) {
                    item {
                        SgButton(
                            if (state.loadingMore) "Memuat…" else "Muat lagi",
                            onClick = onLoadMore,
                            enabled = !state.loadingMore,
                            style = SgButtonStyle.Secondary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Card))
            .background(SgColor.BaseWhite)
            .border(1.dp, SgColor.Hairline, RoundedCornerShape(SgRadius.Card))
            .padding(SgSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(SgSpacing.Sm),
    ) { content() }
}

@Composable
private fun StatusPill(text: String, fg: Color, bg: Color) {
    Text(
        text,
        style = SgTextStyle.TextXsMedium,
        color = fg,
        modifier = Modifier.background(bg, RoundedCornerShape(SgRadius.Pill)).padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

@Composable
private fun FilterRow(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = SgSpacing.Gutter, vertical = SgSpacing.Sm).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm),
    ) { content() }
}

@Composable
private fun AdminField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, singleLine: Boolean = true, keyboard: KeyboardType = KeyboardType.Text, error: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, style = SgTextStyle.Caption) },
        singleLine = singleLine,
        isError = error,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboard),
        textStyle = SgTextStyle.TextSmRegular.copy(color = SgColor.Ink),
        shape = RoundedCornerShape(SgRadius.Thumb),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SgColor.Brand500, unfocusedBorderColor = SgColor.Hairline),
        modifier = modifier.fillMaxWidth(),
    )
}

// ---------------------------------------------------------------- stores

private fun merchantStatusLabel(s: MerchantStatus) = when (s) {
    MerchantStatus.PENDING -> "Menunggu"
    MerchantStatus.APPROVED -> "Disetujui"
    MerchantStatus.REJECTED -> "Ditolak"
    MerchantStatus.SUSPENDED -> "Ditangguhkan"
}

@Composable
private fun MerchantsTab(viewModel: AdminMerchantsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    var confirmReject by remember { mutableStateOf<AdminMerchant?>(null) }

    PagedColumn(
        state = state,
        emptyTitle = "Tidak ada toko",
        emptyBody = "Tidak ada toko dengan status ini.",
        onRetry = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        header = {
            FilterRow {
                listOf<MerchantStatus?>(MerchantStatus.PENDING, MerchantStatus.APPROVED, MerchantStatus.REJECTED, MerchantStatus.SUSPENDED, null).forEach { s ->
                    SgChip(s?.let(::merchantStatusLabel) ?: "Semua", status == s, { viewModel.setStatus(s) })
                }
            }
        },
    ) { m ->
        AdminCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(m.name, style = SgTextStyle.Title, modifier = Modifier.weight(1f))
                val (fg, bg) = when (m.status) {
                    MerchantStatus.APPROVED -> SgColor.Brand700 to SgColor.Mint
                    MerchantStatus.PENDING -> SgColor.YellowStatus to SgColor.Yellow50
                    else -> SgColor.RedStatus to SgColor.RedStatus.copy(alpha = 0.1f)
                }
                StatusPill(merchantStatusLabel(m.status), fg, bg)
            }
            Text(m.address, style = SgTextStyle.Body, maxLines = 2)
            Text("${m.ownerName} · ${m.ownerEmail}", style = SgTextStyle.Caption)
            Text("Mendaftar ${formatDateTime(m.createdAt)}", style = SgTextStyle.Caption)
            Row(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm), modifier = Modifier.fillMaxWidth()) {
                if (m.status != MerchantStatus.REJECTED) {
                    SgButton("Tolak", onClick = { confirmReject = m }, style = SgButtonStyle.Destructive, height = 40.dp, modifier = Modifier.weight(1f))
                }
                if (m.status != MerchantStatus.APPROVED) {
                    SgButton("Setujui", onClick = { viewModel.decide(m, approve = true) }, height = 40.dp, modifier = Modifier.weight(1f))
                }
            }
        }
    }

    confirmReject?.let { m ->
        AlertDialog(
            onDismissRequest = { confirmReject = null },
            title = { Text("Tolak ${m.name}?", style = SgTextStyle.Title) },
            text = { Text("Pemilik toko diberi tahu dan tidak bisa berjualan.", style = SgTextStyle.Body) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.decide(m, approve = false)
                    confirmReject = null
                }) { Text("Tolak", color = SgColor.RedStatus) }
            },
            dismissButton = { TextButton(onClick = { confirmReject = null }) { Text("Batal") } },
        )
    }
}

// ---------------------------------------------------------------- complaints

private fun complaintStatusLabel(s: ComplaintStatus) = when (s) {
    ComplaintStatus.SUBMITTED -> "Terkirim"
    ComplaintStatus.REVIEWING -> "Ditinjau"
    ComplaintStatus.RESOLVED -> "Selesai"
    ComplaintStatus.REJECTED -> "Ditolak"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ComplaintsTab(viewModel: AdminComplaintsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    var handling by remember { mutableStateOf<AdminComplaint?>(null) }

    PagedColumn(
        state = state,
        emptyTitle = "Tidak ada komplain",
        emptyBody = "Tidak ada komplain dengan status ini.",
        onRetry = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        header = {
            FilterRow {
                listOf<ComplaintStatus?>(ComplaintStatus.SUBMITTED, ComplaintStatus.REVIEWING, ComplaintStatus.RESOLVED, ComplaintStatus.REJECTED, null).forEach { s ->
                    SgChip(s?.let(::complaintStatusLabel) ?: "Semua", status == s, { viewModel.setStatus(s) })
                }
            }
        },
    ) { c ->
        AdminCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pesanan ${c.orderCode}", style = SgTextStyle.Title, modifier = Modifier.weight(1f))
                val closed = c.isClosed
                StatusPill(
                    complaintStatusLabel(c.status),
                    if (c.status == ComplaintStatus.REJECTED) SgColor.RedStatus else if (closed) SgColor.Brand700 else SgColor.YellowStatus,
                    if (c.status == ComplaintStatus.REJECTED) SgColor.RedStatus.copy(alpha = 0.1f) else if (closed) SgColor.Mint else SgColor.Yellow50,
                )
            }
            Text("${c.buyerName} → ${c.merchantName}", style = SgTextStyle.Label)
            Text(c.reason.label, style = SgTextStyle.Caption, color = SgColor.RedStatus)
            Text(c.detail, style = SgTextStyle.Body)
            if (c.photoUrls.isNotEmpty()) {
                // Complaint photos are private: the app's image loader adds the admin's token for them.
                Row(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                    c.photoUrls.take(3).forEach { url ->
                        AsyncImage(url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(72.dp).clip(RoundedCornerShape(SgRadius.Thumb)).background(SgColor.Page))
                    }
                }
            }
            c.resolutionNote?.takeIf { it.isNotBlank() }?.let { Text("Catatan: $it", style = SgTextStyle.Caption) }
            Text(formatDateTime(c.createdAt), style = SgTextStyle.Caption)
            if (!c.isClosed) SgButton("Tindak lanjuti", onClick = { handling = c }, height = 40.dp, modifier = Modifier.fillMaxWidth())
        }
    }

    handling?.let { c ->
        var next by remember(c.id) { mutableStateOf(if (c.status == ComplaintStatus.REVIEWING) ComplaintStatus.RESOLVED else ComplaintStatus.REVIEWING) }
        var note by remember(c.id) { mutableStateOf(c.resolutionNote.orEmpty()) }
        ModalBottomSheet(
            onDismissRequest = { handling = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SgColor.BaseWhite,
        ) {
            Column(
                Modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(horizontal = SgSpacing.Gutter).padding(bottom = SgSpacing.Lg),
                verticalArrangement = Arrangement.spacedBy(SgSpacing.Md),
            ) {
                Text("Tindak lanjuti komplain", style = SgTextStyle.Title)
                Text("Pesanan ${c.orderCode} · ${c.reason.label}", style = SgTextStyle.Body)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                    listOf(ComplaintStatus.REVIEWING, ComplaintStatus.RESOLVED, ComplaintStatus.REJECTED).forEach { s ->
                        SgChip(complaintStatusLabel(s), next == s, { next = s })
                    }
                }
                AdminField(
                    note, { note = it.take(500) }, if (next == ComplaintStatus.REVIEWING) "Catatan (opsional)" else "Catatan untuk pembeli (wajib)",
                    singleLine = false,
                )
                SgButton(
                    if (saving) "Menyimpan…" else "Simpan",
                    onClick = { viewModel.update(c, next, note) { handling = null } },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- vouchers

private fun voucherSummary(v: AdminVoucher) =
    (v.percentOff?.let { "$it%" + (v.maxDiscount?.let { m -> " (maks. ${formatRupiah(m)})" } ?: "") } ?: v.flatOff?.let { formatRupiah(it) }.orEmpty()) +
        if (v.minSpend > 0) " · min. ${formatRupiah(v.minSpend)}" else ""

@Composable
private fun VouchersTab(viewModel: AdminVouchersViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()

    PagedColumn(
        state = state,
        emptyTitle = "Belum ada voucher",
        emptyBody = "Buat voucher pertama dengan tombol di atas.",
        onRetry = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        header = {
            SgButton(
                "Buat voucher",
                onClick = viewModel::newVoucher,
                height = 44.dp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = SgSpacing.Gutter, vertical = SgSpacing.Sm),
            )
        },
    ) { v ->
        AdminCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(v.code, style = SgTextStyle.Title)
                    Text(v.title, style = SgTextStyle.Label)
                }
                Switch(checked = v.isActive, onCheckedChange = { viewModel.setActive(v, it) })
            }
            Text(voucherSummary(v), style = SgTextStyle.Body)
            Text(
                "Dipakai ${v.usedCount}" + (v.quota?.let { " dari $it" } ?: "") + " · " +
                    (if (v.isExpired) "Kedaluwarsa " else "Berlaku sampai ") + formatDateTime(v.expiresAt),
                style = SgTextStyle.Caption,
                color = if (v.isExpired) SgColor.RedStatus else SgColor.InkMuted,
            )
            SgButton("Ubah", onClick = { viewModel.edit(v) }, style = SgButtonStyle.Secondary, height = 40.dp, modifier = Modifier.fillMaxWidth())
        }
    }

    form?.let { VoucherSheet(it, viewModel) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun VoucherSheet(form: VoucherForm, viewModel: AdminVouchersViewModel) {
    val creating = form.editing == null
    ModalBottomSheet(
        onDismissRequest = viewModel::dismissForm,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SgColor.BaseWhite,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding()
                .padding(horizontal = SgSpacing.Gutter).padding(bottom = SgSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(SgSpacing.Md),
        ) {
            Text(if (creating) "Buat voucher" else "Ubah ${form.code}", style = SgTextStyle.Title)
            if (creating) {
                AdminField(form.code, { v -> viewModel.update { copy(code = v.uppercase().filter { it.isLetterOrDigit() }.take(20)) } }, "Kode (huruf besar dan angka)")
            }
            AdminField(form.title, { v -> viewModel.update { copy(title = v.take(80)) } }, "Judul")
            AdminField(form.description, { v -> viewModel.update { copy(description = v.take(300)) } }, "Deskripsi", singleLine = false)

            if (creating) {
                Text("Jenis diskon", style = SgTextStyle.Label)
                Row(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                    SgChip("Persen", form.kind == DiscountKind.PERCENT, { viewModel.update { copy(kind = DiscountKind.PERCENT) } })
                    SgChip("Nominal", form.kind == DiscountKind.FLAT, { viewModel.update { copy(kind = DiscountKind.FLAT) } })
                }
                AdminField(form.value, { v -> viewModel.update { copy(value = v.filter(Char::isDigit).take(8)) } }, if (form.kind == DiscountKind.PERCENT) "Diskon (%)" else "Potongan (Rp)", keyboard = KeyboardType.Number)
                if (form.kind == DiscountKind.PERCENT) {
                    AdminField(form.maxDiscount, { v -> viewModel.update { copy(maxDiscount = v.filter(Char::isDigit).take(8)) } }, "Maks. potongan (Rp, opsional)", keyboard = KeyboardType.Number)
                }
                Text("Berlaku untuk", style = SgTextStyle.Label)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                    SgChip("Semua", form.tier == null, { viewModel.update { copy(tier = null) } })
                    SgChip("Siap santap", form.tier == ListingTier.HUMAN, { viewModel.update { copy(tier = ListingTier.HUMAN) } })
                    SgChip("Pakan", form.tier == ListingTier.ANIMAL_FEED, { viewModel.update { copy(tier = ListingTier.ANIMAL_FEED) } })
                    SgChip("Kompos", form.tier == ListingTier.COMPOST, { viewModel.update { copy(tier = ListingTier.COMPOST) } })
                }
            }
            AdminField(form.minSpend, { v -> viewModel.update { copy(minSpend = v.filter(Char::isDigit).take(8)) } }, "Minimal belanja (Rp)", keyboard = KeyboardType.Number)
            AdminField(form.quota, { v -> viewModel.update { copy(quota = v.filter(Char::isDigit).take(7)) } }, "Kuota (kosong = tanpa batas)", keyboard = KeyboardType.Number)

            Text(if (creating) "Berlaku selama" else "Perpanjang masa berlaku", style = SgTextStyle.Label)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                if (!creating) SgChip("Tidak diubah", !form.extend, { viewModel.update { copy(extend = false) } })
                voucherValidityDays.forEach { d ->
                    val selected = form.validDays == d && (creating || form.extend)
                    SgChip("$d hari", selected, { viewModel.update { copy(validDays = d, extend = true) } })
                }
            }

            AdminField(form.terms, { v -> viewModel.update { copy(terms = v.take(1500)) } }, "Syarat (satu per baris, maks. 10)", singleLine = false)

            Text("Warna kartu", style = SgTextStyle.Label)
            Row(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md)) {
                voucherAccents.forEach { hex ->
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(hex)))
                            .border(if (form.accent == hex) 3.dp else 0.dp, SgColor.Ink, CircleShape)
                            .clickable { viewModel.update { copy(accent = hex) } },
                    )
                }
            }

            if (form.submitted) form.error()?.let { Text(it, style = SgTextStyle.Caption, color = SgColor.RedStatus) }
            SgButton(
                if (form.saving) "Menyimpan…" else if (creating) "Buat voucher" else "Simpan perubahan",
                onClick = viewModel::save,
                enabled = !form.saving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
