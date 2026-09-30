package com.sisaguna.android.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.sisaguna.android.core.session.AuthStatus
import com.sisaguna.android.core.session.GuestGateSheet
import com.sisaguna.android.core.session.SessionViewModel
import com.sisaguna.android.feature.activity.ActivityScreen
import com.sisaguna.android.feature.auth.LandingScreen
import com.sisaguna.android.feature.auth.LoginScreen
import com.sisaguna.android.feature.auth.RegisterScreen
import com.sisaguna.android.feature.category.CategoryListScreen
import com.sisaguna.android.feature.home.HomeScreen
import com.sisaguna.android.feature.splash.SplashScreen
import com.sisaguna.android.ui.components.ComingSoonScreen
import com.sisaguna.android.ui.theme.SgColor

/** Routes that require an authenticated session — a guest tapping one of these sees
 * [GuestGateSheet] instead of navigating. Upload is gated the same way but isn't a route (it's
 * a CTA inside Home), so it's checked separately in [HomeScreen]'s `onUploadClick`. */
private val gatedRoutes = setOf(Screen.Activity.route, Screen.Saved.route, Screen.Profile.route)

@Composable
fun SgNavGraph(navController: NavHostController) {
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val authStatus by sessionViewModel.status.collectAsStateWithLifecycle()
    var showGuestGate by remember { mutableStateOf(false) }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    // Category List is pushed from within the Home tab, so it keeps the bottom nav visible
    // with "Beranda" still highlighted rather than showing no active tab.
    val isCategoryRoute = currentRoute?.startsWith("category/") == true
    val showBottomNav = currentRoute in mainRoutes || isCategoryRoute
    val bottomNavRoute = if (isCategoryRoute) Screen.Home.route else currentRoute

    fun navigateOrGate(screen: Screen) {
        if (authStatus == AuthStatus.GUEST && screen.route in gatedRoutes) {
            showGuestGate = true
        } else {
            navController.navigate(screen.route) {
                popUpTo(Screen.Home.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // Login/Register can be reached either from Landing (Landing still on the back stack) or
    // from the guest gate opened on top of Home (Landing already popped, Home is what's on the
    // stack instead) — popUpTo(Screen.Landing.route) only clears the first path. Clearing the
    // whole graph unconditionally handles both without leaving a stale Login/Register entry
    // that "Back" from the new Home would reopen.
    fun navigateHomeAfterAuth() {
        navController.navigate(Screen.Home.route) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // Splash is full-bleed Brand500 (see SplashScreen) — without this, the top status-bar
        // inset applied below is filled by the Scaffold's default container color instead
        // (MaterialTheme.colorScheme.background), showing as a grey band above the green.
        containerColor = if (currentRoute == Screen.Splash.route) SgColor.Brand500 else MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomNav) {
                SgBottomNav(
                    currentRoute = bottomNavRoute,
                    onNavigate = ::navigateOrGate,
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(
                top = padding.calculateTopPadding(),
                bottom = if (showBottomNav) padding.calculateBottomPadding() else 0.dp,
            ),
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onTimeout = {
                        navController.navigate(Screen.Landing.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(Screen.Landing.route) {
                LandingScreen(
                    onLoginClick = { navController.navigate(Screen.Login.route) },
                    onGetStartedClick = { navController.navigate(Screen.Register.route) },
                    onGuestClick = {
                        sessionViewModel.continueAsGuest()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Landing.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        sessionViewModel.login()
                        navigateHomeAfterAuth()
                    },
                    onRegisterClick = { navController.navigate(Screen.Register.route) },
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterComplete = {
                        sessionViewModel.login()
                        navigateHomeAfterAuth()
                    },
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onCategoryClick = { tier -> navController.navigate(Screen.CategoryList.routeFor(tier)) },
                    onUploadClick = {
                        if (authStatus == AuthStatus.GUEST) {
                            showGuestGate = true
                        }
                        // else: opens the create-listing flow — not built yet (sub-project 4).
                    },
                )
            }
            composable(
                route = Screen.CategoryList.route,
                arguments = listOf(navArgument(Screen.CategoryList.ARG_TIER) { type = NavType.StringType }),
            ) {
                CategoryListScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Activity.route) { ActivityScreen() }
            composable(Screen.Saved.route) { ComingSoonScreen(title = "Saved") }
            composable(Screen.Profile.route) { ComingSoonScreen(title = "Profile") }
        }
    }

    if (showGuestGate) {
        GuestGateSheet(
            onDismiss = { showGuestGate = false },
            onLoginClick = {
                showGuestGate = false
                navController.navigate(Screen.Login.route)
            },
            onRegisterClick = {
                showGuestGate = false
                navController.navigate(Screen.Register.route)
            },
        )
    }
}
