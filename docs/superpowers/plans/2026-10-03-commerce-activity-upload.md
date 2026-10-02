# On-Device Feedback Round 2: Commerce, Activity, Upload, Profile Pages (3b + 4)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Close every gap the user reported after testing the 3a build on their Redmi Note 11 (2026-10-03).

**Architecture:** Same as 3a. In-memory `@Singleton` fake repositories expose `StateFlow`s, there is one `@HiltViewModel` per screen, and screens are stateless content plus a thin Hilt wrapper. No backend.

**Tech Stack:** As 3a, plus `org.osmdroid:osmdroid-android` (OpenStreetMap, no API key) for the in-app map picker.

**Specs:** `docs/superpowers/specs/2026-10-01-saved-profile-home-polish-design.md` (3a + addendum). This plan adds the 3b scope and sub-project 4 (Activity & Upload) from the user's feedback list. The user asked to "complete everything that says Segera hadir".

## Decisions (made without an answer; recommended defaults)

- Map picker: OpenStreetMap through osmdroid. The user drags the map under a fixed center pin, reverse-geocodes, and taps "Pilih lokasi ini". Google Maps would need a billing-enabled API key.
- Payment: QRIS, GoPay, OVO, DANA, or "Bayar saat ambil". Online methods simulate a ~2s payment. The result is an order with a pickup code.
- Cart: single merchant, as in Surplus. Adding from another merchant asks to replace the cart.
- Theme: app is forced light. The device was in dark mode, which made unselected chip labels invisible.

## User feedback, mapped to tasks

| # | Feedback | Task |
|---|---|---|
| 1 | Notification filter chips invisible | A1 (forced light theme + explicit `SgChip` colors) |
| 2 | Home tab "Ternak & Kompos · 4" | A1 (label without count) |
| 3 | Search filter on "Cari makanan"; clear button meanings | A3 |
| 4 | Location sheet: black scrim, needs blur; map picker in app; "Lokasi saat ini" doesn't work; button press looks odd | A2 |
| 5 | Saved chips invisible; Saved looks flat | A1, F2 |
| 6 | Everything "Segera hadir" | E1–E7 |
| 7 | Listing tap: no description, no checkout | B1–B3 |
| 8 | Merchant name on the listing opens that store's catalog | B2 |
| 9 | Activity flat, no rating | C1–C2 |
| 10 | Upload not built | D1 |
| 11 | Landing has no animation | F1 |
| 12 | Launcher logo too big vs Surplus | A1 (art 64→42 of 108) |
| 13 | Profile rows not clickable | E1–E7 |
| 14 | Promo banner carousel (several banners, not one) | A4 |
| 15 | Vouchers | A4 (Home voucher strip + voucher sheet, applied at checkout) |
| 16 | "Lihat Semua" too small | A4 (48dp pill button) |
| 17 | More glass effects and animations, using Surplus and other well-designed apps as references | A2, A4, F1 (blurred sheets, frosted top bar on scroll, staggered entrances) |

## Data layer (shared by B–E)

- `Listing` += `description: String = ""`, `stock: Int = 5`, `pickupStart: Instant? = null`.
- `ListingRepository` += `getListingDetail(id): ListingDetail?`, `addListing(Listing)`, `deleteListing(id)`, `MY_MERCHANT_ID = "m0"` ("Dapur Budi", the signed-in user's store). `FakeListingRepository` keeps listings in a `MutableStateFlow`.
- `CartRepository`: `cart: StateFlow<Cart>`, `add(listingId, merchantId, qty): AddResult`, `replace(...)`, `setQuantity`, `clear`.
- `OrderRepository`: `orders: StateFlow<List<Order>>`, `place(...)`, `markPickedUp`, `cancel`, `rate(orderId, stars, tags, comment)`. Seeded with one ready, one completed unrated, one completed rated, and one cancelled order.
- `AddressRepository`: list + selected, plus add/update/delete/select. Used by the Home sheet and Profile > Alamat.
- `PaymentMethodRepository`: list, default, add e-wallet, remove.

## Tasks

- **A1** Theme lock, chip colors, tab label, launcher art.
- **A2** Location sheet: blurred backdrop (API 31+) and a light scrim; real address search (Geocoder); robust current location (last known, then GPS/network race, 12s timeout); full-screen osmdroid map picker; saved addresses from `AddressRepository`; "Tambah alamat baru" goes to the map picker, then a label form.
- **A3** Home filter sheet: Urutkan (Terdekat / Termurah / Diskon terbesar / Segera berakhir), Gratis saja, Jarak maks (1/2/5 km/Semua). The active filter count shows on the filter button. Reset + Terapkan.
- **A4** Home polish: auto-advancing banner carousel (4 banners, pager dots, pauses on touch), voucher strip ("Voucher untuk kamu"), vouchers applicable at checkout, "Lihat Semua" as a 36dp-tall pill with arrow, frosted top bar once scrolled.
- **B1** Listing detail: hero, countdown, price, stock, description, pickup window, merchant row (taps to store), "Lainnya dari toko ini" rail, sticky quantity stepper + "Tambah ke keranjang".
- **B2** Store page: Merchant detail generalized (title = merchant name, heart works for unsaved merchants, cart bar).
- **B3** Cart & checkout: lines with steppers, pickup info, payment method, note, summary. Pay leads to a payment sheet (QRIS mock / e-wallet / cash), then a success screen with pickup code, then Activity.
- **C1** Activity: tabs Berlangsung / Riwayat, impact strip, order cards with status, countdown, and code.
- **C2** Order detail: status timeline, big pickup code, "Sudah saya ambil", cancel, rating sheet (stars + tags + comment), and the rated state.
- **D1** Upload: photo picker, title, tier, description, price/gratis, stock, pickup until. Preview and publish to the Home feed and Katalog saya.
- **E1–E7** Profile pages: Alamat, Riwayat Penyelamatan, Metode Pembayaran, Ganti Password, Pusat Bantuan (FAQ), Kebijakan Privasi, Katalog saya (edit/delete).
- **F1** Landing: staggered entrance (hero scale-in, copy rise, CTA fade) and a gently floating hero.
- **F2** Saved polish: summary header, tinted cards with next pickup + item count chips.

Each task ends with `./gradlew testDebugUnitTest assembleDebug` and an install on the device. ViewModels/repositories get unit tests in the 3a style.
