package com.timecure.app.alarm

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.timecure.app.TimecureApp
import com.timecure.app.ui.screens.ringing.RingingActivity
import kotlinx.coroutines.launch

/** Single place that starts, silences, and escalates a ring, shared by receiver, service, and UI. */
object AlarmActions {

    fun ring(context: Context, alarmId: Long, rerings: Int, wakeCheck: Boolean = false) {
        val app = context.applicationContext
        ContextCompat.startForegroundService(
            app,
            Intent(app, AlarmService::class.java).apply {
                action = AlarmService.ACTION_RING
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmScheduler.EXTRA_RERING_COUNT, rerings)
                putExtra(AlarmScheduler.EXTRA_WAKE_CHECK, wakeCheck)
            }
        )
        runCatching { app.startActivity(ringingIntent(app, alarmId, rerings, wakeCheck)) }
    }

    fun ringingIntent(context: Context, alarmId: Long, rerings: Int, wakeCheck: Boolean = false): Intent =
        Intent(context, RingingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_RERING_COUNT, rerings)
            putExtra(AlarmScheduler.EXTRA_WAKE_CHECK, wakeCheck)
        }

    /** The user solved every required challenge: silence and optionally arm the 5-min anti-relapse check. */
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
            app.appScope.launch {
                app.container.alarmRepository.logSolved(alarmId, seconds, attempts, rerings, wakeCheck)
                if (!wakeCheck) {
                    val alarm = app.container.alarmRepository.getById(alarmId)
                    if (alarm?.wakeCheckEnabled == true) {
                        app.container.alarmScheduler.scheduleWakeCheck(alarmId, delayMinutes = 5)
                    }
                }
            }
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
        app.container.soundManager.stop()
        app.stopService(Intent(app, AlarmService::class.java))
        NotificationManagerCompat.from(app).cancel(AlarmService.NOTIFICATION_ID)
    }
}
