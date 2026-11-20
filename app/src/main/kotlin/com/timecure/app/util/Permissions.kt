package com.timecure.app.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

data class PermissionState(
    val notifications: Boolean,
    val exactAlarms: Boolean,
    val fullScreen: Boolean,
    val batteryUnrestricted: Boolean,
    val dndAccess: Boolean
) {
    /** Without these three an alarm can fail to ring or to appear over the lock screen. */
    val criticalOk: Boolean get() = notifications && exactAlarms && fullScreen
    val missingCriticalCount: Int get() = listOf(notifications, exactAlarms, fullScreen).count { !it }
}

object Permissions {

    fun check(context: Context): PermissionState {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return PermissionState(
            notifications = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            exactAlarms = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms(),
            fullScreen = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || nm.canUseFullScreenIntent(),
            batteryUnrestricted = pm.isIgnoringBatteryOptimizations(context.packageName),
            dndAccess = nm.isNotificationPolicyAccessGranted
        )
    }

    private fun appUri(context: Context) = Uri.parse("package:${context.packageName}")

    fun notificationSettings(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    fun exactAlarmSettings(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, appUri(context))
        else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, appUri(context))

    fun fullScreenSettings(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, appUri(context))
        else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, appUri(context))

    fun batterySettings(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    fun dndSettings(): Intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    fun hasFineLocation(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun hasCoarseLocation(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun hasAnyLocation(context: Context): Boolean =
        hasFineLocation(context) || hasCoarseLocation(context)

    fun hasBackgroundLocation(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun appDetailsSettings(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, appUri(context))

    fun canDrawOverlays(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    fun overlaySettings(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, appUri(context))

    fun isPowerOffAccessibilityEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        return enabled.contains("${context.packageName}/.alarm.PowerOffGuardService", ignoreCase = true) ||
            enabled.contains("${context.packageName}/com.timecure.app.alarm.PowerOffGuardService", ignoreCase = true)
    }

    fun accessibilitySettings(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    fun hasCamera(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    fun hasRecordAudio(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun hasActivityRecognition(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED
}
