package com.sisaguna.android

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.sisaguna.android.navigation.SgNavGraph
import com.sisaguna.android.ui.theme.SisaGunaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always light (see SisaGunaTheme), so system bar icons must be dark even when
        // the phone is in dark mode — the default style followed the system and drew white icons
        // on the light page.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            SisaGunaTheme {
                val navController = rememberNavController()
                SgNavGraph(navController = navController)
            }
        }
    }
}
