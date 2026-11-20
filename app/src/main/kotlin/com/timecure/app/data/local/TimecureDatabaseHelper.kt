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
import com.timecure.app.model.GeofenceTriggerMode
import com.timecure.app.model.PlaceTag
import com.timecure.app.model.PomodoroStats
import com.timecure.app.model.SavedPlace
import com.timecure.app.model.SmartTask
import com.timecure.app.model.TaskAlertMode
import com.timecure.app.model.TaskTriggerType
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

    private val _places = MutableStateFlow<List<SavedPlace>>(emptyList())
    val placesFlow: Flow<List<SavedPlace>> = _places.asStateFlow()

    private val _smartTasks = MutableStateFlow<List<SmartTask>>(emptyList())
    val smartTasksFlow: Flow<List<SmartTask>> = _smartTasks.asStateFlow()

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
                $C_TONE_END INTEGER NOT NULL DEFAULT 0,
                $C_GENTLE_WAKE INTEGER NOT NULL DEFAULT 30,
                $C_TIME_REMINDER INTEGER NOT NULL DEFAULT 0,
                $C_WEATHER_REMINDER INTEGER NOT NULL DEFAULT 0,
                $C_LABEL_REMINDER INTEGER NOT NULL DEFAULT 0,
                $C_EXTRA_LOUD INTEGER NOT NULL DEFAULT 0,
                $C_SNOOZE_ENABLED INTEGER NOT NULL DEFAULT 1,
                $C_SNOOZE_INTERVAL INTEGER NOT NULL DEFAULT 5,
                $C_SNOOZE_MAX INTEGER NOT NULL DEFAULT 3,
                $C_WALLPAPER TEXT NOT NULL DEFAULT 'preset:yesterday_tomorrow',
                $C_WAKE_MISSIONS TEXT NOT NULL DEFAULT '',
                $C_WAKE_CHECK_DELAY INTEGER NOT NULL DEFAULT 2,
                $C_WAKE_CHECK_COUNTDOWN INTEGER NOT NULL DEFAULT 15,
                $C_PREVENT_POWER_OFF INTEGER NOT NULL DEFAULT 0
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
        createSavedPlacesTable(db)
        createSmartTasksTable(db)

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

    private fun createSavedPlacesTable(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS $T_PLACES (
                $PL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $PL_NAME TEXT NOT NULL,
                $PL_TAG TEXT NOT NULL,
                $PL_ADDRESS TEXT NOT NULL DEFAULT '',
                $PL_LAT REAL NOT NULL,
                $PL_LON REAL NOT NULL,
                $PL_RADIUS INTEGER NOT NULL DEFAULT 150,
                $PL_CREATED INTEGER NOT NULL
            )"""
        )
    }

    private fun createSmartTasksTable(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS $T_SMART_TASKS (
                $ST_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $ST_TITLE TEXT NOT NULL,
                $ST_NOTES TEXT NOT NULL DEFAULT '',
                $ST_TRIGGER_TYPE TEXT NOT NULL,
                $ST_ALERT_MODE TEXT NOT NULL,
                $ST_ENABLED INTEGER NOT NULL DEFAULT 1,
                $ST_COMPLETED INTEGER NOT NULL DEFAULT 0,
                $ST_REPEAT_GEO INTEGER NOT NULL DEFAULT 0,
                $ST_HOUR INTEGER NOT NULL DEFAULT 9,
                $ST_MINUTE INTEGER NOT NULL DEFAULT 0,
                $ST_DAYS TEXT NOT NULL DEFAULT '',
                $ST_PLACE_ID INTEGER,
                $ST_RADIUS INTEGER NOT NULL DEFAULT 150,
                $ST_GEO_MODE TEXT NOT NULL DEFAULT 'ARRIVE',
                $ST_LAST_INSIDE INTEGER NOT NULL DEFAULT 0,
                $ST_LAST_TRIGGERED INTEGER NOT NULL DEFAULT 0,
                $ST_CREATED INTEGER NOT NULL
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
        if (oldVersion < 7) {
            createSavedPlacesTable(db)
            createSmartTasksTable(db)
        }
        if (oldVersion < 8) {
            db.execSQL("ALTER TABLE $T_ALARMS ADD COLUMN $C_GENTLE_WAKE INTEGER NOT NULL DEFAULT 30")
            db.execSQL("ALTER TABLE $T_ALARMS ADD COLUMN $C_TIME_REMINDER INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE $T_ALARMS ADD COLUMN $C_WEATHER_REMINDER INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE $T_ALARMS ADD COLUMN $C_LABEL_REMINDER INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE $T_ALARMS ADD COLUMN $C_EXTRA_LOUD INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE $T_ALARMS ADD COLUMN $C_SNOOZE_ENABLED INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE $T_ALARMS ADD COLUMN $C_SNOOZE_INTERVAL INTEGER NOT NULL DEFAULT 5")
            db.execSQL("ALTER TABLE $T_ALARMS ADD COLUMN $C_SNOOZE_MAX INTEGER NOT NULL DEFAULT 3")
            db.execSQL("ALTER TABLE $T_ALARMS ADD COLUMN $C_WALLPAPER TEXT NOT NULL DEFAULT 'preset:yesterday_tomorrow'")
        }
        if (oldVersion < 9) {
            db.execSQL("ALTER TABLE $T_ALARMS ADD COLUMN $C_WAKE_MISSIONS TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE $T_ALARMS ADD COLUMN $C_WAKE_CHECK_DELAY INTEGER NOT NULL DEFAULT 2")
            db.execSQL("ALTER TABLE $T_ALARMS ADD COLUMN $C_WAKE_CHECK_COUNTDOWN INTEGER NOT NULL DEFAULT 15")
            db.execSQL("ALTER TABLE $T_ALARMS ADD COLUMN $C_PREVENT_POWER_OFF INTEGER NOT NULL DEFAULT 0")
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

    suspend fun getAllTasks(): List<AiTaskItem> = withContext(Dispatchers.IO) { queryTasks() }

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

    // ---------- Saved Places (SQLite v7) ----------

    suspend fun refreshPlaces() = withContext(Dispatchers.IO) { _places.value = queryPlaces() }

    private fun queryPlaces(): List<SavedPlace> {
        val out = mutableListOf<SavedPlace>()
        readableDatabase.query(T_PLACES, null, null, null, null, null, "$PL_CREATED DESC").use { c ->
            while (c.moveToNext()) out += c.toSavedPlace()
        }
        return out
    }

    suspend fun getAllPlaces(): List<SavedPlace> = withContext(Dispatchers.IO) { queryPlaces() }

    suspend fun getPlaceById(id: Long): SavedPlace? = withContext(Dispatchers.IO) {
        readableDatabase.query(T_PLACES, null, "$PL_ID = ?", arrayOf(id.toString()), null, null, null).use { c ->
            if (c.moveToFirst()) c.toSavedPlace() else null
        }
    }

    suspend fun insertPlace(place: SavedPlace): Long = withContext(Dispatchers.IO) {
        val id = writableDatabase.insert(T_PLACES, null, place.toValues())
        _places.value = queryPlaces()
        id
    }

    suspend fun updatePlace(place: SavedPlace) = withContext(Dispatchers.IO) {
        writableDatabase.update(T_PLACES, place.toValues(), "$PL_ID = ?", arrayOf(place.id.toString()))
        _places.value = queryPlaces()
    }

    suspend fun deletePlace(id: Long) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val unlink = ContentValues().apply {
                putNull(ST_PLACE_ID)
                put(ST_ENABLED, 0)
            }
            db.update(T_SMART_TASKS, unlink, "$ST_PLACE_ID = ?", arrayOf(id.toString()))
            db.delete(T_PLACES, "$PL_ID = ?", arrayOf(id.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _places.value = queryPlaces()
        _smartTasks.value = querySmartTasks()
    }

    // ---------- Smart Tasks (Time & Location, SQLite v7) ----------

    suspend fun refreshSmartTasks() = withContext(Dispatchers.IO) { _smartTasks.value = querySmartTasks() }

    private fun querySmartTasks(): List<SmartTask> {
        val out = mutableListOf<SmartTask>()
        readableDatabase.query(
            T_SMART_TASKS,
            null,
            null,
            null,
            null,
            null,
            "$ST_COMPLETED ASC, $ST_CREATED DESC"
        ).use { c ->
            while (c.moveToNext()) out += c.toSmartTask()
        }
        return out
    }

    suspend fun getAllSmartTasks(): List<SmartTask> = withContext(Dispatchers.IO) { querySmartTasks() }

    suspend fun getSmartTaskById(id: Long): SmartTask? = withContext(Dispatchers.IO) {
        readableDatabase.query(T_SMART_TASKS, null, "$ST_ID = ?", arrayOf(id.toString()), null, null, null).use { c ->
            if (c.moveToFirst()) c.toSmartTask() else null
        }
    }

    suspend fun insertSmartTask(task: SmartTask): Long = withContext(Dispatchers.IO) {
        val id = writableDatabase.insert(T_SMART_TASKS, null, task.toValues())
        _smartTasks.value = querySmartTasks()
        id
    }

    suspend fun updateSmartTask(task: SmartTask) = withContext(Dispatchers.IO) {
        writableDatabase.update(T_SMART_TASKS, task.toValues(), "$ST_ID = ?", arrayOf(task.id.toString()))
        _smartTasks.value = querySmartTasks()
    }

    suspend fun deleteSmartTask(id: Long) = withContext(Dispatchers.IO) {
        writableDatabase.delete(T_SMART_TASKS, "$ST_ID = ?", arrayOf(id.toString()))
        _smartTasks.value = querySmartTasks()
    }

    suspend fun setSmartTaskEnabled(id: Long, enabled: Boolean) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply { put(ST_ENABLED, if (enabled) 1 else 0) }
        writableDatabase.update(T_SMART_TASKS, cv, "$ST_ID = ?", arrayOf(id.toString()))
        _smartTasks.value = querySmartTasks()
    }

    suspend fun setSmartTaskCompleted(id: Long, completed: Boolean) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put(ST_COMPLETED, if (completed) 1 else 0)
            if (completed) put(ST_ENABLED, 0)
        }
        writableDatabase.update(T_SMART_TASKS, cv, "$ST_ID = ?", arrayOf(id.toString()))
        _smartTasks.value = querySmartTasks()
    }

    suspend fun updateSmartTaskGeoState(id: Long, isInside: Boolean, lastTriggeredMillis: Long) =
        withContext(Dispatchers.IO) {
            val cv = ContentValues().apply {
                put(ST_LAST_INSIDE, if (isInside) 1 else 0)
                put(ST_LAST_TRIGGERED, lastTriggeredMillis)
            }
            writableDatabase.update(T_SMART_TASKS, cv, "$ST_ID = ?", arrayOf(id.toString()))
            _smartTasks.value = querySmartTasks()
        }

    private fun SavedPlace.toValues() = ContentValues().apply {
        put(PL_NAME, name)
        put(PL_TAG, tag.name)
        put(PL_ADDRESS, addressLabel)
        put(PL_LAT, latitude)
        put(PL_LON, longitude)
        put(PL_RADIUS, radiusMeters.coerceIn(50, 2000))
        put(PL_CREATED, createdAtMillis)
    }

    private fun Cursor.toSavedPlace(): SavedPlace {
        fun s(c: String) = getString(getColumnIndexOrThrow(c))
        fun d(c: String) = getDouble(getColumnIndexOrThrow(c))
        fun i(c: String) = getInt(getColumnIndexOrThrow(c))
        fun l(c: String) = getLong(getColumnIndexOrThrow(c))
        return SavedPlace(
            id = l(PL_ID),
            name = s(PL_NAME),
            tag = PlaceTag.fromString(s(PL_TAG)),
            addressLabel = s(PL_ADDRESS),
            latitude = d(PL_LAT),
            longitude = d(PL_LON),
            radiusMeters = i(PL_RADIUS).coerceIn(50, 2000),
            createdAtMillis = l(PL_CREATED)
        )
    }

    private fun SmartTask.toValues() = ContentValues().apply {
        put(ST_TITLE, title)
        put(ST_NOTES, notes)
        put(ST_TRIGGER_TYPE, triggerType.name)
        put(ST_ALERT_MODE, alertMode.name)
        put(ST_ENABLED, if (isEnabled) 1 else 0)
        put(ST_COMPLETED, if (isCompleted) 1 else 0)
        put(ST_REPEAT_GEO, if (repeatEveryTime) 1 else 0)
        put(ST_HOUR, hour)
        put(ST_MINUTE, minute)
        put(ST_DAYS, daysOfWeek.sorted().joinToString(","))
        if (placeId != null) put(ST_PLACE_ID, placeId) else putNull(ST_PLACE_ID)
        put(ST_RADIUS, radiusMeters.coerceIn(50, 2000))
        put(ST_GEO_MODE, geofenceMode.name)
        put(ST_LAST_INSIDE, if (lastInside) 1 else 0)
        put(ST_LAST_TRIGGERED, lastTriggeredMillis)
        put(ST_CREATED, createdAtMillis)
    }

    private fun Cursor.toSmartTask(): SmartTask {
        fun s(c: String) = getString(getColumnIndexOrThrow(c))
        fun i(c: String) = getInt(getColumnIndexOrThrow(c))
        fun l(c: String) = getLong(getColumnIndexOrThrow(c))
        val placeIdx = getColumnIndexOrThrow(ST_PLACE_ID)
        val pid = if (isNull(placeIdx)) null else getLong(placeIdx)
        return SmartTask(
            id = l(ST_ID),
            title = s(ST_TITLE),
            notes = s(ST_NOTES),
            triggerType = TaskTriggerType.fromString(s(ST_TRIGGER_TYPE)),
            alertMode = TaskAlertMode.fromString(s(ST_ALERT_MODE)),
            isEnabled = i(ST_ENABLED) == 1,
            isCompleted = i(ST_COMPLETED) == 1,
            repeatEveryTime = i(ST_REPEAT_GEO) == 1,
            hour = i(ST_HOUR).coerceIn(0, 23),
            minute = i(ST_MINUTE).coerceIn(0, 59),
            daysOfWeek = s(ST_DAYS).split(",").mapNotNull { it.trim().toIntOrNull() }.toSet(),
            placeId = pid,
            radiusMeters = i(ST_RADIUS).coerceIn(50, 2000),
            geofenceMode = GeofenceTriggerMode.fromString(s(ST_GEO_MODE)),
            lastInside = i(ST_LAST_INSIDE) == 1,
            lastTriggeredMillis = l(ST_LAST_TRIGGERED),
            createdAtMillis = l(ST_CREATED)
        )
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
        put(C_GENTLE_WAKE, gentleWakeSeconds)
        put(C_TIME_REMINDER, if (timeReminderEnabled) 1 else 0)
        put(C_WEATHER_REMINDER, if (weatherReminderEnabled) 1 else 0)
        put(C_LABEL_REMINDER, if (labelReminderEnabled) 1 else 0)
        put(C_EXTRA_LOUD, if (extraLoudEnabled) 1 else 0)
        put(C_SNOOZE_ENABLED, if (snoozeEnabled) 1 else 0)
        put(C_SNOOZE_INTERVAL, snoozeIntervalMinutes)
        put(C_SNOOZE_MAX, snoozeMaxCount)
        put(C_WALLPAPER, wallpaperUri)
        put(C_WAKE_MISSIONS, wakeMissionsJson)
        put(C_WAKE_CHECK_DELAY, wakeCheckDelayMinutes)
        put(C_WAKE_CHECK_COUNTDOWN, wakeCheckCountdownSeconds)
        put(C_PREVENT_POWER_OFF, if (preventPowerOff) 1 else 0)
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
            volume = i(C_VOLUME).coerceIn(10, 100),
            vibrate = i(C_VIBRATE) == 1,
            wakeCheckEnabled = i(C_WAKE_CHECK) == 1,
            gentleWakeSeconds = i(C_GENTLE_WAKE).coerceAtLeast(0),
            timeReminderEnabled = i(C_TIME_REMINDER) == 1,
            weatherReminderEnabled = i(C_WEATHER_REMINDER) == 1,
            labelReminderEnabled = i(C_LABEL_REMINDER) == 1,
            extraLoudEnabled = i(C_EXTRA_LOUD) == 1,
            snoozeEnabled = i(C_SNOOZE_ENABLED) == 1,
            snoozeIntervalMinutes = i(C_SNOOZE_INTERVAL).coerceIn(1, 60),
            snoozeMaxCount = i(C_SNOOZE_MAX).coerceAtLeast(0),
            wallpaperUri = s(C_WALLPAPER).ifBlank { "preset:yesterday_tomorrow" },
            wakeMissionsJson = s(C_WAKE_MISSIONS),
            wakeCheckDelayMinutes = i(C_WAKE_CHECK_DELAY).coerceIn(1, 60),
            wakeCheckCountdownSeconds = i(C_WAKE_CHECK_COUNTDOWN).coerceIn(5, 60),
            preventPowerOff = i(C_PREVENT_POWER_OFF) == 1
        )
    }

    companion object {
        const val DATABASE_NAME = "timecure.db"
        const val DATABASE_VERSION = 9

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
        private const val C_GENTLE_WAKE = "gentle_wake_seconds"
        private const val C_TIME_REMINDER = "time_reminder_enabled"
        private const val C_WEATHER_REMINDER = "weather_reminder_enabled"
        private const val C_LABEL_REMINDER = "label_reminder_enabled"
        private const val C_EXTRA_LOUD = "extra_loud_enabled"
        private const val C_SNOOZE_ENABLED = "snooze_enabled"
        private const val C_SNOOZE_INTERVAL = "snooze_interval_minutes"
        private const val C_SNOOZE_MAX = "snooze_max_count"
        private const val C_WALLPAPER = "wallpaper_uri"
        private const val C_WAKE_MISSIONS = "wake_missions_json"
        private const val C_WAKE_CHECK_DELAY = "wake_check_delay_minutes"
        private const val C_WAKE_CHECK_COUNTDOWN = "wake_check_countdown_seconds"
        private const val C_PREVENT_POWER_OFF = "prevent_power_off"

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

        private const val T_PLACES = "saved_places"
        private const val PL_ID = "id"
        private const val PL_NAME = "name"
        private const val PL_TAG = "tag"
        private const val PL_ADDRESS = "address_label"
        private const val PL_LAT = "latitude"
        private const val PL_LON = "longitude"
        private const val PL_RADIUS = "radius_meters"
        private const val PL_CREATED = "created_at"

        private const val T_SMART_TASKS = "smart_tasks"
        private const val ST_ID = "id"
        private const val ST_TITLE = "title"
        private const val ST_NOTES = "notes"
        private const val ST_TRIGGER_TYPE = "trigger_type"
        private const val ST_ALERT_MODE = "alert_mode"
        private const val ST_ENABLED = "is_enabled"
        private const val ST_COMPLETED = "is_completed"
        private const val ST_REPEAT_GEO = "repeat_every_time"
        private const val ST_HOUR = "hour"
        private const val ST_MINUTE = "minute"
        private const val ST_DAYS = "days"
        private const val ST_PLACE_ID = "place_id"
        private const val ST_RADIUS = "radius_meters"
        private const val ST_GEO_MODE = "geofence_mode"
        private const val ST_LAST_INSIDE = "last_inside"
        private const val ST_LAST_TRIGGERED = "last_triggered_ms"
        private const val ST_CREATED = "created_at"
    }
}
