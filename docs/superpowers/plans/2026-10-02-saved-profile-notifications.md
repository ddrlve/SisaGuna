# Saved, Profile, Home Tabs & Notifications Implementation Plan (3a)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship the 3a spec on device: fixed font weights, 3a tokens, tabbed Home, Saved +
merchant detail, Profile + Edit Profile + logout, and the notifications inbox + settings.

**Architecture:** In-memory `@Singleton` fake repositories expose `StateFlow`s. Each screen has one
`@HiltViewModel` that combines repository flows into a `UiState`. Screens are stateless
`...Content` composables plus a thin Hilt wrapper, same as `HomeScreen` / `CategoryListScreen`.

**Tech Stack:** Kotlin 2.0.21, Jetpack Compose (BOM 2024.09.02, Material3), Hilt 2.51.1,
Navigation Compose 2.8.0, JUnit 4 + kotlinx-coroutines-test 1.8.1.

**Spec:** `docs/superpowers/specs/2026-10-01-saved-profile-home-polish-design.md` (including the
2026-10-02 Notifications addendum).

## Global Constraints

- Frontend only. No backend, no persistence. All data lives in fake repositories.
- Page gutter is `SgSpacing.Gutter = 20.dp` on every 3a screen. Rails use `LazyRow(contentPadding = PaddingValues(horizontal = Gutter))`.
- Brand green `#55B931` only for primary actions and the active state.
- All user-facing copy in Indonesian, in `res/values/strings.xml`. Copy must match the spec verbatim.
- Guests cannot reach Saved, Profile, or Notifications (`GuestGateSheet`).
- Commits: no Claude co-author line (user memory `feedback_no_claude_coauthor`).
- Min SDK 26, compile SDK 34.

## Review Focus

1. Search active on Home while on tab 1 with matches only in tab 0: the "Ada n hasil di Siap Santap" hint must show (tested in `HomeViewModelTest`).
2. Unsave then "Batalkan" on Saved must restore the merchant at its original position, not at the end (tested in `FakeSavedMerchantRepositoryTest`).
3. Edit Profile phone typed as `0812...`: leading 0 is stripped and digit count validated after stripping (tested in `EditProfileViewModelTest`).
4. Notification delete then "Batalkan" must restore at the old index even after other changes (tested in `NotificationsViewModelTest`).
5. Logout from Profile must clear the back stack so Back on Landing exits (manual on-device check, Task 9).

---

### Task 1: Static Inter fonts

**Files:**
- Create: `app/src/main/res/font/inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf`, `inter_bold.ttf`
- Modify: `app/src/main/java/com/sisaguna/android/ui/theme/Type.kt`
- Delete: `app/src/main/res/font/inter.ttf`

- [ ] **Step 1: Generate static instances**

```bash
cd app/src/main/res/font
for w in 400:regular 500:medium 600:semibold 700:bold; do
  python -m fontTools.varLib.instancer inter.ttf wght=${w%%:*} opsz=14 -o inter_${w##*:}.ttf
done
```

- [ ] **Step 2: Map weights in `Type.kt`**

```kotlin
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_bold, FontWeight.Black), // no 900 instance; Bold is close enough
)
```

Remove the `ExperimentalTextApi` opt-in and `FontVariation` import, then delete `inter.ttf`.

- [ ] **Step 3: Build** — `./gradlew assembleDebug`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 4: Commit** — `fix: replace variable Inter with static weights so bold renders on device`

### Task 2: 3a tokens + test infra

**Files:**
- Create: `ui/theme/Spacing.kt`
- Modify: `ui/theme/Shape.kt`, `ui/theme/Color.kt`, `ui/theme/Type.kt`, `gradle/libs.versions.toml`, `app/build.gradle.kts`
- Create: `app/src/test/java/com/sisaguna/android/testutil/MainDispatcherRule.kt`

