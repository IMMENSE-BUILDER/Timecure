package com.timecure.app.ui.screens.ringing

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.timecure.app.MainActivity
import com.timecure.app.TimecureApp
import com.timecure.app.alarm.AlarmActions
import com.timecure.app.alarm.AlarmScheduler
import com.timecure.app.alarm.PowerOffGuardState
import com.timecure.app.data.local.ThemeMode
import com.timecure.app.model.Alarm
import com.timecure.app.model.ChallengeCategory
import com.timecure.app.model.ChallengeDifficulty
import com.timecure.app.model.CognitiveChallenge
import com.timecure.app.model.WakeMissionConfig
import com.timecure.app.ui.components.rememberReducedMotion
import com.timecure.app.ui.screens.addalarm.AlarmWallpaperBackdrop
import com.timecure.app.ui.screens.missions.MissionPlayView
import com.timecure.app.ui.theme.TimecureTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class RingingActivity : ComponentActivity() {

    private var alarmId: Long = -1L
    private var rerings: Int = 0
    private var snoozesUsed: Int = 0
    private var wakeCheck: Boolean = false
    private var solved: Boolean = false
    private var snoozed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        rerings = intent.getIntExtra(AlarmScheduler.EXTRA_RERING_COUNT, 0)
        snoozesUsed = intent.getIntExtra(AlarmScheduler.EXTRA_SNOOZES_USED, 0)
        wakeCheck = intent.getBooleanExtra(AlarmScheduler.EXTRA_WAKE_CHECK, false)
        val isTest = alarmId == AlarmScheduler.TEST_ALARM_ID

        configureLockScreenTakeover()

        val container = (application as TimecureApp).container
        val prefs = container.preferences

        setContent {
            val darkTheme = when (prefs.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            TimecureTheme(
                darkTheme = darkTheme,
                palette = prefs.themePalette,
                dynamicColor = prefs.dynamicColor
            ) {
                val scope = rememberCoroutineScope()
                var alarm by remember { mutableStateOf<Alarm?>(null) }
                var challenge by remember { mutableStateOf<CognitiveChallenge?>(null) }
                var isLoading by remember { mutableStateOf(true) }
                var currentStep by remember { mutableIntStateOf(1) }
                var totalSteps by remember { mutableIntStateOf(if (wakeCheck || isTest) 1 else 2) }
                var isAllSolved by remember { mutableStateOf(false) }
                var elapsedSeconds by remember { mutableIntStateOf(0) }
                var wrongAttempts by remember { mutableIntStateOf(0) }

                // Elapsed wake-up stopwatch
                LaunchedEffect(isAllSolved) {
                    while (!isAllSolved) {
                        delay(1000)
                        elapsedSeconds++
                    }
                }

                fun loadNextChallenge(
                    loadedAlarm: Alarm?,
                    previousCategory: ChallengeCategory? = null
                ) {
                    scope.launch {
                        isLoading = true
                        val category = loadedAlarm?.category ?: ChallengeCategory.MIXED
                        val baseDifficulty = loadedAlarm?.difficulty ?: prefs.defaultDifficulty
                        val escalated = when {
                            rerings >= 2 -> ChallengeDifficulty.HARD
                            rerings == 1 -> baseDifficulty.nextLevel()
                            else -> baseDifficulty
                        }
                        val goal = loadedAlarm?.goal?.takeIf { it.isNotBlank() } ?: prefs.defaultGoal
                        challenge = container.challengeRepository.next(
                            category = category,
                            difficulty = escalated,
                            goal = goal,
                            previousCategory = previousCategory
                        )
                        isLoading = false
                    }
                }

                LaunchedEffect(alarmId) {
                    val loaded = if (alarmId > 0) container.alarmRepository.getById(alarmId) else null
                    alarm = loaded
                    if (!isTest && loaded?.preventPowerOff == true) {
                        PowerOffGuardState.lockForRinging(this@RingingActivity, alarmId)
                    }
                    totalSteps = when {
                        wakeCheck -> 1
                        isTest -> 2
                        else -> (loaded?.questionsRequired ?: 2).coerceIn(1, 5)
                    }
                    if (loaded?.wakeMissions.orEmpty().isEmpty() || wakeCheck) {
                        loadNextChallenge(loaded)
                    } else {
                        isLoading = false
                    }
                }

                val missions = if (!wakeCheck) alarm?.wakeMissions.orEmpty() else emptyList()

                if (missions.isNotEmpty()) {
                    MissionRunnerScreen(
                        alarm = alarm,
                        missions = missions,
                        elapsedSeconds = elapsedSeconds,
                        snoozesUsed = snoozesUsed,
                        userName = prefs.userName,
                        isTestSimulation = isTest,
                        isAllSolved = isAllSolved,
                        onAllMissionsComplete = {
                            solved = true
                            isAllSolved = true
                            AlarmActions.solved(
                                context = this@RingingActivity,
                                alarmId = alarmId,
                                seconds = elapsedSeconds,
                                attempts = missions.size,
                                rerings = rerings,
                                wakeCheck = wakeCheck
                            )
                        },
                        onSnooze = {
                            val activeAlarm = alarm ?: return@MissionRunnerScreen
                            snoozed = true
                            AlarmActions.snooze(
                                context = this@RingingActivity,
                                alarmId = alarmId,
                                delayMinutes = activeAlarm.snoozeIntervalMinutes,
                                snoozesUsed = snoozesUsed
                            )
                            finish()
                        },
                        onDone = {
                            if (isTest && !solved) {
                                solved = true
                                AlarmActions.silence(this@RingingActivity)
                            }
                            finish()
                        },
                        onLaunchFocusSprint = { goalText ->
                            val intent = Intent(this@RingingActivity, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                putExtra(MainActivity.EXTRA_OPEN_TAB, "FOCUS")
                                putExtra(MainActivity.EXTRA_START_POMODORO_GOAL, goalText)
                            }
                            startActivity(intent)
                            finish()
                        }
                    )
                } else {
                    RingingScreen(
                        alarm = alarm,
                        challenge = challenge,
                        currentStep = currentStep,
                        totalSteps = totalSteps,
                        rerings = rerings,
                        wakeCheck = wakeCheck,
                        practiceMode = false,
                        isTestSimulation = isTest,
                        isLoadingChallenge = isLoading,
                        isAllSolved = isAllSolved,
                        elapsedSeconds = elapsedSeconds,
                        wrongAttempts = wrongAttempts,
                        userName = prefs.userName,
                        snoozesUsed = snoozesUsed,
                        onSnooze = {
                            val activeAlarm = alarm ?: return@RingingScreen
                            snoozed = true
                            AlarmActions.snooze(
                                context = this@RingingActivity,
                                alarmId = alarmId,
                                delayMinutes = activeAlarm.snoozeIntervalMinutes,
                                snoozesUsed = snoozesUsed
                            )
                            finish()
                        },
                        onSubmitAnswer = { answer ->
                            val active = challenge ?: return@RingingScreen false
                            val ok = active.isAnswerCorrect(answer)
                            if (ok) {
                                if (currentStep >= totalSteps) {
                                    solved = true
                                    isAllSolved = true
                                    AlarmActions.solved(
                                        context = this@RingingActivity,
                                        alarmId = alarmId,
                                        seconds = elapsedSeconds,
                                        attempts = wrongAttempts + totalSteps,
                                        rerings = rerings,
                                        wakeCheck = wakeCheck
                                    )
                                } else {
                                    currentStep++
                                    loadNextChallenge(alarm, previousCategory = active.category)
                                }
                            } else {
                                wrongAttempts++
                            }
                            ok
                        },
                        onDone = {
                            if (isTest && !solved) {
                                solved = true
                                AlarmActions.silence(this@RingingActivity)
                            }
                            finish()
                        },
                        onLaunchFocusSprint = { goalText ->
                            val intent = Intent(this@RingingActivity, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                putExtra(MainActivity.EXTRA_OPEN_TAB, "FOCUS")
                                putExtra(MainActivity.EXTRA_START_POMODORO_GOAL, goalText)
                            }
                            startActivity(intent)
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun configureLockScreenTakeover() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE -> true
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!solved && !snoozed && !isChangingConfigurations) {
            if (alarmId == AlarmScheduler.TEST_ALARM_ID) {
                AlarmActions.silence(this)
            } else {
                AlarmActions.abandoned(this, alarmId, rerings)
            }
        }
    }
}

@Composable
private fun MissionRunnerScreen(
    alarm: Alarm?,
    missions: List<WakeMissionConfig>,
    elapsedSeconds: Int,
    snoozesUsed: Int,
    userName: String,
    isTestSimulation: Boolean,
    isAllSolved: Boolean,
    onAllMissionsComplete: () -> Unit,
    onSnooze: () -> Unit,
    onDone: () -> Unit,
    onLaunchFocusSprint: (String) -> Unit
) {
    BackHandler(enabled = !isAllSolved && !isTestSimulation) { /* Enforce mission completion */ }

    val haptic = LocalHapticFeedback.current
    val reducedMotion = rememberReducedMotion()
    var missionIndex by remember { mutableIntStateOf(0) }

    val goal = alarm?.goal?.takeIf { it.isNotBlank() } ?: "Win the morning"
    val elapsedText = String.format(Locale.getDefault(), "%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        AlarmWallpaperBackdrop(
            wallpaperUri = alarm?.wallpaperUri.orEmpty(),
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .displayCutoutPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = CircleShape) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(elapsedText, style = MaterialTheme.typography.labelLarge)
                    }
                }
                if (isTestSimulation) {
                    IconButton(onClick = onDone) { Icon(Icons.Default.Close, contentDescription = "Close") }
                } else {
                    Text(
                        text = if (isAllSolved) "Complete" else "Mission ${missionIndex + 1} of ${missions.size}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (missions.size > 1) {
                val progress = if (isAllSolved) 1f else missionIndex.toFloat() / missions.size.coerceAtLeast(1)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp)
                )
            }

            if (alarm != null) {
                Text(
                    text = "${alarm.formattedTime} ${alarm.amPm} • $goal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            if (isAllSolved) {
                VictoryCard(
                    goal = goal,
                    userName = userName,
                    elapsedSeconds = elapsedSeconds,
                    wrongAttempts = 0,
                    wakeCheckArmed = alarm?.wakeCheckEnabled == true,
                    practiceMode = false,
                    reducedMotion = reducedMotion,
                    onDone = onDone,
                    onLaunchFocusSprint = { onLaunchFocusSprint(goal) }
                )
            } else {
                val currentMission = missions.getOrNull(missionIndex) ?: missions.last()
                androidx.compose.runtime.key(missionIndex, currentMission.type) {
                    MissionPlayView(
                        config = currentMission,
                        goal = goal,
                        isPreview = false,
                        onMissionComplete = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (missionIndex + 1 >= missions.size) {
                                onAllMissionsComplete()
                            } else {
                                missionIndex++
                            }
                        }
                    )
                }

                val canSnooze = !isTestSimulation &&
                    alarm?.snoozeEnabled == true &&
                    (alarm.snoozeMaxCount == 0 || snoozesUsed < alarm.snoozeMaxCount)
                if (canSnooze) {
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSnooze()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (alarm.snoozeMaxCount <= 0) "Snooze ${alarm.snoozeIntervalMinutes}m"
                            else "Snooze ${alarm.snoozeIntervalMinutes}m (${(alarm.snoozeMaxCount - snoozesUsed).coerceAtLeast(1)} left)"
                        )
                    }
                }
            }
        }
    }
}
