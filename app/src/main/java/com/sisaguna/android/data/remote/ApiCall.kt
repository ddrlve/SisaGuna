package com.sisaguna.android.data.remote

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

@Serializable
data class Envelope<T>(val data: T, val meta: Meta? = null)

@Serializable
data class Meta(
    val nextCursor: String? = null,
    val total: Int? = null,
    val totalPages: Int? = null,
    /** Message pages only: the last of my messages the other side has read. */
    val peerLastReadId: Long? = null,
)

/**
 * Runs one Retrofit call and maps every failure to an [AppException]. [CancellationException] is
 * rethrown untouched so structured concurrency keeps working.
 */
suspend fun <T> apiCall(json: Json = ApiJson, block: suspend () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    throw e.toApiException(json)
} catch (e: IOException) {
    throw NetworkException(e)
} catch (e: SerializationException) {
    throw ApiException(-1, "bad_response", "Respons server tidak dikenali.")
}

suspend fun <T> apiData(json: Json = ApiJson, block: suspend () -> Envelope<T>): T = apiCall(json) { block().data }

internal fun HttpException.toApiException(json: Json): ApiException {
    val body = runCatching { response()?.errorBody()?.string() }.getOrNull().orEmpty()
    val error = runCatching { json.parseToJsonElement(body).jsonObject["error"]?.jsonObject }.getOrNull()
    val code = error?.get("code")?.jsonPrimitive?.contentOrNull ?: "http_${code()}"
    val message = error?.get("message")?.jsonPrimitive?.contentOrNull.orEmpty()
    val extras = JsonObject(error.orEmpty().filterKeys { it != "code" && it != "message" })
    return ApiException(code(), code, message, extras)
}
