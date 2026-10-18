package com.sisaguna.android

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.sisaguna.android.data.push.NotificationChannels
import com.sisaguna.android.data.push.PushRegistrar
import com.sisaguna.android.data.remote.TokenStore
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import okhttp3.OkHttpClient

@HiltAndroidApp
class SisaGunaApp : Application(), ImageLoaderFactory {

    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var pushRegistrar: PushRegistrar

    /** The app's authenticated client: private files (complaint photos) need the user's token, and
     * the interceptor only attaches it to our own API host. */
    @Inject lateinit var okHttp: OkHttpClient

    override fun onCreate() {
        super.onCreate()
        // Opening the Keystore-backed store takes a while on a cold start; do it here instead of
        // on the main thread when the first screen asks for the session.
        Thread { tokenStore.get() }.start()
        NotificationChannels.ensure(this)
        pushRegistrar.start()
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this).okHttpClient(okHttp).build()
}
