package com.sisaguna.android.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/** Upload clips are capped here: long enough to show the food from every side, short enough
 * to stay small on a prepaid data plan. */
const val MAX_VIDEO_SECONDS = 30

/** System camera in video mode with a hard [MAX_VIDEO_SECONDS] limit (the stock CaptureVideo
 * contract can't pass the duration extra). */
private class CaptureShortVideo : ActivityResultContract<Uri, Boolean>() {
    override fun createIntent(context: Context, input: Uri): Intent =
        Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            .putExtra(MediaStore.EXTRA_OUTPUT, input)
            .putExtra(MediaStore.EXTRA_DURATION_LIMIT, MAX_VIDEO_SECONDS)
            .putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean = resultCode == android.app.Activity.RESULT_OK
}

class MediaCapture internal constructor(
    val takePhoto: () -> Unit,
    val recordVideo: () -> Unit,
    val pickPhotos: () -> Unit,
)

private fun newCaptureUri(context: Context, ext: String): Uri {
    val dir = File(context.filesDir, "captures").apply { mkdirs() }
    val file = File(dir, "cap_${System.currentTimeMillis()}.$ext")
    return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
}

/**
 * Camera + gallery in one place for upload, reviews, complaints and the profile photo.
 * Uses the phone's own camera app, so there's no permission prompt and no CameraX dependency.
 */
@Composable
fun rememberMediaCapture(
    onPhotos: (List<Uri>) -> Unit,
    onVideo: (Uri) -> Unit = {},
    maxPick: Int = 5,
): MediaCapture {
    val context = LocalContext.current
    var pending by rememberSaveable { mutableStateOf<String?>(null) }

    val photo = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pending?.let(Uri::parse)
        if (ok && uri != null) onPhotos(listOf(uri))
        pending = null
    }
    val video = rememberLauncherForActivityResult(CaptureShortVideo()) { ok ->
        val uri = pending?.let(Uri::parse)
        if (ok && uri != null) onVideo(uri)
        pending = null
    }
    val pickMany = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxPick.coerceAtLeast(2))) { uris ->
        if (uris.isNotEmpty()) onPhotos(uris)
    }

    fun launchSafely(block: () -> Unit) = try {
        block()
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Aplikasi kamera tidak ditemukan", Toast.LENGTH_SHORT).show()
    }

    return remember(photo, video, pickMany) {
        MediaCapture(
            takePhoto = {
                val uri = newCaptureUri(context, "jpg")
                pending = uri.toString()
                launchSafely { photo.launch(uri) }
            },
            recordVideo = {
                val uri = newCaptureUri(context, "mp4")
                pending = uri.toString()
                launchSafely { video.launch(uri) }
            },
            pickPhotos = { launchSafely { pickMany.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } },
        )
    }
}
