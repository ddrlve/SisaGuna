# Foundation Rebuild — Splash, Auth, Guest Mode, Repo Structure

Status: approved for planning
Sub-project: 1 of 4 (Foundation → Home → Saved & Profile → Activity & Upload)

## Context

Dosen pembimbing memberi dua feedback: (1) pisahkan section makanan-untuk-manusia
dari pakan-ternak/kompos di Home dengan nama yang lebih baik dari "Makanan Manusia"
(ditangani di sub-project 2), dan (2) tambahkan **guest mode** supaya user bisa
membuka & menjelajah aplikasi tanpa perlu login dulu. User juga minta login/register
dilengkapi (Register saat ini cuma punya step pilih tipe akun, tanpa form data diri),
folder repo dirapikan, dan splash screen dibangun ulang sesuai Figma — semua screen
auth-flow di-rewrite dari nol ("mulai dari awal lagi"), bukan ditambal.

Repo ini **frontend-only** (Supabase dikerjakan terpisah oleh rekan tim — lihat
memory `project_frontend_only_scope`), jadi login/register/guest mode di sub-project
ini adalah **UI + in-memory auth state saja**, tanpa backend nyata. Ini konsisten
dengan pola yang sudah ada di `LoginScreen.kt` sekarang ("Masuk" cuma memanggil
`onLoginSuccess()` tanpa validasi server).

Bug yang juga harus difix di sini: `NavGraph.kt` pakai `enableEdgeToEdge()` tapi
`NavHost`-nya cuma menerapkan `padding.calculateBottomPadding()`, top inset dibuang
— akibatnya semua screen (termasuk Home) mentok ke status bar/notch kamera di HP
Xiaomi milik user. Fix ini global (satu baris), berlaku untuk semua screen.

## Approach

**Rebuild penuh di level screen & navigasi untuk auth flow (splash, landing, login,
register) dan tambahkan guest-mode routing.** Design-token layer yang sudah ada
(`ui/theme/*`: `SgColor`, `SgTextStyle`, `Shape.kt`, dan komponen `SgLogo`,
`SgInput`, `SgBadge`, `SgChip`) **dipertahankan** — itu representasi token Figma,
bukan "screen lama" yang perlu dibuang, dan tidak ada indikasi token tersebut salah.
Kalau nanti ditemukan token yang tidak match Figma, itu diperbaiki secara targeted,
bukan re-derive semua dari nol.

File yang akan **dihapus dan ditulis ulang** (bukan diedit inkremental):
`LandingScreen.kt`, `LoginScreen.kt`, `RegisterScreen.kt`.

File yang **baru dibuat**: `SplashScreen.kt`, `AuthViewModel.kt`, `AuthUiState.kt`,
`GuestGate` (nav guard util), plus bottom-sheet "masuk untuk lanjutkan" component.

## Folder Structure

```
feature/
  splash/
    SplashScreen.kt                 (baru — route sendiri, bukan overlay di Landing)
  auth/
    LandingScreen.kt                (rewrite)
    LoginScreen.kt                  (rewrite)
    RegisterScreen.kt               (rewrite, form data diri lengkap)
    AuthViewModel.kt                (baru)
    AuthUiState.kt                  (baru)
  home, category, activity, address (tidak berubah struktur)
core/
  session/
    SessionState.kt                 (baru — isGuest/isAuthenticated in-memory, app-scoped)
    GuestGateSheet.kt                (baru — bottom sheet "Masuk untuk lanjutkan")
navigation/
  Screen.kt                         (tambah Screen.Splash)
  NavGraph.kt                       (guest routing + notch fix)
  SgBottomNav.kt                    (guest tap handling pada tab yang butuh login)
```

`core/session` baru karena auth state dipakai lintas fitur (navigation, bottom nav,
upload CTA di Home) — bukan milik satu feature folder saja. Ini konvensi tambahan,
bukan restrukturisasi besar-besaran; folder `feature/*` yang sudah ada tetap dipakai
apa adanya.

