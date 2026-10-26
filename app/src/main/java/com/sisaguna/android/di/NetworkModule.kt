package com.sisaguna.android.di

import com.sisaguna.android.BuildConfig
import com.sisaguna.android.data.remote.ApiJson
import com.sisaguna.android.data.remote.ApiService
import com.sisaguna.android.data.remote.AuthInterceptor
import com.sisaguna.android.data.remote.EncryptedTokenStore
import com.sisaguna.android.data.remote.RefreshApi
import com.sisaguna.android.data.remote.SessionEvents
import com.sisaguna.android.data.remote.TokenAuthenticator
import com.sisaguna.android.data.remote.TokenStore
import com.sisaguna.android.data.remote.Tokens
import com.sisaguna.android.data.remote.dto.RefreshRequest
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindings {
    @Binds abstract fun bindTokenStore(impl: EncryptedTokenStore): TokenStore
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun json(): Json = ApiJson

    @Provides @Singleton @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides @Singleton @BareClient
    fun bareClient(): OkHttpClient = baseClient()

    @Provides @Singleton
    fun refreshApi(@BareClient client: OkHttpClient, json: Json): RefreshApi =
        retrofit(client, json).create(RefreshApi::class.java)

    @Provides @Singleton
    fun okHttpClient(
        tokens: TokenStore,
        events: SessionEvents,
        refreshApi: RefreshApi,
        @BareClient bare: OkHttpClient,
    ): OkHttpClient {
        val host = BuildConfig.API_BASE_URL.toHttpUrl().host
        val refresh: (String) -> Tokens? = { refreshToken ->
            val response = refreshApi.refresh(RefreshRequest(refreshToken)).execute()
            when {
                response.isSuccessful -> response.body()?.data?.let { Tokens(it.accessToken, it.refreshToken) }
                    ?: throw IOException("Empty refresh response")
                // The server rejected the refresh token: the session is over.
                response.code() == 400 || response.code() == 401 -> null
                // Rate limit / 5xx: unknown outcome, keep the session and fail this request only.
                else -> throw IOException("Refresh failed: ${response.code()}")
            }
        }
        return bare.newBuilder()
            .addInterceptor(AuthInterceptor(tokens, host))
            .authenticator(TokenAuthenticator(tokens, refresh, events::notifyExpired))
            .build()
    }

    @Provides @Singleton
    fun apiService(client: OkHttpClient, json: Json): ApiService =
        retrofit(client, json).create(ApiService::class.java)

    private fun baseClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS) // photo/video uploads
        .apply {
            if (BuildConfig.DEBUG) {
                // HEADERS only: bodies carry passwords and tokens.
                addInterceptor(
                    HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS)
                        .apply { redactHeader("Authorization") },
                )
            }
        }
        .build()

    private fun retrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
