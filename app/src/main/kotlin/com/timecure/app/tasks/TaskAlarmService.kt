package com.timecure.app.tasks

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.timecure.app.TimecureApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TaskAlarmService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "timecure:task_alarm").apply {
            acquire(15 * 60 * 1000L)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_RING_TASK) {
            stopSelf()
            return START_NOT_STICKY
        }
        val taskId = intent.getLongExtra(TaskTriggerActions.EXTRA_TASK_ID, -1L)
        val reason = intent.getStringExtra(TaskTriggerActions.EXTRA_TRIGGER_REASON).orEmpty()
            .ifBlank { "Task reminder active" }

        startForeground(NOTIFICATION_ID, buildNotification(taskId, "Timecure Task Alert", reason))

        val container = (application as TimecureApp).container
        scope.launch {
            val task = if (taskId > 0) container.smartTaskRepository.getTaskById(taskId) else null
            val title = task?.title ?: "Timecure Task Alert"
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            mgr.notify(NOTIFICATION_ID, buildNotification(taskId, title, reason))
            container.soundManager.start(
                toneUri = "",
                vibrate = true,
                rerings = 0
            )
        }
        return START_REDELIVER_INTENT
    }

    private fun buildNotification(taskId: Long, title: String, reason: String): Notification {
        val openPending = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            TaskTriggerActions.taskAlarmIntent(this, taskId, reason),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val silencePending = PendingIntent.getBroadcast(
            this,
            NOTIFICATION_ID + 1,
            Intent(this, TaskTriggerReceiver::class.java).apply {
                action = TaskTriggerReceiver.ACTION_TASK_SILENCE
                putExtra(TaskTriggerReceiver.EXTRA_TASK_ID, taskId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, TimecureApp.CHANNEL_ALARM_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(reason)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPending)
            .setFullScreenIntent(openPending, true)
            .setOngoing(true)
            .addAction(0, "Silence", silencePending)
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
        const val NOTIFICATION_ID = 9350
        const val ACTION_RING_TASK = "com.timecure.app.ACTION_RING_TASK"
    }
}
