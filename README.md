# SisaGuna

Aplikasi mobile redistribusi makanan berlebih atau sisa makanan untuk mitra FnB seperti UMKM kuliner Indonesia. Warung, kantin, bakery, dan resto kecil memposting makanan berlebih sebelum menjadi sampah, pembeli di sekitar membeli atau mengambilnya langsung di tempat lewat app, bayar di lokasi saat ambil.

Repo ini adalah versi native Android (Kotlin, Jetpack Compose) dari SisaGuna, dibangun paralel dengan app Expo React Native tim. Konteks lengkap keputusan produk, stack, dan aturan kerja ada di `ANDROID_CLAUDE.md`.

Venture Creation (ENPR6312), BINUS. Group 4. Class LZ01. Theme: Lingkungan Alam.

Link Terkait:

Canva: [PPT](https://canva.link/d403zux6vbz05gn)

Figma: [Figma](https://www.figma.com/design/LUsLvGVUhvskfA6xrAiSrP/sisaguna?node-id=0-1&t=Ac735hftPAYuJkT2-1)

## Contributors

- Fadhlan Nur Rachman (2802491690)
- Dian Rakhmawati Lestari (2802539085)
- Nasauramecca Nour Haqqanshah Shodiqin (2802541921)
- Catherine Zaneta Adji (2802512442)

## Status Pengerjaan

Layar Home sudah dibangun dengan data mock (belum tersambung ke Supabase). Layar lain (login, register, category list, checkout, order, address, profile, merchant, admin) belum dikerjakan.

## Tech Stack

| Layer | Teknologi | Catatan |
|---|---|---|
| UI | Jetpack Compose + Material3 | Satu satunya jalan dapat animasi halus yang diminta desain |
| Bahasa | Kotlin | Wajib untuk Compose |
| Navigasi | Navigation Compose | Type safe nav dengan sealed class routes |
| Async / DI | Kotlin Coroutines + Flow, Hilt | Standar Android modern |
| Backend | Supabase (supabase kt) | Belum tersambung, masih pakai mock repository |
| Image loading | Coil | Ringan, native Compose support |
| Local state | ViewModel + StateFlow | Bukan LiveData |
| Build | Gradle 8.9, AGP 8.5.2, compileSdk 34, minSdk 26 | |

## Halaman

| Layar | Status | Deskripsi |
|---|---|---|
| Landing / Splash | Belum dikerjakan | Logo, warna brand hijau |
| Login | Belum dikerjakan | Email / HP, password, lupa kata sandi |
| Register + role picker | Belum dikerjakan | Pengguna biasa vs mitra restoran |
| Home / Feed | Sudah dibangun (mock data) | Search, banner promo, kategori 3 tier, rail terdekat / hemat / ternak / kompos |
| Category list | Belum dikerjakan | Filter chip 3 tier, grid 2 kolom |
| Alamat & Preferensi | Belum dikerjakan | Multi alamat dengan status aktif, toggle notifikasi per kategori |
| Status Pesanan | Belum dikerjakan | Timeline status, kode pickup, lokasi & kontak mitra, metode bayar |

## Struktur Project

```
SisaGuna/
├── app/                              Module aplikasi utama (Kotlin + Jetpack Compose)
│   ├── build.gradle.kts              Konfigurasi module: dependency, compileSdk, minSdk
│   ├── proguard-rules.pro            Aturan ProGuard untuk build release
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/sisaguna/android/
│       │   ├── MainActivity.kt       Entry point, memasang tema dan NavGraph
│       │   ├── SisaGunaApp.kt        Application class, entry point Hilt
│       │   ├── data/
│       │   │   ├── model/            Listing, Merchant, enum tier (domain model)
│       │   │   └── repository/       Interface repository + FakeListingRepository (mock)
│       │   ├── di/                   Module Hilt, mengikat interface repository ke implementasinya
│       │   ├── navigation/           Screen (sealed class) dan NavGraph (Navigation Compose)
│       │   ├── feature/
│       │   │   └── home/             HomeScreen, HomeViewModel, HomeUiState
│       │   └── ui/
│       │       ├── theme/            Color, Type, Shape, Theme (Material3 + token brand)
│       │       ├── components/       Komponen primitif lintas layar: SgChip, SgBadge, SgInput
│       │       └── domain/           Komponen komposit domain: ListingCard, TierBadge, CountdownPill
│       └── res/
│           ├── values/               strings.xml, colors.xml, themes.xml
│           ├── drawable/             Aset vector
│           └── mipmap-anydpi-v26/    Ikon launcher adaptif
│
├── figma/                            Referensi visual Figma (screenshot layar, bukan sumber data)
│
├── gradle/
│   ├── libs.versions.toml            Version catalog semua dependency
│   └── wrapper/                      Gradle wrapper (jar dan properties)
│
├── ANDROID_CLAUDE.md                 Konteks produk, stack, dan aturan kerja untuk Claude Code
├── build.gradle.kts                  Konfigurasi plugin level root
├── settings.gradle.kts               Daftar module Gradle
├── gradle.properties                 Konfigurasi JVM dan flag Gradle / AndroidX
├── gradlew, gradlew.bat              Launcher Gradle wrapper
└── README.md                         Dokumen ini
```

Aturan penempatan file mengikuti `ANDROID_CLAUDE.md`: Composable tidak pernah memanggil Supabase langsung, semua lewat Repository lalu ViewModel lalu StateFlow lalu Composable collect.

## Setup Lokal

```
git clone https://github.com/ddrlve/SisaGuna.git
cd SisaGuna
```

Buka folder project di Android Studio, tunggu Gradle sync selesai, sambungkan HP fisik (USB debugging aktif) atau siapkan emulator, lalu tekan tombol Run.

Atau lewat terminal:

```
./gradlew installDebug
```

## Deploy Production

Belum ada build production. Rencana ke depan: backend Supabase disambungkan begitu keputusan project (lihat `ANDROID_CLAUDE.md`) dikonfirmasi, lalu distribusi lewat Play Store internal testing untuk kebutuhan demo.

## Catatan

File `local.properties`, folder `.gradle/`, `app/build/`, dan seluruh config lokal tidak ikut masuk repo (lihat `.gitignore`). Warna dan tipografi di `ui/theme/` masih tanda `[Guessing]` sampai hex asli diambil dari Figma Inspect.

Made with care by Group 4
