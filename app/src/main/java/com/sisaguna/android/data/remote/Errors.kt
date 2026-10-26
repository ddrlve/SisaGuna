package com.sisaguna.android.data.remote

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/** Everything the data layer throws, so a ViewModel catches one type and shows [userMessage]. */
sealed class AppException(val userMessage: String, cause: Throwable? = null) : Exception(userMessage, cause)

class NetworkException(cause: Throwable) :
    AppException("Tidak bisa terhubung ke server. Periksa koneksi internet lalu coba lagi.", cause)

/** The photo/video the user picked can't be read any more (deleted, permission revoked, undecodable). */
class MediaReadException(cause: Throwable) : AppException("Foto atau video tidak bisa dibaca. Pilih ulang filenya.", cause)

/** An action that needs an account was attempted as a guest. */
class NotSignedInException : AppException("Masuk dulu untuk melanjutkan.")

/** A non-2xx answer from the backend: `{"error":{"code","message",...extras}}`. */
class ApiException(
    val status: Int,
    val code: String,
    serverMessage: String,
    /** Extra fields next to `code`/`message`, e.g. `attempts_left`, `reason`, or the new `total` of `price_changed`. */
    val extras: JsonObject = JsonObject(emptyMap()),
) : AppException(friendlyMessage(status, code, serverMessage, extras)) {
    fun int(key: String): Int? = (extras[key] as? JsonPrimitive)?.intOrNull
    fun string(key: String): String? = (extras[key] as? JsonPrimitive)?.contentOrNull

    companion object {
        // The backend already answers in Indonesian; only override what a user can't act on as-is.
        private fun friendlyMessage(status: Int, code: String, serverMessage: String, extras: JsonObject): String = when {
            code == "rate_limited" -> "Terlalu banyak percobaan. Tunggu sebentar lalu coba lagi."
            code == "unauthorized" || code == "invalid_refresh_token" -> "Sesi berakhir. Silakan masuk lagi."
            code == "delivery_unavailable" -> when ((extras["reason"] as? JsonPrimitive)?.contentOrNull) {
                "too_far" -> "Toko terlalu jauh untuk diantar kurir (maks. 15 km). Pilih ambil sendiri."
                "merchant_pickup_only" -> "Toko ini hanya melayani ambil sendiri."
                "address_required" -> "Pilih alamat tujuan dulu untuk pengantaran."
                else -> "Pengantaran sedang tidak tersedia. Pilih ambil sendiri."
            }
            code == "payment_method_not_linked" -> "E-wallet belum ditautkan. Tautkan dulu di Profil, Metode Pembayaran."
            status >= 500 -> "Server sedang bermasalah. Coba lagi beberapa saat."
            serverMessage.isNotBlank() -> serverMessage.replaceFirstChar { it.uppercaseChar() }
            else -> "Terjadi kesalahan ($code)."
        }
    }
}
