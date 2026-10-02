# SisaGuna

Aplikasi mobile redistribusi makanan berlebih untuk mitra FnB, khususnya UMKM kuliner Indonesia. Warung, kantin, bakery, dan resto kecil memposting makanan berlebih sebelum terbuang. Pembeli di sekitar memesan lewat app, lalu mengambil dan membayar langsung di lokasi.

Repo ini adalah versi native Android (Kotlin, Jetpack Compose) dari SisaGuna, dibangun paralel dengan app Expo React Native tim. Repo ini khusus frontend. Backend Supabase dikerjakan terpisah oleh anggota tim lain.

Lecture Venture Creation (ENPR6312), BINUS. Group 4. Class LZ01. Theme: Lingkungan Alam.

## Link Terkait

- Canva: [PPT](https://canva.link/d403zux6vbz05gn)
- Figma: [Design](https://www.figma.com/design/LUsLvGVUhvskfA6xrAiSrP/sisaguna?node-id=0-1&t=Ac735hftPAYuJkT2-1)
- Figma: [Prototype](https://www.figma.com/proto/LUsLvGVUhvskfA6xrAiSrP/sisaguna?node-id=255-5689&t=FJrDzffI5BvBqfaI-1)
- Front End: https://github.com/ddrlve/SisaGuna
- Back End: https://github.com/FadhRach/sisaguna-be

## Contributors

- Fadhlan Nur Rachman (2802491690)
- Dian Rakhmawati Lestari (2802539085)
- Nasauramecca Nour Haqqanshah Shodiqin (2802541921)
- Catherine Zaneta Adji (2802512442)

## Status Pengerjaan

Alur utama pembeli sudah bisa dicoba end to end: splash, landing, login, home, detail produk, checkout, status pesanan, aktivitas, notifikasi, tersimpan, dan profil. Semua data masih dari repository mock (in-memory). App belum tersambung ke Supabase.

Belum dikerjakan: integrasi backend, dashboard mitra penuh, dan panel admin.

## Tech Stack

| Layer | Teknologi | Catatan |
|---|---|---|
| UI | Jetpack Compose + Material3 (BOM 2024.09.02) | Animasi halus sesuai desain |
| Bahasa | Kotlin 2.0.21 | |
| Navigasi | Navigation Compose 2.8.0 | Route dalam sealed class `Screen` |
| Async / DI | Coroutines + Flow, Hilt 2.51.1 | |
| State | ViewModel + StateFlow | Bukan LiveData |
| Image loading | Coil 2.7.0 | |
| Peta | osmdroid 6.1.20 | Peta OpenStreetMap untuk pilih alamat, tanpa API key |
| Backend | Supabase | Repo terpisah, app ini masih pakai mock repository |
| Build | Gradle 8.9, AGP 8.5.2, JDK 17, compileSdk 34, minSdk 26 | |

## Halaman

| Layar | Status | Deskripsi |
|---|---|---|
| Splash & Landing | Selesai | Animasi logo, hero mengambang, CTA |
| Login & Register | Selesai (mock) | Email / HP, password, pilih peran pengguna atau mitra |
| Guest Mode | Selesai | Bisa jelajah tanpa login, aksi tertentu minta login lewat sheet |
| Home | Selesai (mock) | Pilih lokasi, carousel banner, strip voucher, filter, tab sticky, seksi Siap Santap dan Pakan Ternak & Kompos |
| Category list | Selesai (mock) | Filter chip 3 tier, grid 2 kolom |
| Detail produk & toko | Selesai (mock) | Info produk, keranjang, profil merchant |
| Checkout | Selesai (mock) | Pilih voucher, ringkasan potongan, metode bayar |
| Aktivitas & detail pesanan | Selesai (mock) | Riwayat pesanan, timeline status, QR kode pickup |
| Tersimpan | Selesai (mock) | Daftar merchant favorit |
| Notifikasi | Selesai (mock) | Daftar notifikasi dan pengaturan per kategori |
| Profil | Selesai (mock) | Edit profil, alamat, riwayat penyelamatan, metode bayar, ubah password, bantuan, privasi, katalog saya |
| Alamat | Selesai | Multi alamat, cari lokasi, GPS, pin di peta |
| Upload (mitra) | Selesai (mock) | Form posting makanan berlebih |

## Struktur Project

```
SisaGuna/
├── app/
│   ├── build.gradle.kts              Konfigurasi module: dependency, SDK
│   └── src/
│       ├── main/java/com/sisaguna/android/
│       │   ├── MainActivity.kt       Entry point, memasang tema dan NavGraph
│       │   ├── SisaGunaApp.kt        Application class Hilt
│       │   ├── core/session/         Sesi login dan guest gate
│       │   ├── data/
│       │   │   ├── model/            Domain model: Listing, Merchant, Commerce, UserProfile, AppNotification
│       │   │   └── repository/       Interface repository + implementasi mock
│       │   ├── di/                   Module Hilt, binding repository
│       │   ├── navigation/           Screen, NavGraph, SgBottomNav
│       │   ├── feature/              Satu folder per fitur (Screen + ViewModel)
│       │   │   ├── splash/  auth/  home/  category/  listing/
│       │   │   ├── checkout/  activity/  saved/  notifications/
│       │   │   └── profile/  address/  upload/
│       │   └── ui/
│       │       ├── theme/            Color, Type, Shape, Spacing, Theme
│       │       ├── components/       Komponen primitif: SgButtons, SgChip, SgInput, SgBadge, SgMotion
│       │       └── domain/           Komponen domain: ListingCard, VoucherTicket, CartBar, TierBadge
│       ├── main/res/                 strings, font Inter, drawable, ikon launcher
│       └── test/                     Unit test ViewModel dan repository
│
├── docs/superpowers/                 Spec desain dan rencana implementasi
├── figma/                            Screenshot referensi Figma
├── gradle/libs.versions.toml         Version catalog dependency
└── build.gradle.kts, settings.gradle.kts, gradle.properties
```

Alur data: Repository, lalu ViewModel, lalu StateFlow, lalu Composable collect. Composable tidak pernah memanggil backend langsung. Saat Supabase siap, cukup ganti implementasi repository di `di/RepositoryModule.kt`.

## Setup Lokal

```
git clone https://github.com/ddrlve/SisaGuna.git
cd SisaGuna
```

Buka folder project di Android Studio dan tunggu Gradle sync selesai. Sambungkan HP fisik (USB debugging aktif) atau siapkan emulator, lalu tekan Run.

Atau lewat terminal:

```
./gradlew installDebug
```

Jalankan unit test:

```
./gradlew testDebugUnitTest
```

## Deploy Production

Belum ada build production. Setelah backend Supabase tersambung, app didistribusikan lewat Play Store internal testing untuk kebutuhan demo.

## Catatan

- App dipaksa light mode. Warna diset eksplisit supaya tampilan tetap benar di HP yang memakai dark mode.
- File `local.properties`, folder `.gradle/`, `app/build/`, dan config lokal lain tidak masuk repo (lihat `.gitignore`).

Made with care by Group 4
