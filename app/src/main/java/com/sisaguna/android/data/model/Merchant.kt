package com.sisaguna.android.data.model

enum class MerchantStatus { PENDING, APPROVED, REJECTED }

data class Merchant(
    val id: String,
    val name: String,
    val isVerified: Boolean,
    val status: MerchantStatus,
    val location: String,
)
