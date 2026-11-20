package com.timecure.app.data.repository

import com.timecure.app.data.local.TimecureDatabaseHelper
import com.timecure.app.model.PomodoroStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PomodoroRepository(private val db: TimecureDatabaseHelper) {

    private val _stats = MutableStateFlow(PomodoroStats())
    val statsFlow: StateFlow<PomodoroStats> = _stats.asStateFlow()

    suspend fun refresh() {
        _stats.value = db.pomodoroStats()
    }

    suspend fun recordSession(durationMinutes: Int, goal: String, preset: String) {
        db.recordPomodoroSession(durationMinutes, goal, preset)
        _stats.value = db.pomodoroStats()
    }
}