## Screens & Behavior

### Splash (`Screen.Splash`, start destination baru)
- Tampil sekali saat app start, sebelum Landing.
- Sesuai frame Figma "Splash screen" (node 255:5689 dkk.) — spec exact (warna,
  posisi logo, durasi) di-pull via Figma MCP saat implementasi, bukan di spec ini.
- Auto-navigate ke Landing setelah durasi splash selesai (pola timer serupa yang
  sudah ada di `LandingScreen` sekarang, dipindah ke sini).

### Landing (rewrite)
- Hero + value prop (dipertahankan strukturnya kalau memang match Figma — dicek
  ulang saat implementasi), tapi splash overlay-nya **dihapus** dari sini karena
  splash sudah jadi route sendiri.
- CTA: "Mulai Sekarang" → Register, "Masuk" → Login, **+ CTA baru "Jelajahi tanpa
  akun"** → langsung ke Home sebagai guest (`SessionState.isGuest = true`).

### Login (rewrite)
- Form email/HP + password (pola sudah ada, dirapikan ulang sesuai Figma exact).
- Sukses → `SessionState.isAuthenticated = true`, navigate Home, popUpTo Landing.

### Register (rewrite)
- Step 1 (sudah ada): pilih tipe akun (Reguler / Mitra Restoran).
- Step 2 (**baru**): form data diri — nama, email, no. HP, password, konfirmasi
  password. Validasi UI dasar (field kosong, password match) — tanpa backend.
- Submit sukses → sama seperti Login: set authenticated, ke Home.

### Guest Mode
- `SessionState` (in-memory, app-scoped via `remember`/CompositionLocal atau
  ViewModel-scoped ke `SgNavGraph` — detail teknis diputuskan di planning) menyimpan
  status guest vs authenticated.
- Guest **bisa** buka Home penuh: browse kategori, search, lihat detail listing.
- Guest tap Upload / tab Activity / Saved / Profile di bottom nav →
  `GuestGateSheet` muncul ("Masuk untuk lanjutkan ke fitur ini") dengan tombol ke
  Login/Register, TIDAK auto-redirect paksa.
- Setelah login sukses dari gate, kembali ke aksi yang tadi diklik (nice-to-have;
  kalau kompleks di planning, minimal kembali ke Home dengan status authenticated
  sudah cukup — bottom nav tab jadi bisa diakses normal).

## Notch / Status Bar Fix (global)

`NavGraph.kt`, baris `Modifier.padding(bottom = ...)` pada `NavHost` → tambahkan
`top = padding.calculateTopPadding()`. Scaffold sudah menyediakan inset ini lewat
`contentWindowInsets` default karena tidak ada `topBar`; sekarang cuma dibuang.
Berlaku otomatis untuk semua screen (Splash, Landing, Login, Register, Home,
Category, Activity, Saved, Profile) tanpa perlu ubah masing-masing screen.

## Out of Scope (sub-project ini)

- Home section-split & rename ("Siap Santap" / "Pakan Ternak & Kompos") — sub-project 2.
- Saved, Profile + 8 sub-halaman, Activity 5 status, Upload flow — sub-project 3 & 4.
- Backend auth nyata (Supabase) — di luar repo ini sepenuhnya (lihat memory
  `project_frontend_only_scope`).
- Re-derivasi design token dari Figma variables (Approach B yang tidak dipilih).

## Testing

- Compose Preview untuk tiap screen baru/rewrite (pola yang sudah konsisten di
  codebase — `@Preview` dengan mock state).
- Manual verification di device fisik (Xiaomi Redmi Note 11, sesuai catatan
  sebelumnya) untuk memastikan notch fix benar-benar menghilangkan potongan di
  status bar — ini yang paling penting untuk divalidasi visual, bukan unit test.
- Tidak ada unit test baru untuk auth logic karena state-nya trivial (in-memory
  boolean) dan tanpa backend; kalau `SessionState` berkembang jadi lebih kompleks
  di planning, tambahkan test saat itu.
