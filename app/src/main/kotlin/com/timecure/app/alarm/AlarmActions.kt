package com.timecure.app.alarm

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.timecure.app.TimecureApp
import com.timecure.app.ui.screens.ringing.RingingActivity
import com.timecure.app.ui.screens.ringing.WakeUpCheckActivity
import kotlinx.coroutines.launch
import java.util.Calendar

/** Single place that starts, silences, and escalates a ring, shared by receiver, service, and UI. */
object AlarmActions {

    fun ring(
        context: Context,
        alarmId: Long,
        rerings: Int,
        wakeCheck: Boolean = false,
        snoozesUsed: Int = 0
    ) {
        val app = context.applicationContext
        ContextCompat.startForegroundService(
            app,
            Intent(app, AlarmService::class.java).apply {
                action = AlarmService.ACTION_RING
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmScheduler.EXTRA_RERING_COUNT, rerings)
                putExtra(AlarmScheduler.EXTRA_WAKE_CHECK, wakeCheck)
                putExtra(AlarmScheduler.EXTRA_SNOOZES_USED, snoozesUsed)
            }
        )
        runCatching { app.startActivity(ringingIntent(app, alarmId, rerings, wakeCheck, snoozesUsed)) }
    }

    fun ringingIntent(
        context: Context,
        alarmId: Long,
        rerings: Int,
        wakeCheck: Boolean = false,
        snoozesUsed: Int = 0
    ): Intent =
        Intent(context, RingingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_RERING_COUNT, rerings)
            putExtra(AlarmScheduler.EXTRA_WAKE_CHECK, wakeCheck)
            putExtra(AlarmScheduler.EXTRA_SNOOZES_USED, snoozesUsed)
        }

    /** The user solved every required challenge: silence and optionally arm the follow-up wake-up check. */
    fun solved(
        context: Context,
        alarmId: Long,
        seconds: Int,
        attempts: Int,
        rerings: Int,
        wakeCheck: Boolean = false
    ) {
        val app = context.applicationContext as TimecureApp
        silence(app)
        if (alarmId > 0) {
            app.container.alarmScheduler.cancelRering(alarmId)
            app.container.alarmScheduler.cancelSnooze(alarmId)
            app.appScope.launch {
                app.container.alarmRepository.logSolved(alarmId, seconds, attempts, rerings, wakeCheck)
                if (!wakeCheck) {
                    val alarm = app.container.alarmRepository.getById(alarmId)
                    if (alarm?.wakeCheckEnabled == true) {
                        if (alarm.preventPowerOff) {
                            PowerOffGuardState.lockForWakeCheck(app, alarmId)
                        } else {
                            PowerOffGuardState.unlock(app)
                        }
                        app.container.alarmScheduler.scheduleWakeCheck(
                            alarmId,
                            delayMinutes = alarm.wakeCheckDelayMinutes.coerceIn(1, 60)
                        )
                    } else {
                        PowerOffGuardState.unlock(app)
                    }
                } else {
                    PowerOffGuardState.unlock(app)
                }
            }
        } else {
            PowerOffGuardState.unlock(app)
        }
    }

    fun wakeCheckIntent(context: Context, alarmId: Long): Intent =
        Intent(context, WakeUpCheckActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }

    fun launchWakeUpCheck(context: Context, alarmId: Long) {
        val app = context.applicationContext
        val openPending = PendingIntent.getActivity(
            app,
            WAKE_CHECK_NOTIF_ID,
            wakeCheckIntent(app, alarmId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(app, TimecureApp.CHANNEL_ALARM_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Timecure — Wake-up check")
            .setContentText("Tap Awake before the countdown ends to confirm you're up.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPending)
            .setFullScreenIntent(openPending, true)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(app).notify(WAKE_CHECK_NOTIF_ID, notif) }
        runCatching { app.startActivity(wakeCheckIntent(app, alarmId)) }
    }

    /**
     * Confirms the user is genuinely awake on Wake-Up Check:
     * unlocks power-off protection, logs confirmation, and silences any backup/upcoming
     * alarms belonging to the same scheduled activity/goal (e.g. "Get up for college").
     */
    suspend fun confirmAwakeAndSilenceGoalAlarms(context: Context, alarmId: Long): Int {
        val app = context.applicationContext as TimecureApp
        silence(app)
        PowerOffGuardState.unlock(app)
        NotificationManagerCompat.from(app).cancel(WAKE_CHECK_NOTIF_ID)

        if (alarmId <= 0L) return 0
        app.container.alarmScheduler.cancelWakeCheck(alarmId)
        app.container.alarmScheduler.cancelRering(alarmId)
        app.container.alarmScheduler.cancelSnooze(alarmId)
        app.container.alarmRepository.logSolved(
            alarmId = alarmId,
            seconds = 0,
            attempts = 1,
            rerings = 0,
            wakeCheck = true
        )

        val triggerAlarm = app.container.alarmRepository.getById(alarmId) ?: return 0
        val goalNormalized = triggerAlarm.goal.trim()
        if (goalNormalized.isBlank()) return 0

        val now = Calendar.getInstance()
        val windowEnd = now.timeInMillis + 3 * 60 * 60_000L
        val tomorrowGuard = (now.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 4) }

        var silenced = 0
        app.container.alarmRepository.getAll()
            .filter { it.id != alarmId && it.goal.trim().equals(goalNormalized, ignoreCase = true) }
            .forEach { other ->
                app.container.alarmScheduler.cancelRering(other.id)
                app.container.alarmScheduler.cancelSnooze(other.id)
                app.container.alarmScheduler.cancelWakeCheck(other.id)
                if (other.isEnabled) {
                    val nextTrigger = other.computeNextTriggerMillis(now)
                    if (!other.isRepeating) {
                        app.container.alarmRepository.setEnabled(other.id, false)
                        app.container.alarmScheduler.cancel(other.id)
                        silenced++
                    } else if (nextTrigger <= windowEnd) {
                        app.container.alarmScheduler.schedule(other, tomorrowGuard)
                        silenced++
                    }
                }
            }
        return silenced
    }

    const val WAKE_CHECK_NOTIF_ID = 9002

    fun snooze(context: Context, alarmId: Long, delayMinutes: Int, snoozesUsed: Int) {
        val app = context.applicationContext as TimecureApp
        silence(app)
        if (alarmId > 0) {
            app.container.alarmScheduler.cancelRering(alarmId)
            app.container.alarmScheduler.scheduleSnooze(alarmId, delayMinutes, snoozesUsed + 1)
        }
    }

    /** The ringing screen was closed without solving: schedule the escalating re-ring. */
    fun abandoned(context: Context, alarmId: Long, rerings: Int) {
        val app = context.applicationContext as TimecureApp
        silence(app)
        if (alarmId > 0) {
            app.container.alarmScheduler.scheduleRering(
                alarmId, AlarmScheduler.reringDelayMinutes(rerings), rerings + 1
            )
            app.appScope.launch { app.container.alarmRepository.logAbandoned(alarmId, rerings) }
        }
    }

    fun silence(context: Context) {
        val app = context.applicationContext as TimecureApp
        app.container.voiceAssistant.stop()
        app.container.soundManager.stop()
        app.stopService(Intent(app, AlarmService::class.java))
        NotificationManagerCompat.from(app).cancel(AlarmService.NOTIFICATION_ID)
    }
}
