package com.sisaguna.android.data.remote

import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.QuantityUnit
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.sisaguna.android.data.remote.dto.LoginRequest

private class MemoryTokenStore(var tokens: Tokens? = null) : TokenStore {
    override fun get() = tokens
    override fun save(tokens: Tokens) { this.tokens = tokens }
    override fun clear() { tokens = null }
}

class RemoteLayerTest {

    private lateinit var server: MockWebServer
    private lateinit var store: MemoryTokenStore
    private var expired = 0
    private var refreshCalls = 0
    private var refreshResult: () -> Tokens? = { Tokens("new-access", "new-refresh") }

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        store = MemoryTokenStore(Tokens("old-access", "old-refresh"))
        expired = 0
        refreshCalls = 0
    }

    @After
    fun tearDown() = server.shutdown()

    private fun client() = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(store, server.hostName))
        .authenticator(TokenAuthenticator(store, { refreshCalls++; refreshResult() }, { expired++ }))
        .build()

    private fun api(): ApiService = Retrofit.Builder()
        .baseUrl(server.url("/api/v1/"))
        .client(client())
        .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ApiService::class.java)

    @Test
    fun `401 refreshes once and replays the request with the new token`() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"code":"unauthorized","message":"x"}}"""))
        server.enqueue(MockResponse().setBody("""{"data":{"id":"u1","name":"Budi","email":"b@x.id"}}"""))

        val me = runBlocking { apiData { api().me() } }

        assertEquals("Budi", me.name)
        assertEquals(1, refreshCalls)
        assertEquals("Bearer old-access", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer new-access", server.takeRequest().getHeader("Authorization"))
        assertEquals(Tokens("new-access", "new-refresh"), store.tokens)
    }

    @Test
    fun `rejected refresh token clears the session and reports expiry`() {
        refreshResult = { null }
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"code":"unauthorized","message":"x"}}"""))

        try {
            runBlocking { apiData { api().me() } }
            fail("expected ApiException")
        } catch (e: ApiException) {
            assertEquals(401, e.status)
        }
        assertNull(store.tokens)
        assertEquals(1, expired)
    }

    @Test
    fun `offline refresh keeps the session`() {
        refreshResult = { throw IOException("offline") }
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"code":"unauthorized","message":"x"}}"""))

        try {
            runBlocking { apiData { api().me() } }
            fail("expected ApiException")
        } catch (e: ApiException) {
            assertEquals(401, e.status)
        }
        assertEquals(Tokens("old-access", "old-refresh"), store.tokens)
        assertEquals(0, expired)
    }

    @Test
    fun `a stale 401 retries with the token another request already rotated`() {
        // Request was sent with old-access; before its 401 is handled, someone else refreshed.
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"code":"unauthorized","message":"x"}}"""))
        server.enqueue(MockResponse().setBody("""{"data":{"id":"u1","name":"Budi","email":"b@x.id"}}"""))
        val rotating = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(store, server.hostName))
            // Network interceptor: runs before the authenticator sees the 401, like a parallel refresh would.
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                if (response.code == 401) store.save(Tokens("rotated", "rotated-refresh"))
                response
            }
            .authenticator(TokenAuthenticator(store, { refreshCalls++; refreshResult() }, { expired++ }))
            .build()

        val response = rotating.newCall(Request.Builder().url(server.url("/api/v1/me")).build()).execute()

        assertEquals(200, response.code)
        assertEquals(0, refreshCalls)
        server.takeRequest()
        assertEquals("Bearer rotated", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `login 401 is not treated as an expired session`() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"code":"invalid_credentials","message":"email atau password salah"}}"""))

        try {
            runBlocking { apiData { api().login(LoginRequest("a@b.id", "x")) } }
            fail("expected ApiException")
        } catch (e: ApiException) {
            assertEquals("invalid_credentials", e.code)
            assertEquals("Email atau password salah", e.userMessage)
        }
        assertEquals(0, refreshCalls)
        assertEquals(null, server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `token is not sent to another host`() {
        val other = MockWebServer().apply { start() }
        other.enqueue(MockResponse().setBody("ok"))
        val interceptor = AuthInterceptor(store, apiHost = "api.sisaguna.test")

        OkHttpClient.Builder().addInterceptor(interceptor).build()
            .newCall(Request.Builder().url(other.url("/img.jpg")).build()).execute().close()

        assertEquals(null, other.takeRequest().getHeader("Authorization"))
        other.shutdown()
    }

    @Test
    fun `error body extras are exposed`() {
        server.enqueue(
            MockResponse().setResponseCode(409).setBody(
                """{"error":{"code":"price_changed","message":"harga berubah","total":17000,"service_fee":1000}}""",
            ),
        )
        try {
            runBlocking { apiData { api().me() } }
            fail("expected ApiException")
        } catch (e: ApiException) {
            assertEquals("price_changed", e.code)
            assertEquals(17000, e.int("total"))
            assertEquals(1000, e.int("service_fee"))
        }
    }

    @Test
    fun `network failure becomes NetworkException`() {
        server.shutdown()
        try {
            runBlocking { apiData { api().me() } }
            fail("expected NetworkException")
        } catch (e: NetworkException) {
            assertTrue(e.userMessage.contains("koneksi"))
        }
    }

    @Test
    fun `listing card from the real contract maps to the domain model`() {
        server.enqueue(
            MockResponse().setBody(
                """{"data":[{"id":"5f8642b5","title":"Nasi Kuning","tier":"animal_feed","unit":"kg","is_free":false,
                "price":8000,"price_original":25000,"discount_pct":68,"stock":6,"halal":"halal_certified",
                "allergens":["egg","peanut","unknown_thing"],"pickup_start":"2026-10-18T07:20:00.757977Z",
                "pickup_end":"2026-10-18T09:50:00.757977Z","distance_km":null,"cover_url":"https://x/a.jpg",
                "merchant":{"id":"m-1","name":"Warung Bu Sari","location_label":"Kemang","rating_avg":4.9,
                "rating_count":212,"is_verified":true},"sold_count":48,"delivery":{"fee":29500,"eta_min":66,"eta_max":76}}],
                "meta":{"total":1,"total_pages":1}}""",
            ),
        )

        val cards = runBlocking { api().listings(tier = null).data }
        val listing = cards.single().toListing()

        assertEquals(ListingTier.ANIMAL_FEED, listing.tier)
        assertEquals(QuantityUnit.KILOGRAM, listing.unit)
        assertEquals("m-1", listing.merchantId)
        assertEquals(8000, listing.unitPrice)
        assertEquals(2, listing.allergens.size) // the unknown allergen is dropped
        assertNull(listing.distanceKm)
        assertEquals("Warung Bu Sari", cards.single().merchant.toMerchant().name)
    }
}
