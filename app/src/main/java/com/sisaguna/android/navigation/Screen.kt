package com.sisaguna.android.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
}
