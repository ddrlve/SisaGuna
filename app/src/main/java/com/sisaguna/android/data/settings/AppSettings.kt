package com.sisaguna.android.data.settings

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class AppLanguage(val code: String, val nativeName: String) {
    ID("id", "Bahasa Indonesia"),
    EN("en", "English"),
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** User testing: merchants wanted their own home ("tampilan mitra dan user dibedakan"). One
 * account can buy and sell; this picks which home it lands on. */
enum class UserMode { BUYER, MERCHANT }

data class AppSettings(
    val language: AppLanguage = AppLanguage.ID,
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val mode: UserMode = UserMode.BUYER,
)

/**
 * Language + theme, persisted in SharedPreferences so they survive a restart. Two scalar
 * values don't justify pulling in DataStore.
 */
@Singleton
class AppSettingsRepository(
    /** null keeps settings in memory only (unit tests). */
    private val prefs: SharedPreferences?,
) {
    @Inject constructor(@ApplicationContext context: Context) :
        this(context.getSharedPreferences("sg_settings", Context.MODE_PRIVATE))

    private val _settings = MutableStateFlow(
        AppSettings(
            language = prefs?.getString(KEY_LANG, null)?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: AppLanguage.ID,
            themeMode = prefs?.getString(KEY_THEME, null)?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.LIGHT,
            mode = prefs?.getString(KEY_MODE, null)?.let { runCatching { UserMode.valueOf(it) }.getOrNull() } ?: UserMode.BUYER,
        ),
    )
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        prefs?.edit()?.putString(KEY_LANG, language.name)?.apply()
        _settings.value = _settings.value.copy(language = language)
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs?.edit()?.putString(KEY_THEME, mode.name)?.apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun setMode(mode: UserMode) {
        prefs?.edit()?.putString(KEY_MODE, mode.name)?.apply()
        _settings.value = _settings.value.copy(mode = mode)
    }

    private companion object {
        const val KEY_MODE = "mode"
        const val KEY_LANG = "language"
        const val KEY_THEME = "theme"
    }
}
