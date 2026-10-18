package com.sisaguna.android.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdminMerchantDto(
    val id: String,
    val name: String,
    val status: String,
    val address: String = "",
    val ownerName: String = "",
    val ownerEmail: String = "",
    val createdAt: String,
)

@Serializable
data class AdminComplaintDto(
    val id: String,
    val orderCode: String = "",
    val merchantName: String = "",
    val buyerName: String = "",
    val reason: String,
    val detail: String = "",
    val status: String = "submitted",
    val resolutionNote: String? = null,
    val photoUrls: List<String> = emptyList(),
    val createdAt: String,
)

@Serializable
data class UpdateComplaintBody(val status: String, val resolutionNote: String = "")

@Serializable
data class AdminVoucherDto(
    val code: String,
    val title: String,
    val description: String = "",
    val percentOff: Int? = null,
    val flatOff: Int? = null,
    val maxDiscount: Int? = null,
    val minSpend: Int = 0,
    val appliesToTier: String? = null,
    val expiresAt: String,
    val quota: Int? = null,
    val usedCount: Int = 0,
    val terms: List<String> = emptyList(),
    val accentHex: String = "",
    val isActive: Boolean = true,
)

@Serializable
data class CreateVoucherBody(
    val code: String,
    val title: String,
    val description: String,
    val percentOff: Int? = null,
    val flatOff: Int? = null,
    val maxDiscount: Int? = null,
    val minSpend: Int,
    val appliesToTier: String? = null,
    val appliesTo: String = "",
    val expiresAt: String,
    val quota: Int? = null,
    val terms: List<String> = emptyList(),
    val accentHex: String,
)

/** Null fields are left out of the JSON (explicitNulls = false), which the server reads as "unchanged". */
@Serializable
data class PatchVoucherBody(
    val title: String? = null,
    val description: String? = null,
    val minSpend: Int? = null,
    val expiresAt: String? = null,
    val quota: Int? = null,
    val isActive: Boolean? = null,
    val terms: List<String>? = null,
    val accentHex: String? = null,
)