```kotlin
// Spacing.kt
object SgSpacing {
    val Gutter = 20.dp
    val Xs = 4.dp; val Sm = 8.dp; val Md = 12.dp; val Lg = 16.dp; val Xl = 24.dp; val Xxl = 32.dp
}
// Shape.kt (append)
object SgRadius { val Card = 20.dp; val Tile = 16.dp; val Thumb = 12.dp; val Pill = 999.dp }
// Color.kt (append inside SgColor)
val Ink = Color(0xFF1F2A1C); val InkMuted = Color(0xFF6B7466); val Page = Color(0xFFF6F7F4)
val Hairline = Color(0xFFE6E8E3); val Mint = Color(0xFFEAF7E4)
val Farm = Color(0xFFFFF1E2); val FarmInk = Color(0xFFB4570B)
val Compost = Color(0xFFE6F2FB); val CompostInk = Color(0xFF1D6FA5)
// Type.kt (append inside SgTextStyle)
val Display = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.3).sp, color = SgColor.Ink)
val Title = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp, letterSpacing = (-0.1).sp, color = SgColor.Ink)
val Body = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, color = SgColor.InkMuted)
val Label = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, color = SgColor.Ink)
```

Test deps: `kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinxCoroutines" }` and `testImplementation(libs.kotlinx.coroutines.test)`.

```kotlin
// MainDispatcherRule.kt
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
```

- [ ] Build + `./gradlew testDebugUnitTest`. Commit: `feat: add 3a spacing, radius, color, and text tokens`

### Task 3: Home tabs

**Files:** `feature/home/HomeUiState.kt`, `HomeViewModel.kt`, `HomeScreen.kt`, `strings.xml`; Test: `app/src/test/.../feature/home/HomeViewModelTest.kt`

**Interfaces — Produces:** `enum class HomeTab { SIAP_SANTAP, TERNAK_KOMPOS }`, `HomeViewModel.onTabSelected(tab: HomeTab)`, `HomeUiState.Success.selectedTab`, `.otherTabMatchCount`, `HomeViewModel.unreadNotifications: StateFlow<Int>` (wired in Task 8).

- [ ] **Step 1: Failing tests**

```kotlin
class HomeViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private fun vm() = HomeViewModel(FakeFeedRepo(), FakeNotificationRepository())

    @Test fun `selected tab survives retry`() {
        val vm = vm(); vm.onTabSelected(HomeTab.TERNAK_KOMPOS); vm.retry()
        assertEquals(HomeTab.TERNAK_KOMPOS, (vm.uiState.value as HomeUiState.Success).selectedTab)
    }
    @Test fun `other tab hint only while searching with empty active tab`() {
        val vm = vm(); vm.onTabSelected(HomeTab.TERNAK_KOMPOS)
        vm.onSearchQueryChange("nasi")   // only human listings match
        assertEquals(1, (vm.uiState.value as HomeUiState.Success).otherTabMatchCount)
        vm.onSearchQueryChange("")
        assertEquals(0, (vm.uiState.value as HomeUiState.Success).otherTabMatchCount)
    }
}
```

`FakeFeedRepo` returns immediately (no delay): nearby = [Nasi], deals = [], animalFeed = [Ampas], compost = [Kopi]. Count for a tab = distinct listing ids across its lists.

- [ ] **Step 2: Implement.** `Success` gains `selectedTab: HomeTab = HomeTab.SIAP_SANTAP` and `otherTabMatchCount: Int = 0`; helpers `humanIsEmpty`, `farmIsEmpty`, `farmCount = animalFeed.size + compost.size`. `toUiState(query, tab)` computes `otherTabMatchCount` only when `query.isNotBlank()` and the active tab is empty.
- [ ] **Step 3: Screen.** Replace `SiapSantapHeaderAndTile`, `PakanTernakSection`, `RailBlock`, `CategoryTile` with `SegmentedSwitch`, `WideTile`, `OtherTabHint`, rails on the 20dp gutter, page `SgColor.Page`. Tab content cross-fades (`AnimatedContent`, fade 180ms) keyed by tab. Segmented thumb slides with a spring (`animateDpAsState`, `spring(dampingRatio = 0.85f, stiffness = 500f)`).
- [ ] **Step 4:** tests pass, build, commit `feat: rebuild Home as Siap Santap / Ternak & Kompos tabs`

### Task 4: Models + repositories

