package com.sisaguna.android.data.remote

import java.io.IOException
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Answers a 401 by swapping the refresh token for a new pair and replaying the request once.
 *
 * The backend rotates refresh tokens and treats a reused one as theft (whole session revoked),
 * so every refresh runs under one lock: a request that lost the race sees the already-rotated
 * access token and simply retries with it.
 *
 * [refresh] returns the new tokens, null when the server rejected the refresh token, and throws
 * [IOException] when the answer is unknown (offline, 5xx) — then the session is kept.
 */
class TokenAuthenticator(
    private val tokens: TokenStore,
    private val refresh: (refreshToken: String) -> Tokens?,
    private val onExpired: () -> Unit,
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val sent = response.request.header("Authorization")?.removePrefix("Bearer ") ?: return null
        if (response.priorResponseCount() >= 2) return null

        synchronized(lock) {
            val current = tokens.get() ?: return null
            if (current.access != sent) return response.request.withToken(current.access)

            val fresh = try {
                refresh(current.refresh)
            } catch (e: IOException) {
                return null
            }
            if (fresh == null) {
                tokens.clear()
                onExpired()
                return null
            }
            tokens.save(fresh)
            return response.request.withToken(fresh.access)
        }
    }

    private fun Request.withToken(access: String) = newBuilder().header("Authorization", "Bearer $access").build()

    private fun Response.priorResponseCount(): Int {
        var count = 0
        var prior = priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
