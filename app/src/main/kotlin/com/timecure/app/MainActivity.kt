package com.timecure.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.timecure.app.data.local.ThemeMode
import com.timecure.app.model.Alarm
import com.timecure.app.model.ChallengeCategory
import com.timecure.app.model.ChallengeDifficulty
import com.timecure.app.model.CognitiveChallenge
import com.timecure.app.model.WakeEvent
import com.timecure.app.model.WakeStats
import com.timecure.app.ui.components.AnimatedNavShell
import com.timecure.app.ui.components.TopDestination
import com.timecure.app.ui.screens.addalarm.AddEditAlarmScreen
import com.timecure.app.ui.screens.alarms.AlarmsScreen
import com.timecure.app.ui.screens.assistant.AiAssistantSheet
import com.timecure.app.ui.screens.insights.InsightsScreen
import com.timecure.app.ui.screens.permissions.PermissionsScreen
import com.timecure.app.ui.screens.pomodoro.PomodoroScreen
import com.timecure.app.ui.screens.ringing.RingingScreen
import com.timecure.app.ui.screens.settings.SettingsScreen
import com.timecure.app.ui.theme.TimecureTheme
import com.timecure.app.util.Permissions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private sealed interface OverlayRoute {
    data object None : OverlayRoute
    data class AddEdit(val alarm: Alarm?) : OverlayRoute
    data object Permissions : OverlayRoute
    data class PracticeDojo(
        val category: ChallengeCategory,
        val difficulty: ChallengeDifficulty
    ) : OverlayRoute
}

class MainActivity : ComponentActivity() {

