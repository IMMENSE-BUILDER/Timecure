package com.timecure.app.ui.screens.pomodoro

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timecure.app.model.Alarm
import com.timecure.app.model.PomodoroPhase
import com.timecure.app.model.PomodoroPreset
import com.timecure.app.model.PomodoroState
import com.timecure.app.model.PomodoroStats
import com.timecure.app.ui.components.rememberReducedMotion
import com.timecure.app.ui.components.safeTopBarWindowInsets
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    state: PomodoroState,
    stats: PomodoroStats,
    alarms: List<Alarm>,
    onToggleRun: () -> Unit,
    onReset: () -> Unit,
    onSkipPhase: () -> Unit,
    onSelectPreset: (PomodoroPreset) -> Unit,
    onSelectPhase: (PomodoroPhase) -> Unit,
    onUpdateGoal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = rememberReducedMotion()
    val haptic = LocalHapticFeedback.current
    var showGoalDialog by remember { mutableStateOf(false) }

    // Animated phase transition banner when moving between Focus <-> Break
    var showPhaseBanner by remember { mutableStateOf(false) }
    val dialPulse = remember { Animatable(1f) }
    LaunchedEffect(state.phaseTransitionNonce) {
        if (state.phaseTransitionNonce > 0) {
            showPhaseBanner = true
            if (!reducedMotion) {
                dialPulse.snapTo(0.93f)
                dialPulse.animateTo(
                    1f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                )
            }
            delay(2800)
            showPhaseBanner = false
        }
    }

    val phaseAccent by animateColorAsState(
        targetValue = when (state.phase) {
            PomodoroPhase.FOCUS -> MaterialTheme.colorScheme.primary
            PomodoroPhase.SHORT_BREAK -> MaterialTheme.colorScheme.secondary
            PomodoroPhase.LONG_BREAK -> MaterialTheme.colorScheme.tertiary
        },
        animationSpec = tween(420),
        label = "phase_accent"
    )
    val phaseContainer by animateColorAsState(
        targetValue = when (state.phase) {
            PomodoroPhase.FOCUS -> MaterialTheme.colorScheme.primaryContainer
            PomodoroPhase.SHORT_BREAK -> MaterialTheme.colorScheme.secondaryContainer
            PomodoroPhase.LONG_BREAK -> MaterialTheme.colorScheme.tertiaryContainer
        },
        animationSpec = tween(420),
        label = "phase_container"
    )
    val onPhaseContainer by animateColorAsState(
        targetValue = when (state.phase) {
            PomodoroPhase.FOCUS -> MaterialTheme.colorScheme.onPrimaryContainer
            PomodoroPhase.SHORT_BREAK -> MaterialTheme.colorScheme.onSecondaryContainer
            PomodoroPhase.LONG_BREAK -> MaterialTheme.colorScheme.onTertiaryContainer
        },
        animationSpec = tween(420),
        label = "on_phase_container"
    )

    // Expressive shape morphing on Play/Pause primary control
    val actionCorner by animateDpAsState(
        targetValue = if (state.isRunning) 18.dp else 32.dp,
        animationSpec = if (reducedMotion) tween(0) else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "play_pause_corner"
    )
    val actionWidth by animateDpAsState(
        targetValue = if (state.isRunning) 152.dp else 176.dp,
        animationSpec = if (reducedMotion) tween(0) else spring(stiffness = Spring.StiffnessMedium),
        label = "play_pause_width"
    )
    val actionBg by animateColorAsState(
        targetValue = if (state.isRunning) phaseContainer else phaseAccent,
        animationSpec = tween(260),
        label = "play_pause_bg"
    )
    val actionFg by animateColorAsState(
        targetValue = if (state.isRunning) onPhaseContainer else MaterialTheme.colorScheme.onPrimary,
        animationSpec = tween(260),
        label = "play_pause_fg"
    )

    val alarmGoals = remember(alarms) {
        alarms.map { it.goal.trim() }.filter { it.isNotBlank() }.distinct()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Focus Pomodoro") },
                windowInsets = safeTopBarWindowInsets()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Phase Switcher Segmented Buttons
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val phases = PomodoroPhase.entries
                phases.forEachIndexed { idx, p ->
                    SegmentedButton(
                        selected = state.phase == p,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectPhase(p)
                        },
                        shape = SegmentedButtonDefaults.itemShape(idx, phases.size)
                    ) { Text(p.shortLabel) }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Animated Phase Shift Banner
            AnimatedVisibility(
                visible = showPhaseBanner,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    color = phaseContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (state.lastCompletedPhase != null) Icons.Default.CheckCircle
                            else phaseIcon(state.phase),
                            contentDescription = null,
                            tint = onPhaseContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when (state.lastCompletedPhase) {
                                PomodoroPhase.FOCUS -> "Focus block logged! Enjoy your ${state.phase.shortLabel.lowercase()}."
                                PomodoroPhase.SHORT_BREAK, PomodoroPhase.LONG_BREAK -> "Break complete — back into Deep Focus."
                                null -> "${state.phase.title}: ${state.phase.subtitle}"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = onPhaseContainer
                        )
                    }
                }
            }

            // Active Goal Pill synced with your alarms
            Surface(
                onClick = { showGoalDialog = true },
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = phaseIcon(state.phase),
                        contentDescription = null,
                        tint = phaseAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = state.activeGoal,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit focus goal",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Expressive 260dp Chronometer Radial Dial + Rolling Odometer Digits
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(264.dp)
                    .graphicsLayer {
                        scaleX = dialPulse.value
                        scaleY = dialPulse.value
                    }
            ) {
                PomodoroChronometerDial(
                    progress = state.progress,
                    isRunning = state.isRunning,
                    accentColor = phaseAccent,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    haloColor = phaseContainer,
                    reducedMotion = reducedMotion,
                    modifier = Modifier.fillMaxSize()
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.phase.title.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = phaseAccent,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    RollingCountdownDigits(
                        timeText = state.formattedTime,
                        color = MaterialTheme.colorScheme.onSurface,
                        reducedMotion = reducedMotion
                    )
                    Spacer(Modifier.height(6.dp))
                    // Animated cycle progress dots toward Long Recovery
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val totalCycles = state.preset.cyclesBeforeLongBreak
                        repeat(totalCycles) { idx ->
                            val filled = idx < state.cycleIndexInSet ||
                                (state.phase == PomodoroPhase.LONG_BREAK)
                            val dotW by animateDpAsState(
                                targetValue = if (filled) 18.dp else 8.dp,
                                animationSpec = if (reducedMotion) tween(0) else spring(stiffness = Spring.StiffnessMedium),
                                label = "cycle_dot"
                            )
                            Surface(
                                color = if (filled) phaseAccent else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape,
                                modifier = Modifier.width(dotW).height(8.dp)
                            ) {}
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            // Transport Controls: Reset • Morphing Play/Pause • Skip Phase
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FilledTonalIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onReset()
                    },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset timer")
                }

                val playInteraction = remember { MutableInteractionSource() }
                val playPressed by playInteraction.collectIsPressedAsState()
                val playScale by animateFloatAsState(
                    targetValue = if (playPressed && !reducedMotion) 0.94f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessHigh
                    ),
                    label = "play_press_scale"
                )
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleRun()
                    },
                    interactionSource = playInteraction,
                    color = actionBg,
                    contentColor = actionFg,
                    shape = RoundedCornerShape(actionCorner),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .width(actionWidth)
                        .height(60.dp)
                        .graphicsLayer {
                            scaleX = playScale
                            scaleY = playScale
                        }
                        .semantics {
                            contentDescription = if (state.isRunning) "Pause timer" else "Start timer"
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedContent(targetState = state.isRunning, label = "play_icon") { running ->
                            Icon(
                                imageVector = if (running) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (state.isRunning) "Pause" else if (state.remainingSeconds < state.totalSeconds) "Resume" else "Start",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                FilledTonalIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSkipPhase()
                    },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Skip to next phase")
                }
            }

            Spacer(Modifier.height(24.dp))

            // Preset Cadence Selector
            Text(
                text = "Focus cadence",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PomodoroPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = state.preset == preset,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectPreset(preset)
                        },
                        label = { Text(preset.displayName) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Today's Focus Telemetry Strip
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FocusStatMini(value = "${stats.todayMinutes}m", label = "Focused today")
                    FocusStatMini(value = "${stats.totalSessions}", label = "Completed blocks")
                    FocusStatMini(value = "${state.completedFocusCycles}", label = "This session")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showGoalDialog) {
        var draft by remember(state.activeGoal) { mutableStateOf(state.activeGoal) }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Focus target") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        label = { Text("What are you locking in on?") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (alarmGoals.isNotEmpty()) {
                        Text(
                            "From your alarms",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            alarmGoals.forEach { g ->
                                SuggestionChip(onClick = { draft = g }, label = { Text(g) })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (draft.isNotBlank()) onUpdateGoal(draft.trim())
                        showGoalDialog = false
                    }
                ) { Text("Set goal") }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FocusStatMini(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun phaseIcon(phase: PomodoroPhase) = when (phase) {
    PomodoroPhase.FOCUS -> Icons.Default.LocalFireDepartment
    PomodoroPhase.SHORT_BREAK -> Icons.Default.Coffee
    PomodoroPhase.LONG_BREAK -> Icons.Default.SelfImprovement
}

/**
 * Precision 60-tick radial chronometer dial with smooth sweep arc & breathing head halo.
 */
@Composable
private fun PomodoroChronometerDial(
    progress: Float,
    isRunning: Boolean,
    accentColor: Color,
    trackColor: Color,
    haloColor: Color,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    // Keep animated values as State<Float> read strictly inside Canvas DrawScope
    // so ticking or breathing never triggers 60fps recompositions.
    val animatedProgressState = animateFloatAsState(
        targetValue = progress,
        animationSpec = if (reducedMotion) tween(0) else tween(320, easing = FastOutSlowInEasing),
        label = "dial_progress"
    )

    val breathState = if (!isRunning || reducedMotion) null else {
        val inf = rememberInfiniteTransition(label = "dial_breath")
        inf.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "dial_breath_val"
        )
    }

    Canvas(modifier = modifier) {
        val animatedProgress = animatedProgressState.value
        val breath = breathState?.value ?: 0.4f
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerR = size.minDimension / 2f - 6.dp.toPx()
        val ringR = outerR - 18.dp.toPx()
        val strokeWidth = 10.dp.toPx()

        // Subtle breathing inner aura when running
        if (isRunning) {
            drawCircle(
                color = haloColor.copy(alpha = 0.24f + 0.14f * breath),
                radius = ringR * (0.86f + 0.05f * breath),
                center = center
            )
        }

        // 60 chronometer bezel ticks
        val litTicks = (animatedProgress * 60f).toInt()
        for (i in 0 until 60) {
            val angleRad = ((-90.0 + i * 6.0) * PI / 180.0)
            val isMajor = i % 5 == 0
            val len = if (isMajor) 9.dp.toPx() else 5.dp.toPx()
            val startR = outerR - len
            val sx = center.x + (startR * cos(angleRad)).toFloat()
            val sy = center.y + (startR * sin(angleRad)).toFloat()
            val ex = center.x + (outerR * cos(angleRad)).toFloat()
            val ey = center.y + (outerR * sin(angleRad)).toFloat()
            val isLit = i <= litTicks && animatedProgress > 0.001f
            drawLine(
                color = if (isLit) accentColor.copy(alpha = if (isMajor) 0.95f else 0.65f)
                else trackColor.copy(alpha = if (isMajor) 0.85f else 0.45f),
                start = Offset(sx, sy),
                end = Offset(ex, ey),
                strokeWidth = if (isMajor) 2.2.dp.toPx() else 1.3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Track ring
        drawCircle(
            color = trackColor,
            radius = ringR,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        // Active sweep arc
        val topLeft = Offset(center.x - ringR, center.y - ringR)
        val arcSize = Size(ringR * 2f, ringR * 2f)
        val sweep = 360f * animatedProgress
        drawArc(
            color = accentColor,
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Orbiting progress head dot
        val headAngle = (-90.0 + sweep) * PI / 180.0
        val hx = center.x + (ringR * cos(headAngle)).toFloat()
        val hy = center.y + (ringR * sin(headAngle)).toFloat()
        drawCircle(
            color = accentColor.copy(alpha = 0.28f + 0.18f * breath),
            radius = 12.dp.toPx(),
            center = Offset(hx, hy)
        )
        drawCircle(
            color = accentColor,
            radius = 6.dp.toPx(),
            center = Offset(hx, hy)
        )
    }
}

/**
 * Rolling odometer-style digit animation: each character slides independently when it changes.
 */
@Composable
private fun RollingCountdownDigits(
    timeText: String,
    color: Color,
    reducedMotion: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        timeText.forEachIndexed { idx, ch ->
            if (ch == ':' || reducedMotion) {
                Text(
                    text = ch.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 54.sp, lineHeight = 60.sp),
                    color = color
                )
            } else {
                AnimatedContent(
                    targetState = ch,
                    transitionSpec = {
                        (slideInVertically(tween(220)) { it / 2 } + fadeIn(tween(180))) togetherWith
                            (slideOutVertically(tween(220)) { -it / 2 } + fadeOut(tween(140)))
                    },
                    label = "digit_$idx"
                ) { digit ->
                    Text(
                        text = digit.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 54.sp, lineHeight = 60.sp),
                        color = color
                    )
                }
            }
        }
    }
}