**Files:** `data/model/Merchant.kt` (+`rating: Double? = null`), `UserProfile.kt`, `ImpactStats.kt`, `AppNotification.kt`; `data/repository/ListingRepository.kt` (+`getMerchants()`, `getMerchantFeed(id)`, `data class MerchantFeed(val merchant: Merchant, val listings: List<Listing>)`), `FakeListingRepository.kt`, `SavedMerchantRepository.kt`, `ProfileRepository.kt`, `NotificationRepository.kt` (interfaces + fakes in the same file, matching spec §3 and the addendum); `di/RepositoryModule.kt` (+3 `@Binds`). Tests: `FakeSavedMerchantRepositoryTest`, `FakeNotificationRepositoryTest`.

```kotlin
@Singleton
class FakeSavedMerchantRepository @Inject constructor() : SavedMerchantRepository {
    private val order = listOf("m1", "m2", "m4", "m5")          // original seed order
    private val _ids = MutableStateFlow<Set<String>>(LinkedHashSet(order))
    override val savedIds: StateFlow<Set<String>> = _ids.asStateFlow()
    private val removed = mutableMapOf<String, Int>()           // id -> index at removal
    override fun unsave(id: String) {
        val list = _ids.value.toList(); val i = list.indexOf(id); if (i < 0) return
        removed[id] = i; _ids.value = LinkedHashSet(list - id)
    }
    override fun restore(id: String) {
        if (id in _ids.value) return
        val list = _ids.value.toMutableList()
        list.add((removed.remove(id) ?: list.size).coerceIn(0, list.size), id)
        _ids.value = LinkedHashSet(list)
    }
}
```

Tests: `unsave then restore returns id to original position`; `restore of unknown id appends`.
Notification fake tests: `markAllRead leaves zero unread`; `delete then restore puts item back at index`.

Commit: `feat: add saved, profile, and notification repositories with fake data`

### Task 5: Saved list + merchant detail

**Files:** `feature/saved/SavedUiState.kt`, `SavedViewModel.kt`, `SavedScreen.kt`, `MerchantDetailUiState.kt`, `MerchantDetailViewModel.kt`, `MerchantDetailScreen.kt`, `ui/domain/MerchantAvatar.kt`. Tests: `SavedViewModelTest`, `MerchantDetailViewModelTest`.

`SavedViewModel`: `combine(savedIds, flow { emit(getMerchants()); emit(listings) })` into `SavedUiState(loading, merchants: List<SavedMerchantUi(merchant, distanceKm: Double?)>)`; `unsave(id)`, `undo(id)`.
`MerchantDetailViewModel(SavedStateHandle)`: reads `merchantId`, loads `getMerchantFeed`; state `NotFound | Loading | Success(merchant, isSaved, query, tierFilter: ListingTier?, listings)`; `onQueryChange`, `onTierToggle(tier)` (same tier again clears), `toggleSave()`.

Tests: list reflects unsave/undo; empty when all removed; tier filter + search compose; unknown id gives NotFound.

UI per spec §4. Heart button 48dp target with a scale pop (`animateFloatAsState`, spring) on toggle. Snackbar via `SnackbarHostState` owned by the screen.

Commit: `feat: add Saved list and merchant detail`

### Task 6: Profile + Edit Profile

**Files:** `feature/profile/ProfileUiState.kt`, `ProfileViewModel.kt`, `ProfileScreen.kt`, `EditProfileViewModel.kt`, `EditProfileScreen.kt`. Tests: `EditProfileViewModelTest`, `ProfileViewModelTest`.

```kotlin
object ProfileValidation {
    fun name(v: String) = if (v.isBlank() || v.trim().length > 50) "Nama wajib diisi" else null
    fun email(v: String) = if (Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(v.trim())) null else "Format email tidak valid"
    fun normalizePhone(v: String) = v.filter(Char::isDigit).removePrefix("62").removePrefix("0")
    fun phone(v: String) = if (normalizePhone(v).length in 9..13) null else "Nomor harus 9–13 digit"
}
```

`EditProfileViewModel`: `form: StateFlow<EditProfileForm(name, email, phone, touched: Set<Field>, submitted)>`, `canSave = isDirty && isValid`, `onBlur(field)`, `save(): Boolean`. Errors are shown only for touched fields or after a save attempt.

