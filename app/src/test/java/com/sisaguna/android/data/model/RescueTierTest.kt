package com.sisaguna.android.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RescueTierTest {

    @Test
    fun `tier follows portion thresholds`() {
        assertEquals(RescueTier.STARTER, RescueTier.of(0))
        assertEquals(RescueTier.STARTER, RescueTier.of(9))
        assertEquals(RescueTier.RESCUER, RescueTier.of(10))
        assertEquals(RescueTier.HERO, RescueTier.of(34))
        assertEquals(RescueTier.LEGEND, RescueTier.of(60))
    }

    @Test
    fun `progress runs from this tier floor to the next`() {
        assertEquals(0f, RescueTier.HERO.progress(30), 0.001f)
        assertEquals(0.5f, RescueTier.HERO.progress(45), 0.001f)
        assertEquals(1f, RescueTier.LEGEND.progress(500), 0.001f)
        assertNull(RescueTier.LEGEND.next)
    }
}
