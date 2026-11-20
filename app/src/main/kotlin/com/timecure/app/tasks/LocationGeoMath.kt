package com.timecure.app.tasks

import com.timecure.app.model.GeofenceTriggerMode
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class GeofenceEvalResult(
    val shouldTrigger: Boolean,
    val nextInsideState: Boolean,
    val distanceMeters: Double
)

object LocationGeoMath {
    const val EARTH_RADIUS_METERS = 6_371_000.0
    const val EXIT_HYSTERESIS_METERS = 30.0
    const val DEFAULT_COOLDOWN_MILLIS = 15 * 60_000L

    /** Great-circle Haversine distance in meters between two WGS-84 points. */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2.0).pow(2) +
            cos(rLat1) * cos(rLat2) * sin(dLon / 2.0).pow(2)
        val c = 2.0 * atan2(sqrt(a), sqrt((1.0 - a).coerceAtLeast(0.0)))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Evaluates geofence entry/exit with boundary hysteresis (+30m) and anti-spam cooldown
     * so GPS jitter near the perimeter does not cause repeated false triggers.
     */
    fun evaluateGeofence(
        userLat: Double,
        userLon: Double,
        placeLat: Double,
        placeLon: Double,
        radiusMeters: Int,
        mode: GeofenceTriggerMode,
        wasInside: Boolean,
        lastTriggeredMillis: Long,
        nowMillis: Long,
        cooldownMillis: Long = DEFAULT_COOLDOWN_MILLIS
    ): GeofenceEvalResult {
        val dist = distanceMeters(userLat, userLon, placeLat, placeLon)
        val nextInside = when {
            !wasInside && dist <= radiusMeters -> true
            wasInside && dist > radiusMeters + EXIT_HYSTERESIS_METERS -> false
            else -> wasInside
        }
        val cooldownOk = lastTriggeredMillis == 0L || (nowMillis - lastTriggeredMillis) >= cooldownMillis
        val shouldTrigger = when (mode) {
            GeofenceTriggerMode.ARRIVE -> !wasInside && nextInside && cooldownOk
            GeofenceTriggerMode.LEAVE -> wasInside && !nextInside && cooldownOk
        }
        return GeofenceEvalResult(
            shouldTrigger = shouldTrigger,
            nextInsideState = nextInside,
            distanceMeters = dist
        )
    }

    fun formatDistance(meters: Double): String = when {
        meters < 10.0 -> "< 10 m"
        meters < 1000.0 -> "${meters.toInt()} m"
        else -> String.format(Locale.getDefault(), "%.1f km", meters / 1000.0)
    }
}
