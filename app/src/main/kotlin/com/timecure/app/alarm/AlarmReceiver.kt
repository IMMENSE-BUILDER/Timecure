package com.timecure.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.timecure.app.TimecureApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        val rerings = intent.getIntExtra(AlarmScheduler.EXTRA_RERING_COUNT, 0)
        val wakeCheck = intent.getBooleanExtra(AlarmScheduler.EXTRA_WAKE_CHECK, false)
        val snoozesUsed = intent.getIntExtra(AlarmScheduler.EXTRA_SNOOZES_USED, 0)
        Log.i(TAG, "action=$action alarm=$alarmId rerings=$rerings wakeCheck=$wakeCheck snoozes=$snoozesUsed")

        // Wake-up check opens interactive countdown popup rather than immediately screaming full volume.
        if (action == AlarmScheduler.ACTION_WAKE_CHECK || wakeCheck) {
            AlarmActions.launchWakeUpCheck(context, alarmId)
            return
        }

        // Ring immediately: this is time-critical.
        AlarmActions.ring(context, alarmId, rerings, wakeCheck = false, snoozesUsed = snoozesUsed)

        // A scheduled (non re-ring, non wake-check) fire must arm the next occurrence or disable one-shot.
        if (action == AlarmScheduler.ACTION_ALARM_TRIGGER && alarmId > 0) {
            val pending = goAsync()
            val container = (context.applicationContext as TimecureApp).container
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val alarm = container.alarmRepository.getById(alarmId)
                    if (alarm != null) {
                        if (alarm.isRepeating) {
                            val guard = Calendar.getInstance().apply { add(Calendar.MINUTE, 1) }
                            container.alarmScheduler.schedule(alarm, guard)
                        } else {
                            container.alarmRepository.setEnabled(alarmId, false)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to re-arm alarm $alarmId: ${e.message}")
                } finally {
                    pending.finish()
                }
            }
        }
    }

    private companion object {
        const val TAG = "AlarmReceiver"
    }
}
