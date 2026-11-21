package com.timecure.app.ui.screens.tasks

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.timecure.app.TimecureApp
import com.timecure.app.data.local.ThemeMode
import com.timecure.app.model.SavedPlace
import com.timecure.app.model.SmartTask
import com.timecure.app.model.TaskTriggerType
import com.timecure.app.tasks.TaskTriggerActions
import com.timecure.app.ui.components.BreathingAuraRing
import com.timecure.app.ui.components.rememberReducedMotion
import com.timecure.app.ui.screens.ringing.RingingBackdropAtmosphere
import com.timecure.app.ui.theme.TimecureTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAlarmActivity : ComponentActivity() {

    @OptIn(ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockScreen()

        val taskId = intent.getLongExtra(TaskTriggerActions.EXTRA_TASK_ID, -1L)
        val initialReason = intent.getStringExtra(TaskTriggerActions.EXTRA_TRIGGER_REASON).orEmpty()
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
                BackHandler(enabled = true) { /* Require explicit user action to silence task alarm */ }

                val haptic = LocalHapticFeedback.current
                val reducedMotion = rememberReducedMotion()
                val scope = rememberCoroutineScope()

                var task by remember { mutableStateOf<SmartTask?>(null) }
                var place by remember { mutableStateOf<SavedPlace?>(null) }

                LaunchedEffect(taskId) {
                    if (taskId > 0L) {
                        val loaded = container.smartTaskRepository.getTaskById(taskId)
                        task = loaded
                        if (loaded?.placeId != null) {
                            place = container.smartTaskRepository.getPlaceById(loaded.placeId)
                        }
                    }
                }

                val isLocation = task?.triggerType == TaskTriggerType.LOCATION
                val glowColor = if (isLocation) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                val reason = initialReason.ifBlank {
                    task?.let { TaskTriggerActions.buildTriggerReason(it, place) }
                        ?: "Task Reminder Active"
                }
                val nowTime = remember {
                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    RingingBackdropAtmosphere(
                        primaryGlow = glowColor,
                        secondaryGlow = if (isLocation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .displayCutoutPadding()
                            .navigationBarsPadding()
                            .padding(horizontal = 22.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Status Strip: Clock + Trigger Capsule
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = nowTime,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.92f),
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                shape = CircleShape,
                                border = BorderStroke(1.dp, glowColor.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isLocation) Icons.Default.Radar else Icons.Default.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (isLocation) "LOCATION ARRIVAL" else "SCHEDULED TASK",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Centerpiece Visual Radar / Aura + Frosted Task Focus Card
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier.size(204.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLocation) {
                                    TaskLocationSonarRing(
                                        accentColor = glowColor,
                                        reducedMotion = reducedMotion,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    BreathingAuraRing(
                                        primaryColor = MaterialTheme.colorScheme.primary,
                                        accentColor = MaterialTheme.colorScheme.tertiary,
                                        urgent = true,
                                        reducedMotion = reducedMotion,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Surface(
                                    color = glowColor.copy(alpha = 0.18f),
                                    contentColor = glowColor,
                                    shape = CircleShape,
                                    border = BorderStroke(1.5.dp, glowColor.copy(alpha = 0.5f)),
                                    modifier = Modifier.size(88.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isLocation) Icons.Default.LocationOn else Icons.Default.Schedule,
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                                shape = RoundedCornerShape(28.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                                tonalElevation = 4.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(22.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = reason,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = glowColor,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = task?.title ?: "Timecure Task Alert",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )

                                    val notes = task?.notes.orEmpty()
                                    if (notes.isNotBlank()) {
                                        Text(
                                            text = notes,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // Clean inline metadata pills
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        task?.let { t ->
                                            if (t.triggerType == TaskTriggerType.LOCATION) {
                                                place?.let { p ->
                                                    TaskMetaPill("${p.tag.displayName} • ${p.name}")
                                                }
                                                TaskMetaPill("${t.geofenceMode.displayName} ${t.radiusMeters}m")
                                            } else {
                                                TaskMetaPill(t.formattedTime())
                                                TaskMetaPill(t.daysSummary)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Frosted Bottom Action Dock
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                            shape = RoundedCornerShape(28.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                            tonalElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        TaskTriggerActions.silenceTaskAlarm(this@TaskAlarmActivity)
                                        if (taskId > 0L) {
                                            scope.launch {
                                                container.smartTaskRepository.setTaskCompleted(taskId, true)
                                                container.taskScheduler.syncAll(
                                                    container.smartTaskRepository.getAllTasks()
                                                )
                                                finish()
                                            }
                                        } else {
                                            finish()
                                        }
                                    },
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.fillMaxWidth().height(58.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Mark Task Done",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            TaskTriggerActions.silenceTaskAlarm(this@TaskAlarmActivity)
                                            if (taskId > 0L) {
                                                container.taskScheduler.scheduleSnooze(taskId, delayMinutes = 10)
                                            }
                                            finish()
                                        },
                                        shape = RoundedCornerShape(18.dp),
                                        modifier = Modifier.weight(1f).height(52.dp)
                                    ) {
                                        Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Snooze 10m", fontWeight = FontWeight.SemiBold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            TaskTriggerActions.silenceTaskAlarm(this@TaskAlarmActivity)
                                            finish()
                                        },
                                        shape = RoundedCornerShape(18.dp),
                                        modifier = Modifier.weight(1f).height(52.dp)
                                    ) {
                                        Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Silence", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
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

@Composable
private fun TaskMetaPill(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun TaskLocationSonarRing(
    accentColor: Color,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val phaseState: State<Float> = if (reducedMotion) {
        rememberUpdatedState(0.45f)
    } else {
        rememberInfiniteTransition(label = "sonar").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Restart),
            label = "sonar_phase"
        )
    }

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxR = size.minDimension / 2f
        val p = phaseState.value
        for (wave in 0..2) {
            val t = (p + wave / 3f) % 1f
            val r = maxR * (0.42f + 0.56f * t)
            val alpha = (1f - t) * 0.48f
            drawCircle(
                color = accentColor.copy(alpha = alpha),
                radius = r,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )
        }
    }
}
