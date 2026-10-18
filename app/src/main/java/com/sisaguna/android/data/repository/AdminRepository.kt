package com.sisaguna.android.data.repository

import com.sisaguna.android.data.model.AdminComplaint
import com.sisaguna.android.data.model.AdminMerchant
import com.sisaguna.android.data.model.AdminPage
import com.sisaguna.android.data.model.AdminVoucher
import com.sisaguna.android.data.model.ComplaintStatus
import com.sisaguna.android.data.model.MerchantStatus
import com.sisaguna.android.data.model.NewVoucher
import com.sisaguna.android.data.model.VoucherPatch
import com.sisaguna.android.data.remote.ApiService
import com.sisaguna.android.data.remote.apiCall
import com.sisaguna.android.data.remote.apiData
import com.sisaguna.android.data.remote.dto.CreateVoucherBody
import com.sisaguna.android.data.remote.dto.PatchVoucherBody
import com.sisaguna.android.data.remote.dto.UpdateComplaintBody
import com.sisaguna.android.data.remote.toAdminComplaint
import com.sisaguna.android.data.remote.toAdminMerchant
import com.sisaguna.android.data.remote.toAdminVoucher
import javax.inject.Inject
import javax.inject.Singleton

/** Moderation tools. Every call is refused with 403 unless the account is an admin. Pages are 1-based. */
interface AdminRepository {
    suspend fun merchants(status: MerchantStatus?, page: Int): AdminPage<AdminMerchant>
    suspend fun setMerchantApproved(id: String, approved: Boolean)

    suspend fun complaints(status: ComplaintStatus?, page: Int): AdminPage<AdminComplaint>

    /** [note] is required by the server when closing a complaint (resolved or rejected). */
    suspend fun updateComplaint(id: String, status: ComplaintStatus, note: String)

    suspend fun vouchers(page: Int): AdminPage<AdminVoucher>
    suspend fun createVoucher(voucher: NewVoucher): AdminVoucher
    suspend fun updateVoucher(code: String, patch: VoucherPatch): AdminVoucher
}

@Singleton
class ApiAdminRepository @Inject constructor(
    private val api: ApiService,
) : AdminRepository {

    override suspend fun merchants(status: MerchantStatus?, page: Int): AdminPage<AdminMerchant> {
        val response = apiCall { api.adminMerchants(status?.name?.lowercase(), page) }
        return AdminPage(response.data.map { it.toAdminMerchant() }, page, response.meta?.totalPages ?: page)
    }

    override suspend fun setMerchantApproved(id: String, approved: Boolean) = apiCall {
        if (approved) api.approveMerchant(id) else api.rejectMerchant(id)
    }

    override suspend fun complaints(status: ComplaintStatus?, page: Int): AdminPage<AdminComplaint> {
        val response = apiCall { api.adminComplaints(status?.name?.lowercase(), page) }
        return AdminPage(response.data.map { it.toAdminComplaint() }, page, response.meta?.totalPages ?: page)
    }

    override suspend fun updateComplaint(id: String, status: ComplaintStatus, note: String) =
        apiCall { api.updateComplaint(id, UpdateComplaintBody(status.name.lowercase(), note.trim())) }

    override suspend fun vouchers(page: Int): AdminPage<AdminVoucher> {
        val response = apiCall { api.adminVouchers(page) }
        return AdminPage(response.data.map { it.toAdminVoucher() }, page, response.meta?.totalPages ?: page)
    }

    override suspend fun createVoucher(voucher: NewVoucher): AdminVoucher = apiData {
        api.createVoucher(
            CreateVoucherBody(
                code = voucher.code,
                title = voucher.title,
                description = voucher.description,
                percentOff = voucher.percentOff,
                flatOff = voucher.flatOff,
                maxDiscount = voucher.maxDiscount,
                minSpend = voucher.minSpend,
                appliesToTier = voucher.appliesToTier?.name?.lowercase(),
                appliesTo = when (voucher.appliesToTier) {
                    null -> ""
                    com.sisaguna.android.data.model.ListingTier.HUMAN -> "Makanan siap santap"
                    com.sisaguna.android.data.model.ListingTier.ANIMAL_FEED -> "Pakan ternak"
                    com.sisaguna.android.data.model.ListingTier.COMPOST -> "Kompos"
                },
                expiresAt = voucher.expiresAt.toString(),
                quota = voucher.quota,
                terms = voucher.terms,
                accentHex = voucher.accentHex,
            ),
        )
    }.toAdminVoucher()

    override suspend fun updateVoucher(code: String, patch: VoucherPatch): AdminVoucher = apiData {
        api.patchVoucher(
            code,
            PatchVoucherBody(
                title = patch.title, description = patch.description, minSpend = patch.minSpend,
                expiresAt = patch.expiresAt?.toString(), quota = patch.quota, isActive = patch.isActive,
                terms = patch.terms, accentHex = patch.accentHex,
            ),
        )
    }.toAdminVoucher()
}
