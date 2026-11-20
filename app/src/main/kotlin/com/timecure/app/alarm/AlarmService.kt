package com.timecure.app.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.timecure.app.TimecureApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Keeps the process alive and the alarm audible until [AlarmActions] silences it. */
class AlarmService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "timecure:ring").apply {
            acquire(60 * 60 * 1000L)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_RING) {
            stopSelf()
            return START_NOT_STICKY
        }
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        val rerings = intent.getIntExtra(AlarmScheduler.EXTRA_RERING_COUNT, 0)
        val wakeCheck = intent.getBooleanExtra(AlarmScheduler.EXTRA_WAKE_CHECK, false)

        // Must reach foreground within 5 seconds, so post notification before any I/O.
        startForeground(NOTIFICATION_ID, buildNotification(alarmId, rerings, wakeCheck))

        val container = (application as TimecureApp).container
        scope.launch {
            val alarm = if (alarmId > 0) container.alarmRepository.getById(alarmId) else null
            Log.i(TAG, "Ringing alarm=$alarmId rerings=$rerings wakeCheck=$wakeCheck")
            container.soundManager.start(
                toneUri = alarm?.toneUri.orEmpty(),
                vibrate = alarm?.vibrate ?: true,
                rerings = rerings,
                startMs = alarm?.toneStartMs ?: 0L,
                endMs = alarm?.toneEndMs ?: 0L
            )
        }
        return START_REDELIVER_INTENT
    }

    private fun buildNotification(alarmId: Long, rerings: Int, wakeCheck: Boolean): Notification {
        val open = PendingIntent.getActivity(
            this, NOTIFICATION_ID, AlarmActions.ringingIntent(this, alarmId, rerings, wakeCheck),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = if (wakeCheck) "Timecure — Still awake?" else "Timecure — Wake up"
        val text = when {
            wakeCheck -> "5-minute anti-relapse check — solve 1 quick challenge"
            rerings == 0 -> "Solve the challenge to silence this alarm"
            else -> "Re-ring #$rerings — solve the challenge to silence it"
        }
        return NotificationCompat.Builder(this, TimecureApp.CHANNEL_ALARM_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        (application as TimecureApp).container.soundManager.stop()
        scope.cancel()
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "AlarmService"
        const val NOTIFICATION_ID = 9001
        const val ACTION_RING = "com.timecure.app.RING"
    }
}
