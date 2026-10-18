package com.sisaguna.android.data.repository

import com.sisaguna.android.data.model.AdminComplaint
import com.sisaguna.android.data.model.AdminMerchant
import com.sisaguna.android.data.model.AdminPage
import com.sisaguna.android.data.model.AdminVoucher
import com.sisaguna.android.data.model.ComplaintReason
import com.sisaguna.android.data.model.ComplaintStatus
import com.sisaguna.android.data.model.MerchantStatus
import com.sisaguna.android.data.model.NewVoucher
import com.sisaguna.android.data.model.VoucherPatch
import com.sisaguna.android.data.remote.AppException
import java.time.Instant

/** In-memory moderation backend for ViewModel tests; every call is recorded and any of them can be made to fail. */
class FakeAdminRepository : AdminRepository {

    val merchants = mutableListOf(
        AdminMerchant("m1", "Warung Baru", MerchantStatus.PENDING, "Jl. A", "Wati", "wati@x.id", Instant.EPOCH),
        AdminMerchant("m2", "Kantin Lama", MerchantStatus.APPROVED, "Jl. B", "Joko", "joko@x.id", Instant.EPOCH),
    )
    val complaints = mutableListOf(
        AdminComplaint("c1", "SG-1", "Warung Baru", "Budi", ComplaintReason.NOT_FRESH, "Basi sekali", ComplaintStatus.SUBMITTED, null, emptyList(), Instant.EPOCH),
    )
    val vouchers = mutableListOf(
        AdminVoucher("HEMAT10", "Hemat 10%", "", 10, null, 5000, 0, null, Instant.now().plusSeconds(86_400), null, 2, emptyList(), "#4DA62C", true),
    )

    var failWith: AppException? = null
    val calls = mutableListOf<String>()
    var totalPages = 1
    val requestedMerchantStatuses = mutableListOf<MerchantStatus?>()

    private fun <T> guarded(call: String, block: () -> T): T {
        calls += call
        failWith?.let { throw it }
        return block()
    }

    override suspend fun merchants(status: MerchantStatus?, page: Int): AdminPage<AdminMerchant> {
        requestedMerchantStatuses += status
        return AdminPage(merchants.filter { status == null || it.status == status }, page, totalPages)
    }

    override suspend fun setMerchantApproved(id: String, approved: Boolean) {
        guarded("merchant:$id:$approved") {
            val i = merchants.indexOfFirst { it.id == id }
            merchants[i] = merchants[i].copy(status = if (approved) MerchantStatus.APPROVED else MerchantStatus.REJECTED)
        }
    }

    override suspend fun complaints(status: ComplaintStatus?, page: Int) =
        AdminPage(complaints.filter { status == null || it.status == status }, page, totalPages)

    override suspend fun updateComplaint(id: String, status: ComplaintStatus, note: String) {
        guarded("complaint:$id:${status.name}:$note") {
            val i = complaints.indexOfFirst { it.id == id }
            complaints[i] = complaints[i].copy(status = status, resolutionNote = note)
        }
    }

    override suspend fun vouchers(page: Int) = AdminPage(vouchers.toList(), page, totalPages)

    var lastNew: NewVoucher? = null
    var lastPatch: VoucherPatch? = null

    override suspend fun createVoucher(voucher: NewVoucher): AdminVoucher = guarded("create:${voucher.code}") {
        lastNew = voucher
        AdminVoucher(
            voucher.code, voucher.title, voucher.description, voucher.percentOff, voucher.flatOff, voucher.maxDiscount, voucher.minSpend,
            voucher.appliesToTier, voucher.expiresAt, voucher.quota, 0, voucher.terms, voucher.accentHex, true,
        ).also { vouchers += it }
    }

    override suspend fun updateVoucher(code: String, patch: VoucherPatch): AdminVoucher = guarded("update:$code") {
        lastPatch = patch
        val i = vouchers.indexOfFirst { it.code == code }
        vouchers[i] = vouchers[i].copy(
            title = patch.title ?: vouchers[i].title,
            isActive = patch.isActive ?: vouchers[i].isActive,
            expiresAt = patch.expiresAt ?: vouchers[i].expiresAt,
        )
        vouchers[i]
    }
}
