package com.sisaguna.android.feature.address

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

private val IndonesianLocale = Locale("in", "ID")

/** A geocoded place: what the sheet shows and what an [com.sisaguna.android.data.model.Address] stores. */
data class Place(val shortLabel: String, val fullAddress: String, val latitude: Double, val longitude: Double)

/**
 * Current position via the platform LocationManager (no Play Services — this project has no
 * Maps/Places key). Indoors a GPS-only request can wait forever, which is why "Lokasi saat ini"
 * looked broken on device. Order now:
 * 1. a last-known fix younger than 2 minutes from any provider → instant;
 * 2. otherwise GPS and network race, first answer wins, 12s timeout;
 * 3. on timeout, the freshest last-known fix of any age;
 * 4. else a clear error.
 * Caller must already hold a location permission.
 */
@SuppressLint("MissingPermission")
suspend fun fetchCurrentLocation(context: Context): Location {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
    if (providers.none { it != LocationManager.PASSIVE_PROVIDER }) {
        throw IllegalStateException("Lokasi HP sedang mati. Nyalakan GPS/lokasi di pengaturan, lalu coba lagi.")
    }

    val lastKnown = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }
    if (lastKnown != null && System.currentTimeMillis() - lastKnown.time < 2 * 60_000) return lastKnown

    val fresh = withTimeoutOrNull(12_000) {
        suspendCancellableCoroutine { cont ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    manager.removeUpdates(this)
                    if (cont.isActive) cont.resume(location)
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit
            }
            cont.invokeOnCancellation { manager.removeUpdates(listener) }
            providers.filter { it != LocationManager.PASSIVE_PROVIDER }.forEach { provider ->
                runCatching { manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper()) }
            }
        }
    }
    return fresh ?: lastKnown
        ?: throw IllegalStateException("Sinyal lokasi belum didapat. Coba di dekat jendela atau pilih lewat peta.")
}

private fun Address.toPlace(): Place {
    val short = listOfNotNull(subLocality ?: thoroughfare, locality ?: subAdminArea).distinct().joinToString(", ")
    val full = (0..maxAddressLineIndex).mapNotNull { getAddressLine(it) }.joinToString(", ")
    return Place(
        shortLabel = short.ifBlank { "%.4f, %.4f".format(latitude, longitude) },
        fullAddress = full.ifBlank { short },
        latitude = latitude,
        longitude = longitude,
    )
}

/** Reverse geocode on IO. Falls back to coordinates when the device geocoder has no data. */
suspend fun reverseGeocode(context: Context, latitude: Double, longitude: Double): Place = withContext(Dispatchers.IO) {
    val fallback = "%.5f, %.5f".format(latitude, longitude)
    runCatching {
        @Suppress("DEPRECATION")
        Geocoder(context, IndonesianLocale).getFromLocation(latitude, longitude, 1)?.firstOrNull()?.toPlace()
    }.getOrNull() ?: Place(fallback, "Titik di peta ($fallback)", latitude, longitude)
}

/** Forward geocode for the "Cari alamat" field. Empty when nothing matches or no geocoder. */
suspend fun searchPlaces(context: Context, query: String): List<Place> = withContext(Dispatchers.IO) {
    if (query.isBlank() || !Geocoder.isPresent()) return@withContext emptyList()
    runCatching {
        @Suppress("DEPRECATION")
        Geocoder(context, IndonesianLocale).getFromLocationName("$query, Indonesia", 5)?.map { it.toPlace() }
    }.getOrNull().orEmpty()
}
