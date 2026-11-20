package com.timecure.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TimecureApp : Application() {

    lateinit var container: AppContainer
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createChannels()
        appScope.launch {
            container.alarmRepository.refresh()
            container.pomodoroRepository.refresh()
            container.taskPlanRepository.refresh()
            container.smartTaskRepository.refreshAll()
            container.taskScheduler.syncAll(container.smartTaskRepository.getAllTasks())
        }
    }

    private fun createChannels() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.deleteNotificationChannel("timecure_alarm_channel")

        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALARM_ID, "Alarm ringing", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Shown while an alarm is ringing. Sound is played by the app itself."
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_POMODORO_ID, "Focus timer", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Live countdown while a Pomodoro focus or break session is active."
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_TASK_ALERT_ID, "Task alerts & places", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "High-priority alerts when arriving at marked locations or hitting scheduled task times."
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_LOCATION_RADAR_ID, "Location radar", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Background geofence radar status while location-based tasks are active."
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
        )
    }

    companion object {
        const val CHANNEL_ALARM_ID = "timecure_alarm_v2"
        const val CHANNEL_POMODORO_ID = "timecure_pomodoro_v1"
        const val CHANNEL_TASK_ALERT_ID = "timecure_task_alert_v1"
        const val CHANNEL_LOCATION_RADAR_ID = "timecure_location_radar_v1"
    }
}
