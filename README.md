# SisaGuna

Aplikasi Android untuk menyelamatkan makanan berlebih dari UMKM kuliner. Warung, kantin, bakery, dan resto kecil memposting makanan yang masih layak sebelum toko tutup, dengan harga jauh lebih murah atau gratis. Pembeli di sekitar memesan lewat aplikasi, lalu mengambil sendiri atau memakai kurir. Sisa yang tidak layak dimakan manusia disalurkan untuk pakan ternak dan kompos.

**Mata kuliah:** Venture Creation (ENPR6312), BINUS University
**Kelompok:** Group 4, Kelas LZ01
**Tema:** Lingkungan Alam

---

## Daftar Isi

1. [Coba Aplikasinya](#coba-aplikasinya)
2. [Masalah dan Solusi](#masalah-dan-solusi)
3. [Fitur Utama](#fitur-utama)
4. [Model Bisnis](#model-bisnis)
5. [Status Pengerjaan](#status-pengerjaan)
6. [Tech Stack](#tech-stack)
7. [Struktur Project](#struktur-project)
8. [Menjalankan dari Source Code](#menjalankan-dari-source-code)
9. [Tim](#tim)
10. [Link Terkait](#link-terkait)

---

## Coba Aplikasinya

**Download APK:** buka halaman [Releases](https://github.com/ddrlve/SisaGuna/releases), lalu unduh file `SisaGuna-v0.4.0-debug.apk`.

**Cara install di HP Android (minimal Android 8.0):**

1. Buka file APK yang sudah diunduh.
2. Jika muncul *"Install unknown apps"*, izinkan untuk browser atau aplikasi file yang dipakai.
3. Jika Google Play Protect memberi peringatan, pilih **More details**, lalu **Install anyway**. Peringatan ini muncul karena aplikasi tidak dipasang dari Play Store.

**Cara mencoba:**

| Ingin mencoba | Caranya |
|---|---|
| Langsung melihat isi aplikasi | Di halaman awal pilih **Jelajahi tanpa akun** |
| Alur lengkap sebagai pembeli | Pilih **Masuk**, isi email dan password apa saja (login masih simulasi) |
| Sisi mitra / penjual | Profil, lalu Pengaturan, lalu ubah mode ke **Mitra / penjual** |
| Bahasa Inggris dan mode gelap | Profil, lalu Pengaturan |

> Semua data di aplikasi ini masih data contoh dan belum tersambung ke server. Pembayaran QRIS dan e-wallet hanya simulasi, tidak ada uang yang ditarik.

---

## Masalah dan Solusi

**Masalah.** Banyak UMKM kuliner membuang makanan yang masih layak setiap hari karena tidak terjual sebelum tutup. Ini merugikan penjual dan menambah sampah organik.

**Solusi.** SisaGuna mempertemukan penjual dengan pembeli di sekitar dalam waktu singkat:

- Penjual memposting makanan berlebih dengan diskon 50-70% atau gratis.
- Pembeli memesan dan membayar di aplikasi, lalu mengambil sendiri atau memakai kurir.
- Makanan yang sudah tidak layak dikonsumsi manusia dialihkan menjadi pakan ternak atau bahan kompos.
- Setiap posting makanan wajib melewati checklist kelayakan, dan sistem menghitung batas aman konsumsi dari waktu masak serta cara penyimpanan.

---

## Fitur Utama

### Untuk pembeli

| Fitur | Keterangan |
|---|---|
| Beranda | Pilih lokasi, banner promo, voucher, filter, makanan terdekat, terlaris, dan diskon terbesar |
| Kategori | Siap santap, pakan ternak, dan kompos |
| Detail produk | Foto, harga normal dan diskon, stok, label halal, alergen, status kelayakan, review |
| Halaman toko | Profil toko, jam buka, menu tersedia, dan review pembeli |
| Ringkasan pesanan | Ambil sendiri atau kurir (GoSend, GrabExpress, SPX Instant, Lalamove) dengan pilihan Prioritas, Standar, atau Hemat; alamat tujuan bisa diubah; tambah menu lain dari toko yang sama; voucher; rincian biaya |
| Pembayaran | QRIS, GoPay, OVO, DANA (simulasi) |
| Aktivitas | Status pesanan, kode pickup, rating, dan komplain |
| Chat ke toko | Tanya stok, jam masak, halal, atau pengantaran sebelum pesan, dengan pertanyaan cepat sekali ketuk |
| Tersimpan dan notifikasi | Toko favorit, notifikasi pesanan dan promo, pengaturan per kategori |
| Profil | Avatar buah atau foto sendiri, level penyelamat dan dampak, alamat, riwayat, metode bayar, bantuan, privasi |

### Untuk mitra (penjual)

| Fitur | Keterangan |
|---|---|
| Dashboard | Pendapatan bersih hari ini (sudah dipotong komisi), pesanan masuk, listing aktif |
| Posting makanan | Siap santap (per porsi) atau pakan dan kompos (per gram/kg), dengan checklist kelayakan |
| Konfirmasi pickup | Cocokkan kode dari pembeli sebelum menyerahkan pesanan |
| Chat pembeli | Balas pertanyaan pembeli, dengan balasan cepat untuk penjual yang sedang sibuk |
| Notifikasi | Pemberitahuan pesanan dan pengambilan |

### Umum

- Bahasa Indonesia dan Inggris, bisa diganti di Pengaturan.
- Mode terang dan gelap.
- Mode tamu: bisa melihat-lihat tanpa akun, aksi tertentu meminta login.

---

## Model Bisnis

Biaya platform dibagi antara pembeli dan mitra:

| Pihak | Biaya | Keterangan |
|---|---|---|
| Pembeli | Biaya layanan Rp1.000-3.000 per pesanan | Rp1.000 untuk belanja di bawah Rp25.000, Rp2.000 di bawah Rp75.000, Rp3.000 di atasnya. Makanan gratis tidak dikenakan biaya |
| Mitra | Komisi 10% dari penjualan | Ditampilkan langsung di dashboard sebagai pendapatan bersih |
| Kurir | Ongkir diteruskan penuh ke penyedia kurir | Tidak dikenakan komisi |

---

## Status Pengerjaan

**Versi saat ini: 0.4.0 (demo)**

| Bagian | Status |
|---|---|
| Alur pembeli end to end | Selesai, memakai data contoh |
| Dashboard dan posting mitra | Selesai, memakai data contoh |
| Bahasa Inggris dan mode gelap | Selesai |
| Chat pembeli dan mitra | Selesai, balasan toko masih otomatis dari data contoh |
| Unit test | 106 test, semua lulus |
| Integrasi backend (Supabase) | Belum, dikerjakan di repo backend |
| Pembayaran asli (payment gateway) dan API kurir | Belum |
| Panel admin | Belum |

---

## Tech Stack

| Bagian | Teknologi |
|---|---|
| Bahasa | Kotlin 2.0.21 |
| UI | Jetpack Compose, Material 3 |
| Navigasi | Navigation Compose |
| Arsitektur | MVVM: Repository, ViewModel, StateFlow |
| Dependency injection | Hilt |
| Gambar | Coil |
| Peta | osmdroid (OpenStreetMap, tanpa API key) |
| Build | Gradle 8.9, AGP 8.5.2, JDK 17 |
| Android | minSdk 26 (Android 8.0), targetSdk 34 |

---

## Struktur Project

```
app/src/main/java/com/sisaguna/android/
├── MainActivity.kt        Titik masuk aplikasi
├── core/session/          Sesi login dan mode tamu
├── data/
│   ├── model/             Model data: produk, toko, pesanan, kurir, biaya, keamanan pangan
│   └── repository/        Sumber data (saat ini data contoh di memori)
├── di/                    Konfigurasi Hilt
├── navigation/            Daftar layar, navigasi, bottom bar
├── feature/               Satu folder per fitur, berisi Screen dan ViewModel
│   ├── auth/  home/  category/  listing/  checkout/
│   ├── activity/  saved/  notifications/  profile/
│   └── address/  upload/  settings/  splash/
└── ui/
    ├── theme/             Warna, tipografi, bentuk, jarak
    ├── components/        Komponen dasar (tombol, input, chip)
    ├── domain/            Komponen khusus aplikasi (kartu produk, voucher)
    └── i18n/              Terjemahan Indonesia dan Inggris
```

**Alur data:** Repository, lalu ViewModel, lalu tampilan. Tampilan tidak pernah memanggil server langsung. Saat backend siap, cukup ganti implementasi repository di `di/RepositoryModule.kt` tanpa mengubah tampilan.

---

## Menjalankan dari Source Code

**Kebutuhan:** Android Studio (versi terbaru) dan JDK 17.

```
git clone https://github.com/ddrlve/SisaGuna.git
cd SisaGuna
```

1. Buka folder project di Android Studio dan tunggu Gradle sync selesai.
2. Sambungkan HP (USB debugging aktif) atau jalankan emulator.
3. Tekan **Run**.

Lewat terminal:

```
./gradlew installDebug        # pasang ke HP yang tersambung
./gradlew assembleDebug       # buat APK di app/build/outputs/apk/debug/
./gradlew testDebugUnitTest   # jalankan unit test
```

---

## Tim

| Nama | NIM |
|---|---|
| Dian Rakhmawati Lestari | 2802539085 |
| Fadhlan Nur Rachman | 2802491690 |
| Nasauramecca Nour Haqqanshah Shodiqin | 2802541921 |
| Catherine Zaneta Adji | 2802512442 |

---

## Link Terkait

| Tipe | Link |
|---|---|
| Presentasi | [Canva](https://canva.link/d403zux6vbz05gn) |
| Desain | [Figma Design](https://www.figma.com/design/LUsLvGVUhvskfA6xrAiSrP/sisaguna?node-id=0-1&t=Ac735hftPAYuJkT2-1) |
| Prototype | [Figma Prototype](https://www.figma.com/proto/LUsLvGVUhvskfA6xrAiSrP/sisaguna?node-id=255-5689&t=FJrDzffI5BvBqfaI-1) |
| Frontend (repo ini) | https://github.com/ddrlve/SisaGuna |
| Backend | https://github.com/FadhRach/sisaguna-be |

Foto makanan dan logo kurir berasal dari Wikimedia Commons; sumber lengkap ada di `app/src/main/assets/img/CREDITS.txt`.
