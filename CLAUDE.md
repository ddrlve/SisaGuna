# SisaGuna

Android app (Kotlin + Jetpack Compose): marketplace food-rescue untuk UMKM kuliner Indonesia. Warung/kantin/bakery posting makanan sisa diskon 50-70% atau gratis; pembeli ambil sendiri atau pakai kurir. Makanan tak layak konsumsi manusia diarahkan ke pakan ternak / kompos (tier `HUMAN`, `ANIMAL_FEED`, `COMPOST`).
Proyek kuliah BINUS "Venture Creation (ENPR6312)", Group 4. Repo: `ddrlve/SisaGuna`. Backend: repo terpisah `FadhRach/sisaguna-be` (Go, dikloning ke `sisaguna-be/` hanya untuk dibaca; jangan diubah dari sini).

**Status: tersambung ke backend** `https://sisaguna.fadh.tech/api/v1` (WebSocket `wss://sisaguna.fadh.tech/ws`). Data terpusat; server adalah sumber kebenaran untuk harga, stok, dan aturan bisnis.

## Perintah
```
./gradlew assembleDebug        # APK: SisaGuna-v0.5.0-debug.apk
./gradlew installDebug
./gradlew testDebugUnitTest    # 180 unit test (app/src/test), semua JVM
./gradlew assembleRelease      # R8 aktif, APK belum ditandatangani
```
Tidak ada androidTest, CI, atau lint config. Butuh JDK 17+ terpasang sendiri (plugin foojay di `settings.gradle.kts` tidak mengunduh apa pun karena tidak ada `jvmToolchain`). Di Git Bash, heredoc panjang dengan banyak tanda kutip sering gagal parse: tulis skrip ke file lalu jalankan.

## Stack
Kotlin 2.0.21, AGP 8.5.2, Gradle 8.9, compile/target SDK 34, min SDK 26. Compose BOM 2024.09.02 + Material3, Navigation Compose 2.8.0, Hilt 2.51.1 (KSP), Coil 2.7.0, osmdroid. Jaringan: Retrofit 2.11 + OkHttp 4.12 + kotlinx-serialization 1.7.3; token di `androidx.security:security-crypto`. Versi di `gradle/libs.versions.toml`. Tes: JUnit4, coroutines-test, MockWebServer.

## Arsitektur
MVVM: interface repository mengekspos `StateFlow`, satu `@HiltViewModel` per layar. Package root `app/src/main/java/com/sisaguna/android`:
- `data/remote/` -- `ApiService` (semua endpoint), `dto/*` (snake_case lewat `ApiJson` + `JsonNamingStrategy.SnakeCase`), `Mappers.kt` (DTO -> model domain), `TokenStore`/`AuthInterceptor`/`TokenAuthenticator`, `RealtimeClient` (WebSocket), `Errors.kt` (`AppException`: `NetworkException`, `ApiException(code, status, extras)`).
- `data/push/` -- FCM siap-pakai: `PushRegistrar` (daftarkan token ke `PUT /me/devices` setelah login), `FcmService`, `NotificationChannels`; logout melepas token di `ApiAuthRepository`. Tanpa `app/google-services.json` plugin Google Services tidak dipasang dan push dilewati (build tetap jalan).
- `data/repository/` -- interface + implementasi `Api*Repository` (yang diikat di `di/RepositoryModule.kt`). `InMemoryCartRepository` sengaja lokal (keranjang satu toko). `SeedImages.kt` = gambar bawaan Landing/Home.
- `di/NetworkModule.kt` (OkHttp/Retrofit/Json, `@ApplicationScope`, `@BareClient` untuk refresh & WS) dan `RepositoryModule.kt`.
- `core/session/SessionViewModel` (tamu/masuk, dari `AuthRepository`), `core/ui/AppMessenger` (pesan error/sukses global, ditampilkan snackbar di `MainActivity`).
- `navigation/Screen.kt` + `NavGraph.kt`; `feature/*` (auth, home + MerchantHome, category, listing, saved, checkout, activity, chat, notifications, profile, address, upload, store, settings); `ui/components` (`Sg*`), `ui/domain`, `ui/theme`, `ui/i18n`.

