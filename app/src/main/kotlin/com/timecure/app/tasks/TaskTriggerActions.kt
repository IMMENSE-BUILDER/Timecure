package com.timecure.app.tasks

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.timecure.app.MainActivity
import com.timecure.app.TimecureApp
import com.timecure.app.model.GeofenceTriggerMode
import com.timecure.app.model.SavedPlace
import com.timecure.app.model.SmartTask
import com.timecure.app.model.TaskAlertMode
import com.timecure.app.model.TaskTriggerType
import com.timecure.app.ui.screens.tasks.TaskAlarmActivity

object TaskTriggerActions {

    fun buildTriggerReason(task: SmartTask, place: SavedPlace?): String = when (task.triggerType) {
        TaskTriggerType.LOCATION -> {
            val placeName = place?.name ?: "Marked place"
            val verb = if (task.geofenceMode == GeofenceTriggerMode.ARRIVE) "Arrived at" else "Left"
            "$verb $placeName (${task.radiusMeters} m zone)"
        }
        TaskTriggerType.TIME -> "Scheduled for ${task.formattedTime()}"
    }

    fun fireTask(context: Context, task: SmartTask, place: SavedPlace?, reasonOverride: String? = null) {
        val app = context.applicationContext
        val reason = reasonOverride ?: buildTriggerReason(task, place)
        if (task.alertMode == TaskAlertMode.ALARM) {
            ContextCompat.startForegroundService(
                app,
                Intent(app, TaskAlarmService::class.java).apply {
                    action = TaskAlarmService.ACTION_RING_TASK
                    putExtra(EXTRA_TASK_ID, task.id)
                    putExtra(EXTRA_TRIGGER_REASON, reason)
                }
            )
            runCatching { app.startActivity(taskAlarmIntent(app, task.id, reason)) }
        } else {
            postTaskNotification(app, task, reason)
        }
    }

    fun taskAlarmIntent(context: Context, taskId: Long, reason: String): Intent =
        Intent(context, TaskAlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TRIGGER_REASON, reason)
        }

    fun postTaskNotification(context: Context, task: SmartTask, reason: String) {
        val notifId = notifIdFor(task.id)
        val openPending = PendingIntent.getActivity(
            context,
            notifId,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(MainActivity.EXTRA_OPEN_TAB, "TASKS")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val donePending = PendingIntent.getBroadcast(
            context,
            notifId + 1000,
            Intent(context, TaskTriggerReceiver::class.java).apply {
                action = TaskTriggerReceiver.ACTION_TASK_COMPLETE
                putExtra(TaskTriggerReceiver.EXTRA_TASK_ID, task.id)
                putExtra(TaskTriggerReceiver.EXTRA_NOTIF_ID, notifId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozePending = PendingIntent.getBroadcast(
            context,
            notifId + 2000,
            Intent(context, TaskTriggerReceiver::class.java).apply {
                action = TaskTriggerReceiver.ACTION_TASK_SNOOZE
                putExtra(TaskTriggerReceiver.EXTRA_TASK_ID, task.id)
                putExtra(TaskTriggerReceiver.EXTRA_NOTIF_ID, notifId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = buildString {
            append(reason)
            if (task.notes.isNotBlank()) {
                append(" — ")
                append(task.notes)
            }
        }

        val notif = NotificationCompat.Builder(context, TimecureApp.CHANNEL_TASK_ALERT_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(task.title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .addAction(0, "Mark Done", donePending)
            .addAction(0, "Snooze 10m", snoozePending)
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(notifId, notif) }
            .onFailure { Log.w(TAG, "Task notification blocked: ${it.message}") }
    }

    fun silenceTaskAlarm(context: Context) {
        val app = context.applicationContext as TimecureApp
        app.container.soundManager.stop()
        app.stopService(Intent(app, TaskAlarmService::class.java))
        NotificationManagerCompat.from(app).cancel(TaskAlarmService.NOTIFICATION_ID)
    }

    fun notifIdFor(taskId: Long): Int = 9400 + ((taskId.coerceAtLeast(0L) % 500L).toInt())

    private const val TAG = "TaskTriggerActions"
    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TRIGGER_REASON = "extra_trigger_reason"
}
