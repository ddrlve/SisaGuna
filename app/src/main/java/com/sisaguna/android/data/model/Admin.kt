package com.sisaguna.android.data.model

import java.time.Instant

/** One page of an admin list; [totalPages] comes from the server so the screen knows whether to offer "Muat lagi". */
data class AdminPage<T>(val items: List<T>, val page: Int, val totalPages: Int) {
    val hasMore: Boolean get() = page < totalPages
}

/** A store in the admin review queue. */
data class AdminMerchant(
    val id: String,
    val name: String,
    val status: MerchantStatus,
    val address: String,
    val ownerName: String,
    val ownerEmail: String,
    val createdAt: Instant,
)

/** A buyer complaint with the context an admin needs to decide it. [photoUrls] are private (need the user's token). */
data class AdminComplaint(
    val id: String,
    val orderCode: String,
    val merchantName: String,
    val buyerName: String,
    val reason: ComplaintReason,
    val detail: String,
    val status: ComplaintStatus,
    val resolutionNote: String?,
    val photoUrls: List<String>,
    val createdAt: Instant,
) {
    /** Resolved and rejected complaints are closed: the server refuses further changes. */
    val isClosed: Boolean get() = status == ComplaintStatus.RESOLVED || status == ComplaintStatus.REJECTED
}

data class AdminVoucher(
    val code: String,
    val title: String,
    val description: String,
    val percentOff: Int?,
    val flatOff: Int?,
    val maxDiscount: Int?,
    val minSpend: Int,
    val appliesToTier: ListingTier?,
    val expiresAt: Instant,
    val quota: Int?,
    val usedCount: Int,
    val terms: List<String>,
    val accentHex: String,
    val isActive: Boolean,
) {
    val isExpired: Boolean get() = expiresAt.isBefore(Instant.now())
}

/** Everything the create form collects. Exactly one of [percentOff] / [flatOff] is set. */
data class NewVoucher(
    val code: String,
    val title: String,
    val description: String,
    val percentOff: Int?,
    val flatOff: Int?,
    val maxDiscount: Int?,
    val minSpend: Int,
    val appliesToTier: ListingTier?,
    val expiresAt: Instant,
    val quota: Int?,
    val terms: List<String>,
    val accentHex: String,
)

/** The fields an admin can change afterwards (the kind and size of the discount are fixed). Null = unchanged. */
data class VoucherPatch(
    val title: String? = null,
    val description: String? = null,
    val minSpend: Int? = null,
    val expiresAt: Instant? = null,
    val quota: Int? = null,
    val isActive: Boolean? = null,
    val terms: List<String>? = null,
    val accentHex: String? = null,
)
