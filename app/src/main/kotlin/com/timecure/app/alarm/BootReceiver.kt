package com.timecure.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.timecure.app.TimecureApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Re-arms every enabled alarm after reboot, app update, or a clock/timezone change. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "Re-arming alarms after ${intent.action}")
        val pending = goAsync()
        val container = (context.applicationContext as TimecureApp).container
        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.alarmRepository.getAll()
                    .filter { it.isEnabled }
                    .forEach { container.alarmScheduler.schedule(it) }
                container.smartTaskRepository.refreshAll()
                container.taskScheduler.syncAll(container.smartTaskRepository.getAllTasks())
            } catch (e: Exception) {
                Log.e(TAG, "Re-arm failed: ${e.message}")
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "BootReceiver"
    }
}
