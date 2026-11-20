package com.timecure.app.data.repository

import com.timecure.app.data.local.TimecureDatabaseHelper
import com.timecure.app.model.Alarm
import com.timecure.app.model.WakeEvent
import com.timecure.app.model.WakeStats
import com.timecure.app.util.StatsCalculator
import kotlinx.coroutines.flow.Flow

class AlarmRepository(private val db: TimecureDatabaseHelper) {

    val alarmsFlow: Flow<List<Alarm>> = db.alarmsFlow

    suspend fun refresh() = db.refresh()
    suspend fun getAll(): List<Alarm> = db.getAll()
    suspend fun getById(id: Long): Alarm? = db.getById(id)
    suspend fun insert(alarm: Alarm): Long = db.insert(alarm)
    suspend fun update(alarm: Alarm) = db.update(alarm)
    suspend fun delete(id: Long) = db.delete(id)
    suspend fun setEnabled(id: Long, enabled: Boolean) = db.setEnabled(id, enabled)

    suspend fun logSolved(alarmId: Long, seconds: Int, attempts: Int, rerings: Int, wakeCheck: Boolean = false) =
        db.recordHistory(
            alarmId,
            if (wakeCheck) TimecureDatabaseHelper.ACTION_WAKE_CONFIRMED else TimecureDatabaseHelper.ACTION_SOLVED,
            "attempts=$attempts;rerings=$rerings",
            seconds
        )

    suspend fun logAbandoned(alarmId: Long, rerings: Int) =
        db.recordHistory(alarmId, TimecureDatabaseHelper.ACTION_ABANDONED, "rerings=$rerings")

    suspend fun recentEvents(limit: Int = 30): List<WakeEvent> {
        val since = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
        return db.recentEvents(since, limit)
    }

    suspend fun stats(): WakeStats {
        val since = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
        return StatsCalculator.compute(db.recentEvents(since, 500))
    }
}
