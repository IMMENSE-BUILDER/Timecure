package com.timecure.app.alarm

import android.content.Context

/**
 * Shared persistent state queried by [PowerOffGuardService] so the power-off menu
 * is ONLY blocked while an alarm with Prevent Power-Off is actively ringing or
 * waiting for Wake-Up Check confirmation.
 */
object PowerOffGuardState {
    private const val PREFS = "timecure_power_guard"
    private const val KEY_LOCKED = "locked"
    private const val KEY_ALARM_ID = "alarm_id"
    private const val KEY_WAITING_WAKE_CHECK = "waiting_wake_check"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun lockForRinging(context: Context, alarmId: Long) {
        prefs(context).edit()
            .putBoolean(KEY_LOCKED, true)
            .putLong(KEY_ALARM_ID, alarmId)
            .putBoolean(KEY_WAITING_WAKE_CHECK, false)
            .apply()
    }

    fun lockForWakeCheck(context: Context, alarmId: Long) {
        prefs(context).edit()
            .putBoolean(KEY_LOCKED, true)
            .putLong(KEY_ALARM_ID, alarmId)
            .putBoolean(KEY_WAITING_WAKE_CHECK, true)
            .apply()
    }

    fun unlock(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_LOCKED, false)
            .putLong(KEY_ALARM_ID, -1L)
            .putBoolean(KEY_WAITING_WAKE_CHECK, false)
            .apply()
    }

    fun isLocked(context: Context): Boolean = prefs(context).getBoolean(KEY_LOCKED, false)

    fun lockedAlarmId(context: Context): Long = prefs(context).getLong(KEY_ALARM_ID, -1L)

    fun isWaitingWakeCheck(context: Context): Boolean =
        prefs(context).getBoolean(KEY_WAITING_WAKE_CHECK, false)
}
