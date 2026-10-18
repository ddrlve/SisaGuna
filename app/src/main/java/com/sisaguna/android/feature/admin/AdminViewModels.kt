package com.sisaguna.android.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sisaguna.android.core.ui.AppMessenger
import com.sisaguna.android.data.model.AdminComplaint
import com.sisaguna.android.data.model.AdminMerchant
import com.sisaguna.android.data.model.AdminPage
import com.sisaguna.android.data.model.AdminVoucher
import com.sisaguna.android.data.model.ComplaintStatus
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.MerchantStatus
import com.sisaguna.android.data.model.NewVoucher
import com.sisaguna.android.data.model.VoucherPatch
import com.sisaguna.android.data.remote.AppException
import com.sisaguna.android.data.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PagedState<T>(
    val items: List<T> = emptyList(),
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null,
)

/** A server-paged list: the first page replaces the items, "load more" appends, edits can be applied locally. */
class PagedList<T>(
    private val scope: CoroutineScope,
    private val fetch: suspend (page: Int) -> AdminPage<T>,
) {
    private val _state = MutableStateFlow(PagedState<T>())
    val state: StateFlow<PagedState<T>> = _state.asStateFlow()

    private var page = 0
    private var job: Job? = null

    fun refresh() {
        job?.cancel()
        _state.update { it.copy(loading = true, error = null) }
        job = scope.launch {
            try {
                val first = fetch(1)
                page = 1
                _state.value = PagedState(first.items, loading = false, hasMore = first.hasMore)
            } catch (e: CancellationException) {
                throw e
            } catch (e: AppException) {
                _state.update { it.copy(loading = false, error = e.userMessage) }
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loading || s.loadingMore || !s.hasMore) return
        _state.update { it.copy(loadingMore = true) }
        job = scope.launch {
            try {
                val next = fetch(page + 1)
                page += 1
                _state.update { it.copy(items = it.items + next.items, loadingMore = false, hasMore = next.hasMore) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: AppException) {
                _state.update { it.copy(loadingMore = false, error = e.userMessage) }
            }
        }
    }

    fun edit(transform: (List<T>) -> List<T>) = _state.update { it.copy(items = transform(it.items)) }
}

// ---------------------------------------------------------------- stores

@HiltViewModel
class AdminMerchantsViewModel @Inject constructor(
    private val repository: AdminRepository,
    private val messenger: AppMessenger,
) : ViewModel() {

    private val _status = MutableStateFlow<MerchantStatus?>(MerchantStatus.PENDING)
    val status: StateFlow<MerchantStatus?> = _status.asStateFlow()

    private val list = PagedList(viewModelScope) { page -> repository.merchants(_status.value, page) }
    val state: StateFlow<PagedState<AdminMerchant>> = list.state

    init {
        list.refresh()
    }

    fun setStatus(status: MerchantStatus?) {
        _status.value = status
        list.refresh()
    }

    fun refresh() = list.refresh()
    fun loadMore() = list.loadMore()

    /** The decision leaves the pending queue at once; if the server refuses, the store comes back. */
    fun decide(merchant: AdminMerchant, approve: Boolean) {
        val before = list.state.value.items
        if (_status.value == MerchantStatus.PENDING) list.edit { items -> items.filterNot { it.id == merchant.id } }
        viewModelScope.launch {
            try {
                repository.setMerchantApproved(merchant.id, approve)
                if (_status.value != MerchantStatus.PENDING) list.refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: AppException) {
                list.edit { before }
                messenger.show(e)
            }
        }
    }
}

// ---------------------------------------------------------------- complaints

@HiltViewModel
class AdminComplaintsViewModel @Inject constructor(
    private val repository: AdminRepository,
    private val messenger: AppMessenger,
) : ViewModel() {

    private val _status = MutableStateFlow<ComplaintStatus?>(ComplaintStatus.SUBMITTED)
    val status: StateFlow<ComplaintStatus?> = _status.asStateFlow()

    private val list = PagedList(viewModelScope) { page -> repository.complaints(_status.value, page) }
    val state: StateFlow<PagedState<AdminComplaint>> = list.state

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    init {
        list.refresh()
    }

    fun setStatus(status: ComplaintStatus?) {
        _status.value = status
        list.refresh()
    }

    fun refresh() = list.refresh()
    fun loadMore() = list.loadMore()

    /** Closing a complaint (resolved/rejected) needs a note the buyer will read; the server enforces it too. */
    fun update(complaint: AdminComplaint, status: ComplaintStatus, note: String, onDone: () -> Unit) {
        if ((status == ComplaintStatus.RESOLVED || status == ComplaintStatus.REJECTED) && note.isBlank()) {
            messenger.show("Tulis catatan penyelesaian untuk pembeli.")
            return
        }
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            try {
                repository.updateComplaint(complaint.id, status, note)
                list.refresh()
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: AppException) {
                messenger.show(e)
            } finally {
                _saving.value = false
            }
        }
    }
}

// ---------------------------------------------------------------- vouchers

enum class DiscountKind { PERCENT, FLAT }

/** The palette offered for a voucher card (hex as the API expects). */
val voucherAccents = listOf("#4DA62C", "#C0306A", "#B4570B", "#1D6FA5", "#6B4EE6")

val voucherValidityDays = listOf(3, 7, 14, 30)

data class VoucherForm(
    val editing: AdminVoucher? = null,
    val code: String = "",
    val title: String = "",
    val description: String = "",
    val kind: DiscountKind = DiscountKind.PERCENT,
    val value: String = "",
    val maxDiscount: String = "",
    val minSpend: String = "",
    val tier: ListingTier? = null,
    val validDays: Int = 7,
    val quota: String = "",
    val terms: String = "",
    val accent: String = voucherAccents.first(),
    /** Editing only: also push the expiry to "now + [validDays]". */
    val extend: Boolean = false,
    val submitted: Boolean = false,
    val saving: Boolean = false,
) {
    private fun amount(text: String) = text.filter(Char::isDigit).toIntOrNull()

    private val termLines: List<String> get() = terms.lines().map { it.trim() }.filter { it.isNotEmpty() }

    /** Mirrors the server's rules so the admin sees the problem before sending. */
    fun error(): String? = when {
        editing == null && !CODE.matches(code) -> "Kode 4-20 karakter: huruf besar dan angka saja."
        title.isBlank() -> "Judul wajib diisi."
        title.trim().length > 80 -> "Judul maksimal 80 karakter."
        editing == null && (amount(value) ?: 0) <= 0 -> "Isi besar diskon."
        editing == null && kind == DiscountKind.PERCENT && (amount(value) ?: 0) !in 1..100 -> "Persen diskon antara 1 dan 100."
        quota.isNotBlank() && (amount(quota) ?: 0) < 1 -> "Kuota minimal 1."
        termLines.size > 10 -> "Syarat maksimal 10 baris."
        termLines.any { it.length > 120 } -> "Tiap syarat maksimal 120 karakter."
        else -> null
    }

    fun toNew(now: Instant): NewVoucher = NewVoucher(
        code = code,
        title = title.trim(),
        description = description.trim(),
        percentOff = if (kind == DiscountKind.PERCENT) amount(value) else null,
        flatOff = if (kind == DiscountKind.FLAT) amount(value) else null,
        maxDiscount = if (kind == DiscountKind.PERCENT) amount(maxDiscount) else null,
        minSpend = amount(minSpend) ?: 0,
        appliesToTier = tier,
        expiresAt = now.plus(validDays.toLong(), ChronoUnit.DAYS),
        quota = amount(quota),
        terms = termLines,
        accentHex = accent,
    )

    fun toPatch(now: Instant, extend: Boolean): VoucherPatch = VoucherPatch(
        title = title.trim(),
        description = description.trim(),
        minSpend = amount(minSpend) ?: 0,
        expiresAt = if (extend) now.plus(validDays.toLong(), ChronoUnit.DAYS) else null,
        quota = amount(quota),
        terms = termLines,
        accentHex = accent,
    )

    companion object {
        private val CODE = Regex("^[A-Z0-9]{4,20}$")

        fun from(v: AdminVoucher) = VoucherForm(
            editing = v,
            code = v.code,
            title = v.title,
            description = v.description,
            kind = if (v.percentOff != null) DiscountKind.PERCENT else DiscountKind.FLAT,
            value = (v.percentOff ?: v.flatOff)?.toString().orEmpty(),
            maxDiscount = v.maxDiscount?.toString().orEmpty(),
            minSpend = v.minSpend.toString(),
            tier = v.appliesToTier,
            quota = v.quota?.toString().orEmpty(),
            terms = v.terms.joinToString("\n"),
            accent = v.accentHex.uppercase().takeIf { it in voucherAccents } ?: voucherAccents.first(),
        )
    }
}

@HiltViewModel
class AdminVouchersViewModel @Inject constructor(
    private val repository: AdminRepository,
    private val messenger: AppMessenger,
) : ViewModel() {

    private val list = PagedList(viewModelScope) { page -> repository.vouchers(page) }
    val state: StateFlow<PagedState<AdminVoucher>> = list.state

    /** Non-null while the create/edit sheet is open. */
    private val _form = MutableStateFlow<VoucherForm?>(null)
    val form: StateFlow<VoucherForm?> = _form.asStateFlow()

    /** Test seam: the clock behind "berlaku N hari". */
    internal var clock: () -> Instant = Instant::now

    init {
        list.refresh()
    }

    fun refresh() = list.refresh()
    fun loadMore() = list.loadMore()

    fun newVoucher() {
        _form.value = VoucherForm()
    }

    fun edit(voucher: AdminVoucher) {
        _form.value = VoucherForm.from(voucher)
    }

    fun update(transform: VoucherForm.() -> VoucherForm) = _form.update { it?.transform() }

    fun dismissForm() {
        _form.value = null
    }

    fun save() {
        val f = _form.value ?: return
        if (f.saving) return
        f.error()?.let {
            _form.value = f.copy(submitted = true)
            messenger.show(it)
            return
        }
        _form.value = f.copy(saving = true)
        viewModelScope.launch {
            try {
                val editing = f.editing
                if (editing == null) repository.createVoucher(f.toNew(clock())) else repository.updateVoucher(editing.code, f.toPatch(clock(), extend = f.extend))
                _form.value = null
                list.refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: AppException) {
                messenger.show(e)
                _form.update { it?.copy(saving = false) }
            }
        }
    }

    /** Switch a voucher on or off without opening the sheet; reverts if the server refuses. */
    fun setActive(voucher: AdminVoucher, active: Boolean) {
        list.edit { items -> items.map { if (it.code == voucher.code) it.copy(isActive = active) else it } }
        viewModelScope.launch {
            try {
                repository.updateVoucher(voucher.code, VoucherPatch(isActive = active))
            } catch (e: CancellationException) {
                throw e
            } catch (e: AppException) {
                list.edit { items -> items.map { if (it.code == voucher.code) it.copy(isActive = voucher.isActive) else it } }
                messenger.show(e)
            }
        }
    }
}
