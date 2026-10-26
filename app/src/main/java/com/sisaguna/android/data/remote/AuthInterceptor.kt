package com.sisaguna.android.data.remote

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * Adds `Authorization: Bearer` to API calls only. The check on [apiHost] keeps the token away
 * from any other host (e.g. image CDNs) that shares this OkHttp client.
 */
class AuthInterceptor(
    private val tokens: TokenStore,
    private val apiHost: String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val access = tokens.get()?.access
        if (access == null || request.url.host != apiHost || request.header("Authorization") != null || request.isCredentialCall()) {
            return chain.proceed(request)
        }
        return chain.proceed(request.newBuilder().header("Authorization", "Bearer $access").build())
    }

    private companion object {
        // Calls made before there is a session: they must never carry (or trigger a refresh of) a token.
        val CREDENTIAL_PATHS = listOf("/auth/login", "/auth/register", "/auth/refresh", "/auth/forgot-password", "/auth/reset-password")
    }

    private fun Request.isCredentialCall(): Boolean {
        val path = url.encodedPath
        return CREDENTIAL_PATHS.any { path.endsWith(it) }
    }
}
