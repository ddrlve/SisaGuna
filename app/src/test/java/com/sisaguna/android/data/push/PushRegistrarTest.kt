package com.sisaguna.android.data.push

import com.sisaguna.android.data.remote.ApiJson
import com.sisaguna.android.data.remote.ApiService
import com.sisaguna.android.testutil.FakeAuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class PushRegistrarTest {

    private lateinit var server: MockWebServer
    private lateinit var api: ApiService
    private val store = PushTokenStore(null)
    private var deviceToken: String? = "tok-1"
    private val source = object : PushTokenSource {
        override suspend fun current() = deviceToken
    }

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun registrar(signedIn: Boolean) =
        PushRegistrar(api, FakeAuthRepository(signedIn), source, store, CoroutineScope(Job() + Dispatchers.Unconfined))

    @Test
    fun `signed in, the device token is sent and remembered`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))
        registrar(signedIn = true).registerCurrent()

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/me/devices", request.path)
        assertEquals("""{"token":"tok-1","platform":"android"}""", request.body.readUtf8())
        assertEquals("tok-1", store.token)
    }

    @Test
    fun `a build without Firebase sends nothing`() = runBlocking {
        deviceToken = null
        registrar(signedIn = true).registerCurrent()
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `a refused registration is not remembered so it is retried later`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))
        registrar(signedIn = true).registerCurrent()
        assertNull(store.token)
    }

    @Test
    fun `a rotated token before sign-in waits, after sign-in it is sent`() {
        server.enqueue(MockResponse().setResponseCode(204))
        registrar(signedIn = false).onNewToken("tok-2")
        assertEquals(0, server.requestCount)
        assertEquals("tok-2", store.token)

        registrar(signedIn = true).onNewToken("tok-3")
        assertEquals("/api/v1/me/devices", server.takeRequest().path)
        // The token is saved once the response comes back, on another thread.
        val deadline = System.currentTimeMillis() + 2_000
        while (store.token != "tok-3" && System.currentTimeMillis() < deadline) Thread.sleep(10)
        assertEquals("tok-3", store.token)
    }
}
