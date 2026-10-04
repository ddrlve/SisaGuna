package com.sisaguna.android.data.model

import kotlin.math.roundToInt

enum class Fulfillment { PICKUP, DELIVERY }

/** Third-party couriers SisaGuna hands the order to — we never run our own drivers. */
enum class Courier(val label: String, val app: String, val brandColor: Long, val baseFee: Int, val perKm: Int, private val logo: String) {
    GOSEND("GoSend", "Gojek", 0xFF00AA13, 6_000, 2_500, "courier_gosend"),
    GRAB("GrabExpress", "Grab", 0xFF00B14F, 6_500, 2_400, "courier_grab"),
    SHOPEE("SPX Instant", "Shopee Express", 0xFFEE4D2D, 5_000, 2_600, "courier_shopee"),
    LALAMOVE("Lalamove", "Lalamove", 0xFFF16622, 8_000, 2_200, "courier_lalamove");

    /** Bundled brand mark (assets/img/couriers), loaded by Coil like the seed photos. */
    val logoUrl: String get() = "file:///android_asset/img/couriers/$logo.png"
}

enum class DeliverySpeed(val label: String, val labelEn: String, val feeFactor: Double, val extraMinutes: Int, val blurb: String, val blurbEn: String) {
    PRIORITY("Prioritas", "Priority", 1.35, 0, "Langsung dijemput, tanpa digabung order lain", "Picked up right away, never batched with other orders"),
    STANDARD("Standar", "Standard", 1.0, 10, "Kurir terdekat, rute normal", "Nearest courier, normal route"),
    SAVER("Hemat", "Saver", 0.75, 25, "Lebih lama, bisa digabung order lain", "Slower, may be batched with other orders"),
}

data class DeliveryQuote(
    val courier: Courier,
    val speed: DeliverySpeed,
    val fee: Int,
    val discount: Int,
    val etaMinMinutes: Int,
    val etaMaxMinutes: Int,
    val promoLabel: String? = null,
    val promoLabelEn: String? = null,
) {
    val payable: Int get() = (fee - discount).coerceAtLeast(0)
}

object DeliveryPricing {
    /** Below this the buyer is nudged to pick up instead; over 15 km food quality suffers. */
    const val MAX_DISTANCE_KM = 15.0

    fun quote(courier: Courier, speed: DeliverySpeed, distanceKm: Double, subtotal: Int, prepMinutes: Int): DeliveryQuote {
        val km = distanceKm.coerceAtLeast(0.5)
        val raw = (courier.baseFee + courier.perKm * km) * speed.feeFactor
        val fee = (raw / 500.0).roundToInt() * 500 // couriers quote in Rp 500 steps
        // Fake promo table — stands in for the courier's live promo API.
        val (discount, promo) = when {
            courier == Courier.GOSEND && subtotal >= 20_000 -> (fee / 2).coerceAtMost(5_000) to ("Diskon ongkir 50%" to "50% off delivery")
            courier == Courier.GRAB && speed == DeliverySpeed.SAVER -> 3_000.coerceAtMost(fee) to ("Hemat Rp 3.000" to "Save Rp 3,000")
            courier == Courier.SHOPEE && subtotal >= 15_000 -> 2_000.coerceAtMost(fee) to ("Potongan Rp 2.000" to "Rp 2,000 off")
            else -> 0 to null
        }
        val travel = (km * 4).roundToInt() + 8
        val etaMin = prepMinutes + travel + speed.extraMinutes
        return DeliveryQuote(courier, speed, fee, discount, etaMin, etaMin + 10, promo?.first, promo?.second)
    }

    fun allQuotes(distanceKm: Double, subtotal: Int, prepMinutes: Int, speed: DeliverySpeed): List<DeliveryQuote> =
        Courier.entries.map { quote(it, speed, distanceKm, subtotal, prepMinutes) }.sortedBy { it.payable }
}
