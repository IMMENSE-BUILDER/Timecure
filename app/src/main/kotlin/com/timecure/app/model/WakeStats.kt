package com.timecure.app.model

/** One successful wake-up: when it happened and how long solving took. */
data class WakeEvent(
    val timestampMillis: Long,
    val solveSeconds: Int,
    val alarmId: Long = 0,
    val action: String = "SOLVED",
    val details: String = ""
)

data class DailyWakePoint(
    val dayLabel: String,
    val solvedCount: Int,
    val averageSolveSeconds: Int
)

data class WakeStats(
    val streakDays: Int = 0,
    val averageSolveSeconds: Int = 0,
    val fastestSolveSeconds: Int = 0,
    val totalWakes: Int = 0,
    val last7Days: List<DailyWakePoint> = emptyList()
)
