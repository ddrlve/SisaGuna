package com.sisaguna.android.navigation

import com.sisaguna.android.ui.components.SgEaseOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import com.sisaguna.android.feature.address.AddressViewModel
import com.sisaguna.android.feature.address.LocationPickerSheet
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sisaguna.android.R
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
import com.sisaguna.android.feature.activity.OrderDetailScreen
import com.sisaguna.android.feature.checkout.CheckoutScreen
import com.sisaguna.android.feature.listing.ListingDetailScreen
import com.sisaguna.android.feature.auth.LandingScreen
import com.sisaguna.android.feature.auth.LoginScreen
import com.sisaguna.android.feature.auth.RegisterScreen
import com.sisaguna.android.feature.category.CategoryListScreen
import com.sisaguna.android.feature.home.HomeScreen
import com.sisaguna.android.feature.notifications.NotificationSettingsScreen
import com.sisaguna.android.feature.notifications.NotificationsScreen
import com.sisaguna.android.feature.profile.EditProfileScreen
import com.sisaguna.android.feature.profile.AddressesScreen
import com.sisaguna.android.feature.profile.ChangePasswordScreen
import com.sisaguna.android.feature.profile.HelpScreen
import com.sisaguna.android.feature.profile.MyCatalogScreen
import com.sisaguna.android.feature.profile.PaymentMethodsScreen
import com.sisaguna.android.feature.profile.PrivacyScreen
import com.sisaguna.android.feature.profile.ProfileScreen
import com.sisaguna.android.feature.profile.RescueHistoryScreen
import com.sisaguna.android.feature.upload.UploadScreen
import com.sisaguna.android.feature.saved.MerchantDetailScreen
import com.sisaguna.android.feature.saved.SavedScreen
import com.sisaguna.android.feature.splash.SplashScreen
import com.sisaguna.android.ui.theme.SgColor

/** Routes that require an authenticated session — a guest tapping one of these sees
 * [GuestGateSheet] instead of navigating. Upload is gated the same way but isn't a route (it's
 * a CTA inside Home), so it's checked separately in [HomeScreen]'s `onUploadClick`. */
private val gatedRoutes = setOf(Screen.Activity.route, Screen.Saved.route, Screen.Profile.route, Screen.Notifications.route)

/** Set on Profile's back-stack entry by Edit Profile so Profile can show "Profil diperbarui". */
private const val KEY_PROFILE_UPDATED = "profile_updated"

/** Free-text confirmation for Profile's snackbar from sub-pages (e.g. password changed). */
private const val KEY_PROFILE_MESSAGE = "profile_message"

private val tabRoutes = setOf(Screen.Home.route, Screen.Activity.route, Screen.Saved.route, Screen.Profile.route)

/** Tab swaps and the splash hand-off crossfade; everything else is a push. */
private fun isTabSwitch(from: String?, to: String?): Boolean =
    (from in tabRoutes && to in tabRoutes) || from == Screen.Splash.route

