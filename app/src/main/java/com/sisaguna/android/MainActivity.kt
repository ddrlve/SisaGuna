package com.sisaguna.android

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.sisaguna.android.data.settings.AppSettingsRepository
import com.sisaguna.android.data.settings.ThemeMode
import com.sisaguna.android.navigation.SgNavGraph
import com.sisaguna.android.ui.i18n.SgLocale
import com.sisaguna.android.ui.theme.SgPalette
import com.sisaguna.android.ui.theme.SisaGunaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: AppSettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Seed before the first frame so a dark/English user never sees a light/Indonesian flash.
        settingsRepository.settings.value.let {
            SgLocale.language = it.language
            SgPalette.isDark = it.themeMode == ThemeMode.DARK
        }
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val dark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            LaunchedEffect(settings.language) { SgLocale.language = settings.language }
            SideEffect {
                SgPalette.isDark = dark
                // System bar icons follow the app's theme, not the phone's.
                val bars = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            SisaGunaTheme(darkTheme = dark) {
                val navController = rememberNavController()
                SgNavGraph(navController = navController)
            }
        }
    }
}
