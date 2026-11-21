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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@OptIn(ExperimentalLayoutApi::class)
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
    val imeVisible = WindowInsets.isImeVisible
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
        RingingBackdropAtmosphere(
            primaryGlow = MaterialTheme.colorScheme.primary,
            secondaryGlow = MaterialTheme.colorScheme.tertiary
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .displayCutoutPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(elapsedText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (isTestSimulation) {
                    Surface(
                        onClick = onDone,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Stop test", modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Stop Test", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = if (isAllSolved) "Complete" else "Mission ${missionIndex + 1} of ${missions.size}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            StepPillBar(
                currentStep = missionIndex + 1,
                totalSteps = missions.size,
                isAllSolved = isAllSolved
            )

            AnimatedVisibility(
                visible = !imeVisible || isAllSolved,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (alarm != null) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = alarm.formattedTime,
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontSize = 46.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-1).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = alarm.amPm,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 4.dp, bottom = 7.dp)
                            )
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Flag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = goal,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = isAllSolved to missionIndex,
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInHorizontally { it / 12 }) togetherWith
                        (fadeOut(tween(150)) + slideOutHorizontally { -it / 12 })
                },
                label = "mission_stage_anim"
            ) { (solved, idx) ->
                if (solved) {
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
                    val currentMission = missions.getOrNull(idx) ?: missions.last()
                    androidx.compose.runtime.key(idx, currentMission.type) {
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
                }
            }

            if (!isAllSolved) {
                val canSnooze = !isTestSimulation &&
                    alarm?.snoozeEnabled == true &&
                    (alarm.snoozeMaxCount == 0 || snoozesUsed < alarm.snoozeMaxCount)
                if (canSnooze) {
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSnooze()
                        },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (alarm.snoozeMaxCount <= 0) "Snooze ${alarm.snoozeIntervalMinutes} min"
                            else "Snooze ${alarm.snoozeIntervalMinutes} min (${(alarm.snoozeMaxCount - snoozesUsed).coerceAtLeast(1)} left)",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
