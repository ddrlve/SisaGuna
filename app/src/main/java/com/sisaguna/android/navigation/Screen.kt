package com.sisaguna.android.navigation

import com.sisaguna.android.data.model.ListingTier

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Landing : Screen("landing")
    data object Login : Screen("login")
    data object Register : Screen("register")

    data object Home : Screen("home")
    data object Activity : Screen("activity")
    data object Saved : Screen("saved")
    data object Profile : Screen("profile")

    /** Figma node 45:9805 — grid of listings for one tier, opened from Home's category tiles
     * and "Lihat Semua" links. */
    data object CategoryList : Screen("category/{tier}") {
        const val ARG_TIER = "tier"
        fun routeFor(tier: ListingTier) = "category/${tier.name}"
    }
}

/** Routes that show the bottom nav bar. */
val mainRoutes = setOf(Screen.Home.route, Screen.Activity.route, Screen.Saved.route, Screen.Profile.route)
