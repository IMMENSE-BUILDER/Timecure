package com.timecure.app.util

import com.timecure.app.model.DailyWakePoint
import com.timecure.app.model.WakeEvent
import com.timecure.app.model.WakeStats
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object StatsCalculator {

    /**
     * Streak = consecutive calendar days (ending today, or yesterday if today has none yet)
     * with at least one solved wake-up. Also builds the 7-day breakdown for the Insights chart.
     */
    fun compute(events: List<WakeEvent>, now: Calendar = Calendar.getInstance()): WakeStats {
        val solved = events.filter { it.action == "SOLVED" }
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

        val byDay = solved.groupBy { dayKey(it.timestampMillis) }
        val last7 = (6 downTo 0).map { offset ->
            val cal = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val key = dayKey(cal.timeInMillis)
            val dayEvents = byDay[key].orEmpty()
            DailyWakePoint(
                dayLabel = dayFormat.format(cal.time).take(3),
                solvedCount = dayEvents.size,
                averageSolveSeconds = if (dayEvents.isEmpty()) 0
                else dayEvents.map { it.solveSeconds }.average().toInt()
            )
        }

        if (solved.isEmpty()) return WakeStats(last7Days = last7)

        val days = byDay.keys
        val cursor = (now.clone() as Calendar)
        if (dayKey(cursor.timeInMillis) !in days) cursor.add(Calendar.DAY_OF_YEAR, -1)
        var streak = 0
        while (dayKey(cursor.timeInMillis) in days) {
            streak++
            cursor.add(Calendar.DAY_OF_YEAR, -1)
        }

        val recent = solved.sortedByDescending { it.timestampMillis }.take(14)
        return WakeStats(
            streakDays = streak,
            averageSolveSeconds = recent.map { it.solveSeconds }.average().toInt(),
            fastestSolveSeconds = solved.minOf { it.solveSeconds },
            totalWakes = solved.size,
            last7Days = last7
        )
    }

    private fun dayKey(millis: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return c.get(Calendar.YEAR) * 1000L + c.get(Calendar.DAY_OF_YEAR)
    }
}