    private val externalTabRequest = MutableStateFlow<TopDestination?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as TimecureApp).container
        val prefs = container.preferences
        handleIncomingIntent(intent, container)

        setContent {
            var themeMode by remember { mutableStateOf(prefs.themeMode) }
            var themePalette by remember { mutableStateOf(prefs.themePalette) }
            var dynamicColor by remember { mutableStateOf(prefs.dynamicColor) }
            var userName by remember { mutableStateOf(prefs.userName) }

            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            TimecureTheme(
                darkTheme = darkTheme,
                palette = themePalette,
                dynamicColor = dynamicColor
            ) {
                val alarms by container.alarmRepository.alarmsFlow.collectAsState(initial = emptyList())
                val aiTasks by container.taskPlanRepository.tasksFlow.collectAsState(initial = emptyList())
                val pomodoroState by container.pomodoroEngine.state.collectAsState()
                val pomodoroStats by container.pomodoroRepository.statsFlow.collectAsState()
                val pendingTab by externalTabRequest.collectAsState()
                val scope = rememberCoroutineScope()

                var destination by remember { mutableStateOf(TopDestination.ALARMS) }
                var overlay by remember {
                    val initialMissing = Permissions.check(this@MainActivity).missingCriticalCount
                    mutableStateOf<OverlayRoute>(
                        if (!prefs.onboardingCompleted && initialMissing > 0) OverlayRoute.Permissions
                        else OverlayRoute.None
                    )
                }
                var missingPerms by remember {
                    mutableIntStateOf(Permissions.check(this@MainActivity).missingCriticalCount)
                }
                var stats by remember { mutableStateOf(WakeStats()) }
                var recentEvents by remember { mutableStateOf<List<WakeEvent>>(emptyList()) }
                var showAiAssistant by remember { mutableStateOf(false) }

                LaunchedEffect(pendingTab) {
                    if (pendingTab != null) {
                        destination = pendingTab!!
                        overlay = OverlayRoute.None
                        externalTabRequest.value = null
                    }
                }

                fun reloadStats() {
                    scope.launch {
                        stats = container.alarmRepository.stats()
                        recentEvents = container.alarmRepository.recentEvents(20)
                        container.pomodoroRepository.refresh()
                    }
                }

                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    missingPerms = Permissions.check(this@MainActivity).missingCriticalCount
                    reloadStats()
                }

                LaunchedEffect(Unit) { reloadStats() }

                val screenWidthDp = LocalConfiguration.current.screenWidthDp
                val useRail = screenWidthDp >= 600

                // Keep AnimatedNavShell permanently mounted beneath overlays so opening/closing
                // Add/Edit Alarm, Permissions, or Practice Dojo never destroys the warm tab cache.
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedNavShell(
                        useRail = useRail,
                        destination = destination,
                        pomodoroRunning = pomodoroState.isRunning,
                        onSelect = {
                            destination = it
                            if (it == TopDestination.INSIGHTS || it == TopDestination.FOCUS) {
                                reloadStats()
                            }
                        }
                    ) { tab, contentModifier ->
                        when (tab) {
                            TopDestination.ALARMS -> AlarmsScreen(
                                alarms = alarms,
                                stats = stats,
                                userName = userName,
                                missingPermissions = missingPerms,
                                onAddAlarm = { overlay = OverlayRoute.AddEdit(null) },
                                onEditAlarm = { overlay = OverlayRoute.AddEdit(it) },
                                onToggleAlarm = { alarm, enabled ->
                                    scope.launch {
                                        val updated = alarm.copy(isEnabled = enabled)
                                        container.alarmRepository.update(updated)
                                        if (enabled) container.alarmScheduler.schedule(updated)
                                        else container.alarmScheduler.cancel(alarm.id)
                                    }
                                },
                                onDeleteAlarm = { alarm ->
                                    scope.launch {
                                        container.alarmScheduler.cancel(alarm.id)
                                        container.alarmRepository.delete(alarm.id)
                                    }
                                },
                                onUndoDelete = { deleted ->
                                    scope.launch {
                                        val restoredId = container.alarmRepository.insert(deleted.copy(id = 0))
                                        if (deleted.isEnabled) {
                                            container.alarmScheduler.schedule(deleted.copy(id = restoredId))
                                        }
                                    }
                                },
                                onFixPermissions = { overlay = OverlayRoute.Permissions },
                                onOpenAiAssistant = { showAiAssistant = true },
                                onQuickPresetAlarm = { presetGoal, presetLabel, presetPrompt ->
                                    overlay = OverlayRoute.AddEdit(
                                        Alarm(
                                            id = 0L,
                                            hour = 7,
                                            minute = 0,
                                            daysOfWeek = setOf(
                                                java.util.Calendar.MONDAY,
                                                java.util.Calendar.TUESDAY,
                                                java.util.Calendar.WEDNESDAY,
                                                java.util.Calendar.THURSDAY,
                                                java.util.Calendar.FRIDAY
                                            ),
                                            label = presetLabel,
                                            goal = presetGoal,
                                            wakePrompt = presetPrompt,
                                            difficulty = prefs.defaultDifficulty,
                                            wakeCheckEnabled = true
                                        )
                                    )
                                },
                                modifier = contentModifier
                            )

                            TopDestination.FOCUS -> PomodoroScreen(
                                state = pomodoroState,
                                stats = pomodoroStats,
                                alarms = alarms,
                                onToggleRun = { container.pomodoroEngine.toggleRun() },
                                onReset = { container.pomodoroEngine.reset() },
                                onSkipPhase = { container.pomodoroEngine.skipPhase() },
                                onSelectPreset = { container.pomodoroEngine.selectPreset(it) },
                                onSelectPhase = { container.pomodoroEngine.selectPhase(it) },
                                onUpdateGoal = { container.pomodoroEngine.setGoal(it) },
                                modifier = contentModifier
                            )

                            TopDestination.INSIGHTS -> InsightsScreen(
                                stats = stats,
                                pomodoroStats = pomodoroStats,
                                aiTasks = aiTasks,
                                recentEvents = recentEvents,
                                onToggleTaskDone = { task, done ->
                                    scope.launch { container.taskPlanRepository.toggleCompleted(task.id, done) }
                                },
                                onClearTasks = {
                                    scope.launch { container.taskPlanRepository.clearAll() }
                                },
                                onCreateAlarmFromTask = { task ->
                                    val h = task.suggestedAlarmHour ?: 7
                                    val m = task.suggestedAlarmMinute ?: 0
                                    overlay = OverlayRoute.AddEdit(
                                        Alarm(
                                            id = 0L,
                                            hour = h,
                                            minute = m,
                                            daysOfWeek = setOf(
                                                java.util.Calendar.MONDAY,
                                                java.util.Calendar.TUESDAY,
                                                java.util.Calendar.WEDNESDAY,
                                                java.util.Calendar.THURSDAY,
                                                java.util.Calendar.FRIDAY
                                            ),
                                            label = task.planTitle,
                                            goal = task.taskText,
                                            wakePrompt = "${task.taskText} (${task.technique})",
                                            difficulty = prefs.defaultDifficulty,
                                            wakeCheckEnabled = true
                                        )
                                    )
                                },
                                onStartPractice = { cat, diff ->
                                    overlay = OverlayRoute.PracticeDojo(cat, diff)
                                },
                                modifier = contentModifier
                            )

                            TopDestination.SETTINGS -> SettingsScreen(
                                themeMode = themeMode,
                                themePalette = themePalette,
                                dynamicColor = dynamicColor,
                                missingPermissions = missingPerms,
                                onThemeModeChange = {
                                    themeMode = it
                                    prefs.themeMode = it
                                },
                                onThemePaletteChange = {
                                    themePalette = it
                                    prefs.themePalette = it
                                },
                                onDynamicColorChange = {
                                    dynamicColor = it
                                    prefs.dynamicColor = it
                                },
                                onUserNameChange = { userName = it },
                                onOpenPermissions = { overlay = OverlayRoute.Permissions },
                                modifier = contentModifier
                            )
                        }
                    }

                    AnimatedContent(
                        targetState = overlay,
                        transitionSpec = {
                            (fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.97f)) togetherWith
                                fadeOut(tween(110))
                        },
                        label = "root_overlay"
                    ) { activeOverlay ->
                        when (activeOverlay) {
                            OverlayRoute.None -> Unit

                            is OverlayRoute.Permissions -> {
                                PermissionsScreen(
                                    onDone = {
                                        prefs.onboardingCompleted = true
                                        missingPerms = Permissions.check(this@MainActivity).missingCriticalCount
                                        overlay = OverlayRoute.None
                                    }
                                )
                            }

                            is OverlayRoute.AddEdit -> {
                                AddEditAlarmScreen(
                                    initialAlarm = activeOverlay.alarm,
                                    defaultGoal = prefs.defaultGoal,
                                    defaultDifficulty = prefs.defaultDifficulty,
                                    onSave = { saved ->
                                        scope.launch {
                                            if (saved.id == 0L) {
                                                val newId = container.alarmRepository.insert(saved)
                                                container.alarmScheduler.schedule(saved.copy(id = newId))
                                            } else {
                                                container.alarmRepository.update(saved)
                                                container.alarmScheduler.schedule(saved)
                                            }
                                            overlay = OverlayRoute.None
                                        }
                                    },
                                    onDelete = activeOverlay.alarm?.let { existing ->
                                        {
                                            scope.launch {
                                                container.alarmScheduler.cancel(existing.id)
                                                container.alarmRepository.delete(existing.id)
                                                overlay = OverlayRoute.None
                                            }
                                        }
                                    },
                                    onCancel = { overlay = OverlayRoute.None }
                                )
                            }

                            is OverlayRoute.PracticeDojo -> {
                                PracticeDojoHost(
                                    container = container,
                                    category = activeOverlay.category,
                                    difficulty = activeOverlay.difficulty,
                                    userName = userName,
                                    defaultGoal = prefs.defaultGoal,
                                    onExit = { overlay = OverlayRoute.None }
                                )
                            }
                        }
                    }
                }

                if (showAiAssistant) {
                    AiAssistantSheet(
                        agent = container.aiAgent,
                        modelName = prefs.modelName,
                        onDismiss = { showAiAssistant = false },
                        onOpenFocusTab = { destination = TopDestination.FOCUS },
                        onThemeChanged = {
                            themeMode = it
                            prefs.themeMode = it
                        },
                        onPaletteChanged = {
                            themePalette = it
                            prefs.themePalette = it
                        },
                        onUserNameChanged = { userName = it },
                        onCreateAlarmFromTask = { task ->
                            showAiAssistant = false
                            val h = task.suggestedAlarmHour ?: 7
                            val m = task.suggestedAlarmMinute ?: 0
                            scope.launch {
                                val alarm = Alarm(
                                    hour = h,
                                    minute = m,
                                    daysOfWeek = setOf(
                                        java.util.Calendar.MONDAY,
                                        java.util.Calendar.TUESDAY,
                                        java.util.Calendar.WEDNESDAY,
                                        java.util.Calendar.THURSDAY,
                                        java.util.Calendar.FRIDAY
                                    ),
                                    label = task.planTitle,
                                    goal = task.taskText,
                                    wakePrompt = "${task.taskText} — ${task.technique}",
                                    difficulty = prefs.defaultDifficulty,
                                    wakeCheckEnabled = true
                                )
                                val id = container.alarmRepository.insert(alarm)
                                container.alarmScheduler.schedule(alarm.copy(id = id))
                            }
                        },
                        onLaunchPomodoroForTask = { taskTitle ->
                            container.pomodoroEngine.launchSprintForGoal(taskTitle)
                            destination = TopDestination.FOCUS
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent, (application as TimecureApp).container)
    }

    private fun handleIncomingIntent(intent: Intent?, container: AppContainer) {
        if (intent == null) return
        val tabName = intent.getStringExtra(EXTRA_OPEN_TAB)
        if (tabName != null) {
            TopDestination.entries.firstOrNull { it.name == tabName }?.let {
                externalTabRequest.value = it
            }
        }
        val sprintGoal = intent.getStringExtra(EXTRA_START_POMODORO_GOAL)
        if (!sprintGoal.isNullOrBlank()) {
            container.pomodoroEngine.launchSprintForGoal(sprintGoal)
            externalTabRequest.value = TopDestination.FOCUS
            intent.removeExtra(EXTRA_START_POMODORO_GOAL)
        }
    }

    companion object {
        const val EXTRA_OPEN_TAB = "extra_open_tab"
        const val EXTRA_START_POMODORO_GOAL = "extra_start_pomodoro_goal"
    }
}

