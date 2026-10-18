package com.sisaguna.android.core.ui

import com.sisaguna.android.data.remote.AppException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * One-shot, app-wide messages (shown as a snackbar by MainActivity). Repositories that apply a
 * change optimistically and then roll it back use this to say why, without every screen having
 * to wire an error channel.
 */
@Singleton
class AppMessenger @Inject constructor() {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var last: Pair<String, Long>? = null

    /** The same text twice within a few seconds is shown once (several repositories fail together when offline). */
    @Synchronized
    fun show(message: String) {
        val now = System.currentTimeMillis()
        if (last?.let { it.first == message && now - it.second < DEDUPE_MS } == true) return
        last = message to now
        _messages.tryEmit(message)
    }

    fun show(error: Throwable) = show((error as? AppException)?.userMessage ?: "Terjadi kesalahan. Coba lagi.")

    private companion object {
        const val DEDUPE_MS = 4_000L
    }
}
