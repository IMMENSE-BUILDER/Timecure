package com.timecure.app.model

import java.util.Calendar
import java.util.Locale

enum class PlaceTag(val displayName: String) {
    COLLEGE("College"),
    RESTAURANT("Restaurant"),
    HOME("Home"),
    WORK("Work"),
    GYM("Gym"),
    SHOP("Shop"),
    CUSTOM("Custom");

    companion object {
        fun fromString(raw: String?): PlaceTag =
            entries.firstOrNull { it.name == raw } ?: CUSTOM
    }
}

data class SavedPlace(
    val id: Long = 0L,
    val name: String,
    val tag: PlaceTag = PlaceTag.CUSTOM,
    val addressLabel: String = "",
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int = 150,
    val createdAtMillis: Long = System.currentTimeMillis()
)

enum class TaskTriggerType(val displayName: String) {
    TIME("Time"),
    LOCATION("Location");

    companion object {
        fun fromString(raw: String?): TaskTriggerType =
            entries.firstOrNull { it.name == raw } ?: TIME
    }
}

enum class GeofenceTriggerMode(val displayName: String) {
    ARRIVE("On arrive"),
    LEAVE("On leave");

    companion object {
        fun fromString(raw: String?): GeofenceTriggerMode =
            entries.firstOrNull { it.name == raw } ?: ARRIVE
    }
}

enum class TaskAlertMode(val displayName: String) {
    NOTIFICATION("Notification"),
    ALARM("Alarm ring");

    companion object {
        fun fromString(raw: String?): TaskAlertMode =
            entries.firstOrNull { it.name == raw } ?: NOTIFICATION
    }
}

data class SmartTask(
    val id: Long = 0L,
    val title: String,
    val notes: String = "",
    val triggerType: TaskTriggerType = TaskTriggerType.TIME,
    val alertMode: TaskAlertMode = TaskAlertMode.NOTIFICATION,
    val isEnabled: Boolean = true,
    val isCompleted: Boolean = false,
    val repeatEveryTime: Boolean = false,
    val hour: Int = 9,
    val minute: Int = 0,
    /** Calendar.SUNDAY..Calendar.SATURDAY. Empty = one-shot. */
    val daysOfWeek: Set<Int> = emptySet(),
    val placeId: Long? = null,
    val radiusMeters: Int = 150,
    val geofenceMode: GeofenceTriggerMode = GeofenceTriggerMode.ARRIVE,
    val lastInside: Boolean = false,
    val lastTriggeredMillis: Long = 0L,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val isRepeatingTime: Boolean get() = daysOfWeek.isNotEmpty()

    val daysSummary: String
        get() {
            if (daysOfWeek.isEmpty()) return "Once"
            if (daysOfWeek.size == 7) return "Every day"
            val weekdays = setOf(
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                Calendar.THURSDAY, Calendar.FRIDAY
            )
            if (daysOfWeek == weekdays) return "Weekdays"
            if (daysOfWeek == setOf(Calendar.SATURDAY, Calendar.SUNDAY)) return "Weekends"
            return Alarm.WEEK_ORDER.filter { it.first in daysOfWeek }.joinToString(", ") { it.second }
        }

    fun formattedTime(): String {
        val h = if (hour % 12 == 0) 12 else hour % 12
        val amPm = if (hour < 12) "AM" else "PM"
        return String.format(Locale.getDefault(), "%02d:%02d %s", h, minute, amPm)
    }

    fun computeNextTimeTriggerMillis(now: Calendar = Calendar.getInstance()): Long {
        val base = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        for (offset in 0..7) {
            val candidate = (base.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            val dayOk = daysOfWeek.isEmpty() || candidate.get(Calendar.DAY_OF_WEEK) in daysOfWeek
            if (dayOk && candidate.timeInMillis > now.timeInMillis) return candidate.timeInMillis
        }
        return base.timeInMillis + 24L * 60 * 60 * 1000
    }
}
