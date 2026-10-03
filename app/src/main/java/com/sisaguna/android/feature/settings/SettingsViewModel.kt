package com.sisaguna.android.feature.settings

import androidx.lifecycle.ViewModel
import com.sisaguna.android.data.settings.AppLanguage
import com.sisaguna.android.data.settings.AppSettings
import com.sisaguna.android.data.settings.AppSettingsRepository
import com.sisaguna.android.data.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: AppSettingsRepository,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = repository.settings
    fun setLanguage(language: AppLanguage) = repository.setLanguage(language)
    fun setThemeMode(mode: ThemeMode) = repository.setThemeMode(mode)
    fun setMode(mode: com.sisaguna.android.data.settings.UserMode) = repository.setMode(mode)
}
