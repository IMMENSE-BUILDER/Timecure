package com.timecure.app.tasks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.timecure.app.TimecureApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskTriggerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)
        val action = intent.action ?: return
        val app = context.applicationContext as TimecureApp
        val repo = app.container.smartTaskRepository
        val scheduler = app.container.taskScheduler

        when (action) {
            ACTION_TASK_SILENCE -> {
                TaskTriggerActions.silenceTaskAlarm(app)
                return
            }

            ACTION_TASK_COMPLETE -> {
                TaskTriggerActions.silenceTaskAlarm(app)
                if (notifId > 0) NotificationManagerCompat.from(app).cancel(notifId)
                if (taskId > 0) NotificationManagerCompat.from(app).cancel(TaskTriggerActions.notifIdFor(taskId))
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (taskId > 0) {
                            repo.setTaskCompleted(taskId, true)
                            scheduler.syncAll(repo.getAllTasks())
                        }
                    } finally {
                        pending.finish()
                    }
                }
            }

            ACTION_TASK_SNOOZE -> {
                TaskTriggerActions.silenceTaskAlarm(app)
                if (notifId > 0) NotificationManagerCompat.from(app).cancel(notifId)
                if (taskId > 0) {
                    NotificationManagerCompat.from(app).cancel(TaskTriggerActions.notifIdFor(taskId))
                    scheduler.scheduleSnooze(taskId, delayMinutes = 10)
                }
            }

            ACTION_TASK_TIME_TRIGGER -> {
                if (taskId <= 0L) return
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val task = repo.getTaskById(taskId)
                        if (task != null && !task.isCompleted) {
                            val place = task.placeId?.let { repo.getPlaceById(it) }
                            if (task.isRepeatingTime) {
                                scheduler.scheduleTimeTask(task)
                            } else {
                                repo.setTaskEnabled(task.id, false)
                            }
                            TaskTriggerActions.fireTask(app, task, place)
                        }
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_TASK_TIME_TRIGGER = "com.timecure.app.ACTION_TASK_TIME_TRIGGER"
        const val ACTION_TASK_COMPLETE = "com.timecure.app.ACTION_TASK_COMPLETE"
        const val ACTION_TASK_SNOOZE = "com.timecure.app.ACTION_TASK_SNOOZE"
        const val ACTION_TASK_SILENCE = "com.timecure.app.ACTION_TASK_SILENCE"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_NOTIF_ID = "extra_notif_id"
    }
}
