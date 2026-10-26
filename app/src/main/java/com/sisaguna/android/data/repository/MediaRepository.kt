package com.sisaguna.android.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.sisaguna.android.data.remote.ApiService
import com.sisaguna.android.data.remote.MediaReadException
import com.sisaguna.android.data.remote.apiData
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.source

/** What an upload is for; the backend validates and stores each purpose differently. */
enum class MediaPurpose(val api: String) { LISTING("listing"), AVATAR("avatar"), MERCHANT("merchant"), REVIEW("review"), COMPLAINT("complaint") }

data class UploadedMedia(val id: String, val url: String)

interface MediaRepository {
    /** Uploads a local photo or video ([uri] is `content://` or `file://`) and returns the stored media. */
    suspend fun upload(uri: String, purpose: MediaPurpose): UploadedMedia
}

@Singleton
class ApiMediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ApiService,
) : MediaRepository {

    override suspend fun upload(uri: String, purpose: MediaPurpose): UploadedMedia = withContext(Dispatchers.IO) {
        val parsed = Uri.parse(uri)
        val mime = context.contentResolver.getType(parsed) ?: if (uri.endsWith(".mp4", true)) "video/mp4" else "image/jpeg"
        val isVideo = mime.startsWith("video/")
        val body = try {
            if (isVideo) streamBody(parsed, mime.toMediaType()) else jpegBody(parsed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw MediaReadException(e)
        } catch (e: OutOfMemoryError) {
            throw MediaReadException(e)
        }
        val part = MultipartBody.Part.createFormData("file", if (isVideo) "upload.mp4" else "upload.jpg", body)
        val media = apiData { api.upload(purpose.api.toRequestBody("text/plain".toMediaType()), part) }
        UploadedMedia(media.id, media.url)
    }

    /** Videos are streamed from disk, never held in memory (up to 30 MB). */
    private fun streamBody(uri: Uri, type: MediaType): RequestBody = object : RequestBody() {
        override fun contentType() = type
        override fun contentLength(): Long =
            runCatching { context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } }.getOrNull() ?: -1L
        override fun writeTo(sink: BufferedSink) {
            val input = context.contentResolver.openInputStream(uri) ?: throw FileNotFoundException(uri.toString())
            input.use { sink.writeAll(it.source()) }
        }
    }

    /**
     * Phone cameras can exceed the 8 MB photo limit. Re-encode to a JPEG of at most [MAX_SIDE] px,
     * upright: the backend strips EXIF, so the orientation has to be baked into the pixels first.
     */
    private fun jpegBody(uri: Uri): RequestBody {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: throw FileNotFoundException(uri.toString())
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw FileNotFoundException(uri.toString())

        val degrees = resolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        val upright = if (degrees == 0f) decoded else Bitmap.createBitmap(
            decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(degrees) }, true,
        )
        val out = ByteArrayOutputStream()
        upright.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        if (upright !== decoded) decoded.recycle()
        upright.recycle()
        return out.toByteArray().toRequestBody("image/jpeg".toMediaType())
    }

    private companion object {
        const val MAX_SIDE = 2048
        const val JPEG_QUALITY = 85
    }
}
