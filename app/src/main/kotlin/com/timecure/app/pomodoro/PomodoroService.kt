package com.timecure.app.pomodoro

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.timecure.app.MainActivity
import com.timecure.app.TimecureApp

class PomodoroService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val engine = (application as TimecureApp).container.pomodoroEngine
        when (intent?.action) {
            ACTION_TOGGLE -> engine.toggleRun()
            ACTION_STOP -> {
                engine.pause(stopService = false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val state = engine.state.value
        startForeground(NOTIFICATION_ID, buildNotification(state.phase.title, state.activeGoal, state.formattedTime, state.isRunning))
        return START_NOT_STICKY
    }

    private fun buildNotification(
        phaseTitle: String,
        goal: String,
        formattedTime: String,
        isRunning: Boolean
    ): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_TAB, "FOCUS")
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = PendingIntent.getService(
            this, 1,
            Intent(this, PomodoroService::class.java).setAction(ACTION_TOGGLE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 2,
            Intent(this, PomodoroService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val status = if (isRunning) "$formattedTime remaining" else "$formattedTime (paused)"
        return NotificationCompat.Builder(this, TimecureApp.CHANNEL_POMODORO_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("$phaseTitle — $status")
            .setContentText(goal)
            .setOngoing(isRunning)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openApp)
            .addAction(0, if (isRunning) "Pause" else "Resume", toggleIntent)
            .addAction(0, "Dismiss", stopIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 9002
        const val ACTION_START_OR_UPDATE = "com.timecure.app.POMO_UPDATE"
        const val ACTION_TOGGLE = "com.timecure.app.POMO_TOGGLE"
        const val ACTION_STOP = "com.timecure.app.POMO_STOP"
    }
}