@Composable
fun SgNavGraph(navController: NavHostController) {
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val authStatus by sessionViewModel.status.collectAsStateWithLifecycle()
    var showGuestGate by remember { mutableStateOf(false) }
    val addressViewModel: AddressViewModel = hiltViewModel()
    val addressState by addressViewModel.uiState.collectAsStateWithLifecycle()
    var showLocationSheet by remember { mutableStateOf(false) }
    // Frosted backdrop: blur the app behind the location sheet instead of a black scrim.
    // RenderEffect blur needs API 31+; older devices just get the light scrim.
    val backdropBlur by animateDpAsState(if (showLocationSheet) 14.dp else 0.dp, tween(220), label = "backdropBlur")

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    // Nested routes (category/…, saved/merchant/…, profile/edit) keep the bottom nav visible
    // with their parent tab lit — see showsBottomNav / bottomNavTabFor in Screen.kt.
    val showBottomNav = showsBottomNav(currentRoute)
    val bottomNavRoute = bottomNavTabFor(currentRoute)

    fun openListing(id: String) = navController.navigate(Screen.ListingDetail.routeFor(id))
    fun openStore(id: String) = navController.navigate(Screen.Store.routeFor(id)) { launchSingleTop = true }
    fun openCart() {
        if (authStatus == AuthStatus.GUEST) showGuestGate = true
        else navController.navigate(Screen.Checkout.route) { launchSingleTop = true }
    }

    fun openUpload(tier: com.sisaguna.android.data.model.ListingTier = com.sisaguna.android.data.model.ListingTier.HUMAN) {
        if (authStatus == AuthStatus.GUEST) showGuestGate = true
        else navController.navigate(Screen.Upload.routeFor(tier)) { launchSingleTop = true }
    }

    fun navigateOrGate(screen: Screen) {
        if (authStatus == AuthStatus.GUEST && screen.route in gatedRoutes) {
            showGuestGate = true
        } else if (screen == Screen.Home && navController.popBackStack(Screen.Home.route, inclusive = false)) {
            // Home is the root tab: when it's already on the stack, just pop back to it.
            // navigate(Home) { popUpTo(Home) { saveState } ; restoreState } left the user on
            // Activity after checkout, because Home's own saved state was restored over it.
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
        // Default Navigation transitions are a 700ms crossfade, which made tab taps feel laggy.
        // Tabs now swap with a quick 150ms fade (no movement: they're siblings, not a stack);
        // pushed screens slide a short distance in from the right with the strong ease-out and
        // pop back the same way they came (spatial consistency).
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            enterTransition = {
                if (isTabSwitch(initialState.destination.route, targetState.destination.route)) fadeIn(tween(150))
                else slideInHorizontally(tween(300, easing = SgEaseOut)) { it / 6 } + fadeIn(tween(220, easing = SgEaseOut))
            },
            exitTransition = {
                if (isTabSwitch(initialState.destination.route, targetState.destination.route)) fadeOut(tween(100))
                else slideOutHorizontally(tween(300, easing = SgEaseOut)) { -it / 12 } + fadeOut(tween(160))
            },
            popEnterTransition = {
                if (isTabSwitch(initialState.destination.route, targetState.destination.route)) fadeIn(tween(150))
                else slideInHorizontally(tween(300, easing = SgEaseOut)) { -it / 12 } + fadeIn(tween(220, easing = SgEaseOut))
            },
            popExitTransition = {
                if (isTabSwitch(initialState.destination.route, targetState.destination.route)) fadeOut(tween(100))
                else slideOutHorizontally(tween(240, easing = SgEaseOut)) { it / 6 } + fadeOut(tween(160))
            },
            modifier = Modifier
                .padding(
                    top = padding.calculateTopPadding(),
                    bottom = if (showBottomNav) padding.calculateBottomPadding() else 0.dp,
                )
                .then(if (backdropBlur > 0.dp) Modifier.blur(backdropBlur) else Modifier),
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
                    onListingClick = { openListing(it.id) },
                    onCategoryClick = { tier -> navController.navigate(Screen.CategoryList.routeFor(tier)) },
                    onUploadClick = { openUpload() },
                    onUploadTierClick = { tier -> openUpload(tier) },
                    onCartClick = ::openCart,
                    onMerchantClick = ::openStore,
                    onMyCatalogClick = { navController.navigate(Screen.MyCatalog.route) { launchSingleTop = true } },
                    onNotificationsClick = {
                        if (authStatus == AuthStatus.GUEST) {
                            showGuestGate = true
                        } else {
                            navController.navigate(Screen.Notifications.route) { launchSingleTop = true }
                        }
                    },
                    showNotificationBadge = authStatus == AuthStatus.AUTHENTICATED,
                    addressLabel = addressState.chipLabel,
                    onAddressClick = { showLocationSheet = true },
                )
            }
            composable(
                route = Screen.CategoryList.route,
                arguments = listOf(navArgument(Screen.CategoryList.ARG_TIER) { type = NavType.StringType }),
            ) {
                CategoryListScreen(
                    onBack = { navController.popBackStack() },
                    onListingClick = { openListing(it.id) },
                )
            }
            composable(Screen.Activity.route) {
                ActivityScreen(
                    onOrderClick = { id -> navController.navigate(Screen.OrderDetail.routeFor(id)) },
                    onExplore = { navigateOrGate(Screen.Home) },
                )
            }
            composable(
                route = Screen.ListingDetail.route,
                arguments = listOf(navArgument(Screen.ListingDetail.ARG_ID) { type = NavType.StringType }),
            ) {
                ListingDetailScreen(
                    onBack = { navController.popBackStack() },
                    onMerchantClick = ::openStore,
                    onListingClick = ::openListing,
                    onCartClick = ::openCart,
                )
            }
            composable(
                route = Screen.Store.route,
                arguments = listOf(navArgument(Screen.MerchantDetail.ARG_ID) { type = NavType.StringType }),
            ) {
                MerchantDetailScreen(
                    onBack = { navController.popBackStack() },
                    onListingClick = ::openListing,
                    onCartClick = ::openCart,
                )
            }
            composable(Screen.Checkout.route) {
                CheckoutScreen(
                    onBack = { navController.popBackStack() },
                    onExplore = { navigateOrGate(Screen.Home) },
                    onPlaced = { orderId ->
                        // Replace checkout (and the listing pages under it) with the order, so
                        // Back from the order lands on Activity instead of an empty cart.
                        navController.navigate(Screen.Activity.route) {
                            popUpTo(Screen.Home.route) { saveState = false }
                            launchSingleTop = true
                        }
                        navController.navigate(Screen.OrderDetail.routeFor(orderId, justPlaced = true))
                    },
                )
            }
            composable(
                route = Screen.OrderDetail.route,
                arguments = listOf(
                    navArgument(Screen.OrderDetail.ARG_ID) { type = NavType.StringType },
                    navArgument(Screen.OrderDetail.ARG_JUST_PLACED) {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                ),
            ) {
                OrderDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Saved.route) {
                SavedScreen(
                    onMerchantClick = { id -> navController.navigate(Screen.MerchantDetail.routeFor(id)) },
                    onExploreClick = { navigateOrGate(Screen.Home) },
                )
            }
            composable(
                route = Screen.MerchantDetail.route,
                arguments = listOf(navArgument(Screen.MerchantDetail.ARG_ID) { type = NavType.StringType }),
            ) {
                MerchantDetailScreen(
                    onBack = { navController.popBackStack() },
                    onListingClick = ::openListing,
                    onCartClick = ::openCart,
                    title = stringResource(R.string.saved_title),
                )
            }
            composable(Screen.Profile.route) { entry ->
                val updated by entry.savedStateHandle
                    .getStateFlow(KEY_PROFILE_UPDATED, false)
                    .collectAsStateWithLifecycle()
                val message by entry.savedStateHandle
                    .getStateFlow<String?>(KEY_PROFILE_MESSAGE, null)
                    .collectAsStateWithLifecycle()
                fun go(screen: Screen) = navController.navigate(screen.route) { launchSingleTop = true }
                ProfileScreen(
                    onEditProfile = { go(Screen.EditProfile) },
                    onNotificationSettings = { go(Screen.NotificationSettings) },
                    onAddresses = { go(Screen.Addresses) },
                    onHistory = { go(Screen.RescueHistory) },
                    onPayments = { go(Screen.PaymentMethods) },
                    onPassword = { go(Screen.ChangePassword) },
                    onHelp = { go(Screen.Help) },
                    onPrivacy = { go(Screen.Privacy) },
                    onCatalog = { go(Screen.MyCatalog) },
                    onSettings = { go(Screen.Settings) },
                    onListingClick = ::openListing,
                    onLogout = {
                        sessionViewModel.logout()
                        // Clear everything so Back on Landing exits instead of returning to a
                        // Profile the guest can no longer see.
                        navController.navigate(Screen.Landing.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                    profileUpdated = updated,
                    onProfileUpdatedShown = { entry.savedStateHandle[KEY_PROFILE_UPDATED] = false },
                    message = message,
                    onMessageShown = { entry.savedStateHandle[KEY_PROFILE_MESSAGE] = null },
                )
            }
            composable(Screen.EditProfile.route) {
                EditProfileScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        navController.previousBackStackEntry?.savedStateHandle?.set(KEY_PROFILE_UPDATED, true)
                        navController.popBackStack()
                    },
                )
            }
            composable(
                route = Screen.Upload.route,
                arguments = listOf(navArgument(Screen.Upload.ARG_TIER) { type = NavType.StringType; defaultValue = "HUMAN" }),
            ) {
                UploadScreen(
                    onBack = { navController.popBackStack() },
                    onPublished = { id ->
                        navController.popBackStack()
                        openListing(id)
                    },
                )
            }
            composable(Screen.Addresses.route) { AddressesScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.RescueHistory.route) {
                RescueHistoryScreen(
                    onBack = { navController.popBackStack() },
                    onOrderClick = { navController.navigate(Screen.OrderDetail.routeFor(it)) },
                )
            }
            composable(Screen.PaymentMethods.route) { PaymentMethodsScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.ChangePassword.route) {
                ChangePasswordScreen(
                    onBack = { navController.popBackStack() },
                    onDone = {
                        navController.previousBackStackEntry?.savedStateHandle?.set(KEY_PROFILE_MESSAGE, "Password berhasil diganti")
                        navController.popBackStack()
                    },
                )
            }
            composable(Screen.Settings.route) {
                com.sisaguna.android.feature.settings.SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Help.route) { HelpScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.Privacy.route) { PrivacyScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.MyCatalog.route) {
                MyCatalogScreen(
                    onBack = { navController.popBackStack() },
                    onUpload = { openUpload() },
                    onListingClick = ::openListing,
                )
            }
            composable(Screen.NotificationSettings.route) {
                NotificationSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Notifications.route) {
                NotificationsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenSettings = { navController.navigate(Screen.NotificationSettings.route) { launchSingleTop = true } },
                )
            }
        }
    }

    if (showLocationSheet) {
        LocationPickerSheet(
            state = addressState,
            onSelectAddress = {
                addressViewModel.select(it)
                showLocationSheet = false
            },
            onUseCurrentLocation = {
                addressViewModel.useCurrentLocation(it)
                showLocationSheet = false
            },
            onSaveAddress = { id, label, place, note ->
                addressViewModel.save(id, label, place, note)
                showLocationSheet = false
            },
            onDeleteAddress = addressViewModel::delete,
            onDismiss = { showLocationSheet = false },
        )
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
