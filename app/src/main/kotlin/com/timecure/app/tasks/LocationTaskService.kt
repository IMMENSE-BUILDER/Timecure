package com.timecure.app.tasks

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.timecure.app.MainActivity
import com.timecure.app.TimecureApp
import com.timecure.app.model.TaskTriggerType
import com.timecure.app.util.Permissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

object LocationRadarState {
    val isRunning = MutableStateFlow(false)
    val currentLocation = MutableStateFlow<LocationFix?>(null)
}

/**
 * Foreground location geofence radar monitoring marked places (College, Restaurant, etc.)
 * and firing Timecure notifications or full alarms whenever a user enters/leaves a zone.
 */
class LocationTaskService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationManager: LocationManager
    private var registered = false

    private val locationListener = LocationListener { location ->
        handleLocationUpdate(location)
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        LocationRadarState.isRunning.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Permissions.hasAnyLocation(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Start foreground immediately before I/O
        try {
            startForeground(NOTIFICATION_ID, buildRadarNotification(watchedPlacesCount = 1))
        } catch (se: SecurityException) {
            Log.e(TAG, "Cannot start location foreground service: ${se.message}")
            stopSelf()
            return START_NOT_STICKY
        }

        registerLocationUpdatesIfNeeded()

        scope.launch {
            val container = (application as TimecureApp).container
            val tasks = container.smartTaskRepository.getAllTasks()
            val activeLocationTasks = tasks.filter {
                it.triggerType == TaskTriggerType.LOCATION && it.isEnabled && !it.isCompleted && it.placeId != null
            }
            if (activeLocationTasks.isEmpty()) {
                stopSelf()
                return@launch
            }
            val uniquePlaceCount = activeLocationTasks.mapNotNull { it.placeId }.distinct().size
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, buildRadarNotification(uniquePlaceCount))

            // Populate initial fix right away so distances render instantaneously in UI
            LocationHelper.bestLastKnownLocation(this@LocationTaskService)?.let { fix ->
                LocationRadarState.currentLocation.value = fix
                evaluateTasksAgainstCoords(fix.latitude, fix.longitude)
            }
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun registerLocationUpdatesIfNeeded() {
        if (registered || !Permissions.hasAnyLocation(this)) return
        val providers = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                locationManager.allProviders.contains(LocationManager.FUSED_PROVIDER)
            ) {
                add(LocationManager.FUSED_PROVIDER)
            }
            if (locationManager.allProviders.contains(LocationManager.GPS_PROVIDER)) {
                add(LocationManager.GPS_PROVIDER)
            }
            if (locationManager.allProviders.contains(LocationManager.NETWORK_PROVIDER)) {
                add(LocationManager.NETWORK_PROVIDER)
            }
        }
        var anyBound = false
        for (provider in providers) {
            runCatching {
                locationManager.requestLocationUpdates(
                    provider,
                    15_000L,
                    15f,
                    locationListener,
                    Looper.getMainLooper()
                )
                anyBound = true
            }.onFailure {
                Log.w(TAG, "Provider $provider skipped: ${it.message}")
            }
        }
        registered = anyBound
    }

    private fun handleLocationUpdate(location: Location) {
        val fix = LocationFix(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracy,
            timestampMillis = location.time
        )
        LocationRadarState.currentLocation.value = fix
        scope.launch {
            evaluateTasksAgainstCoords(fix.latitude, fix.longitude)
        }
    }

    private suspend fun evaluateTasksAgainstCoords(userLat: Double, userLon: Double) {
        val container = (application as TimecureApp).container
        val repo = container.smartTaskRepository
        val tasks = repo.getAllTasks().filter {
            it.triggerType == TaskTriggerType.LOCATION && it.isEnabled && !it.isCompleted && it.placeId != null
        }
        if (tasks.isEmpty()) {
            stopSelf()
            return
        }
        val placesById = repo.getAllPlaces().associateBy { it.id }
        val now = System.currentTimeMillis()
        var tasksChanged = false

        for (task in tasks) {
            val place = placesById[task.placeId] ?: continue
            val eval = LocationGeoMath.evaluateGeofence(
                userLat = userLat,
                userLon = userLon,
                placeLat = place.latitude,
                placeLon = place.longitude,
                radiusMeters = task.radiusMeters,
                mode = task.geofenceMode,
                wasInside = task.lastInside,
                lastTriggeredMillis = task.lastTriggeredMillis,
                nowMillis = now
            )
            if (eval.shouldTrigger) {
                repo.updateTaskGeoState(task.id, eval.nextInsideState, now)
                if (!task.repeatEveryTime) {
                    repo.setTaskEnabled(task.id, false)
                }
                tasksChanged = true
                val reason = TaskTriggerActions.buildTriggerReason(task, place)
                TaskTriggerActions.fireTask(this, task, place, reason)
            } else if (eval.nextInsideState != task.lastInside) {
                repo.updateTaskGeoState(task.id, eval.nextInsideState, task.lastTriggeredMillis)
            }
        }

        if (tasksChanged) {
            container.taskScheduler.syncLocationMonitoring(repo.getAllTasks())
        }
    }

    private fun buildRadarNotification(watchedPlacesCount: Int): Notification {
        val openPending = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(MainActivity.EXTRA_OPEN_TAB, "TASKS")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val placeWord = if (watchedPlacesCount == 1) "place" else "places"
        return NotificationCompat.Builder(this, TimecureApp.CHANNEL_LOCATION_RADAR_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Timecure Location Radar Active")
            .setContentText("Watching $watchedPlacesCount marked $placeWord for your tasks")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(openPending)
            .build()
    }

    override fun onDestroy() {
        if (registered) {
            runCatching { locationManager.removeUpdates(locationListener) }
        }
        LocationRadarState.isRunning.value = false
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "LocationTaskService"
        const val NOTIFICATION_ID = 9301
        const val ACTION_START_OR_UPDATE = "com.timecure.app.LOCATION_RADAR_START"
    }
}
