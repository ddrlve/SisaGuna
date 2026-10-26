package com.sisaguna.android.di

import javax.inject.Qualifier

/** App-lifetime scope for work that must outlive any one screen (optimistic writes, sockets). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/** OkHttp client without auth interceptors, used only to refresh tokens. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BareClient
