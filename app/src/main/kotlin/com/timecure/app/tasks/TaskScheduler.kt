package com.timecure.app.tasks

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.timecure.app.MainActivity
import com.timecure.app.model.SmartTask
import com.timecure.app.model.TaskTriggerType
import com.timecure.app.util.Permissions
import java.util.Calendar

class TaskScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleTimeTask(task: SmartTask, now: Calendar = Calendar.getInstance()): Boolean {
        if (task.triggerType != TaskTriggerType.TIME || !task.isEnabled || task.isCompleted) {
            cancelTimeTask(task.id)
            return true
        }
        val triggerMillis = task.computeNextTimeTriggerMillis(now)
        val op = broadcast(
            taskId = task.id,
            action = TaskTriggerReceiver.ACTION_TASK_TIME_TRIGGER,
            requestCode = timeReqCode(task.id)
        )
        return setExactTrigger(triggerMillis, op)
    }

    fun cancelTimeTask(taskId: Long) {
        cancelPending(taskId, TaskTriggerReceiver.ACTION_TASK_TIME_TRIGGER, timeReqCode(taskId))
        cancelPending(taskId, TaskTriggerReceiver.ACTION_TASK_TIME_TRIGGER, snoozeReqCode(taskId))
    }

    fun scheduleSnooze(taskId: Long, delayMinutes: Int = 10) {
        val triggerMillis = System.currentTimeMillis() + delayMinutes * 60_000L
        val op = broadcast(
            taskId = taskId,
            action = TaskTriggerReceiver.ACTION_TASK_TIME_TRIGGER,
            requestCode = snoozeReqCode(taskId)
        )
        setExactTrigger(triggerMillis, op)
    }

    fun syncLocationMonitoring(activeLocationTasks: List<SmartTask>) {
        val shouldMonitor = activeLocationTasks.any {
            it.triggerType == TaskTriggerType.LOCATION &&
                it.isEnabled &&
                !it.isCompleted &&
                it.placeId != null
        }
        val intent = Intent(context, LocationTaskService::class.java)
        if (shouldMonitor && Permissions.hasAnyLocation(context)) {
            intent.action = LocationTaskService.ACTION_START_OR_UPDATE
            runCatching { ContextCompat.startForegroundService(context, intent) }
                .onFailure { Log.w(TAG, "Unable to start LocationTaskService: ${it.message}") }
        } else {
            runCatching { context.stopService(intent) }
        }
    }

    fun syncAll(tasks: List<SmartTask>) {
        tasks.forEach { task ->
            if (task.triggerType == TaskTriggerType.TIME) {
                scheduleTimeTask(task)
            } else {
                cancelTimeTask(task.id)
            }
        }
        syncLocationMonitoring(tasks)
    }

    private fun timeReqCode(taskId: Long) = TIME_REQ_BASE + (taskId and 0x7FFF).toInt()
    private fun snoozeReqCode(taskId: Long) = SNOOZE_REQ_BASE + (taskId and 0x7FFF).toInt()

    private fun broadcast(taskId: Long, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, TaskTriggerReceiver::class.java).apply {
            this.action = action
            putExtra(TaskTriggerReceiver.EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancelPending(taskId: Long, action: String, requestCode: Int) {
        val intent = Intent(context, TaskTriggerReceiver::class.java).apply {
            this.action = action
            putExtra(TaskTriggerReceiver.EXTRA_TASK_ID, taskId)
        }
        PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun setExactTrigger(triggerMillis: Long, operation: PendingIntent): Boolean {
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        val show = PendingIntent.getActivity(
            context,
            TIME_REQ_BASE,
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_TAB, "TASKS")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return try {
            if (canExact) {
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerMillis, show), operation)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, operation)
            }
            true
        } catch (se: SecurityException) {
            Log.e(TAG, "Exact alarm denied for task: ${se.message}")
            false
        }
    }

    companion object {
        private const val TAG = "TaskScheduler"
        const val TIME_REQ_BASE = 300_000
        const val SNOOZE_REQ_BASE = 400_000
    }
}
