package com.sisaguna.android.data.model

import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodSafetyAndDeliveryTest {

    private val now = Instant.parse("2026-10-03T10:00:00Z")
    private val all = SafetyCheck.entries.toSet()
    private fun hoursAgo(h: Long) = now.minus(h, ChronoUnit.HOURS)

    @Test
    fun `fresh cooked food with every check is safe`() {
        val a = FoodSafety.assess(ListingTier.HUMAN, hoursAgo(1), StorageMethod.ROOM_TEMP, all, now)
        assertEquals(SafetyLevel.SAFE, a.level)
        assertEquals(now.plus(3, ChronoUnit.HOURS), a.safeUntil)
    }

    @Test
    fun `missing a required check is unsafe even when fresh`() {
        val a = FoodSafety.assess(ListingTier.HUMAN, hoursAgo(0), StorageMethod.ROOM_TEMP, all - SafetyCheck.UNTOUCHED, now)
        assertEquals(SafetyLevel.UNSAFE, a.level)
        assertEquals(listOf(SafetyCheck.UNTOUCHED), a.missingRequired)
    }

    @Test
    fun `optional hygiene check is not required`() {
        val a = FoodSafety.assess(ListingTier.HUMAN, hoursAgo(0), StorageMethod.ROOM_TEMP, all - SafetyCheck.HYGIENE, now)
        assertEquals(SafetyLevel.SAFE, a.level)
    }

    @Test
    fun `last quarter of the window is caution, past it suggests feed`() {
        assertEquals(SafetyLevel.CAUTION, FoodSafety.assess(ListingTier.HUMAN, hoursAgo(3), StorageMethod.ROOM_TEMP, all, now).level)
        val past = FoodSafety.assess(ListingTier.HUMAN, hoursAgo(5), StorageMethod.ROOM_TEMP, all, now)
        assertEquals(SafetyLevel.UNSAFE, past.level)
        assertEquals(ListingTier.ANIMAL_FEED, past.suggestTier)
    }

    @Test
    fun `chilled pudding keeps two days like the interview said`() {
        assertEquals(SafetyLevel.SAFE, FoodSafety.assess(ListingTier.HUMAN, hoursAgo(30), StorageMethod.CHILLED, all, now).level)
        assertEquals(SafetyLevel.UNSAFE, FoodSafety.assess(ListingTier.HUMAN, hoursAgo(49), StorageMethod.CHILLED, all, now).level)
    }

    @Test
    fun `feed and compost are never judged as human food`() {
        val a = FoodSafety.assess(ListingTier.COMPOST, hoursAgo(100), StorageMethod.ROOM_TEMP, emptySet(), now)
        assertEquals(SafetyLevel.NOT_FOR_HUMANS, a.level)
        assertNull(a.safeUntil)
    }

    @Test
    fun `priority costs more and arrives sooner than saver`() {
        val p = DeliveryPricing.quote(Courier.GOSEND, DeliverySpeed.PRIORITY, 3.0, 10_000, 10)
        val s = DeliveryPricing.quote(Courier.GOSEND, DeliverySpeed.SAVER, 3.0, 10_000, 10)
        assertTrue(p.fee > s.fee)
        assertTrue(p.etaMinMinutes < s.etaMinMinutes)
        assertEquals(0, p.fee % 500)
    }

    @Test
    fun `gosend promo halves delivery above Rp 20rb, capped at Rp 5rb`() {
        val q = DeliveryPricing.quote(Courier.GOSEND, DeliverySpeed.STANDARD, 10.0, 25_000, 10)
        assertEquals((q.fee / 2).coerceAtMost(5_000), q.discount)
        assertEquals(q.fee - q.discount, q.payable)
        assertEquals(0, DeliveryPricing.quote(Courier.GOSEND, DeliverySpeed.STANDARD, 10.0, 5_000, 10).discount)
    }

    @Test
    fun `order total adds delivery but savings ignore it`() {
        val line = OrderLine("l", "Nasi", ListingTier.HUMAN, "", 2, 5_000, 20_000)
        val quote = DeliveryPricing.quote(Courier.GRAB, DeliverySpeed.STANDARD, 2.0, 10_000, 10)
        val order = Order(
            "o", "SG-1", Merchant("m", "M", true, MerchantStatus.APPROVED, ""), listOf(line), PaymentKind.QRIS, "",
            OrderStatus.READY, now, now, fulfillment = Fulfillment.DELIVERY, delivery = quote,
        )
        assertEquals(10_000 + quote.payable, order.total)
        assertEquals(30_000, order.savings)
    }
}
