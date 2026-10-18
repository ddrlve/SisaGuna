package com.sisaguna.android.data.repository

import com.sisaguna.android.data.model.ChatSide
import com.sisaguna.android.data.remote.ApiJson
import com.sisaguna.android.data.remote.ApiService
import com.sisaguna.android.data.remote.RealtimeClient
import com.sisaguna.android.data.remote.RealtimeEvent
import com.sisaguna.android.data.remote.dto.MessageDto
import com.sisaguna.android.testutil.FakeAuthRepository
import com.sisaguna.android.testutil.TestListingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** The chat repository against a scripted server: REST calls, de-duplication, unread counts, ordering. */
class ApiChatRepositoryTest {

    private lateinit var server: MockWebServer
    private val seen = java.util.concurrent.CopyOnWriteArrayList<String>() // written by the server thread
    private lateinit var repo: ApiChatRepository
    private lateinit var api: ApiService
    private lateinit var scope: CoroutineScope
    private lateinit var realtime: RealtimeClient

    private val msg = { id: Int, side: String, body: String, at: String ->
        """{"id":$id,"conversation_id":"cv1","sender_side":"$side","body":"$body","listing_id":null,"client_id":"c$id","created_at":"$at"}"""
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                seen += "${request.method} ${path.substringBefore('?')}"
                return MockResponse().setBody(
                    when {
                        path.startsWith("/api/v1/conversations?as=buyer") -> """{"data":[]}"""
                        path.startsWith("/api/v1/conversations?as=merchant") ->
                            """{"data":[{"id":"cv2","counterpart":{"id":"u9","name":"Clara T."},
                            "last_message":{"body":"Halo","side":"buyer","at":"2026-10-18T05:00:00Z"},"unread":1}]}"""
                        request.method == "POST" && path == "/api/v1/conversations" ->
                            """{"data":{"id":"cv1","counterpart":{"id":"m1","name":"Warung Bu Sari"},"last_message":null,"unread":0}}"""
                        path.startsWith("/api/v1/conversations/cv1/messages") && request.method == "GET" ->
                            """{"data":[${msg(2, "buyer", "Masih ada?", "2026-10-18T05:01:00Z")},${msg(1, "merchant", "Halo kak", "2026-10-18T05:00:00Z")}],"meta":{"peer_last_read_id":2}}"""
                        path.startsWith("/api/v1/conversations/cv1/messages") ->
                            """{"data":${msg(3, "buyer", "Dimasak jam berapa?", "2026-10-18T05:02:00Z")}}"""
                        else -> """{"data":{}}"""
                    },
                )
            }
        }
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
        scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        realtime = RealtimeClient(api, OkHttpClient(), ApiJson, FakeAuthRepository(signedIn = true), scope)
        repo = ApiChatRepository(api, FakeAuthRepository(signedIn = true), TestListingRepository(), realtime, scope)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `inbox loads both ends and the store owner sees the buyer thread`() = runBlocking {
        repo.refresh()
        val asStore = repo.conversations.value.single { it.side == ChatSide.MERCHANT }
        assertEquals("Clara T.", asStore.counterpartName)
        assertEquals(1, asStore.unread)
        assertEquals(ChatSide.BUYER, asStore.lastSender)
    }

    @Test
    fun `a guest opening the inbox makes no request`() = runBlocking {
        // Its own server, so the signed-in repository's background refreshes can't be counted by mistake.
        val quiet = MockWebServer().apply { start() }
        val quietApi = Retrofit.Builder()
            .baseUrl(quiet.url("/api/v1/"))
            .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
        val guestRepo = ApiChatRepository(quietApi, FakeAuthRepository(signedIn = false), TestListingRepository(), realtime, scope)
        guestRepo.refresh()
        assertEquals(0, quiet.requestCount)
        quiet.shutdown()
    }

    @Test
    fun `starting a chat is reused instead of created twice`() = runBlocking {
        val first = repo.conversationWith("m1")
        val second = repo.conversationWith("m1")
        assertEquals(first, second)
        assertEquals(1, seen.count { it == "POST /api/v1/conversations" })
    }

    @Test
    fun `messages load oldest first and my read receipts follow the peer pointer`() = runBlocking {
        repo.conversationWith("m1")
        repo.loadMessages("cv1")
        val thread = repo.messages.value.filter { it.conversationId == "cv1" }
        assertEquals(listOf("Halo kak", "Masih ada?"), thread.map { it.text })
        assertTrue(thread.last().isRead) // my message 2 is covered by peer_last_read_id = 2
    }

    @Test
    fun `sending replaces the placeholder with the server message once`() = runBlocking {
        repo.conversationWith("m1")
        repo.send("cv1", "  Dimasak jam berapa?  ", null)
        val texts = repo.messages.value.map { it.text }
        assertEquals(listOf("Dimasak jam berapa?"), texts)
        assertEquals("3", repo.messages.value.single().id)

        // The same message echoed back over the socket must not duplicate it.
        repo.onEvent(RealtimeEvent.MessageCreated(echo(3, "buyer", "Dimasak jam berapa?")))
        assertEquals(1, repo.messages.value.size)
    }

    @Test
    fun `an incoming message bumps unread and the preview, and reading clears it`() = runBlocking {
        repo.conversationWith("m1")
        repo.onEvent(RealtimeEvent.MessageCreated(echo(9, "merchant", "Masih ada kak")))
        val conversation = repo.conversations.value.single { it.id == "cv1" }
        assertEquals(1, conversation.unread)
        assertEquals("Masih ada kak", conversation.lastText)

        repo.markRead("cv1")
        assertEquals(0, repo.conversations.value.single { it.id == "cv1" }.unread)
    }

    @Test
    fun `typing shows for a moment and a new message ends it`() = runBlocking {
        repo.conversationWith("m1")
        repo.onEvent(RealtimeEvent.Typing("cv1"))
        assertTrue("cv1" in repo.typing.value)
        repo.onEvent(RealtimeEvent.MessageCreated(echo(10, "merchant", "Ya")))
        assertTrue("cv1" !in repo.typing.value)
    }

    private fun echo(id: Long, side: String, body: String) =
        MessageDto(id, "cv1", side, body, null, "c$id", "2026-10-18T05:03:00Z")
}
