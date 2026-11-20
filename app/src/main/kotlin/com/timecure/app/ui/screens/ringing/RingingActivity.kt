package com.timecure.app.ui.screens.ringing

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.timecure.app.MainActivity
import com.timecure.app.TimecureApp
import com.timecure.app.alarm.AlarmActions
import com.timecure.app.alarm.AlarmScheduler
import com.timecure.app.data.local.ThemeMode
import com.timecure.app.model.Alarm
import com.timecure.app.model.ChallengeCategory
import com.timecure.app.model.ChallengeDifficulty
import com.timecure.app.model.CognitiveChallenge
import com.timecure.app.ui.theme.TimecureTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RingingActivity : ComponentActivity() {

    private var alarmId: Long = -1L
    private var rerings: Int = 0
    private var wakeCheck: Boolean = false
    private var solved: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        rerings = intent.getIntExtra(AlarmScheduler.EXTRA_RERING_COUNT, 0)
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
                    totalSteps = when {
                        wakeCheck -> 1
                        isTest -> 2
                        else -> (loaded?.questionsRequired ?: 2).coerceIn(1, 5)
                    }
                    loadNextChallenge(loaded)
                }

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
        if (!solved && !isChangingConfigurations) {
            if (alarmId == AlarmScheduler.TEST_ALARM_ID) {
                AlarmActions.silence(this)
            } else {
                AlarmActions.abandoned(this, alarmId, rerings)
            }
        }
    }
}
