package com.timecure.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.timecure.app.MainActivity
import com.timecure.app.model.Alarm
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /** Arms the next occurrence of [alarm]. Returns false if exact scheduling was refused. */
    fun schedule(alarm: Alarm, now: Calendar = Calendar.getInstance()): Boolean {
        if (!alarm.isEnabled) {
            cancel(alarm.id)
            return true
        }
        val trigger = alarm.computeNextTriggerMillis(now)
        Log.i(TAG, "Arming alarm ${alarm.id} for $trigger")
        return setClock(trigger, broadcast(alarm.id, ACTION_ALARM_TRIGGER, 0, alarm.id.toInt()))
    }

    fun cancel(alarmId: Long) {
        cancelPending(alarmId, ACTION_ALARM_TRIGGER, alarmId.toInt())
        cancelRering(alarmId)
        cancelWakeCheck(alarmId)
    }

    /** Escalating re-ring: first miss = 2 minutes, every later miss = 5 minutes. */
    fun scheduleRering(alarmId: Long, delayMinutes: Int, count: Int) {
        val trigger = System.currentTimeMillis() + delayMinutes * 60_000L
        Log.i(TAG, "Re-ring #$count for alarm $alarmId in $delayMinutes min")
        setClock(trigger, broadcast(alarmId, ACTION_RERING_TRIGGER, count, reringCode(alarmId)))
    }

    fun cancelRering(alarmId: Long) {
        cancelPending(alarmId, ACTION_RERING_TRIGGER, reringCode(alarmId))
    }

    /** Anti-relapse wake check scheduled 5 minutes after solving an alarm. */
    fun scheduleWakeCheck(alarmId: Long, delayMinutes: Int = 5) {
        val trigger = System.currentTimeMillis() + delayMinutes * 60_000L
        Log.i(TAG, "Wake-check for alarm $alarmId in $delayMinutes min")
        setClock(trigger, broadcast(alarmId, ACTION_WAKE_CHECK, 0, wakeCheckCode(alarmId), wakeCheck = true))
    }

    fun cancelWakeCheck(alarmId: Long) {
        cancelPending(alarmId, ACTION_WAKE_CHECK, wakeCheckCode(alarmId))
    }

    private fun reringCode(alarmId: Long) = (alarmId + RERING_OFFSET).toInt()
    private fun wakeCheckCode(alarmId: Long) = (alarmId + WAKE_CHECK_OFFSET).toInt()

    private fun broadcast(
        alarmId: Long,
        action: String,
        count: Int,
        requestCode: Int,
        wakeCheck: Boolean = false
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_RERING_COUNT, count)
            putExtra(EXTRA_WAKE_CHECK, wakeCheck)
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancelPending(alarmId: Long, action: String, requestCode: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun setClock(trigger: Long, operation: PendingIntent): Boolean {
        val show = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return try {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, show), operation)
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Exact alarm refused, falling back to inexact: ${e.message}")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, operation)
            false
        }
    }

    companion object {
        private const val TAG = "AlarmScheduler"
        private const val RERING_OFFSET = 100_000L
        private const val WAKE_CHECK_OFFSET = 200_000L

        const val ACTION_ALARM_TRIGGER = "com.timecure.app.ALARM_TRIGGER"
        const val ACTION_RERING_TRIGGER = "com.timecure.app.RERING_TRIGGER"
        const val ACTION_WAKE_CHECK = "com.timecure.app.WAKE_CHECK"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_RERING_COUNT = "extra_rering_count"
        const val EXTRA_WAKE_CHECK = "extra_wake_check"

        /** Negative id marks a test simulation: it never re-rings or logs stats. */
        const val TEST_ALARM_ID = -2L

        fun reringDelayMinutes(previousRerings: Int): Int = if (previousRerings == 0) 2 else 5
    }
}
