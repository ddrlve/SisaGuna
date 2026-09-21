package com.sisaguna.android.feature.address

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Real GPS fix via the platform LocationManager (no Play Services dependency — this project
 * has neither Maps nor a Places API key, so a plain provider request is the honest option).
 * Caller must already hold ACCESS_FINE_LOCATION/ACCESS_COARSE_LOCATION before calling this.
 */
@SuppressLint("MissingPermission")
suspend fun fetchCurrentLocation(context: Context): Location {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val provider = when {
        manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> throw IllegalStateException("Tidak ada provider lokasi yang aktif. Nyalakan GPS/lokasi di HP kamu.")
    }
    return suspendCancellableCoroutine { cont ->
        val listener = object : android.location.LocationListener {
            override fun onLocationChanged(location: Location) {
                manager.removeUpdates(this)
                if (cont.isActive) cont.resume(location)
            }
        }
        cont.invokeOnCancellation { manager.removeUpdates(listener) }
        try {
            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        } catch (e: Exception) {
            if (cont.isActive) cont.resumeWithException(e)
        }
    }
}

/** Reverse-geocodes to a short label ("Kecamatan, Kota") — falls back to raw coordinates if
 * the system geocoder has no data (common on emulators / devices without Play services). */
fun reverseGeocodeLabel(context: Context, location: Location): String {
    return try {
        @Suppress("DEPRECATION")
        val results = Geocoder(context, Locale("in", "ID")).getFromLocation(location.latitude, location.longitude, 1)
        val address = results?.firstOrNull()
        val parts = listOfNotNull(address?.subLocality, address?.locality ?: address?.subAdminArea)
        if (parts.isNotEmpty()) parts.joinToString(", ") else rawCoordinateLabel(location)
    } catch (e: Exception) {
        rawCoordinateLabel(location)
    }
}

private fun rawCoordinateLabel(location: Location): String =
    "%.5f, %.5f".format(location.latitude, location.longitude)
