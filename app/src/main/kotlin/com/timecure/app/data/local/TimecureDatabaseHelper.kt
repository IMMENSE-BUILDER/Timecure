package com.timecure.app.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.timecure.app.model.AiTaskItem
import com.timecure.app.model.Alarm
import com.timecure.app.model.ChallengeCategory
import com.timecure.app.model.ChallengeDifficulty
import com.timecure.app.model.PomodoroStats
import com.timecure.app.model.WakeEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Calendar

class TimecureDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val _alarms = MutableStateFlow<List<Alarm>>(emptyList())
    val alarmsFlow: Flow<List<Alarm>> = _alarms.asStateFlow()

    private val _tasks = MutableStateFlow<List<AiTaskItem>>(emptyList())
    val tasksFlow: Flow<List<AiTaskItem>> = _tasks.asStateFlow()

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $T_ALARMS (
                $C_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $C_HOUR INTEGER NOT NULL,
                $C_MINUTE INTEGER NOT NULL,
                $C_DAYS TEXT NOT NULL,
                $C_LABEL TEXT NOT NULL,
                $C_GOAL TEXT NOT NULL,
                $C_DIFFICULTY TEXT NOT NULL,
                $C_CATEGORY TEXT NOT NULL,
                $C_ENABLED INTEGER NOT NULL,
                $C_VOLUME INTEGER NOT NULL,
                $C_VIBRATE INTEGER NOT NULL,
                $C_QUESTIONS INTEGER NOT NULL DEFAULT 2,
                $C_TONE TEXT NOT NULL DEFAULT '',
                $C_WAKE_CHECK INTEGER NOT NULL DEFAULT 0,
                $C_WAKE_PROMPT TEXT NOT NULL DEFAULT '',
                $C_TONE_TITLE TEXT NOT NULL DEFAULT '',
                $C_TONE_START INTEGER NOT NULL DEFAULT 0,
                $C_TONE_END INTEGER NOT NULL DEFAULT 0
            )"""
        )
        db.execSQL(
            """CREATE TABLE $T_HISTORY (
                $H_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $H_ALARM_ID INTEGER NOT NULL,
                $H_TIME INTEGER NOT NULL,
                $H_ACTION TEXT NOT NULL,
                $H_DETAILS TEXT NOT NULL,
                $H_VALUE INTEGER NOT NULL DEFAULT 0
            )"""
        )
        createPomodoroTable(db)
        createTasksTable(db)

        val cv = ContentValues().apply {
            put(C_HOUR, 7); put(C_MINUTE, 0); put(C_DAYS, "2,3,4,5,6")
            put(C_LABEL, "College Morning"); put(C_GOAL, "Get up for college")
            put(C_WAKE_PROMPT, "Get up for college — attendance & morning momentum start right now.")
            put(C_DIFFICULTY, ChallengeDifficulty.MEDIUM.name)
            put(C_CATEGORY, ChallengeCategory.MIXED.name)
            put(C_ENABLED, 1); put(C_VOLUME, 100); put(C_VIBRATE, 1)
            put(C_QUESTIONS, 2); put(C_TONE, ""); put(C_TONE_TITLE, "")
            put(C_TONE_START, 0L); put(C_TONE_END, 0L); put(C_WAKE_CHECK, 1)
        }
        db.insert(T_ALARMS, null, cv)
    }

    private fun createPomodoroTable(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS $T_POMODORO (
                $P_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $P_TIME INTEGER NOT NULL,
                $P_MINUTES INTEGER NOT NULL,
                $P_GOAL TEXT NOT NULL,
                $P_PRESET TEXT NOT NULL
            )"""
        )
    }

    private fun createTasksTable(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS $T_TASKS (
                $TK_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $TK_PLAN TEXT NOT NULL,
                $TK_SLOT TEXT NOT NULL,
                $TK_TEXT TEXT NOT NULL,
                $TK_TECH TEXT NOT NULL,
                $TK_HOUR INTEGER NOT NULL DEFAULT -1,
                $TK_MIN INTEGER NOT NULL DEFAULT -1,
                $TK_DONE INTEGER NOT NULL DEFAULT 0,
                $TK_CREATED INTEGER NOT NULL
            )"""
        )
    }

    /** Additive migrations only: user data survives every upgrade. */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $T_ALARMS ADD COLUMN $C_QUESTIONS INTEGER NOT NULL DEFAULT 2")
            db.execSQL("ALTER TABLE $T_ALARMS ADD COLUMN $C_TONE TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE $T_HISTORY ADD COLUMN $H_VALUE INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $T_ALARMS ADD COLUMN $C_WAKE_CHECK INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 4) {
            createPomodoroTable(db)
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE $T_ALARMS ADD COLUMN $C_WAKE_PROMPT TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE $T_ALARMS ADD COLUMN $C_TONE_TITLE TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE $T_ALARMS ADD COLUMN $C_TONE_START INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE $T_ALARMS ADD COLUMN $C_TONE_END INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 6) {
            createTasksTable(db)
        }
    }

    suspend fun refresh() = withContext(Dispatchers.IO) { _alarms.value = queryAll() }

    private fun queryAll(): List<Alarm> {
        val out = mutableListOf<Alarm>()
        readableDatabase.query(T_ALARMS, null, null, null, null, null, "$C_HOUR ASC, $C_MINUTE ASC").use {
            while (it.moveToNext()) out += it.toAlarm()
        }
        return out
    }

    suspend fun getAll(): List<Alarm> = withContext(Dispatchers.IO) { queryAll() }

    suspend fun getById(id: Long): Alarm? = withContext(Dispatchers.IO) {
        readableDatabase.query(T_ALARMS, null, "$C_ID = ?", arrayOf(id.toString()), null, null, null).use {
            if (it.moveToFirst()) it.toAlarm() else null
        }
    }

    suspend fun insert(alarm: Alarm): Long = withContext(Dispatchers.IO) {
        val id = writableDatabase.insert(T_ALARMS, null, alarm.toValues())
        _alarms.value = queryAll()
        id
    }

    suspend fun update(alarm: Alarm) = withContext(Dispatchers.IO) {
        writableDatabase.update(T_ALARMS, alarm.toValues(), "$C_ID = ?", arrayOf(alarm.id.toString()))
        _alarms.value = queryAll()
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        writableDatabase.delete(T_ALARMS, "$C_ID = ?", arrayOf(id.toString()))
        _alarms.value = queryAll()
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply { put(C_ENABLED, if (enabled) 1 else 0) }
        writableDatabase.update(T_ALARMS, cv, "$C_ID = ?", arrayOf(id.toString()))
        _alarms.value = queryAll()
    }

    suspend fun recordHistory(alarmId: Long, action: String, details: String, value: Int = 0) =
        withContext(Dispatchers.IO) {
            val cv = ContentValues().apply {
                put(H_ALARM_ID, alarmId); put(H_TIME, System.currentTimeMillis())
                put(H_ACTION, action); put(H_DETAILS, details); put(H_VALUE, value)
            }
            writableDatabase.insert(T_HISTORY, null, cv)
        }

    suspend fun recentEvents(sinceMillis: Long, limit: Int = 100): List<WakeEvent> = withContext(Dispatchers.IO) {
        val out = mutableListOf<WakeEvent>()
        readableDatabase.query(
            T_HISTORY,
            arrayOf(H_ALARM_ID, H_TIME, H_ACTION, H_DETAILS, H_VALUE),
            "$H_TIME >= ?",
            arrayOf(sinceMillis.toString()),
            null, null, "$H_TIME DESC", limit.toString()
        ).use {
            while (it.moveToNext()) {
                out += WakeEvent(
                    alarmId = it.getLong(0),
                    timestampMillis = it.getLong(1),
                    action = it.getString(2),
                    details = it.getString(3),
                    solveSeconds = it.getInt(4)
                )
            }
        }
        out
    }

    suspend fun recordPomodoroSession(durationMinutes: Int, goal: String, preset: String) =
        withContext(Dispatchers.IO) {
            val cv = ContentValues().apply {
                put(P_TIME, System.currentTimeMillis())
                put(P_MINUTES, durationMinutes)
                put(P_GOAL, goal)
                put(P_PRESET, preset)
            }
            writableDatabase.insert(T_POMODORO, null, cv)
        }

    suspend fun pomodoroStats(): PomodoroStats = withContext(Dispatchers.IO) {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        var todayMins = 0
        var totalMins = 0
        var totalCount = 0

        readableDatabase.query(T_POMODORO, arrayOf(P_TIME, P_MINUTES), null, null, null, null, null).use {
            while (it.moveToNext()) {
                val ts = it.getLong(0)
                val mins = it.getInt(1)
                totalMins += mins
                totalCount++
                if (ts >= startOfDay) todayMins += mins
            }
        }
        PomodoroStats(todayMinutes = todayMins, totalMinutes = totalMins, totalSessions = totalCount)
    }

    suspend fun refreshTasks() = withContext(Dispatchers.IO) { _tasks.value = queryTasks() }

    private fun queryTasks(): List<AiTaskItem> {
        val out = mutableListOf<AiTaskItem>()
        readableDatabase.query(T_TASKS, null, null, null, null, null, "$TK_ID ASC").use { c ->
            while (c.moveToNext()) {
                val h = c.getInt(c.getColumnIndexOrThrow(TK_HOUR))
                val m = c.getInt(c.getColumnIndexOrThrow(TK_MIN))
                out += AiTaskItem(
                    id = c.getLong(c.getColumnIndexOrThrow(TK_ID)),
                    planTitle = c.getString(c.getColumnIndexOrThrow(TK_PLAN)),
                    timeSlot = c.getString(c.getColumnIndexOrThrow(TK_SLOT)),
                    taskText = c.getString(c.getColumnIndexOrThrow(TK_TEXT)),
                    technique = c.getString(c.getColumnIndexOrThrow(TK_TECH)),
                    suggestedAlarmHour = if (h in 0..23) h else null,
                    suggestedAlarmMinute = if (m in 0..59) m else null,
                    isCompleted = c.getInt(c.getColumnIndexOrThrow(TK_DONE)) == 1,
                    createdAtMillis = c.getLong(c.getColumnIndexOrThrow(TK_CREATED))
                )
            }
        }
        return out
    }

    suspend fun replaceTaskPlan(items: List<AiTaskItem>) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(T_TASKS, null, null)
            items.forEach { item ->
                val cv = ContentValues().apply {
                    put(TK_PLAN, item.planTitle)
                    put(TK_SLOT, item.timeSlot)
                    put(TK_TEXT, item.taskText)
                    put(TK_TECH, item.technique)
                    put(TK_HOUR, item.suggestedAlarmHour ?: -1)
                    put(TK_MIN, item.suggestedAlarmMinute ?: -1)
                    put(TK_DONE, if (item.isCompleted) 1 else 0)
                    put(TK_CREATED, item.createdAtMillis)
                }
                db.insert(T_TASKS, null, cv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _tasks.value = queryTasks()
    }

    suspend fun setTaskCompleted(id: Long, completed: Boolean) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply { put(TK_DONE, if (completed) 1 else 0) }
        writableDatabase.update(T_TASKS, cv, "$TK_ID = ?", arrayOf(id.toString()))
        _tasks.value = queryTasks()
    }

    suspend fun clearTaskPlan() = withContext(Dispatchers.IO) {
        writableDatabase.delete(T_TASKS, null, null)
        _tasks.value = emptyList()
    }

    private fun Alarm.toValues() = ContentValues().apply {
        put(C_HOUR, hour); put(C_MINUTE, minute)
        put(C_DAYS, daysOfWeek.sorted().joinToString(","))
        put(C_LABEL, label); put(C_GOAL, goal); put(C_WAKE_PROMPT, wakePrompt)
        put(C_DIFFICULTY, difficulty.name); put(C_CATEGORY, category.name)
        put(C_ENABLED, if (isEnabled) 1 else 0)
        put(C_VOLUME, volume); put(C_VIBRATE, if (vibrate) 1 else 0)
        put(C_QUESTIONS, questionsRequired); put(C_TONE, toneUri); put(C_TONE_TITLE, toneTitle)
        put(C_TONE_START, toneStartMs); put(C_TONE_END, toneEndMs)
        put(C_WAKE_CHECK, if (wakeCheckEnabled) 1 else 0)
    }

    private fun Cursor.toAlarm(): Alarm {
        fun s(c: String) = getString(getColumnIndexOrThrow(c))
        fun i(c: String) = getInt(getColumnIndexOrThrow(c))
        fun l(c: String) = getLong(getColumnIndexOrThrow(c))
        return Alarm(
            id = l(C_ID),
            hour = i(C_HOUR), minute = i(C_MINUTE),
            daysOfWeek = s(C_DAYS).split(",").mapNotNull { it.trim().toIntOrNull() }.toSet(),
            label = s(C_LABEL), goal = s(C_GOAL), wakePrompt = s(C_WAKE_PROMPT),
            difficulty = ChallengeDifficulty.entries.firstOrNull { it.name == s(C_DIFFICULTY) } ?: ChallengeDifficulty.MEDIUM,
            category = ChallengeCategory.fromString(s(C_CATEGORY)),
            questionsRequired = i(C_QUESTIONS).coerceIn(1, 5),
            toneUri = s(C_TONE), toneTitle = s(C_TONE_TITLE),
            toneStartMs = l(C_TONE_START), toneEndMs = l(C_TONE_END),
            isEnabled = i(C_ENABLED) == 1,
            volume = i(C_VOLUME), vibrate = i(C_VIBRATE) == 1,
            wakeCheckEnabled = i(C_WAKE_CHECK) == 1
        )
    }

    companion object {
        const val DATABASE_NAME = "timecure.db"
        const val DATABASE_VERSION = 6

        const val ACTION_SOLVED = "SOLVED"
        const val ACTION_ABANDONED = "ABANDONED"
        const val ACTION_WAKE_CONFIRMED = "WAKE_CONFIRMED"

        private const val T_ALARMS = "alarms"
        private const val C_ID = "id"
        private const val C_HOUR = "hour"
        private const val C_MINUTE = "minute"
        private const val C_DAYS = "days"
        private const val C_LABEL = "label"
        private const val C_GOAL = "goal"
        private const val C_WAKE_PROMPT = "wake_prompt"
        private const val C_DIFFICULTY = "difficulty"
        private const val C_CATEGORY = "category"
        private const val C_ENABLED = "is_enabled"
        private const val C_VOLUME = "volume"
        private const val C_VIBRATE = "vibrate"
        private const val C_QUESTIONS = "questions_required"
        private const val C_TONE = "tone_uri"
        private const val C_TONE_TITLE = "tone_title"
        private const val C_TONE_START = "tone_start_ms"
        private const val C_TONE_END = "tone_end_ms"
        private const val C_WAKE_CHECK = "wake_check_enabled"

        private const val T_HISTORY = "alarm_history"
        private const val H_ID = "id"
        private const val H_ALARM_ID = "alarm_id"
        private const val H_TIME = "timestamp"
        private const val H_ACTION = "action"
        private const val H_DETAILS = "details"
        private const val H_VALUE = "value"

        private const val T_POMODORO = "pomodoro_sessions"
        private const val P_ID = "id"
        private const val P_TIME = "timestamp"
        private const val P_MINUTES = "duration_minutes"
        private const val P_GOAL = "goal"
        private const val P_PRESET = "preset"

        private const val T_TASKS = "ai_tasks"
        private const val TK_ID = "id"
        private const val TK_PLAN = "plan_title"
        private const val TK_SLOT = "time_slot"
        private const val TK_TEXT = "task_text"
        private const val TK_TECH = "technique"
        private const val TK_HOUR = "alarm_hour"
        private const val TK_MIN = "alarm_minute"
        private const val TK_DONE = "completed"
        private const val TK_CREATED = "created_at"
    }
}
