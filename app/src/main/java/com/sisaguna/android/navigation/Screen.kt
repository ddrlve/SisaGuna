package com.sisaguna.android.navigation

sealed class Screen(val route: String) {
    data object Landing : Screen("landing")
    data object Login : Screen("login")
    data object Register : Screen("register")

    data object Home : Screen("home")
    data object Activity : Screen("activity")
    data object Saved : Screen("saved")
    data object Profile : Screen("profile")
}

/** Routes that show the bottom nav bar. */
val mainRoutes = setOf(Screen.Home.route, Screen.Activity.route, Screen.Saved.route, Screen.Profile.route)
