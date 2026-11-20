package com.timecure.app.model

enum class PomodoroPhase(
    val title: String,
    val subtitle: String,
    val shortLabel: String
) {
    FOCUS("Deep Focus", "Lock in on one task — no distractions", "Focus"),
    SHORT_BREAK("Short Break", "Step away, breathe, reset your eyes", "Break"),
    LONG_BREAK("Long Recovery", "Full recharge before the next block", "Recharge");

    val isBreak: Boolean get() = this != FOCUS
}

enum class PomodoroPreset(
    val displayName: String,
    val focusMinutes: Int,
    val shortBreakMinutes: Int,
    val longBreakMinutes: Int,
    val cyclesBeforeLongBreak: Int
) {
    SPRINT("Sprint 15/3", 15, 3, 10, 4),
    CLASSIC("Classic 25/5", 25, 5, 15, 4),
    DEEP_WORK("Deep 50/10", 50, 10, 20, 3),
    ULTRADIAN("Flow 90/20", 90, 20, 30, 2);

    fun durationSeconds(phase: PomodoroPhase): Int = when (phase) {
        PomodoroPhase.FOCUS -> focusMinutes * 60
        PomodoroPhase.SHORT_BREAK -> shortBreakMinutes * 60
        PomodoroPhase.LONG_BREAK -> longBreakMinutes * 60
    }
}

data class PomodoroState(
    val preset: PomodoroPreset = PomodoroPreset.CLASSIC,
    val phase: PomodoroPhase = PomodoroPhase.FOCUS,
    val remainingSeconds: Int = PomodoroPreset.CLASSIC.focusMinutes * 60,
    val totalSeconds: Int = PomodoroPreset.CLASSIC.focusMinutes * 60,
    val isRunning: Boolean = false,
    val completedFocusCycles: Int = 0,
    val activeGoal: String = "Deep Work",
    val phaseTransitionNonce: Int = 0,
    val lastCompletedPhase: PomodoroPhase? = null
) {
    val progress: Float
        get() = if (totalSeconds <= 0) 0f
        else (1f - remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)

    val formattedTime: String
        get() {
            val mins = (remainingSeconds / 60).coerceAtLeast(0)
            val secs = (remainingSeconds % 60).coerceAtLeast(0)
            return "%02d:%02d".format(mins, secs)
        }

    val cycleIndexInSet: Int
        get() = completedFocusCycles % preset.cyclesBeforeLongBreak
}

data class PomodoroStats(
    val todayMinutes: Int = 0,
    val totalMinutes: Int = 0,
    val totalSessions: Int = 0
)