### Aturan jaringan yang tidak boleh dilanggar
- Respons sukses `{"data","meta"}`; error `{"error":{"code","message",...extras}}` -> `apiData { }`/`apiCall { }` memetakan semuanya ke `AppException`. ViewModel menangkap `AppException` (jangan telan `CancellationException`).
- **Refresh token berotasi**: refresh paralel dianggap pencurian. Hanya `TokenAuthenticator` yang me-refresh, di bawah satu lock. Jangan panggil `/auth/refresh` dari tempat lain.
- `Idempotency-Key` wajib di `POST /orders` dan `POST /listings`; satu niat = satu key (lihat `CheckoutViewModel.attempt`). Kirim `expected_total`; `409 price_changed` -> pakai angka server (`ServerPrice`).
- Enum API huruf kecil snake_case == nama enum Kotlin di-lowercase; gunakan `toEnum()` (nilai tak dikenal jatuh ke default, bukan crash).
- Perubahan kecil (baca notifikasi, ganti alamat, simpan toko) optimistis + rollback dengan `AppMessenger`; aksi penting (checkout, kirim chat, buat listing) `suspend` dan melempar error ke ViewModel.
- Repository melakukan reset sendiri saat `AuthRepository.isAuthenticated` false dan memuat ulang saat true.
- Aturan server yang diikuti app: hanya mitra yang menyelesaikan pesanan (kode pickup, tidak ada tombol "sudah diambil" untuk pembeli); ulasan hanya dari pesanan selesai (<= 7 hari); komplain <= 24 jam; batalkan hanya saat `can_cancel`; foto listing wajib minimal 1; toko baru berstatus `pending` sampai disetujui admin.
- WebSocket hanya memberi tahu; REST tetap sumber kebenaran. Setelah `Connected`, data yang terlewat diambil ulang.

## Konvensi
- Copy UI ditulis **Bahasa Indonesia**. Terjemahan English lewat `ui/i18n/Text.kt` (`Text` pengganti yang memanggil `tr()`, `EnDictionary`, `EnPatterns`, `l(id, en)` inline). Teks dari server (notifikasi, voucher, `todays_offer`, pesan error) tidak diterjemahkan.
- Komentar kode English; banyak komentar merujuk node Figma. Test name English.
- `Sg` untuk elemen design system; route lowercase dengan helper `routeFor()`.
- Shortcut yang disengaja ditandai komentar `shortcut:` (mis. daftar ulasan toko dan pesanan masuk mitra hanya 50 teratas; pesanan riwayat dan notifikasi sudah punya "Muat lebih banyak" lewat `meta.next_cursor`).
- Unit test: `Fake*Repository` ada di `app/src/test` (bukan di APK); `testutil/` berisi `MainDispatcherRule`, `TestListingRepository`, `FakeAuthRepository`. `src/test/resources/contract/*.json` = respons asli server untuk `ContractFixturesTest` (unduh ulang bila backend berubah).

## Gotcha
- `ApiException.status` 404 pada `/me/merchant` berarti akun belum punya toko (normal, bukan error).
- Android menolak HTTP polos (`usesCleartextTraffic=false`); server lokal butuh `network_security_config` khusus debug. `allowBackup=false` agar token tidak ikut backup.
- Release memakai R8 + `proguard-rules.pro` (serializer DTO, interface Retrofit, Tink). Build rilis lolos tetapi belum diuji di perangkat.
- Pembayaran/kurir asli belum ada di backend (mode `PARTNERS_MODE=dummy`); UI memberi label "simulasi" (`BuildConfig.PARTNERS_SIMULATED`). Login hanya dengan email. Lupa kata sandi (kode 6 digit lewat email) sudah ada.
- Push butuh `app/google-services.json` (dari Firebase) dan kredensial FCM di server; teks push yang digambar sistem saat app tertutup tetap bahasa server. Izin `POST_NOTIFICATIONS` diminta sekali setelah login pertama (`MainActivity`).
- Panel admin (`feature/admin`, tab Toko/Komplain/Voucher) hanya muncul untuk akun `role=admin`; belum diuji end to end karena akun admin tidak tersedia.
- Terjemahan Inggris: teks Indonesia baru perlu entri di `EnDictionary`/`ui/i18n/EnNewCopy.kt` (pesan error server dan teks notifikasi server juga di `EnNewCopy`, ada test yang menjaganya); `l(id, en)` tidak perlu entri.
- `EnDictionary`/`SeedContentEn` masih memuat terjemahan untuk data contoh lama (aman dihapus bertahap).
- Temuan backend yang belum diperbaiki pemiliknya dicatat di laporan sesi (mis. `.gitignore` menelan `cmd/seed/data`), bukan di repo ini.
