package com.timecure.app

import android.content.Context
import com.timecure.app.alarm.AlarmScheduler
import com.timecure.app.alarm.SoundManager
import com.timecure.app.data.local.PreferencesManager
import com.timecure.app.data.local.TimecureDatabaseHelper
import com.timecure.app.data.remote.TimecureAiAgent
import com.timecure.app.data.remote.VercelAiClient
import com.timecure.app.data.repository.AlarmRepository
import com.timecure.app.data.repository.ChallengeRepository
import com.timecure.app.data.repository.PomodoroRepository
import com.timecure.app.data.repository.TaskPlanRepository
import com.timecure.app.pomodoro.PomodoroEngine

/** Hand-rolled DI: single module, zero annotation processing overhead. */
class AppContainer(context: Context) {
    private val app = context.applicationContext

    val preferences by lazy { PreferencesManager(app) }
    private val database by lazy { TimecureDatabaseHelper(app) }
    val alarmRepository by lazy { AlarmRepository(database) }
    val pomodoroRepository by lazy { PomodoroRepository(database) }
    val taskPlanRepository by lazy { TaskPlanRepository(database) }
    val aiClient by lazy { VercelAiClient(preferences) }
    val challengeRepository by lazy { ChallengeRepository(aiClient, preferences) }
    val alarmScheduler by lazy { AlarmScheduler(app) }
    val soundManager by lazy { SoundManager(app) }
    val pomodoroEngine by lazy { PomodoroEngine(app, pomodoroRepository, preferences) }
    val aiAgent by lazy {
        TimecureAiAgent(app, preferences, alarmRepository, alarmScheduler, pomodoroEngine, taskPlanRepository)
    }
}
