package com.sisaguna.android.feature.address

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** What the UI needs to run the "use my location" flow: ask once, show progress, show why it failed. */
class LocationRequester internal constructor(
    val busy: Boolean,
    val error: String?,
    val request: () -> Unit,
    val clearError: () -> Unit,
)

fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

/**
 * Asks for the location permission if needed, then reads the current position and names it.
 * [onPlace] gets the result; [onDenied] runs when the user refuses the permission (so the caller
 * can stop asking). Shared by the Home prompt and the location sheet.
 */
@Composable
fun rememberLocationRequester(onDenied: () -> Unit = {}, onPlace: (Place) -> Unit): LocationRequester {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestOnPlace by rememberUpdatedState(onPlace)
    val latestOnDenied by rememberUpdatedState(onDenied)
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun fetch() {
        busy = true
        error = null
        scope.launch {
            try {
                val location = fetchCurrentLocation(context)
                latestOnPlace(reverseGeocode(context, location.latitude, location.longitude))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "Gagal mengambil lokasi."
            } finally {
                busy = false
            }
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) {
            fetch()
        } else {
            error = "Izin lokasi ditolak. Kamu tetap bisa pilih lewat peta."
            latestOnDenied()
        }
    }

    return LocationRequester(
        busy = busy,
        error = error,
        request = {
            if (!busy) {
                if (hasLocationPermission(context)) {
                    fetch()
                } else {
                    permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }
            }
        },
        clearError = { error = null },
    )
}
