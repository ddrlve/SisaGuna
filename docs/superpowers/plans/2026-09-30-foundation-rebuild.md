# Foundation Rebuild Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the app's entry flow — Splash, Landing, Login, Register — add a guest browsing mode, and fix the global notch/status-bar clipping bug, as sub-project 1 of 4 in the SisaGuna Figma-fidelity rebuild.

**Architecture:** Compose + Hilt, matching the existing feature-per-package layout. A new `core/session` package holds a small `SessionViewModel` (in-memory `GUEST`/`AUTHENTICATED` state, no backend — this repo is frontend-only) that `NavGraph.kt` reads to gate bottom-nav tabs and the Upload CTA behind a `GuestGateSheet`. `Splash` becomes its own route ahead of `Landing` instead of an overlay baked into `LandingScreen`.

**Tech Stack:** Kotlin, Jetpack Compose, Navigation-Compose, Hilt, kotlinx.coroutines (StateFlow). JUnit4 added for the first pure-logic unit tests in this repo.

**Spec:** `docs/superpowers/specs/2026-09-30-foundation-rebuild-design.md`

## Global Constraints

- Frontend-only repo — no real backend calls for login/register/guest state (see spec Context; also `memory/project_frontend_only_scope.md`). All auth is in-memory `SessionViewModel` state, reset on process death.
- Reuse existing design tokens (`SgColor`, `SgTextStyle`, `Shape.kt`, `SgLogo`) — do not introduce new color/spacing values that duplicate an existing token.
- No dedicated Figma frame exists for Register or the guest-mode CTA/gate sheet (confirmed via `get_metadata` search over the whole `LUsLvGVUhvskfA6xrAiSrP` file — only "Login", node `317:10108`, and "Splash screen", nodes `255:5689`–`255:5722`, exist for this flow). Where a task says "no Figma source," the styling instead reuses an existing matched pattern (Login's field style, the brand CTA button) rather than inventing new visual language.
- Splash background is `Brand/500` (`#55B931`), confirmed via `get_design_context` variable annotation on node `255:5722` — not the pre-existing unused `SgColor.SplashGreen` (`#49C22E`), which this plan retires.
- This codebase has zero automated tests today (no `src/test`, no `src/androidTest`, no test dependencies in `libs.versions.toml`). Verification convention is `@Preview` + manual device check. This plan adds JUnit4 for the one piece of pure, framework-free logic (`SessionViewModel`) and keeps Preview + manual verification for everything Compose-UI-shaped — it does not add Compose UI test infrastructure, which is a larger, separate change.

## Review Focus

- Guest taps a gated tab (Activity/Saved/Profile) twice quickly, or taps a second gated tab while the sheet is already open — the sheet must not stack or re-trigger; covered in Task 9's manual verification step.
- Login/Register succeeding from inside the `GuestGateSheet` flow must leave the user able to open the previously-gated tab directly afterward (no residual guest gate) — covered in Task 9's manual verification step.
- Register step 2 submitted with an empty field or mismatched passwords must show the inline error and must NOT call `onRegisterComplete` — covered in Task 7's manual verification step (pure logic is inline in the composable, no extraction needed since it has no reuse elsewhere).
- The notch fix must not double-pad screens that already had their own top padding (e.g. `CategoryListScreen`'s white header) — covered in Task 1's manual device check, since this is the one Global Constraint whose regression is only visible on-device, not in a Preview.
- `SessionViewModel`'s `login()` must not implicitly also flip an already-`AUTHENTICATED` session back to `GUEST`, and `logout()`/`continueAsGuest()` must be idempotent when called twice — covered directly in Task 2's unit tests.

---

## File Structure

```
app/src/main/java/com/sisaguna/android/
  core/session/
    SessionViewModel.kt        (new)
    GuestGateSheet.kt           (new)
  feature/splash/
    SplashScreen.kt              (new)
  feature/auth/
    LandingScreen.kt             (rewrite — drop the splash overlay, add guest CTA)
    RegisterScreen.kt            (rewrite — add step 2 detail form)
    LoginScreen.kt                (untouched — already matches Figma node 317:10108 pixel-for-pixel; only its caller wiring changes, in NavGraph.kt)
  feature/home/
    HomeScreen.kt                 (small additive change — onUploadClick param)
  navigation/
    Screen.kt                     (add Screen.Splash)
    NavGraph.kt                    (wire SessionViewModel, Splash start dest, guest gating, notch fix)
  ui/theme/Color.kt                (drop unused SgColor.SplashGreen)
app/src/test/java/com/sisaguna/android/
  core/session/SessionViewModelTest.kt  (new)
gradle/libs.versions.toml           (add junit)
app/build.gradle.kts                 (add testImplementation(libs.junit))
```

---

### Task 1: Notch / status-bar padding fix (global)

**Files:**
- Modify: `app/src/main/java/com/sisaguna/android/navigation/NavGraph.kt:49-54`

**Interfaces:**
- Consumes: nothing new.
- Produces: nothing new — pure bug fix, no signature changes.

- [ ] **Step 1: Apply the fix**

In `NavGraph.kt`, the `Scaffold` block currently reads:

```kotlin
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Landing.route,
            modifier = Modifier.padding(bottom = if (showBottomNav) padding.calculateBottomPadding() else 0.dp),
        ) {
```

Change the `modifier` line to also apply the top inset (Scaffold reserves it via its default `contentWindowInsets` since there's no `topBar`, but it was being discarded):

```kotlin
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Landing.route,
            modifier = Modifier.padding(
                top = padding.calculateTopPadding(),
                bottom = if (showBottomNav) padding.calculateBottomPadding() else 0.dp,
            ),
        ) {
```

(`startDestination` changes to `Screen.Splash.route` in Task 9 — leave it as `Screen.Landing.route` here so this task stays independently testable.)

- [ ] **Step 2: Build**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Manual device check**

Install on the Xiaomi test device (`./gradlew :app:installDebug`), open the app, and confirm the top bar / hero content on Landing, Login, Register, and Home no longer sits under the front-camera notch. Also open `CategoryListScreen` (tap a category from Home) and confirm its own white header still sits correctly below the status bar with no double-gap (its header has no independent top padding, so this is a visual "is there now an awkward double gap" check, not a code check).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/sisaguna/android/navigation/NavGraph.kt
git commit -m "fix: apply top status-bar/notch inset to NavHost content"
```

---

### Task 2: SessionViewModel (guest/authenticated state) + unit tests

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/sisaguna/android/core/session/SessionViewModel.kt`
- Test: `app/src/test/java/com/sisaguna/android/core/session/SessionViewModelTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `enum class AuthStatus { GUEST, AUTHENTICATED }`, `class SessionViewModel : ViewModel()` with `val status: StateFlow<AuthStatus>`, `fun continueAsGuest()`, `fun login()`, `fun logout()`. Package `com.sisaguna.android.core.session`. Consumed by `NavGraph.kt` (Task 9) via `hiltViewModel<SessionViewModel>()`.

- [ ] **Step 1: Add JUnit4 to the version catalog**

In `gradle/libs.versions.toml`, add to `[versions]` (after `ksp`):

```toml
junit = "4.13.2"
```

Add to `[libraries]` (after `kotlinx-coroutines-android`):

```toml
junit = { group = "junit", name = "junit", version.ref = "junit" }
```

- [ ] **Step 2: Wire it into the app module**

In `app/build.gradle.kts`, add to the `dependencies` block (after `debugImplementation(libs.androidx.ui.tooling)`):

```kotlin
    testImplementation(libs.junit)
```

- [ ] **Step 3: Write the failing test**

Create `app/src/test/java/com/sisaguna/android/core/session/SessionViewModelTest.kt`:

```kotlin
package com.sisaguna.android.core.session

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionViewModelTest {

    @Test
    fun `initial status is guest`() {
        val viewModel = SessionViewModel()
        assertEquals(AuthStatus.GUEST, viewModel.status.value)
    }

    @Test
    fun `login sets status to authenticated`() {
        val viewModel = SessionViewModel()
        viewModel.login()
        assertEquals(AuthStatus.AUTHENTICATED, viewModel.status.value)
    }

    @Test
    fun `login is idempotent`() {
        val viewModel = SessionViewModel()
        viewModel.login()
        viewModel.login()
        assertEquals(AuthStatus.AUTHENTICATED, viewModel.status.value)
    }

    @Test
    fun `logout after login returns to guest`() {
        val viewModel = SessionViewModel()
        viewModel.login()
        viewModel.logout()
        assertEquals(AuthStatus.GUEST, viewModel.status.value)
    }

    @Test
    fun `logout is idempotent when already guest`() {
        val viewModel = SessionViewModel()
        viewModel.logout()
        viewModel.logout()
        assertEquals(AuthStatus.GUEST, viewModel.status.value)
    }

    @Test
    fun `continueAsGuest keeps status guest explicitly`() {
        val viewModel = SessionViewModel()
        viewModel.continueAsGuest()
        assertEquals(AuthStatus.GUEST, viewModel.status.value)
    }
}
```

- [ ] **Step 4: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.sisaguna.android.core.session.SessionViewModelTest"`
Expected: FAIL — compilation error, `SessionViewModel` and `AuthStatus` don't exist yet.

- [ ] **Step 5: Implement SessionViewModel**

Create `app/src/main/java/com/sisaguna/android/core/session/SessionViewModel.kt`:

```kotlin
package com.sisaguna.android.core.session

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** Frontend-only in-memory auth state — no backend, no persistence. Guest is the default so the
 * app is fully browsable without an account; [login] is called from Login/Register success, and
 * a guest tap on a gated tab shows [com.sisaguna.android.core.session.GuestGateSheet] instead of
 * navigating. Reset on process death, same as every other piece of UI state in this repo. */
enum class AuthStatus { GUEST, AUTHENTICATED }

@HiltViewModel
class SessionViewModel @Inject constructor() : ViewModel() {

    private val _status = MutableStateFlow(AuthStatus.GUEST)
    val status: StateFlow<AuthStatus> = _status.asStateFlow()

    fun continueAsGuest() {
        _status.value = AuthStatus.GUEST
    }

    fun login() {
        _status.value = AuthStatus.AUTHENTICATED
    }

    fun logout() {
        _status.value = AuthStatus.GUEST
    }
}
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.sisaguna.android.core.session.SessionViewModelTest"`
Expected: PASS — 6 tests, 0 failures.

- [ ] **Step 7: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/java/com/sisaguna/android/core/session/SessionViewModel.kt app/src/test/java/com/sisaguna/android/core/session/SessionViewModelTest.kt
git commit -m "feat: add SessionViewModel for in-memory guest/authenticated state"
```

---

### Task 3: GuestGateSheet component

**Files:**
- Create: `app/src/main/java/com/sisaguna/android/core/session/GuestGateSheet.kt`

**Interfaces:**
- Consumes: nothing (no Figma source — see Global Constraints; styled from Login's CTA button pattern and `SgColor`/`SgTextStyle`).
- Produces: `@Composable fun GuestGateSheet(onDismiss: () -> Unit, onLoginClick: () -> Unit, onRegisterClick: () -> Unit)` in package `com.sisaguna.android.core.session`. Consumed by `NavGraph.kt` (Task 9).

- [ ] **Step 1: Implement**

Create `app/src/main/java/com/sisaguna/android/core/session/GuestGateSheet.kt`:

```kotlin
package com.sisaguna.android.core.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgTextStyle
import com.sisaguna.android.ui.theme.SisaGunaTheme

/** Shown when a guest taps a feature that needs an account (Upload CTA, Activity/Saved/Profile
 * tabs). No dedicated Figma frame exists for this yet, so it reuses Login's rounded-pill CTA
 * language for visual consistency rather than inventing new UI. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestGateSheet(
    onDismiss: () -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        GuestGateSheetContent(onLoginClick = onLoginClick, onRegisterClick = onRegisterClick)
    }
}

@Composable
private fun GuestGateSheetContent(onLoginClick: () -> Unit, onRegisterClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Masuk untuk lanjutkan",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = SgColor.Neutral800,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Buat atau masuk ke akun SisaGuna kamu untuk pakai fitur ini.",
            style = SgTextStyle.TextSmRegular,
            color = SgColor.Neutral500,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SgColor.Brand500, RoundedCornerShape(100.dp))
                .clickable(onClick = onLoginClick)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "Masuk", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SgColor.BaseWhite)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SgColor.Neutral100, RoundedCornerShape(100.dp))
                .clickable(onClick = onRegisterClick)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "Daftar Akun Baru", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SgColor.Neutral800)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GuestGateSheetContentPreview() {
    SisaGunaTheme {
        GuestGateSheetContent(onLoginClick = {}, onRegisterClick = {})
    }
}
```

- [ ] **Step 2: Build and check the Preview**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. Open `GuestGateSheetContentPreview` in Android Studio's preview pane and confirm it renders (two pill buttons + heading, centered, matches Login's button styling).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/sisaguna/android/core/session/GuestGateSheet.kt
git commit -m "feat: add GuestGateSheet for gating auth-only features from guests"
```

---

### Task 4: Add Screen.Splash route

**Files:**
- Modify: `app/src/main/java/com/sisaguna/android/navigation/Screen.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `Screen.Splash` (route `"splash"`). Consumed by `NavGraph.kt` (Task 9) as the new `startDestination`.

- [ ] **Step 1: Add the route**

In `Screen.kt`, add `Splash` before `Landing`:

```kotlin
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Landing : Screen("landing")
    data object Login : Screen("login")
    data object Register : Screen("register")
```

(No other lines in this file change — `Splash` deliberately isn't added to `mainRoutes`, same as `Landing`/`Login`/`Register`, since it never shows the bottom nav.)

- [ ] **Step 2: Build**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/sisaguna/android/navigation/Screen.kt
git commit -m "feat: add Screen.Splash route"
```

---

### Task 5: SplashScreen

**Files:**
- Create: `app/src/main/java/com/sisaguna/android/feature/splash/SplashScreen.kt`
- Modify: `app/src/main/java/com/sisaguna/android/ui/theme/Color.kt`

**Interfaces:**
- Consumes: `SgLogo` (`com.sisaguna.android.ui.components.SgLogo`), `SgColor.Brand500`, `SgColor.BaseWhite`.
- Produces: `@Composable fun SplashScreen(onTimeout: () -> Unit)` in package `com.sisaguna.android.feature.splash`. Consumed by `NavGraph.kt` (Task 9).

- [ ] **Step 1: Retire the unused SplashGreen token**

In `ui/theme/Color.kt`, remove this block (it's replaced by `Brand500`, confirmed as the real splash background via Figma's variable defs on node `255:5722` — see Global Constraints):

```kotlin
    // Splash-only fill from Landing Page's "Brand Opening Motion" node — distinct from Brand500.
    val SplashGreen = Color(0xFF49C22E)
```

- [ ] **Step 2: Implement SplashScreen**

Create `app/src/main/java/com/sisaguna/android/feature/splash/SplashScreen.kt`:

```kotlin
package com.sisaguna.android.feature.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.sisaguna.android.ui.components.SgLogo
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SisaGunaTheme
import kotlinx.coroutines.delay

private const val SplashDurationMs = 1900L

/** Matches Figma "Splash screen" (nodes 255:5689–255:5722, fileKey LUsLvGVUhvskfA6xrAiSrP) — a
 * brand-opening motion sequence. Background is Brand/500 per the frame's own variable defs. The
 * raw keyframes are mid-animation (the wordmark is still brand-green on brand-green background,
 * i.e. invisible) so white logo/wordmark here is a deliberate finished-state choice, not a
 * literal trace of one keyframe. */
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(SplashDurationMs)
        onTimeout()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(SgColor.Brand500),
        contentAlignment = Alignment.Center,
    ) {
        SgLogo(
            markSize = 40,
            textSize = 28,
            markTint = Color.Unspecified,
            textColor = SgColor.BaseWhite,
        )
    }
}

@Preview(showBackground = true, heightDp = 852)
@Composable
private fun SplashScreenPreview() {
    SisaGunaTheme {
        SplashScreen(onTimeout = {})
    }
}
```

- [ ] **Step 3: Build**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. (This also confirms no other file still referenced `SgColor.SplashGreen` — `LandingScreen.kt`'s reference to it is removed in Task 6, which must land before or together with this if built independently; if compiling this task alone before Task 6 fails on that reference, that's expected — see Task 6's note.)

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/sisaguna/android/feature/splash/SplashScreen.kt app/src/main/java/com/sisaguna/android/ui/theme/Color.kt
git commit -m "feat: add SplashScreen, retire unused SgColor.SplashGreen"
```

---

### Task 6: Rewrite LandingScreen — drop embedded splash, add guest CTA

**Files:**
- Modify (full rewrite): `app/src/main/java/com/sisaguna/android/feature/auth/LandingScreen.kt`

**Interfaces:**
- Consumes: nothing new.
- Produces: `@Composable fun LandingScreen(onLoginClick: () -> Unit, onGetStartedClick: () -> Unit, onGuestClick: () -> Unit)` — **signature change**: adds `onGuestClick`, removes the old internal splash-timer/`AnimatedVisibility` reveal logic entirely. Consumed by `NavGraph.kt` (Task 9).

- [ ] **Step 1: Replace the file**

Replace the full contents of `app/src/main/java/com/sisaguna/android/feature/auth/LandingScreen.kt` with:

```kotlin
package com.sisaguna.android.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sisaguna.android.R
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgTextStyle
import com.sisaguna.android.ui.theme.SisaGunaTheme

/** Matches the Figma Landing frame (fileKey LUsLvGVUhvskfA6xrAiSrP) — hero, value props, and
 * CTAs. The brand-opening splash motion now lives in its own route
 * ([com.sisaguna.android.feature.splash.SplashScreen]), shown before this screen, rather than
 * as a timer-driven overlay baked into this composable. */
@Composable
fun LandingScreen(
    onLoginClick: () -> Unit,
    onGetStartedClick: () -> Unit,
    onGuestClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(SgColor.Neutral50)) {
        Column(modifier = Modifier.weight(1f)) {
            HeroArea()
            Column(
                modifier = Modifier.padding(top = 28.dp, start = 24.dp, end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Selamatkan Makanan, Lindungi Bumi Kita",
                    fontSize = 26.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = SgColor.Neutral800,
                )
                Text(
                    text = "Setiap tahun, berton-ton makanan layak makan terbuang sia-sia. Bersama SisaGuna, ambil bagian menyelamatkan surplus makanan lezat di sekitarmu dengan harga sangat terjangkau atau bahkan gratis!",
                    style = SgTextStyle.TextSmRegular,
                    color = SgColor.Neutral500,
                )
            }
            Column(
                modifier = Modifier.padding(top = 20.dp, start = 24.dp, end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FeatureRow(
                    emoji = "🌱",
                    iconBg = SgColor.Green50,
                    title = "100% Eco-Friendly",
                    description = "Mengurangi emisi karbon langsung dari limbah makanan organik.",
                )
                FeatureRow(
                    emoji = "💰",
                    iconBg = SgColor.Orange100,
                    title = "Hemat & Berbagi",
                    description = "Dapatkan surplus lezat dengan potongan harga s/d 70%.",
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(100.dp))
                    .background(SgColor.Brand500)
                    .clickable(onClick = onGetStartedClick)
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Mulai Sekarang", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SgColor.BaseWhite)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(100.dp))
                    .border(BorderStroke(1.dp, SgColor.Neutral200), RoundedCornerShape(100.dp))
                    .clickable(onClick = onGuestClick)
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Jelajahi tanpa akun", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SgColor.Neutral800)
            }
            Row {
                Text(text = "Sudah punya akun? ", style = SgTextStyle.TextSmRegular, color = SgColor.Neutral500)
                Text(
                    text = "Masuk",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SgColor.Brand600,
                    modifier = Modifier.clickable(onClick = onLoginClick),
                )
            }
        }
    }
}

@Composable
private fun HeroArea(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .padding(24.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.hero_area),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(SgColor.Brand500, RoundedCornerShape(100.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(text = "sisaguna", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SgColor.BaseWhite)
        }
    }
}

@Composable
private fun FeatureRow(emoji: String, iconBg: Color, title: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier.size(32.dp).background(iconBg, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 14.sp)
        }
        Column {
            Text(text = title, style = SgTextStyle.TextSmSemibold, color = SgColor.Neutral800)
            Text(text = description, fontSize = 12.sp, color = SgColor.Neutral500)
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun LandingScreenPreview() {
    SisaGunaTheme {
        LandingScreen(onLoginClick = {}, onGetStartedClick = {}, onGuestClick = {})
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. This is also the point where any stray `SgColor.SplashGreen` reference (removed in Task 5) would surface as a compile error — there should be none, since this file no longer references it.

- [ ] **Step 3: Check the Preview**

Open `LandingScreenPreview` and confirm: hero image, headline, two feature rows, "Mulai Sekarang" filled button, new "Jelajahi tanpa akun" outlined button beneath it, "Sudah punya akun? Masuk" — no splash overlay renders (it's gone).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/sisaguna/android/feature/auth/LandingScreen.kt
git commit -m "refactor: rewrite LandingScreen without embedded splash, add guest CTA"
```

---

### Task 7: Rewrite RegisterScreen — add step 2 detail form

**Files:**
- Modify (full rewrite): `app/src/main/java/com/sisaguna/android/feature/auth/RegisterScreen.kt`

**Interfaces:**
- Consumes: `SgColor.RedStatus` (existing token, for the inline error message).
- Produces: `enum class AccountType { REGULAR, MERCHANT }` (unchanged), `@Composable fun RegisterScreen(onRegisterComplete: (AccountType) -> Unit, modifier: Modifier = Modifier)` — **signature/behavior change**: old `onContinue` fired after step 1 alone; new `onRegisterComplete` fires only after step 2's form passes basic validation. Consumed by `NavGraph.kt` (Task 9).

- [ ] **Step 1: Replace the file**

Replace the full contents of `app/src/main/java/com/sisaguna/android/feature/auth/RegisterScreen.kt` with:

```kotlin
package com.sisaguna.android.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sisaguna.android.ui.components.SgLogo
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgTextStyle
import com.sisaguna.android.ui.theme.SisaGunaTheme

enum class AccountType { REGULAR, MERCHANT }

private enum class RegisterStep { ACCOUNT_TYPE, DETAILS }

/** No dedicated Figma frame exists for Register (confirmed via get_metadata search — only
 * "Login", node 317:10108, is designed for this flow). Step 1 (account type) keeps its existing
 * validated layout; step 2's form fields reuse Login's exact field styling (bg #FAFAFA, border
 * #E5E5E5, radius 12dp, label style) for visual consistency rather than inventing new UI.
 * Frontend-only: "Daftar Sekarang" only does basic UI validation, no real backend call. */
@Composable
fun RegisterScreen(
    onRegisterComplete: (AccountType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableStateOf(RegisterStep.ACCOUNT_TYPE) }
    var accountType by remember { mutableStateOf(AccountType.REGULAR) }

    when (step) {
        RegisterStep.ACCOUNT_TYPE -> AccountTypeStep(
            modifier = modifier,
            selected = accountType,
            onSelect = { accountType = it },
            onNext = { step = RegisterStep.DETAILS },
        )
        RegisterStep.DETAILS -> DetailsStep(
            modifier = modifier,
            onBack = { step = RegisterStep.ACCOUNT_TYPE },
            onSubmit = { onRegisterComplete(accountType) },
        )
    }
}

@Composable
private fun AccountTypeStep(
    selected: AccountType,
    onSelect: (AccountType) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(SgColor.Neutral50)) {
        Column(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SgLogo(textColor = SgColor.Brand500)
                Text(
                    text = "Pilih Tipe Akun Anda",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = SgColor.Neutral800,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Sesuaikan peran Anda untuk mengakses sistem terbaik SisaGuna.",
                    style = SgTextStyle.TextSmRegular,
                    color = SgColor.Neutral500,
                    textAlign = TextAlign.Center,
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AccountTypeCard(
                    emoji = "😋",
                    title = "Pengguna Biasa",
                    description = "Ambil makanan surplus lezat dari resto sekitar dengan diskon melimpah atau gratis demi misi penyelamatan lingkungan.",
                    selected = selected == AccountType.REGULAR,
                    onClick = { onSelect(AccountType.REGULAR) },
                )
                AccountTypeCard(
                    emoji = "🏪",
                    title = "Mitra Restoran",
                    description = "Redistribusikan makanan sisa hari ini, kurangi sampah organik, dan raih profit tambahan secara cepat dan transparan.",
                    selected = selected == AccountType.MERCHANT,
                    onClick = { onSelect(AccountType.MERCHANT) },
                )
            }
        }
        PrimaryButton(label = "Lanjutkan Registrasi", onClick = onNext)
    }
}

@Composable
private fun DetailsStep(
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize().background(SgColor.BaseWhite)) {
        Column(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Kembali",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SgColor.Brand600,
                    modifier = Modifier.clickable(onClick = onBack),
                )
                Text(text = "Lengkapi Data Diri", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = SgColor.Neutral800)
                Text(text = "Data ini dipakai untuk akun SisaGuna kamu.", style = SgTextStyle.TextSmRegular, color = SgColor.Neutral500)
            }
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RegisterField(label = "Nama Lengkap", value = fullName, onValueChange = { fullName = it })
                RegisterField(label = "Email", value = email, onValueChange = { email = it }, keyboardType = KeyboardType.Email)
                RegisterField(label = "Nomor HP", value = phone, onValueChange = { phone = it }, keyboardType = KeyboardType.Phone)
                PasswordField(
                    label = "Kata Sandi",
                    value = password,
                    onValueChange = { password = it },
                    visible = passwordVisible,
                    onToggleVisible = { passwordVisible = !passwordVisible },
                )
                PasswordField(
                    label = "Konfirmasi Kata Sandi",
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    visible = passwordVisible,
                    onToggleVisible = { passwordVisible = !passwordVisible },
                )
                errorMessage?.let { message ->
                    Text(text = message, fontSize = 12.sp, color = SgColor.RedStatus)
                }
            }
        }
        PrimaryButton(
            label = "Daftar Sekarang",
            onClick = {
                errorMessage = when {
                    fullName.isBlank() || email.isBlank() || phone.isBlank() -> "Lengkapi semua data terlebih dahulu."
                    password.isBlank() -> "Kata sandi tidak boleh kosong."
                    password != confirmPassword -> "Konfirmasi kata sandi tidak cocok."
                    else -> null
                }
                if (errorMessage == null) onSubmit()
            },
        )
    }
}

@Composable
private fun RegisterField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = SgTextStyle.TextXsMedium, color = SgColor.Neutral500)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SgColor.Neutral50, RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, SgColor.Neutral200), RoundedCornerShape(12.dp))
                .padding(14.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                textStyle = SgTextStyle.TextSmRegular.copy(color = SgColor.Neutral800),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisible: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = SgTextStyle.TextXsMedium, color = SgColor.Neutral500)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SgColor.Neutral50, RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, SgColor.Neutral200), RoundedCornerShape(12.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                textStyle = SgTextStyle.TextSmRegular.copy(color = SgColor.Neutral800),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (visible) "Sembunyikan" else "Tampilkan",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = SgColor.Brand600,
                modifier = Modifier.clickable(onClick = onToggleVisible),
            )
        }
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .background(SgColor.Brand500, RoundedCornerShape(100.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SgColor.BaseWhite)
    }
}

@Composable
private fun AccountTypeCard(
    emoji: String,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) SgColor.Green50 else SgColor.BaseWhite, RoundedCornerShape(20.dp))
            .border(
                BorderStroke(1.dp, if (selected) SgColor.Brand500 else SgColor.Neutral200),
                RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = emoji, fontSize = 22.sp)
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) SgColor.Brand600 else SgColor.Neutral800,
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .background(SgColor.Brand500, RoundedCornerShape(100.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(text = "Aktif", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SgColor.BaseWhite)
                }
            }
        }
        Text(
            text = description,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = if (selected) SgColor.Neutral800 else SgColor.Neutral500,
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun RegisterScreenAccountTypePreview() {
    SisaGunaTheme {
        AccountTypeStep(selected = AccountType.REGULAR, onSelect = {}, onNext = {})
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun RegisterScreenDetailsPreview() {
    SisaGunaTheme {
        DetailsStep(onBack = {}, onSubmit = {})
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Manual verification (Review Focus item)**

Open `RegisterScreenDetailsPreview` in interactive preview mode (or run the app, navigate Landing → Mulai Sekarang → pick a type → Lanjutkan Registrasi):
1. Tap "Daftar Sekarang" with all fields empty → confirm the red error text "Lengkapi semua data terlebih dahulu." appears and nothing navigates.
2. Fill name/email/phone, put different values in the two password fields, tap submit → confirm "Konfirmasi kata sandi tidak cocok." appears and nothing navigates.
3. Fill everything with matching passwords, tap submit → confirm it proceeds (calls `onRegisterComplete`, wired to navigation in Task 9).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/sisaguna/android/feature/auth/RegisterScreen.kt
git commit -m "feat: add Register step 2 detail form with basic UI validation"
```

---

### Task 8: HomeScreen — thread through an onUploadClick callback

**Files:**
- Modify: `app/src/main/java/com/sisaguna/android/feature/home/HomeScreen.kt`

**Interfaces:**
- Consumes: nothing new.
- Produces: `HomeScreen(..., onUploadClick: () -> Unit = {})` — additive param with a default, so this is not a breaking change for any other caller. Consumed by `NavGraph.kt` (Task 9) to route the Upload CTA through the guest gate.

- [ ] **Step 1: Thread the parameter through**

In `HomeScreen.kt`, update the public `HomeScreen` composable (around line 62-91) to accept and forward `onUploadClick`:

```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onListingClick: (Listing) -> Unit = {},
    onCategoryClick: (ListingTier) -> Unit = {},
    onUploadClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var addressLabel by remember { mutableStateOf("Rumah") }
    var showLocationSheet by remember { mutableStateOf(false) }

    HomeScreenContent(
        uiState = uiState,
        addressLabel = addressLabel,
        onAddressClick = { showLocationSheet = true },
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onRetry = viewModel::retry,
        onListingClick = onListingClick,
        onCategoryClick = onCategoryClick,
        onUploadClick = onUploadClick,
    )
```

- [ ] **Step 2: Forward through HomeScreenContent → HomeFeedList → HomeTopBar**

Update `HomeScreenContent`'s signature (around line 93-101) to accept `onUploadClick: () -> Unit` and pass it into the `HomeUiState.Success` branch's `HomeFeedList` call:

```kotlin
@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    addressLabel: String,
    onAddressClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onListingClick: (Listing) -> Unit,
    onCategoryClick: (ListingTier) -> Unit = {},
    onUploadClick: () -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize().background(SgColor.Neutral100)) {
        when (uiState) {
            is HomeUiState.Loading -> LoadingState()
            is HomeUiState.Error -> ErrorState(message = uiState.message, onRetry = onRetry)
            is HomeUiState.Success -> HomeFeedList(
                state = uiState,
                addressLabel = addressLabel,
                onAddressClick = onAddressClick,
                onSearchQueryChange = onSearchQueryChange,
                onListingClick = onListingClick,
                onCategoryClick = onCategoryClick,
                onUploadClick = onUploadClick,
            )
        }
    }
}
```

Update `HomeFeedList`'s signature (around line 138-146) to accept `onUploadClick: () -> Unit` and pass it to `HomeTopBar`'s call (around line 161-166):

```kotlin
@Composable
private fun HomeFeedList(
    state: HomeUiState.Success,
    addressLabel: String,
    onAddressClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onListingClick: (Listing) -> Unit,
    onCategoryClick: (ListingTier) -> Unit,
    onUploadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // ...unchanged body until the HomeTopBar item...
        item {
            HomeTopBar(
                addressLabel = addressLabel,
                onAddressClick = onAddressClick,
                onUploadClick = onUploadClick,
                modifier = Modifier.padding(horizontal = 23.dp, vertical = 16.dp),
            )
        }
```

- [ ] **Step 3: Wire it into HomeTopBar's Upload button**

Update `HomeTopBar`'s signature (around line 209) to accept `onUploadClick: () -> Unit`, and replace the Upload `Row`'s no-op click (around line 241-249) to use it:

```kotlin
@Composable
private fun HomeTopBar(
    addressLabel: String,
    onAddressClick: () -> Unit,
    onUploadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // ...unchanged Row/location chip...
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(SgColor.Brand500, RoundedCornerShape(30.dp))
                    .clickableNoRipple(onUploadClick)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
```

(Remove the now-stale comment above that `Row` about the create-listing flow not being built — it's wired now, even though the destination itself is still a no-op until sub-project 4 builds Upload.)

- [ ] **Step 4: Update the Previews**

`HomeScreenPreview`, `HomeScreenLoadingPreview`, `HomeScreenErrorPreview` (bottom of the file) call `HomeScreenContent(...)` with named args — since `onUploadClick` has a default value in both `HomeScreen` and `HomeScreenContent`, these previews compile unchanged. No edit needed here — this step is just confirming that, not a code change.

- [ ] **Step 5: Build**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/sisaguna/android/feature/home/HomeScreen.kt
git commit -m "feat: thread onUploadClick through HomeScreen for guest-gating"
```

---

### Task 9: Wire NavGraph — Splash start dest, SessionViewModel, guest gating

**Files:**
- Modify (full rewrite): `app/src/main/java/com/sisaguna/android/navigation/NavGraph.kt`

**Interfaces:**
- Consumes: `SessionViewModel`/`AuthStatus` (Task 2), `GuestGateSheet` (Task 3), `Screen.Splash` (Task 4), `SplashScreen` (Task 5), `LandingScreen(onLoginClick, onGetStartedClick, onGuestClick)` (Task 6), `RegisterScreen(onRegisterComplete, modifier)` (Task 7), `HomeScreen(..., onUploadClick)` (Task 8), `LoginScreen(onLoginSuccess, onRegisterClick, modifier)` (unchanged, existing signature).
- Produces: nothing consumed elsewhere — this is the top of the composition graph.

- [ ] **Step 1: Replace the file**

Replace the full contents of `app/src/main/java/com/sisaguna/android/navigation/NavGraph.kt` with:

```kotlin
package com.sisaguna.android.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
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
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Landing.route) { inclusive = true }
                        }
                    },
                    onRegisterClick = { navController.navigate(Screen.Register.route) },
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterComplete = {
                        sessionViewModel.login()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Landing.route) { inclusive = true }
                        }
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
```

- [ ] **Step 2: Build**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Manual verification — full flow (Review Focus items)**

Run: `./gradlew :app:installDebug`, open the app, and walk through:
1. Splash shows briefly (green, logo+wordmark, no notch clipping per Task 1) then Landing appears.
2. Tap "Jelajahi tanpa akun" → lands on Home as guest.
3. Tap the "Activity" bottom-nav tab → `GuestGateSheet` appears instead of navigating; tap outside it or swipe down → dismisses, still on Home.
4. Tap "Activity" again, then "Masuk" inside the sheet → sheet closes, Login screen opens.
5. Log in (any input — no backend validation) → lands back on Home; tap "Activity" again → this time it navigates for real (no gate), confirming `authStatus` flipped to `AUTHENTICATED` and stayed that way.
6. From Home (still authenticated), tap the Upload pill in the top bar → no crash, no gate sheet (since authenticated) — it's a no-op until sub-project 4, which is expected.
7. Repeat steps 2-3 but this time tap "Saved" then immediately tap "Profile" while the sheet from "Saved" is still open → confirm only one sheet is ever shown, no stacking/duplicate sheets.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/sisaguna/android/navigation/NavGraph.kt
git commit -m "feat: wire Splash start destination, SessionViewModel, and guest gating into NavGraph"
```

---

## Self-Review Notes

- **Spec coverage:** Splash (Task 5), Login/Register completeness (Task 7 — Login already matched Figma so no rewrite was needed, documented in File Structure), Guest mode (Tasks 2, 3, 9), notch fix (Task 1), folder cleanup (`core/session`, `feature/splash` — Tasks 2, 3, 5) all have owning tasks. Out-of-scope items (Home split, Saved/Profile, Activity/Upload) are explicitly deferred to sub-projects 2-4 per the spec and not touched here.
- **Placeholder scan:** none — every step has real code or an exact command.
- **Type consistency:** `AuthStatus`/`SessionViewModel` (Task 2) → consumed identically in `NavGraph.kt` (Task 9). `LandingScreen`'s `onGuestClick` (Task 6) → wired in Task 9. `RegisterScreen`'s `onRegisterComplete: (AccountType) -> Unit` (Task 7) → wired in Task 9 with matching signature. `HomeScreen`'s `onUploadClick: () -> Unit` (Task 8) → wired in Task 9.
- **Review Focus:** all five items have an owning task and either a unit test (SessionViewModel idempotency, Task 2) or an explicit manual-verification step (everything Compose-UI-shaped, consistent with this repo's existing Preview+device testing convention — see Global Constraints).
