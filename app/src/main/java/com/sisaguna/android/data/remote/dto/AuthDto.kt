package com.sisaguna.android.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val phone: String,
    val password: String,
    val accountType: String,
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class ForgotPasswordBody(val email: String)

@Serializable
data class ResetPasswordBody(val email: String, val code: String, val newPassword: String)

@Serializable
data class ChangePasswordRequest(val current: String, val new: String)

@Serializable
data class SessionDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: String? = null,
    val user: UserDto,
)

@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val phone: String = "",
    val avatarKey: String? = null,
    val photoUrl: String? = null,
    val locationLabel: String? = null,
    val selectedAddressId: String? = null,
    val isAdmin: Boolean = false,
    val createdAt: String? = null,
    val unreadNotifications: Int = 0,
)

@Serializable
data class AddressDto(
    val id: String,
    val label: String,
    val fullAddress: String,
    val note: String = "",
    val lat: Double,
    val lng: Double,
)

@Serializable
data class AddressRequest(
    val label: String,
    val fullAddress: String,
    val note: String = "",
    val lat: Double,
    val lng: Double,
)

@Serializable
data class MediaDto(
    val id: String,
    val kind: String = "image",
    val url: String,
    val thumbUrl: String? = null,
)

@Serializable
data class ImpactDto(
    val completedOrders: Int = 0,
    val portions: Int = 0,
    val feedCompostKg: Double = 0.0,
    val savedRupiah: Long = 0,
    val carbonKg: Double = 0.0,
    val tier: String = "starter",
)
