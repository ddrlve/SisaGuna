package com.sisaguna.android.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppFeesTest {

    @Test
    fun `free food has no service fee`() {
        assertEquals(0, AppFees.buyerServiceFee(0))
    }

    @Test
    fun `buyer service fee steps from 1k to 3k by basket size`() {
        assertEquals(1_000, AppFees.buyerServiceFee(3_000))
        assertEquals(1_000, AppFees.buyerServiceFee(24_999))
        assertEquals(2_000, AppFees.buyerServiceFee(25_000))
        assertEquals(2_000, AppFees.buyerServiceFee(74_999))
        assertEquals(3_000, AppFees.buyerServiceFee(75_000))
        assertEquals(3_000, AppFees.buyerServiceFee(500_000))
    }

    @Test
    fun `merchant keeps 90 percent of the sale`() {
        assertEquals(1_800, AppFees.merchantCommission(18_000))
        assertEquals(16_200, AppFees.merchantPayout(18_000))
        assertEquals(0, AppFees.merchantPayout(0))
    }
}
