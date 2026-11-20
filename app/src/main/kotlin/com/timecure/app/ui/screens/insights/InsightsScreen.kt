package com.timecure.app.ui.screens.insights

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.timecure.app.data.local.TimecureDatabaseHelper
import com.timecure.app.model.AiTaskItem
import com.timecure.app.model.ChallengeCategory
import com.timecure.app.model.ChallengeDifficulty
import com.timecure.app.model.DailyWakePoint
import com.timecure.app.model.PomodoroStats
import com.timecure.app.model.WakeEvent
import com.timecure.app.model.WakeStats
import com.timecure.app.ui.components.rememberReducedMotion
import com.timecure.app.ui.components.safeTopBarWindowInsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InsightsScreen(
    stats: WakeStats,
    pomodoroStats: PomodoroStats,
    aiTasks: List<AiTaskItem>,
    recentEvents: List<WakeEvent>,
    onToggleTaskDone: (AiTaskItem, Boolean) -> Unit,
    onClearTasks: () -> Unit,
    onCreateAlarmFromTask: (AiTaskItem) -> Unit,
    onStartPractice: (ChallengeCategory, ChallengeDifficulty) -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = rememberReducedMotion()
    val haptic = LocalHapticFeedback.current
    var dojoCategory by remember { mutableStateOf(ChallengeCategory.MIXED) }
    var dojoDifficulty by remember { mutableStateOf(ChallengeDifficulty.MEDIUM) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Insights & Dojo") },
                windowInsets = safeTopBarWindowInsets()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Key Metrics Grid (2x2 clean tonal tiles)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricTile(
                    icon = Icons.Default.LocalFireDepartment,
                    label = "Wake streak",
                    value = "${stats.streakDays}d",
                    highlight = stats.streakDays > 0,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    icon = Icons.Default.Speed,
                    label = "Avg solve",
                    value = if (stats.totalWakes > 0) "${stats.averageSolveSeconds}s" else "--",
                    highlight = false,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricTile(
                    icon = Icons.Default.Bolt,
                    label = "Fastest wake",
                    value = if (stats.totalWakes > 0) "${stats.fastestSolveSeconds}s" else "--",
                    highlight = false,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    icon = Icons.Default.Timer,
                    label = "Focused today",
                    value = "${pomodoroStats.todayMinutes}m",
                    highlight = pomodoroStats.todayMinutes > 0,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricTile(
                    icon = Icons.Default.CheckCircle,
                    label = "Wake wins",
                    value = "${stats.totalWakes}",
                    highlight = false,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    icon = Icons.Default.LocalFireDepartment,
                    label = "Focus blocks",
                    value = "${pomodoroStats.totalSessions}",
                    highlight = false,
                    modifier = Modifier.weight(1f)
                )
            }

            // Saved AI Vision Task Table (generated from uploaded timetable / study materials)
            if (aiTasks.isNotEmpty()) {
                val planTitle = aiTasks.first().planTitle
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TableChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(planTitle, style = MaterialTheme.typography.titleMedium)
                            }
                            TextButton(onClick = onClearTasks) { Text("Clear") }
                        }
                        aiTasks.forEach { task ->
                            ListItem(
                                leadingContent = {
                                    Checkbox(
                                        checked = task.isCompleted,
                                        onCheckedChange = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onToggleTaskDone(task, it)
                                        }
                                    )
                                },
                                headlineContent = { Text(task.taskText) },
                                supportingContent = { Text("${task.timeSlot}  •  ${task.technique}") },
                                trailingContent = {
                                    if (task.hasSuggestedAlarm) {
                                        IconButton(onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onCreateAlarmFromTask(task)
                                        }) {
                                            Icon(
                                                Icons.Default.AddAlarm,
                                                contentDescription = "Create alarm for ${task.timeSlot}",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }

            // 7-Day Consistency Bar Chart
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Last 7 days", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Solved wake-ups & average response speed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    SevenDayWakeChart(points = stats.last7Days, reducedMotion = reducedMotion)
                }
            }

            // Brain Warm-Up Dojo
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Brain Warm-Up Dojo", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Practice any cognitive challenge silently (3 questions) — including live AI Coach.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ChallengeCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = dojoCategory == cat,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    dojoCategory = cat
                                },
                                label = { Text(cat.shortName) }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val diffs = ChallengeDifficulty.entries
                        diffs.forEachIndexed { idx, d ->
                            SegmentedButton(
                                selected = dojoDifficulty == d,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    dojoDifficulty = d
                                },
                                shape = SegmentedButtonDefaults.itemShape(idx, diffs.size)
                            ) { Text(d.displayName) }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStartPractice(dojoCategory, dojoDifficulty)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Start 3-question warm-up")
                    }
                }
            }

            // Recent Wake-Up Activity Log
            Text("Recent wake log", style = MaterialTheme.typography.titleMedium)
            if (recentEvents.isEmpty()) {
                Text(
                    text = "No wake-up sessions logged yet. Solve your first alarm to start building your streak.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val fmt = remember { SimpleDateFormat("EEE, MMM d  •  h:mm a", Locale.getDefault()) }
                Column {
                    recentEvents.take(12).forEach { event ->
                        val (icon, tint, title, subtitle) = when (event.action) {
                            TimecureDatabaseHelper.ACTION_SOLVED -> EventPresentation(
                                Icons.Default.CheckCircle,
                                MaterialTheme.colorScheme.primary,
                                "Solved in ${event.solveSeconds}s",
                                fmt.format(Date(event.timestampMillis))
                            )
                            TimecureDatabaseHelper.ACTION_WAKE_CONFIRMED -> EventPresentation(
                                Icons.Default.Verified,
                                MaterialTheme.colorScheme.tertiary,
                                "5-min wake check confirmed (${event.solveSeconds}s)",
                                fmt.format(Date(event.timestampMillis))
                            )
                            else -> EventPresentation(
                                Icons.Default.Warning,
                                MaterialTheme.colorScheme.error,
                                "Dismissed early — escalated re-ring triggered",
                                fmt.format(Date(event.timestampMillis))
                            )
                        }
                        ListItem(
                            leadingContent = { Icon(icon, contentDescription = null, tint = tint) },
                            headlineContent = { Text(title) },
                            supportingContent = { Text(subtitle) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

private data class EventPresentation(
    val icon: ImageVector,
    val tint: androidx.compose.ui.graphics.Color,
    val title: String,
    val subtitle: String
)

@Composable
private fun MetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (highlight) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = modifier
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SevenDayWakeChart(
    points: List<DailyWakePoint>,
    reducedMotion: Boolean
) {
    var revealed by remember { mutableStateOf(reducedMotion) }
    LaunchedEffect(Unit) { revealed = true }

    val animProgress by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = if (reducedMotion) tween(0) else spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "chart_bars"
    )

    val maxCount = (points.maxOfOrNull { it.solvedCount } ?: 1).coerceAtLeast(1)
    val activeColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Column(Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
        ) {
            if (points.isEmpty()) return@Canvas
            val slotWidth = size.width / points.size
            val barWidth = (slotWidth * 0.44f).coerceAtMost(32.dp.toPx())
            val radius = CornerRadius(8.dp.toPx(), 8.dp.toPx())

            points.forEachIndexed { idx, pt ->
                val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                val targetHeight = if (pt.solvedCount == 0) 10.dp.toPx()
                else (size.height * (0.28f + 0.72f * (pt.solvedCount.toFloat() / maxCount))) * animProgress
                val top = size.height - targetHeight
                drawRoundRect(
                    color = if (pt.solvedCount > 0) activeColor else emptyColor,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, targetHeight),
                    cornerRadius = radius
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            points.forEach { pt ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pt.dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (pt.solvedCount > 0) {
                        Text(
                            text = "${pt.averageSolveSeconds}s",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
