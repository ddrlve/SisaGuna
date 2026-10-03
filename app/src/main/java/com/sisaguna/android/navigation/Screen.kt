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

    /** Figma 273:12143 — one saved merchant's listings. Nested under "saved/" so the Saved tab
     * stays lit. */
    data object MerchantDetail : Screen("saved/merchant/{merchantId}") {
        const val ARG_ID = "merchantId"
        fun routeFor(id: String) = "saved/merchant/$id"
    }

    /** Figma 273:12569. */
    data object EditProfile : Screen("profile/edit")

    data object NotificationSettings : Screen("profile/notifications")

    /** Inbox opened from the Home bell. Full-screen: no bottom nav. */
    data object Notifications : Screen("notifications")

    data object ListingDetail : Screen("listing/{listingId}") {
        const val ARG_ID = "listingId"
        fun routeFor(id: String) = "listing/$id"
    }

    /** A merchant's public catalog, opened from a listing's merchant row. */
    data object Store : Screen("store/{merchantId}") {
        fun routeFor(id: String) = "store/$id"
    }

    data object Checkout : Screen("checkout")

    data object OrderDetail : Screen("order/{orderId}?justPlaced={justPlaced}") {
        const val ARG_ID = "orderId"
        const val ARG_JUST_PLACED = "justPlaced"
        fun routeFor(id: String, justPlaced: Boolean = false) = "order/$id?justPlaced=$justPlaced"
    }

    /** Opened as Siap santap or Pakan & kompos — the two upload forms differ (portion vs weight). */
    data object Upload : Screen("upload?tier={tier}") {
        const val ARG_TIER = "tier"
        fun routeFor(tier: ListingTier = ListingTier.HUMAN) = "upload?tier=${tier.name}"
    }

    /** Language, theme, buyer/seller mode. */
    data object Settings : Screen("profile/settings")

    // Profile sub-pages (3b). Nested under profile/ so the Profile tab stays lit.
    data object Addresses : Screen("profile/addresses")
    data object RescueHistory : Screen("profile/history")
    data object PaymentMethods : Screen("profile/payments")
    data object ChangePassword : Screen("profile/password")
    data object Help : Screen("profile/help")
    data object Privacy : Screen("profile/privacy")
    data object MyCatalog : Screen("profile/catalog")
}

/** First path segment of every route that shows the bottom nav. Nested routes ("saved/…",
 * "profile/…", "category/…") keep their parent tab lit. */
private val bottomNavSegments = setOf("home", "activity", "saved", "profile", "category")

fun showsBottomNav(route: String?): Boolean = route?.substringBefore('/') in bottomNavSegments

/** The bottom-nav tab route to highlight for [route]; Category List belongs to Home. */
fun bottomNavTabFor(route: String?): String? = when (val segment = route?.substringBefore('/')) {
    "category" -> Screen.Home.route
    null -> null
    else -> segment
}
