package com.timecure.app.ui.screens.alarms

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.timecure.app.model.Alarm
import com.timecure.app.model.WakeStats
import com.timecure.app.ui.components.TimecureAiLogoButton
import com.timecure.app.ui.components.rememberReducedMotion
import com.timecure.app.ui.components.safeTopBarWindowInsets
import com.timecure.app.util.TimeFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(
    alarms: List<Alarm>,
    stats: WakeStats,
    userName: String,
    missingPermissions: Int,
    onAddAlarm: () -> Unit,
    onEditAlarm: (Alarm) -> Unit,
    onToggleAlarm: (Alarm, Boolean) -> Unit,
    onDeleteAlarm: (Alarm) -> Unit,
    onUndoDelete: (Alarm) -> Unit,
    onFixPermissions: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onQuickPresetAlarm: (goal: String, label: String, wakePrompt: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val reducedMotion = rememberReducedMotion()

    // Live-ticking timestamp so "Next alarm in Xh Ym" never goes stale while open
    val nowTick = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            nowTick.longValue = System.currentTimeMillis()
        }
    }

    val greeting = remember(userName, nowTick.longValue / 3_600_000L) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val prefix = when (hour) {
            in 4..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Late night"
        }
        if (userName.isNotBlank()) "$prefix, $userName" else "Timecure"
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(greeting) },
                windowInsets = safeTopBarWindowInsets(),
                actions = {
                    TimecureAiLogoButton(
                        onClick = onOpenAiAssistant,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddAlarm,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add alarm") }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (missingPermissions > 0) PermissionBanner(missingPermissions, onFixPermissions)

            SummaryHeader(alarms, stats, nowTick.longValue, reducedMotion)

            // AI Command Bar + Quick Personal Routine Prompts (e.g. "Get up for college")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = onOpenAiAssistant,
                    label = { Text("Ask AI to set alarm") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
                Alarm.PERSONAL_PROMPTS.take(3).forEach { (presetGoal, presetLabel, presetPrompt) ->
                    AssistChip(
                        onClick = { onQuickPresetAlarm(presetGoal, presetLabel, presetPrompt) },
                        label = { Text("+ $presetGoal") }
                    )
                }
            }

            if (alarms.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(items = alarms, key = { it.id }) { alarm ->
                        val dismissState = rememberSwipeToDismissBoxState()
                        LaunchedEffect(dismissState.currentValue) {
                            if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                                onDeleteAlarm(alarm)
                                scope.launch {
                                    val result = snackbar.showSnackbar(
                                        message = "Alarm deleted",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Long
                                    )
                                    if (result == SnackbarResult.ActionPerformed) onUndoDelete(alarm)
                                }
                            }
                        }
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                Box(
                                    Modifier.fillMaxSize().padding(horizontal = 24.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete alarm",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        ) {
                            AlarmRow(
                                alarm = alarm,
                                onClick = { onEditAlarm(alarm) },
                                onToggle = { onToggleAlarm(alarm, it) }
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmRow(alarm: Alarm, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val timeColor by animateColorAsState(
        targetValue = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        label = "alarm_time_color"
    )
    Surface(color = MaterialTheme.colorScheme.surface) {
        ListItem(
            modifier = Modifier.clickable {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                onClick()
            },
            headlineContent = {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(alarm.formattedTime, style = MaterialTheme.typography.displayMedium, color = timeColor)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        alarm.amPm,
                        style = MaterialTheme.typography.titleMedium,
                        color = timeColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val title = alarm.label.ifBlank { alarm.goal }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (alarm.wakePrompt.isNotBlank()) {
                        Text(
                            text = alarm.wakePrompt,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                    val meta = buildString {
                        append(alarm.daysSummary)
                        append("  •  ")
                        append(alarm.category.shortName)
                        append("  •  ")
                        append("${alarm.questionsRequired}Q")
                        if (alarm.wakeCheckEnabled) append("  •  5m check")
                        if (alarm.toneTitle.isNotBlank()) append("  •  MP3")
                    }
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            trailingContent = {
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { checked ->
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onToggle(checked)
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Alarm at ${alarm.formattedTime} ${alarm.amPm}"
                    }
                )
            }
        )
    }
}

@Composable
private fun SummaryHeader(
    alarms: List<Alarm>,
    stats: WakeStats,
    nowMillis: Long,
    reducedMotion: Boolean
) {
    val nextAlarm = remember(alarms, nowMillis / 60_000L) {
        alarms.filter { it.isEnabled }.minByOrNull { it.computeNextTriggerMillis() }
    }
    val nextMillis = nextAlarm?.computeNextTriggerMillis()
    if (nextMillis == null && stats.totalWakes == 0) return

    val dotAlphaState = if (reducedMotion || nextMillis == null) {
        androidx.compose.runtime.rememberUpdatedState(1f)
    } else {
        val transition = rememberInfiniteTransition(label = "armed_dot")
        transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot_alpha"
        )
    }

    val dotColor = MaterialTheme.colorScheme.primary

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 6.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (nextMillis != null) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawCircle(color = dotColor.copy(alpha = dotAlphaState.value))
                    }
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text = if (nextMillis != null)
                        "Next alarm ${TimeFormat.until(nextMillis, nowMillis)}"
                    else "No alarm is currently armed",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (nextAlarm != null && nextAlarm.goal.isNotBlank()) {
                Text(
                    text = "Target: ${nextAlarm.goal}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (stats.totalWakes > 0) {
                Text(
                    text = "${stats.streakDays}-day wake streak  •  avg ${stats.averageSolveSeconds}s to solve",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PermissionBanner(count: Int, onFix: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Text(
                "$count permission(s) missing: alarms may not ring reliably",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onFix) { Text("Fix", color = MaterialTheme.colorScheme.onErrorContainer) }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Alarm, contentDescription = null,
                modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(16.dp))
            Text("No alarms yet", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Add one and attach a morning goal. You will have to think your way out of bed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
