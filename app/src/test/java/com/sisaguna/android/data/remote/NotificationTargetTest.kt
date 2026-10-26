package com.sisaguna.android.data.remote

import com.sisaguna.android.data.remote.dto.NotificationDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationTargetTest {

    private fun parse(json: String) = ApiJson.decodeFromString<NotificationDto>(json).toNotification()

    @Test
    fun `an order notification points at its order`() {
        val n = parse("""{"id":"n1","type":"order","title":"t","body":"b","data":{"order_id":"o9"},"created_at":"2026-10-18T05:00:00Z"}""")
        assertEquals("o9", n.orderId)
        assertNull(n.merchantId)
    }

    @Test
    fun `a store notification points at its store and one without data points nowhere`() {
        val store = parse("""{"id":"n2","type":"merchant","title":"t","data":{"merchant_id":"m3"},"created_at":"2026-10-18T05:00:00Z"}""")
        assertEquals("m3", store.merchantId)
        val bare = parse("""{"id":"n3","type":"promo","title":"t","created_at":"2026-10-18T05:00:00Z"}""")
        assertNull(bare.orderId)
        assertNull(bare.merchantId)
    }
}
