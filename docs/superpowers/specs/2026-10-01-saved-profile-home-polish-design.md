# Saved, Profile & Home Polish — Visual Foundation, Home Tabs, Saved, Profile

Status: approved for planning
Sub-project: 3a of 4 (Foundation ✅ → Home split ✅ → **Saved & Profile (3a: core, 3b: sub-pages)** → Activity & Upload)

## Context

Sub-project 3 was split in two during brainstorming (2026-10-01):

- **3a (this spec):** a visual foundation pass (fonts, spacing, color tokens), a Home rebuild,
  Saved (list + merchant detail), Profile main, Edit Profile, and logout.
- **3b (later spec):** the remaining Profile sub-pages: Alamat, Riwayat Penyelamatan,
  Metode Pembayaran, Ganti Password, Notifikasi, Pusat Bantuan, Kebijakan Privasi, and the full
  Katalog screen.

After testing sub-project 2 on a device, the user said Home still felt unbalanced: padding was
inconsistent, the single "Siap Santap" tile floated alone with empty space beside it, the
human-food section wasn't prominent enough, and the category icons were badly placed. The user
asked for the UI to follow sound UI/UX principles, taking cues from food-rescue and super-apps
(Surplus, Gojek, Grab) without copying them, and for SisaGuna to keep its own identity. Figma
is the starting point, but the user explicitly allowed deviating from it where it improves
spacing, color, or clarity.

Home's direction was settled with a throwaway prototype rather than on paper. Three variants
were built (branch `prototype/home-variants`, commit `91e6d11`, never merged): A (Siap Santap
dominant), B (segmented tabs), and C (category doors). The user tested them on their Redmi
(393dp wide) and **chose B: tabs**.

The same device test turned up a root-cause bug. Every heading renders at Regular weight even
when the code asks for Bold/SemiBold. `res/font/inter.ttf` is a variable font (axes `wght`
100–900, `opsz` 14–32), and the `FontVariation.Settings` used in `Type.kt` are not applied on
this device, so all text falls back to the default 400 instance. That is a large part of why
Home looked flat. Without fixing it, "make Siap Santap bold" can't be satisfied.

The repo is still **frontend-only** (see memory `project_frontend_only_scope`). All data in
this sub-project is in-memory fake data behind repository interfaces, the same pattern as
`ListingRepository` / `FakeListingRepository`.

## Decisions made during brainstorming

| Question | Decision |
|---|---|
| How interactive should the screens be? | Interactive with dummy data: forms save to in-memory state, and changes show up across screens. Basic UI validation. No backend. |
| Split sub-project 3? | Yes: 3a (this spec) then 3b. |
| Where does a user save (follow) a merchant? | Nowhere yet. Saved starts **seeded** with 4 merchants. The user can only remove them. A save action comes later, with a listing/merchant detail page. |
| State architecture | **Approach A:** in-memory singleton repositories exposing `StateFlow`, plus one ViewModel per screen. (Rejected: putting it in `SessionViewModel`, which mixes concerns and is hard to swap to a backend; screen-local `remember`, which loses edits on navigation.) |
| Home layout | **Variant B, segmented tabs**, chosen after on-device prototype testing. |
| Visual direction | Draft tokens below, approved. The signature elements are the pickup countdown and rescue impact. |

## 1. Visual foundation (3a-0)

### 1.1 Font fix

Replace the single variable `inter.ttf` with **four static instances**: Regular 400, Medium
500, SemiBold 600, Bold 700. Generate them from the existing file with
`fontTools.varLib.instancer`, pinning `opsz=14`. No external download is needed.

```
res/font/inter_regular.ttf
res/font/inter_medium.ttf
res/font/inter_semibold.ttf
res/font/inter_bold.ttf
```

`Type.kt`: the `Inter` `FontFamily` maps each `FontWeight` to its static file, with no
`variationSettings`. `FontWeight.Black` (used by the promo banner) maps to the Bold file. A
900 instance isn't worth another ~300KB. Delete `inter.ttf` once nothing references it.

