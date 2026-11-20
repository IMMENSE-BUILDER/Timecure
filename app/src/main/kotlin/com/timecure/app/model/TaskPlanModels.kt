package com.timecure.app.model

data class AiTaskItem(
    val id: Long = 0L,
    val planTitle: String,
    val timeSlot: String,
    val taskText: String,
    val technique: String,
    val suggestedAlarmHour: Int? = null,
    val suggestedAlarmMinute: Int? = null,
    val isCompleted: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val hasSuggestedAlarm: Boolean
        get() = suggestedAlarmHour != null && suggestedAlarmMinute != null
}
