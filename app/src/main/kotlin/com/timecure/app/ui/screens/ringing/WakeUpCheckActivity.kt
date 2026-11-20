package com.timecure.app.ui.screens.ringing

import android.app.KeyguardManager
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.timecure.app.TimecureApp
import com.timecure.app.alarm.AlarmActions
import com.timecure.app.alarm.AlarmScheduler
import com.timecure.app.data.local.ThemeMode
import com.timecure.app.model.Alarm
import com.timecure.app.ui.components.AnimatedCheckmarkBadge
import com.timecure.app.ui.components.rememberReducedMotion
import com.timecure.app.ui.theme.TimecureTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pop-up dialog / lock-screen overlay triggered N minutes after dismissing an alarm:
 * counts down "3, 2, 1" over a customizable window (default 15s), confirms you are awake,
 * and silences all remaining alarms for that scheduled task/goal (e.g. "Get up for college").
 * If the timer expires without confirmation, re-rings the alarm automatically.
 */
class WakeUpCheckActivity : ComponentActivity() {

    private var handled = false
    private var alarmId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockScreen()

        alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        NotificationManagerCompat.from(this).cancel(AlarmActions.WAKE_CHECK_NOTIF_ID)

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
                BackHandler(enabled = !handled) { /* Must confirm Awake or timeout */ }

                val haptic = LocalHapticFeedback.current
                val reducedMotion = rememberReducedMotion()
                val scope = rememberCoroutineScope()

                var alarm by remember { mutableStateOf<Alarm?>(null) }
                var totalSeconds by remember { mutableIntStateOf(15) }
                var remaining by remember { mutableIntStateOf(15) }
                var confirmedCount by remember { mutableIntStateOf(-1) }

                val toneGen = remember {
                    runCatching { ToneGenerator(AudioManager.STREAM_ALARM, 75) }.getOrNull()
                }
                DisposableEffect(Unit) {
                    onDispose { runCatching { toneGen?.release() } }
                }

                LaunchedEffect(alarmId) {
                    val loaded = if (alarmId > 0L) container.alarmRepository.getById(alarmId) else null
                    alarm = loaded
                    val windowSec = (loaded?.wakeCheckCountdownSeconds ?: 15).coerceIn(5, 60)
                    totalSeconds = windowSec
                    remaining = windowSec
                    runCatching { toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP2, 240) }
                }

                // Second-by-second countdown ending in 3, 2, 1 -> auto re-ring if ignored
                LaunchedEffect(totalSeconds, confirmedCount) {
                    if (confirmedCount >= 0) return@LaunchedEffect
                    while (remaining > 0 && !handled) {
                        delay(1000L)
                        if (handled) break
                        remaining--
                        if (remaining in 1..3) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            runCatching { toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 160) }
                        }
                    }
                    if (!handled && remaining <= 0) {
                        handled = true
                        AlarmActions.ring(
                            context = this@WakeUpCheckActivity,
                            alarmId = alarmId,
                            rerings = 1,
                            wakeCheck = false
                        )
                        finish()
                    }
                }

                val goalText = alarm?.goal?.takeIf { it.isNotBlank() } ?: prefs.defaultGoal

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (confirmedCount >= 0) {
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AnimatedCheckmarkBadge(reducedMotion = reducedMotion)
                                Text(
                                    text = "Confirmed awake!",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (confirmedCount > 0)
                                        "Silenced $confirmedCount related alarm(s) for \"$goalText\" and unlocked power controls."
                                    else
                                        "\"$goalText\" cleared — power controls unlocked. Have a great morning!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        val fraction = (remaining.toFloat() / totalSeconds.coerceAtLeast(1)).coerceIn(0f, 1f)
                        val animatedSweep by animateFloatAsState(
                            targetValue = 360f * fraction,
                            animationSpec = tween(260),
                            label = "wake_check_sweep"
                        )
                        val primary = MaterialTheme.colorScheme.primary
                        val urgentColor = if (remaining <= 3) MaterialTheme.colorScheme.error else primary
                        val track = MaterialTheme.colorScheme.surfaceContainerHighest

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                shape = CircleShape
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "WAKE-UP CHECK • $goalText",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(220.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val r = size.minDimension / 2f - 12.dp.toPx()
                                        val stroke = 12.dp.toPx()
                                        val topLeft = Offset(size.width / 2f - r, size.height / 2f - r)
                                        drawCircle(
                                            color = track,
                                            radius = r,
                                            style = Stroke(width = stroke)
                                        )
                                        drawArc(
                                            color = urgentColor,
                                            startAngle = -90f,
                                            sweepAngle = animatedSweep,
                                            useCenter = false,
                                            topLeft = topLeft,
                                            size = Size(r * 2f, r * 2f),
                                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                                        )
                                    }
                                    AnimatedContent(targetState = remaining, label = "check_digit") { sec ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "$sec",
                                                style = MaterialTheme.typography.displayLarge.copy(
                                                    fontSize = 68.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                ),
                                                color = urgentColor
                                            )
                                            Text(
                                                text = if (sec <= 3) "Almost ringing!" else "seconds left",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Are you actually awake?",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Tap Awake before 3, 2, 1 hits zero to silence all remaining alarms for \"$goalText\".",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (handled) return@Button
                                        handled = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        scope.launch {
                                            val silenced = AlarmActions.confirmAwakeAndSilenceGoalAlarms(
                                                this@WakeUpCheckActivity,
                                                alarmId
                                            )
                                            confirmedCount = silenced
                                            delay(1_800L)
                                            finish()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(58.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Awake — I'm Ready", style = MaterialTheme.typography.titleMedium)
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (handled) return@OutlinedButton
                                        handled = true
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        AlarmActions.ring(
                                            context = this@WakeUpCheckActivity,
                                            alarmId = alarmId,
                                            rerings = 1,
                                            wakeCheck = false
                                        )
                                        finish()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) {
                                    Icon(Icons.Default.AlarmOn, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Still sleepy — ring again")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!handled && !isChangingConfigurations && alarmId > 0L) {
            // Closing popup without tapping Awake re-rings immediately
            AlarmActions.ring(this, alarmId, rerings = 1, wakeCheck = false)
        }
    }

    private fun configureLockScreen() {
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
}
