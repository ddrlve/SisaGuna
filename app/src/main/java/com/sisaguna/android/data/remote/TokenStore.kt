package com.sisaguna.android.data.remote

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class Tokens(val access: String, val refresh: String)

/** Where the session lives between launches. Kept behind an interface so JVM tests can fake it. */
interface TokenStore {
    fun get(): Tokens?
    fun save(tokens: Tokens)
    fun clear()
}

/** Tokens in [EncryptedSharedPreferences] (Keystore-backed), never in the plain `sg_settings` file. */
@Singleton
class EncryptedTokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : TokenStore {

    private val prefs: SharedPreferences by lazy { openPrefs() }

    @Volatile private var cached: Tokens? = null
    @Volatile private var loaded = false

    override fun get(): Tokens? {
        if (!loaded) {
            cached = prefs.getString(KEY_ACCESS, null)?.let { a ->
                prefs.getString(KEY_REFRESH, null)?.let { r -> Tokens(a, r) }
            }
            loaded = true
        }
        return cached
    }

    override fun save(tokens: Tokens) {
        prefs.edit().putString(KEY_ACCESS, tokens.access).putString(KEY_REFRESH, tokens.refresh).apply()
        cached = tokens
        loaded = true
    }

    override fun clear() {
        prefs.edit().clear().apply()
        cached = null
        loaded = true
    }

    private fun create(): SharedPreferences {
        val key = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(
            context, FILE, key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    // A restored/corrupted Keystore throws on open; losing the session beats crashing at launch.
    private fun openPrefs(): SharedPreferences = try {
        create()
    } catch (e: Exception) {
        context.deleteSharedPreferences(FILE)
        create()
    }

    private companion object {
        const val FILE = "sg_session"
        const val KEY_ACCESS = "access"
        const val KEY_REFRESH = "refresh"
    }
}
