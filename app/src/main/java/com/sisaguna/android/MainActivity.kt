package com.sisaguna.android

import android.os.Bundle
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
        enableEdgeToEdge()
        setContent {
            SisaGunaTheme {
                val navController = rememberNavController()
                SgNavGraph(navController = navController)
            }
        }
    }
}
