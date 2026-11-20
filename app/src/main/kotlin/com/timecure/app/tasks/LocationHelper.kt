package com.timecure.app.tasks

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import com.timecure.app.util.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestampMillis: Long
)

data class GeocodedPlace(
    val label: String,
    val latitude: Double,
    val longitude: Double
)

object LocationHelper {

    fun bestLastKnownLocation(context: Context): LocationFix? {
        if (!Permissions.hasAnyLocation(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(LocationManager.FUSED_PROVIDER)
            }
            add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.PASSIVE_PROVIDER)
        }
        var best: Location? = null
        for (provider in providers) {
            val loc = getLastKnownSafe(lm, provider) ?: continue
            if (best == null || loc.time > best.time - 120_000L && loc.accuracy < best.accuracy) {
                best = loc
            } else if (loc.time > best.time + 120_000L) {
                best = loc
            }
        }
        return best?.let {
            LocationFix(it.latitude, it.longitude, it.accuracy, it.time)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownSafe(lm: LocationManager, provider: String): Location? =
        runCatching {
            if (lm.allProviders.contains(provider)) lm.getLastKnownLocation(provider) else null
        }.getOrNull()

    /**
     * Requests a fresh active GPS/fused fix on API 30+, falling back cleanly to the best
     * last-known location if an indoor lock times out after 8 seconds.
     */
    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(context: Context): LocationFix? {
        if (!Permissions.hasAnyLocation(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val provider = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    lm.allProviders.contains(LocationManager.FUSED_PROVIDER) -> LocationManager.FUSED_PROVIDER
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> lm.allProviders.firstOrNull { it != LocationManager.PASSIVE_PROVIDER }
            }
            if (provider != null) {
                val fresh = withTimeoutOrNull(8_000L) {
                    suspendCancellableCoroutine<Location?> { cont ->
                        val signal = CancellationSignal()
                        cont.invokeOnCancellation { signal.cancel() }
                        runCatching {
                            lm.getCurrentLocation(
                                provider,
                                signal,
                                ContextCompat.getMainExecutor(context)
                            ) { loc ->
                                if (cont.isActive) cont.resume(loc)
                            }
                        }.onFailure {
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                }
                if (fresh != null) {
                    return LocationFix(
                        latitude = fresh.latitude,
                        longitude = fresh.longitude,
                        accuracyMeters = fresh.accuracy,
                        timestampMillis = fresh.time
                    )
                }
            }
        }
        return bestLastKnownLocation(context)
    }

    suspend fun reverseGeocode(context: Context, lat: Double, lon: Double): String =
        withContext(Dispatchers.IO) {
            val coordsFallback = String.format(Locale.getDefault(), "%.5f, %.5f", lat, lon)
            if (!Geocoder.isPresent()) return@withContext coordsFallback
            val geocoder = Geocoder(context, Locale.getDefault())
            val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                withTimeoutOrNull(5_000L) {
                    suspendCancellableCoroutine<Address?> { cont ->
                        runCatching {
                            geocoder.getFromLocation(lat, lon, 1) { list ->
                                if (cont.isActive) cont.resume(list.firstOrNull())
                            }
                        }.onFailure {
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                }
            } else {
                legacyReverseGeocode(geocoder, lat, lon)
            }
            address?.let { formatAddress(it) }?.takeIf { it.isNotBlank() } ?: coordsFallback
        }

    suspend fun searchAddress(context: Context, query: String): List<GeocodedPlace> =
        withContext(Dispatchers.IO) {
            val trimmed = query.trim()
            if (trimmed.isBlank() || !Geocoder.isPresent()) return@withContext emptyList()
            val geocoder = Geocoder(context, Locale.getDefault())
            val results = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                withTimeoutOrNull(6_000L) {
                    suspendCancellableCoroutine<List<Address>> { cont ->
                        runCatching {
                            geocoder.getFromLocationName(trimmed, 5) { list ->
                                if (cont.isActive) cont.resume(list)
                            }
                        }.onFailure {
                            if (cont.isActive) cont.resume(emptyList())
                        }
                    }
                }.orEmpty()
            } else {
                legacySearchAddress(geocoder, trimmed)
            }
            results.mapNotNull { addr ->
                val label = formatAddress(addr).ifBlank { trimmed }
                GeocodedPlace(label = label, latitude = addr.latitude, longitude = addr.longitude)
            }
        }

    @Suppress("DEPRECATION")
    private fun legacyReverseGeocode(geocoder: Geocoder, lat: Double, lon: Double): Address? =
        runCatching { geocoder.getFromLocation(lat, lon, 1)?.firstOrNull() }.getOrNull()

    @Suppress("DEPRECATION")
    private fun legacySearchAddress(geocoder: Geocoder, query: String): List<Address> =
        runCatching { geocoder.getFromLocationName(query, 5).orEmpty() }.getOrDefault(emptyList())

    private fun formatAddress(addr: Address): String {
        val parts = listOfNotNull(
            addr.featureName?.takeIf { it.isNotBlank() && it != addr.subThoroughfare },
            addr.thoroughfare?.takeIf { it.isNotBlank() },
            addr.subLocality?.takeIf { it.isNotBlank() },
            addr.locality?.takeIf { it.isNotBlank() },
            addr.adminArea?.takeIf { it.isNotBlank() }
        ).distinct()
        return if (parts.isNotEmpty()) parts.take(3).joinToString(", ")
        else addr.getAddressLine(0).orEmpty()
    }
}
