package com.sisaguna.android.data.remote

import com.sisaguna.android.testutil.FakeAuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class RealtimeParseTest {

    private val api = Retrofit.Builder()
        .baseUrl("http://localhost/api/v1/")
        .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ApiService::class.java)

    // Nothing is connected here: only the frame parser is exercised.
    private val client = RealtimeClient(api, OkHttpClient(), ApiJson, FakeAuthRepository(), CoroutineScope(Job() + Dispatchers.Unconfined))

    @Test
    fun `a chat message frame becomes MessageCreated`() {
        val event = client.parse(
            """{"type":"message.created","at":"2026-10-18T05:00:00Z","data":{"id":10,"conversation_id":"cv1",
            "sender_side":"merchant","body":"Masih ada","listing_id":null,"client_id":"c","created_at":"2026-10-18T05:00:00Z"}}""",
        ) as RealtimeEvent.MessageCreated
        assertEquals(10L, event.message.id)
        assertEquals("merchant", event.message.senderSide)
        assertEquals("cv1", event.message.conversationId)
    }

    @Test
    fun `order, stock, typing and read frames`() {
        assertEquals(RealtimeEvent.OrderChanged("o1"), client.parse("""{"type":"order.updated","data":{"order_id":"o1","status":"completed"}}"""))
        assertEquals(RealtimeEvent.OrderChanged("o2"), client.parse("""{"type":"order.created","data":{"order_id":"o2","item_count":1,"total":9000}}"""))
        assertEquals(RealtimeEvent.StockChanged("l1", 3), client.parse("""{"type":"listing.stock_changed","data":{"listing_id":"l1","stock":3}}"""))
        assertEquals(RealtimeEvent.Typing("cv1"), client.parse("""{"type":"typing","data":{"conversation_id":"cv1"}}"""))
        assertEquals(RealtimeEvent.ConversationRead("cv1", 7), client.parse("""{"type":"conversation.read","data":{"conversation_id":"cv1","last_read_id":7}}"""))
        assertEquals(RealtimeEvent.NotificationCreated, client.parse("""{"type":"notification.created","data":{"notification_id":"n","type":"order","title":"t"}}"""))
        assertEquals(RealtimeEvent.MerchantStatusChanged, client.parse("""{"type":"merchant.status_changed","data":{"merchant_id":"m","status":"approved"}}"""))
    }

    @Test
    fun `unknown and malformed frames are ignored, not fatal`() {
        assertNull(client.parse("""{"type":"error","data":{"code":"forbidden"}}"""))
        assertNull(client.parse("not json at all"))
        assertNull(client.parse("""{"type":"typing","data":{}}""")) // missing conversation id
        assertNull(client.parse("""{"type":"message.created","data":{"id":"nope"}}"""))
        assertTrue(true)
    }
}