Tests: each rule; `0812345678901` normalizes to `812345678901`; save disabled when unchanged; save updates repository and `ProfileViewModel` sees the new name.

Profile UI per spec §5; the "Notifikasi Aplikasi" row navigates to `profile/notifications` and its hint reflects `prefs.enabled`. Logout dialog per spec.

Commit: `feat: add Profile, Edit Profile, and logout`

### Task 7: Notifications inbox + settings

**Files:** `feature/notifications/NotificationFormat.kt`, `NotificationsUiState.kt`, `NotificationsViewModel.kt`, `NotificationsScreen.kt`, `NotificationSettingsViewModel.kt`, `NotificationSettingsScreen.kt`. Tests: `NotificationFormatTest`, `NotificationsViewModelTest`, `NotificationSettingsViewModelTest`.

```kotlin
enum class DayGroup { TODAY, YESTERDAY, THIS_WEEK, EARLIER }
fun dayGroup(at: Instant, now: Instant, zone: ZoneId): DayGroup {
    val days = ChronoUnit.DAYS.between(at.atZone(zone).toLocalDate(), now.atZone(zone).toLocalDate())
    return when { days <= 0 -> DayGroup.TODAY; days == 1L -> DayGroup.YESTERDAY; days <= 6 -> DayGroup.THIS_WEEK; else -> DayGroup.EARLIER }
}
fun relativeTime(at: Instant, now: Instant, zone: ZoneId): String {
    val mins = Duration.between(at, now).toMinutes()
    return when (dayGroup(at, now, zone)) {
        DayGroup.TODAY -> when { mins < 1 -> "Baru saja"; mins < 60 -> "$mins mnt lalu"; else -> "${mins / 60} jam lalu" }
        DayGroup.YESTERDAY -> "Kemarin"
        else -> "${ChronoUnit.DAYS.between(at.atZone(zone).toLocalDate(), now.atZone(zone).toLocalDate())} hari lalu"
    }
}
enum class NotificationFilter(val types: Set<NotificationType>) {
    ALL(NotificationType.entries.toSet()), ORDERS(setOf(PICKUP, ORDER)), MERCHANTS(setOf(MERCHANT)), PROMOS(setOf(PROMO))
}
```

VM: `uiState = combine(repo.notifications, filter)` into `NotificationsUiState(filter, groups: List<Pair<DayGroup, List<AppNotification>>>, unreadCount)`; `onFilter`, `onOpen(id)` (marks read), `markAllRead`, `delete(id): Pair<AppNotification, Int>?`, `undoDelete(n, index)`.
Settings VM exposes `repo.prefs` and `update(transform: (NotificationPrefs) -> NotificationPrefs)`.

UI per addendum. Swipe via Material3 `SwipeToDismissBox` (end-to-start only). Unread → read background animates (`animateColorAsState`, 200ms).

Commit: `feat: add notifications inbox and notification settings`

### Task 8: Navigation

**Files:** `navigation/Screen.kt`, `NavGraph.kt`, `SgBottomNav.kt`, `feature/home/HomeScreen.kt` (bell callback + badge).

- Add routes `MerchantDetail("saved/merchant/{merchantId}")`, `EditProfile("profile/edit")`, `NotificationSettings("profile/notifications")`, `Notifications("notifications")`.
- Bottom nav visible when the first path segment is `home|activity|saved|profile|category`; active tab from that segment (`category` maps to Home). Hidden on `notifications`.
- `gatedRoutes` adds `Notifications`. Bell: guest shows the gate, otherwise navigate.
- Logout: `sessionViewModel.logout()` then `navigate(Landing) { popUpTo(0) { inclusive = true } }`.
- Edit Profile save result: `previousBackStackEntry.savedStateHandle["profile_updated"] = true`; Profile shows "Profil diperbarui".

Commit: `feat: wire Saved, Profile, and Notifications routes`

### Task 9: Verify

- [ ] `./gradlew testDebugUnitTest assembleDebug` green.
- [ ] Install on Redmi; check the spec §9 list plus: bell badge count, mark all read, swipe delete + undo, settings master switch disables children.
- [ ] Code review of the whole branch, fix findings, then hand back to the user to commit/merge.
