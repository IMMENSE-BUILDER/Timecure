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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.timecure.app.TimecureApp
import com.timecure.app.data.local.ThemeMode
import com.timecure.app.model.SavedPlace
import com.timecure.app.model.SmartTask
import com.timecure.app.model.TaskTriggerType
import com.timecure.app.tasks.TaskTriggerActions
import com.timecure.app.ui.components.BreathingAuraRing
import com.timecure.app.ui.components.rememberReducedMotion
import com.timecure.app.ui.theme.TimecureTheme
import kotlinx.coroutines.launch

class TaskAlarmActivity : ComponentActivity() {

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

                val reason = initialReason.ifBlank {
                    task?.let { TaskTriggerActions.buildTriggerReason(it, place) }
                        ?: "Task Reminder Active"
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
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
                                Icon(
                                    imageVector = if (task?.triggerType == TaskTriggerType.LOCATION)
                                        Icons.Default.LocationOn else Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = reason.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(210.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                BreathingAuraRing(
                                    primaryColor = MaterialTheme.colorScheme.primary,
                                    accentColor = MaterialTheme.colorScheme.tertiary,
                                    urgent = true,
                                    reducedMotion = reducedMotion,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    shape = CircleShape,
                                    modifier = Modifier.size(86.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (task?.triggerType == TaskTriggerType.LOCATION)
                                                Icons.Default.LocationOn else Icons.Default.Schedule,
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = task?.title ?: "Timecure Task Alert",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            val notes = task?.notes.orEmpty()
                            if (notes.isNotBlank()) {
                                Text(
                                    text = notes,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Mark Task Done", style = MaterialTheme.typography.titleMedium)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                                    modifier = Modifier.weight(1f).height(50.dp)
                                ) {
                                    Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Snooze 10m")
                                }

                                OutlinedButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        TaskTriggerActions.silenceTaskAlarm(this@TaskAlarmActivity)
                                        finish()
                                    },
                                    modifier = Modifier.weight(1f).height(50.dp)
                                ) {
                                    Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Silence")
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
