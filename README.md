# SisaGuna

Aplikasi mobile redistribusi makanan berlebih atau sisa makanan untuk mitra FnB seperti UMKM kuliner Indonesia.
Warung, kantin, bakery, dan resto kecil memposting makanan berlebih sebelum
menjadi sampah. Pembeli di sekitar dapat membeli atau mengambilnya langsung di tempat by app.

Venture Creation (ENPR6312), BINUS.

Group 4.

Class LZ01.

Theme: Lingkungan Alam.

Canva PPT Idea: [PPT](https://canva.link/d403zux6vbz05gn)

Figma: [Figma](https://www.figma.com/design/LUsLvGVUhvskfA6xrAiSrP/sisaguna?node-id=0-1&t=Ac735hftPAYuJkT2-1)

## Tentang project ini

Repo ini berisi versi native Android dari SisaGuna, ditulis dengan Kotlin dan
Jetpack Compose, dijalankan paralel dengan app Expo React Native tim. Konteks
lengkap keputusan produk, stack, dan cara kerja dengan Claude Code ada di
`ANDROID_CLAUDE.md`. Baca file itu dulu sebelum menulis kode di project ini.

Status saat ini: layar Home sudah dibangun dengan data mock (belum tersambung
ke Supabase). Layar lain (login, register, category list, checkout, order,
address, profile, merchant, admin) belum dikerjakan.

## Requirement

1. Android Studio versi terbaru (disarankan Ladybug atau lebih baru).
2. JDK 17.
3. Android SDK dengan compileSdk 34 dan minSdk 26.

## Cara menjalankan

1. Buka folder project ini di Android Studio, tunggu Gradle sync selesai.
2. Sambungkan HP fisik dengan USB debugging aktif, atau siapkan emulator lewat
   Device Manager.
3. Tekan tombol Run di Android Studio.

Atau lewat terminal:

```
./gradlew installDebug
```

## Struktur folder

```
SisaGuna/
  ANDROID_CLAUDE.md          konteks produk, stack, dan aturan kerja untuk Claude Code
  README.md                  dokumen ini
  figma/                     referensi visual Figma (screenshot layar, jangan jadi sumber data)
  settings.gradle.kts        daftar module Gradle
  build.gradle.kts           konfigurasi plugin level root
  gradle.properties          konfigurasi JVM dan flag Gradle/AndroidX
  gradle/
    libs.versions.toml       version catalog semua dependency
    wrapper/                 Gradle wrapper (jar dan properties)
  gradlew, gradlew.bat       launcher Gradle wrapper untuk mac/linux dan windows

  app/
    build.gradle.kts         konfigurasi module app (dependency, compileSdk, minSdk)
    proguard-rules.pro       aturan ProGuard untuk build release
    src/main/
      AndroidManifest.xml
      java/com/sisaguna/android/
        MainActivity.kt          entry point, memasang tema dan NavGraph
        SisaGunaApp.kt           Application class, entry point Hilt

        data/
          model/                 data class domain: Listing, Merchant, enum tier
          repository/             interface repository plus implementasi mock
                                   (FakeListingRepository), akan diganti implementasi
                                   Supabase begitu keputusan project dikonfirmasi

        di/                      module Hilt, mengikat interface repository ke
                                   implementasinya

        navigation/              sealed class Screen dan NavGraph (Navigation Compose)

        feature/
          home/                  layar Home: HomeScreen, HomeViewModel, HomeUiState

        ui/
          theme/                 Color, Type, Shape, Theme (Material3 + token brand)
          components/             komponen primitif lintas layar: SgChip, SgBadge, SgInput
          domain/                 komponen komposit khusus domain aplikasi:
                                   ListingCard, TierBadge, CountdownPill

      res/
        values/                  strings.xml, colors.xml, themes.xml
        drawable/                aset vector
        mipmap-anydpi-v26/       ikon launcher adaptif
```

Aturan penempatan file mengikuti `ANDROID_CLAUDE.md`: Composable tidak pernah
memanggil Supabase langsung, semua lewat Repository lalu ViewModel lalu
StateFlow lalu Composable collect.

## Catatan

File `local.properties`, folder `.gradle/`, `app/build/`, dan seluruh config
lokal tidak ikut masuk repo (lihat `.gitignore`). Warna dan tipografi di
`ui/theme/` masih tanda `[Guessing]` sampai hex asli diambil dari Figma
Inspect.
