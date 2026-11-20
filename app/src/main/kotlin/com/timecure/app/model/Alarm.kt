package com.timecure.app.model

import java.util.Calendar
import java.util.Locale

data class Alarm(
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    /** Calendar.SUNDAY..Calendar.SATURDAY. Empty = one-shot. */
    val daysOfWeek: Set<Int> = emptySet(),
    val label: String = "",
    val goal: String = "Get up for college",
    /** Personal wake-up prompt shown on the alarm card, lock screen, and fed into AI Coach. */
    val wakePrompt: String = "Get up for college — show up sharp and on time.",
    val difficulty: ChallengeDifficulty = ChallengeDifficulty.MEDIUM,
    val category: ChallengeCategory = ChallengeCategory.MIXED,
    /** How many challenges must be solved in a row to silence the alarm (1..5). */
    val questionsRequired: Int = 2,
    /** Empty = system default alarm tone; otherwise content:// or file:// path (including trimmed MP3s). */
    val toneUri: String = "",
    /** Human-readable name for custom or trimmed MP3 alarm tone. */
    val toneTitle: String = "",
    /** Optional start/end trim bounds in milliseconds (endMs == 0 means play full track). */
    val toneStartMs: Long = 0L,
    val toneEndMs: Long = 0L,
    val isEnabled: Boolean = true,
    val volume: Int = 100,
    val vibrate: Boolean = true,
    /** When true, schedules a 5-minute anti-relapse wake check after solving. */
    val wakeCheckEnabled: Boolean = false
) {
    val isRepeating: Boolean get() = daysOfWeek.isNotEmpty()

    val formattedTime: String
        get() {
            val h = if (hour % 12 == 0) 12 else hour % 12
            return String.format(Locale.getDefault(), "%d:%02d", h, minute)
        }

    val amPm: String get() = if (hour < 12) "AM" else "PM"

    val hasTrimmedSegment: Boolean
        get() = toneEndMs > toneStartMs && toneEndMs > 0L

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
            return WEEK_ORDER.filter { it.first in daysOfWeek }.joinToString(", ") { it.second }
        }

    fun computeNextTriggerMillis(now: Calendar = Calendar.getInstance()): Long {
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

    companion object {
        val WEEK_ORDER = listOf(
            Calendar.MONDAY to "Mon",
            Calendar.TUESDAY to "Tue",
            Calendar.WEDNESDAY to "Wed",
            Calendar.THURSDAY to "Thu",
            Calendar.FRIDAY to "Fri",
            Calendar.SATURDAY to "Sat",
            Calendar.SUNDAY to "Sun"
        )

        /** Curated personal wake-up routine templates for quick creation. */
        val PERSONAL_PROMPTS = listOf(
            Triple("Get up for college", "College Morning", "Get up for college — attendance, momentum, and your future self are counting on you."),
            Triple("Deep Work Sprint", "Deep Work", "Feet on the floor. 90 minutes of uninterrupted execution before the world wakes up."),
            Triple("Gym & Morning Run", "Morning Training", "Lace up now — the hardest rep is getting out of bed."),
            Triple("Exam & Study Prep", "Study Block", "Clear mind, sharp recall — lock in your highest-leverage study hour."),
            Triple("Early Coding", "Build & Ship", "Open the laptop and ship something real before noon.")
        )
    }
}
