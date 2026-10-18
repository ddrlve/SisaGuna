package com.sisaguna.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.sisaguna.android.data.repository.AuthRepository
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.sisaguna.android.core.ui.AppMessenger
import com.sisaguna.android.data.remote.RealtimeClient
import com.sisaguna.android.data.settings.AppSettingsRepository
import com.sisaguna.android.data.settings.ThemeMode
import com.sisaguna.android.navigation.SgNavGraph
import com.sisaguna.android.ui.i18n.SgLocale
import com.sisaguna.android.ui.i18n.SgSnackbarHost
import com.sisaguna.android.ui.theme.SgPalette
import com.sisaguna.android.ui.theme.SisaGunaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: AppSettingsRepository
    @Inject lateinit var messenger: AppMessenger
    @Inject lateinit var realtime: RealtimeClient
    @Inject lateinit var auth: AuthRepository

    /** Bumped each time a push notification opens the app; the nav graph consumes it. */
    private var pushTaps by mutableIntStateOf(0)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // Open only while visible; while the app is closed the server falls back to push.
    override fun onStart() {
        super.onStart()
        realtime.start()
    }

    override fun onStop() {
        realtime.stop()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.isPushTap()) pushTaps++
    }

    /** FCM puts the payload of a tapped system notification into the launch intent's extras. */
    private fun Intent?.isPushTap() = this?.hasExtra("notification_id") == true

    /** Android 13+ needs a runtime grant for pushes. Asked once, right after the first sign-in (a moment
     * the user expects order updates), and never again; the settings screen can reopen the system page. */
    private fun askNotificationPermissionAfterSignIn() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                auth.isAuthenticated.collect { signedIn ->
                    val granted = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
                    if (signedIn && !granted && !settingsRepository.settings.value.notificationPermissionAsked) {
                        settingsRepository.setNotificationPermissionAsked()
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Seed before the first frame so a dark/English user never sees a light/Indonesian flash.
        settingsRepository.settings.value.let {
            SgLocale.language = it.language
            SgPalette.isDark = it.themeMode == ThemeMode.DARK
        }
        if (intent.isPushTap()) pushTaps++
        askNotificationPermissionAfterSignIn()
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
                // App-wide messages (failed saves, expired session) float above whichever screen is open.
                val snackbar = remember { SnackbarHostState() }
                LaunchedEffect(Unit) {
                    messenger.messages.collect {
                        snackbar.currentSnackbarData?.dismiss()
                        snackbar.showSnackbar(it)
                    }
                }
                Box(Modifier.fillMaxSize()) {
                    SgNavGraph(navController = navController, pushTaps = pushTaps)
                    SgSnackbarHost(
                        snackbar,
                        Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 64.dp),
                    )
                }
            }
        }
    }
}