**Acceptance:** on the Redmi, "Siap Santap" (Bold) is visibly heavier than "Terdekat dari kamu"
(SemiBold), and that is heavier than body text (Regular).

### 1.2 Tokens

New, additive (existing `SgColor` entries stay, so screens outside this spec don't break):

```kotlin
object SgSpacing {          // ui/theme/Spacing.kt
    val Gutter = 20.dp      // the single left/right page margin for every 3a screen
    val Xs = 4.dp; val Sm = 8.dp; val Md = 12.dp; val Lg = 16.dp; val Xl = 24.dp; val Xxl = 32.dp
}
object SgRadius {           // ui/theme/Shape.kt
    val Card = 20.dp; val Tile = 16.dp; val Thumb = 12.dp; val Pill = 999.dp
}
// SgColor additions
Ink        = #1F2A1C   // primary text (green-tinted, not pure black)
InkMuted   = #6B7466   // secondary text
Page       = #F6F7F4   // page background for 3a screens
Hairline   = #E6E8E3   // 1dp borders, segmented-control track
Mint       = #EAF7E4   // Siap Santap tint
Farm       = #FFF1E2 / FarmInk    = #B4570B   // animal-feed tint / accent
Compost    = #E6F2FB / CompostInk = #1D6FA5   // compost tint / accent
```

`SgTextStyle` additions: `Display` (22sp Bold, -0.3sp tracking), `Title` (17sp SemiBold,
-0.1sp), `Body` (13sp Regular, InkMuted), and `Label` (14sp SemiBold).

**Layout rules for 3a screens:**

- Every left edge sits on the 20dp gutter.
- Full-width bands extend their background edge to edge but keep their content on the gutter.
  There are no inset cards with extra padding that shift content off the gutter.
- Horizontal rails use `LazyRow(contentPadding = PaddingValues(horizontal = Gutter))` so cards
  scroll edge to edge but rest on the gutter.
- Vertical rhythm: 24dp between a rail and the next heading, 16dp from the search field to the
  next block.
- Brand green `#55B931` is reserved for primary actions and the active state.

## 2. Home: segmented tabs (variant B)

### Structure

```
HomeTopBar (location chip · Upload · bell)        gutter 20, top 12
Search field                                      gutter 20
[ Siap Santap | Ternak & Kompos · 4 ]             segmented switch, top 16
── tab 0: Siap Santap ──────────────────────
PromoBanner
Rail "Terdekat dari kamu"   [Radius < 2km]
Rail "Buat kamu yang hemat" [Lihat Semua → Category(HUMAN)]
── tab 1: Ternak & Kompos ──────────────────
[ 🐔 Pakan Hewan · n tersedia ] [ ♻ Untuk Kompos · n tersedia ]   two wide tiles, weight 1f each
Rail "Untuk makanan ternakmu" [Lihat Semua → Category(ANIMAL_FEED)]
Rail "Kompos"                 [Lihat Semua → Category(COMPOST)]
```

- **Segmented switch:** pill track in `Hairline`. The selected segment is white with a 2dp
  shadow and `Ink` label. The unselected label is `InkMuted`. The second label includes the
  farm-item count (`animalFeed.size + compost.size`) when it is above 0, so the hidden tab
  advertises that it has content.
- **Wide tile:** a white `Tile`-radius card with a 1dp `Hairline` border and 12dp padding. A
  44dp tinted icon square (`Farm` / `Compost`, 32dp image) sits on the left. On the right are
  the label (`Label`, 1 line, ellipsis) and "n tersedia" (`Body`). The whole tile is one click
  target and opens Category List for its tier.
- The old `SiapSantapHeaderAndTile`, `PakanTernakSection`, `RailBlock`, and `CategoryTile` are
  removed. Section titles and subtitles from sub-project 2 are dropped where the tab label now
  carries the meaning.

### State

`HomeUiState.Success` gains:

```kotlin
val selectedTab: HomeTab = HomeTab.SIAP_SANTAP          // enum HomeTab { SIAP_SANTAP, TERNAK_KOMPOS }
val otherTabMatchCount: Int = 0                          // > 0 only while searching
```

`HomeViewModel.onTabSelected(tab)` stores the tab, so it survives navigating to Category and
back. While the search query is non-blank, if the active tab's lists are all empty and the
other tab has matches, the screen shows **"Ada n hasil di <tab lain>"** with a "Lihat" action
that switches tabs. If neither tab has matches, it shows the existing `EmptySearchState`.

## 3. Data layer

### Models

```kotlin
// Merchant.kt: add
val rating: Double?           // null = no ratings yet; UI hides the star row

// UserProfile.kt (new)
data class UserProfile(
    val name: String, val email: String, val phone: String,
    val location: String, val memberSince: YearMonth,
) { val initial: Char get() = name.trim().firstOrNull()?.uppercaseChar() ?: '?' }

// ImpactStats.kt (new)
data class ImpactStats(val portions: Int, val compostKg: Int, val carbonKg: Int, val savedRupiah: Long)
```

### Repositories (all bound `@Singleton` in `RepositoryModule`)

```kotlin
interface SavedMerchantRepository {
    val savedIds: StateFlow<Set<String>>   // insertion-ordered (LinkedHashSet)
    fun unsave(id: String)
    fun restore(id: String)                // for Snackbar "Batalkan"
}
// FakeSavedMerchantRepository: seeded with 4 merchant ids from FakeListingRepository's merchants

interface ProfileRepository {
    val profile: StateFlow<UserProfile>
    fun update(name: String, email: String, phone: String)
    suspend fun getImpact(): ImpactStats
    suspend fun getMyCatalog(): List<Listing>
}
// FakeProfileRepository: "Budi Santoso", Tangerang, Banten, member since 2026-01;
// impact 34 porsi / 3 kg / 32 kg / Rp 245.000 (Figma values)

// ListingRepository: add
suspend fun getMerchants(): List<Merchant>
suspend fun getMerchantFeed(merchantId: String): MerchantFeed   // merchant + its listings
```

A merchant's distance in the Saved card is the minimum `distanceKm` across its listings, or
hidden if it has none.

### Data flow

```
FakeProfileRepository ──StateFlow<UserProfile>──┬─> ProfileViewModel ──> ProfileUiState
                                                └─> EditProfileViewModel ──update()──┘ (Profile re-renders)

FakeSavedMerchantRepository ──StateFlow<Set<id>>──┬─> SavedViewModel  (combine with getMerchants())
                                                  └─> MerchantDetailViewModel(merchantId)
                                                        (+ getMerchantFeed(id), heart = unsave/restore)
```

## 4. Saved

### Saved list (`Screen.Saved`, Figma 273:12037)

- Title "Disimpan" (`Display`) on the page, then section label "Penyedia" (`Title`).
- Card per merchant: a 48dp initial avatar (`Mint` fill, brand ring, verified check badge if
  `isVerified`), the name (`Label`), a Verified pill, "★ 4.9" (hidden if `rating == null`),
  and "• Kemang (0.8 km)" (`Body`). A red filled heart is on the right with a 48dp touch target.
- Tapping a card opens `MerchantDetail`. Tapping the heart unsaves immediately and shows the
  Snackbar **"Penyedia dihapus"** with **"Batalkan"**, which restores the merchant at its
  original position.
- **Empty state:** "Belum ada penyedia disimpan" with the line "Penyedia yang kamu simpan akan
  muncul di sini." and a **"Jelajahi makanan"** button that goes to Home.

### Merchant detail (`Screen.MerchantDetail`, route `saved/merchant/{merchantId}`, Figma 273:12143)

- Back button and title "Disimpan". Search field (filters this merchant's listings by title).
  Tier chips "Siap Santap / Untuk Ternak / Untuk kompos" (toggle; none selected = all).
- Merchant header band (white, full width, content on the gutter): the same avatar/name/rating
  row as the list card, with the heart. Unsaving here shows the same Snackbar and keeps the
  user on the page; the heart turns to an outline and can re-save via `restore`.
- A 2-column grid of `CategoryListingCard`. Tapping a card does nothing for now (no listing
  detail screen yet), same as Home.
- An unknown `merchantId` shows "Penyedia tidak ditemukan" with a back action.

## 5. Profile

### Profile main (`Screen.Profile`, Figma 273:12725)

1. **Header:** a 64dp initial avatar (red-tint ring as in Figma, verified badge), the name
   (`Display`), "📍 Tangerang, Banten" (`Body`), "Anggota aktif sejak Januari 2026" (12sp
   InkMuted), and a pencil icon button that opens `EditProfile`.
2. **"Dampak Penyelamatan Anda"** card (white, `Card` radius): a 2×2 grid of tinted `Mint`
   stat tiles with a big number (`Display`, brand color) and a label: "34 Porsi / Makanan",
   "3 kg / Kompos", "32 kg / Karbon Dicegah". Below them, a full-width row with a yellow "Rp"
   coin: "Anda menghemat Rp 245.000" / "Dibandingkan harga normal produk retail".
3. **"Katalog saya"**: a rail of `ListingCard` with "Lihat Semua" (no-op in 3a, Katalog is
   3b).
4. **"Pengaturan Akun"** list card: Alamat · Ubah, Riwayat Penyelamatan · Lihat semua, Metode
   Pembayaran · 1 aktif, Ganti Password · Ubah, Notifikasi Aplikasi · Aktif. Each row has an
   icon, a label, a trailing hint, and a chevron, with a 56dp minimum height and hairline
   dividers inset to the text. Rows are no-ops in 3a.
5. **"Dukungan dan Hukum"** list card: Pusat Bantuan · Tanya jawab, Kebijakan Privasi · Syarat
   ketentuan (no-ops in 3a).
6. **"Keluar dari Akun"**: a full-width text button in `RedStatus`. It opens an `AlertDialog`
   ("Keluar dari akun?" / "Kamu bisa masuk lagi kapan saja." / Batal / **Keluar**). On confirm:
   `sessionViewModel.logout()`, then navigate to `Landing` with `popUpTo(0) { inclusive = true }`.

### Edit Profile (`Screen.EditProfile`, route `profile/edit`, Figma 273:12569)

- Back button, title "Profil". The avatar initial with "Ubah Foto Profile" (brand color). Tapping
  it shows the Snackbar "Segera hadir".
- A white card with three labelled `SgInput` fields: Nama, Email, Nomor (phone keyboard,
  shows the `+62` prefix).
- **Inline validation** (shown after a field loses focus or on save attempt, not while typing):
  - Nama: required, max 50 characters → "Nama wajib diisi"
  - Email: must match a basic `x@y.z` shape → "Format email tidak valid"
  - Nomor: 9–13 digits after +62 → "Nomor harus 9–13 digit". `UserProfile.phone` stores
    digits only, without the `+62` prefix (the field draws the prefix as decoration), and a
    leading `0` is stripped on save.
- A sticky bottom button **"Simpan perubahan"**, enabled only when the form differs from the
  saved profile **and** is valid. On save: `ProfileRepository.update(...)`, pop back, and show
  the Snackbar "Profil diperbarui" on Profile.
- Back with unsaved changes: an `AlertDialog` "Buang perubahan?" (Batal / Buang).

## 6. Navigation

```kotlin
// Screen.kt
data object MerchantDetail : Screen("saved/merchant/{merchantId}") {
    const val ARG_ID = "merchantId"
    fun routeFor(id: String) = "saved/merchant/$id"
}
data object EditProfile : Screen("profile/edit")
```

- `NavGraph.kt`: replace the two `ComingSoonScreen`s and add the two routes.
- The bottom nav shows on all four new screens (as in Figma). The active tab is derived from
  the route's first path segment (`saved/…` → Saved, `profile/…` → Profile), so nested routes
  light up their parent tab.
- `gatedRoutes` stays as is. Guests still can't reach Saved or Profile.
- Logout clears the whole back stack, so Back on Landing exits the app instead of returning to
  Profile as a guest.

## 7. Files

```
res/font/inter_{regular,medium,semibold,bold}.ttf      new (inter.ttf removed)
ui/theme/Type.kt                                       static font family + new styles
ui/theme/Color.kt                                      + new tokens
ui/theme/Spacing.kt                                    new: SgSpacing
ui/theme/Shape.kt                                      + SgRadius
data/model/Merchant.kt                                 + rating
data/model/UserProfile.kt, ImpactStats.kt              new
data/repository/ListingRepository.kt                   + getMerchants, getMerchantFeed, MerchantFeed
data/repository/FakeListingRepository.kt               ratings + new functions
data/repository/SavedMerchantRepository.kt (+Fake)     new
data/repository/ProfileRepository.kt (+Fake)           new
di/RepositoryModule.kt                                 + 2 @Singleton bindings
feature/home/HomeScreen.kt, HomeUiState.kt, HomeViewModel.kt   tabs rebuild
feature/saved/{SavedScreen,SavedViewModel,SavedUiState}.kt                     new
feature/saved/{MerchantDetailScreen,MerchantDetailViewModel,MerchantDetailUiState}.kt  new
feature/profile/{ProfileScreen,ProfileViewModel,ProfileUiState}.kt             new
feature/profile/{EditProfileScreen,EditProfileViewModel}.kt                    new
navigation/Screen.kt, NavGraph.kt, SgBottomNav.kt      routes, active-tab derivation, logout
res/values/strings.xml                                 all new copy
```

## 8. Build order

Each step builds, passes its tests, and is ready to commit. The user commits manually.

1. Static Inter fonts, then a device check that weights differ.
2. Tokens (`SgSpacing`, `SgRadius`, colors, text styles).
3. Home tabs and `HomeViewModel` tests (tab persistence, other-tab hint).
4. Models and repositories, with repository tests.
5. Saved list (unsave + undo).
6. Merchant detail.
7. Profile main.
8. Edit Profile (validation, dirty-state save, discard dialog).
9. Navigation (routes, active-tab derivation, logout dialog + back-stack clear).
10. Code review, full on-device pass against Figma and the spacing rules, then merge.

## 9. Testing

**Unit tests** (JUnit + `kotlinx-coroutines-test`, same setup as `SessionViewModelTest`):

- `HomeViewModel`: the selected tab survives a feed reload. The other-tab hint appears only
  while searching with an empty active tab and matches in the other tab.
- `FakeSavedMerchantRepository`: `unsave` then `restore` returns the id to its original
  position.
- `SavedViewModel`: the list reflects unsave/restore. Empty state when all are removed.
- `MerchantDetailViewModel`: tier filter and search compose. An unknown id gives the
  not-found state.
- `EditProfileViewModel`: each validation rule. Save is disabled when unchanged or invalid.
  Save updates the repository, so `ProfileViewModel` sees the new name.

**On-device checks (Redmi Note 11, 393dp):**

- Three distinct font weights are visible.
- Every left edge on Home, Saved, and Profile lines up on 20dp.
- Rails scroll edge to edge.
- Tab switch, the tab persisting after Category and Back, and the search hint.
- Unsave plus Batalkan.
- Edit name, which updates the Profile header.
- Logout, then Back exits.
- Check small-screen overflow on a 360dp emulator: the wide tiles ellipsize instead of
  clipping.

## Out of Scope

- 3b: Alamat, Riwayat Penyelamatan, Metode Pembayaran, Ganti Password, Notifikasi, Pusat
  Bantuan, Kebijakan Privasi, the Katalog full screen.
- A save/follow-merchant action (needs a listing or merchant detail page).
- Profile photo upload.
- Backend (Supabase), handled outside this repo.
- Re-skinning Activity, Category List, Landing, Login, and Register with the new tokens. They
  do pick up the font-weight fix automatically.
- Dark mode.

## Risks

- **Narrow screens:** the two wide tiles share one row. Below ~360dp they could get tight.
  Mitigated by single-line ellipsized labels, with a check on a 360dp emulator.
- **Temporary inconsistency:** after 3a, Home, Saved, and Profile use the new tokens while
  other screens use the old ones. This is accepted and gets resolved in later sub-projects.
- **Font fix side effects:** every screen gets heavier headings at once. A quick visual pass
  over Landing, Login, Register, Category, and Activity is part of step 1.

## Addendum (2026-10-02): Notifications

Added after the user tested the build on their phone and asked for complete notifications. The
Figma MCP hit its Starter-plan call limit, so the user chose to let Claude design these screens
from the 3a tokens and common food-rescue UX patterns, using Surplus as a loose reference.
Approved in chat on 2026-10-02.

### Data

```kotlin
// data/model/AppNotification.kt
enum class NotificationType { PICKUP, ORDER, MERCHANT, PROMO, IMPACT }
data class AppNotification(
    val id: String, val type: NotificationType, val title: String, val body: String,
    val createdAt: Instant, val isRead: Boolean,
)
data class NotificationPrefs(
    val enabled: Boolean = true, val pickup: Boolean = true, val order: Boolean = true,
    val merchant: Boolean = true, val promo: Boolean = false, val impact: Boolean = true,
)

interface NotificationRepository {
    val notifications: StateFlow<List<AppNotification>>   // newest first
    val prefs: StateFlow<NotificationPrefs>
    fun markRead(id: String)
    fun markAllRead()
    fun delete(id: String)
    fun restore(notification: AppNotification, index: Int) // Snackbar "Batalkan"
    fun updatePrefs(prefs: NotificationPrefs)
}
```

`FakeNotificationRepository` (`@Singleton`) is seeded with 8 items spread over today, yesterday,
and earlier this week: pickup reminders, an order ready for pickup, a saved merchant posting new
food, a promo, and a weekly impact summary. 3 are unread.

### Inbox (`Screen.Notifications`, route `notifications`)

- Opened from the Home bell. Guests see `GuestGateSheet` instead.
- The bell shows a red count badge (`9+` cap) when unread > 0 and the user is authenticated.
- Top bar: back, title "Notifikasi", trailing text action "Tandai dibaca" (hidden when
  nothing is unread).
- Filter chips: Semua · Pesanan (PICKUP + ORDER) · Penyedia (MERCHANT) · Promo (PROMO).
  IMPACT shows under Semua only.
- Groups: "Hari ini", "Kemarin", "Minggu ini" (2–6 days), "Sebelumnya".
- Row: 40dp tinted circle icon per type, title (`Label`), body (`Body`, max 2 lines),
  relative time ("Baru saja", "5 mnt lalu", "3 jam lalu", "Kemarin", "4 hari lalu"). Unread rows
  have a `Mint` background and an 8dp brand dot. Tapping a row marks it read.
- Swipe end-to-start deletes the row, then shows the Snackbar "Notifikasi dihapus" with
  "Batalkan", which restores it at its old index.
- Empty state (overall or per filter): "Belum ada notifikasi" / "Kabar soal pesanan dan
  penyedia favoritmu akan muncul di sini."

### Settings (`Screen.NotificationSettings`, route `profile/notifications`)

- Opened from Profile, "Notifikasi Aplikasi". The row hint shows "Aktif" or "Nonaktif" from
  `prefs.enabled`.
- A master switch "Izinkan notifikasi". When off, the per-type switches are disabled (shown at
  38% alpha) but keep their values.
- Per-type switches with one-line descriptions: Pengingat pickup, Status pesanan, Menu baru dari
  penyedia tersimpan, Promo & diskon, Ringkasan dampak mingguan.
- Changes save immediately to the repository. There is no save button.

### Tests

- Grouping and relative-time formatting are pure functions with unit tests.
- `NotificationsViewModel`: filters, mark read, mark all read, delete then restore at index.
- `NotificationSettingsViewModel`: master off keeps child values; a toggle updates the repository.
