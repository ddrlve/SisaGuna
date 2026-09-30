package com.sisaguna.android.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.sisaguna.android.feature.activity.ActivityScreen
import com.sisaguna.android.feature.auth.LandingScreen
import com.sisaguna.android.feature.auth.LoginScreen
import com.sisaguna.android.feature.auth.RegisterScreen
import com.sisaguna.android.feature.category.CategoryListScreen
import com.sisaguna.android.feature.home.HomeScreen
import com.sisaguna.android.ui.components.ComingSoonScreen

@Composable
fun SgNavGraph(navController: NavHostController) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    // Category List is pushed from within the Home tab, so it keeps the bottom nav visible
    // with "Beranda" still highlighted rather than showing no active tab.
    val isCategoryRoute = currentRoute?.startsWith("category/") == true
    val showBottomNav = currentRoute in mainRoutes || isCategoryRoute
    val bottomNavRoute = if (isCategoryRoute) Screen.Home.route else currentRoute

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomNav) {
                SgBottomNav(
                    currentRoute = bottomNavRoute,
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Landing.route,
            modifier = Modifier.padding(bottom = if (showBottomNav) padding.calculateBottomPadding() else 0.dp),
        ) {
            composable(Screen.Landing.route) {
                LandingScreen(
                    onLoginClick = { navController.navigate(Screen.Login.route) },
                    onGetStartedClick = { navController.navigate(Screen.Register.route) },
                )
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Landing.route) { inclusive = true }
                        }
                    },
                    onRegisterClick = { navController.navigate(Screen.Register.route) },
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    onContinue = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Landing.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onCategoryClick = { tier -> navController.navigate(Screen.CategoryList.routeFor(tier)) },
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
}
