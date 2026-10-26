package com.sisaguna.android.data.remote

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

/** The backend speaks snake_case; DTO properties stay camelCase and the strategy bridges them. */
@OptIn(ExperimentalSerializationApi::class)
val ApiJson: Json = Json {
    namingStrategy = JsonNamingStrategy.SnakeCase
    ignoreUnknownKeys = true
    explicitNulls = false
    // PUT /me/notification-prefs needs all six switches even when a value equals its default.
    encodeDefaults = true
    coerceInputValues = true
}