@Composable
private fun PracticeDojoHost(
    container: AppContainer,
    category: ChallengeCategory,
    difficulty: ChallengeDifficulty,
    userName: String,
    defaultGoal: String,
    onExit: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val totalSteps = 3
    var currentStep by remember { mutableIntStateOf(1) }
    var challenge by remember { mutableStateOf<CognitiveChallenge?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isAllSolved by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var wrongAttempts by remember { mutableIntStateOf(0) }

    LaunchedEffect(isAllSolved) {
        while (!isAllSolved) {
            delay(1000)
            elapsedSeconds++
        }
    }

    fun loadNext(previous: ChallengeCategory? = null) {
        scope.launch {
            isLoading = true
            challenge = container.challengeRepository.next(
                category = category,
                difficulty = difficulty,
                goal = defaultGoal,
                previousCategory = previous
            )
            isLoading = false
        }
    }

    LaunchedEffect(category, difficulty) { loadNext() }

    RingingScreen(
        alarm = Alarm(hour = 7, minute = 0, goal = defaultGoal),
        challenge = challenge,
        currentStep = currentStep,
        totalSteps = totalSteps,
        rerings = 0,
        wakeCheck = false,
        practiceMode = true,
        isTestSimulation = false,
        isLoadingChallenge = isLoading,
        isAllSolved = isAllSolved,
        elapsedSeconds = elapsedSeconds,
        wrongAttempts = wrongAttempts,
        userName = userName,
        onSubmitAnswer = { answer ->
            val active = challenge ?: return@RingingScreen false
            val ok = active.isAnswerCorrect(answer)
            if (ok) {
                if (currentStep >= totalSteps) {
                    isAllSolved = true
                } else {
                    currentStep++
                    loadNext(previous = active.category)
                }
            } else {
                wrongAttempts++
            }
            ok
        },
        onDone = onExit
    )
}
