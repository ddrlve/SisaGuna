package com.sisaguna.android.data.repository

import com.sisaguna.android.core.ui.AppMessenger
import com.sisaguna.android.data.remote.ApiJson
import com.sisaguna.android.data.remote.ApiService
import com.sisaguna.android.data.remote.RealtimeClient
import com.sisaguna.android.testutil.FakeAuthRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** "Load more" on the inbox: older pages append, and a refresh renews the newest page without losing them. */
class ApiNotificationPagingTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: ApiNotificationRepository

    private fun n(id: String, at: String) =
        """{"id":"$id","type":"order","title":"t$id","body":"","created_at":"$at"}"""

    @Before
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                return MockResponse().setBody(
                    when {
                        path.startsWith("/api/v1/me/notifications?cursor=c1") ->
                            """{"data":[${n("n3", "2026-10-16T05:00:00Z")}],"meta":{}}"""
                        path.startsWith("/api/v1/me/notifications") ->
                            """{"data":[${n("n1", "2026-10-18T05:00:00Z")},${n("n2", "2026-10-17T05:00:00Z")}],"meta":{"next_cursor":"c1"}}"""
                        path.startsWith("/api/v1/me/notification-prefs") ->
                            """{"data":{"enabled":true,"pickup":true,"order":true,"merchant":true,"promo":true,"impact":true}}"""
                        else -> """{"data":{}}"""
                    },
                )
            }
        }
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        val auth = FakeAuthRepository(signedIn = true)
        repo = ApiNotificationRepository(api, auth, AppMessenger(), RealtimeClient(api, OkHttpClient(), ApiJson, auth, scope), scope)
        waitFor { repo.notifications.value.isNotEmpty() } // the sign-in refresh runs on its own
    }

    @After
    fun tearDown() = server.shutdown()

    private fun waitFor(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 3_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(10)
        assertTrue("timed out", condition())
    }

    @Test
    fun `the first page offers more and loading it appends the older entries`() = runBlocking {
        assertTrue(repo.hasMore.value)
        assertEquals(listOf("n1", "n2"), repo.notifications.value.map { it.id })

        repo.loadMore()

        assertEquals(listOf("n1", "n2", "n3"), repo.notifications.value.map { it.id })
        assertFalse(repo.hasMore.value)
    }

    @Test
    fun `a refresh keeps the older pages already loaded`() = runBlocking {
        repo.loadMore()
        repo.refresh()
        assertEquals(listOf("n1", "n2", "n3"), repo.notifications.value.map { it.id })
        assertFalse(repo.hasMore.value) // still knows there is nothing older than n3
    }
}
