package com.sisaguna.android.data.remote

import com.sisaguna.android.data.model.OrderStatus
import com.sisaguna.android.data.remote.dto.OrderDto
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorMessagesTest {

    private fun delivery(reason: String?) = ApiException(
        422, "delivery_unavailable", "pengantaran tidak tersedia",
        JsonObject(reason?.let { mapOf("reason" to JsonPrimitive(it)) } ?: emptyMap()),
    )

    @Test
    fun `each delivery refusal says what to do`() {
        assertTrue(delivery("too_far").userMessage.contains("15 km"))
        assertTrue(delivery("merchant_pickup_only").userMessage.contains("ambil sendiri"))
        assertTrue(delivery("address_required").userMessage.contains("alamat"))
        assertTrue(delivery(null).userMessage.contains("ambil sendiri"))
    }

    @Test
    fun `an unlinked wallet points to where to link it`() {
        val e = ApiException(422, "payment_method_not_linked", "e-wallet belum ditautkan")
        assertTrue(e.userMessage.contains("Metode Pembayaran"))
    }

    @Test
    fun `other server messages pass through capitalised`() {
        assertEquals("Stok tidak cukup", ApiException(409, "out_of_stock", "stok tidak cukup").userMessage)
        assertEquals("Server sedang bermasalah. Coba lagi beberapa saat.", ApiException(503, "x", "boom").userMessage)
    }

    @Test
    fun `a simulated payment is recognised from the order`() {
        val json = """{"id":"o1","status":"ready","payment_kind":"qris","merchant":{"id":"m","name":"M"},
            "pickup_end":"2026-10-18T10:00:00Z","created_at":"2026-10-18T09:00:00Z",
            "payment":{"kind":"qris","status":"paid","provider":"dummy","paid_at":"2026-10-18T09:00:00Z"}}"""
        val order = ApiJson.decodeFromString<OrderDto>(json).toOrder()
        assertEquals(OrderStatus.READY, order.status)
        assertTrue(order.paymentPaid)
        assertTrue(order.paymentSimulated)

        val cash = ApiJson.decodeFromString<OrderDto>(json.replace("\"qris\",\"status\":\"paid\",\"provider\":\"dummy\"", "\"cash\",\"status\":\"unpaid\",\"provider\":null")).toOrder()
        assertFalse(cash.paymentPaid)
        assertFalse(cash.paymentSimulated)
    }
}
