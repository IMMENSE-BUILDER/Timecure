package com.timecure.app.ui.components

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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

enum class TopDestination(
    val label: String,
    val phaseTagline: String,
    val icon: ImageVector
) {
    ALARMS("Alarms", "Wake-Up Schedule", Icons.Default.Alarm),
    FOCUS("Focus", "Pomodoro Flow", Icons.Default.Timer),
    INSIGHTS("Insights", "Wake & Focus Stats", Icons.Default.Insights),
    SETTINGS("Settings", "AI & System Control", Icons.Default.Settings)
}

/**
 * High-performance M3 Adaptive Navigation Shell with:
 * - Warm tab caching (tabs are never torn down/rebuilt when switching — eliminates the ~1s lag!)
 * - Pure GPU-layer directional slide + scale + fade transitions (0 recompositions per frame!)
 * - Animated Transition Phase Ribbon & Pill (reads progress only in Draw/Layer phase)
 * - Immediate tactile press-down scale + M3 ripple + haptic click feedback on every nav button
 */
@Composable
fun AnimatedNavShell(
    useRail: Boolean,
    destination: TopDestination,
    pomodoroRunning: Boolean,
    onSelect: (TopDestination) -> Unit,
    content: @Composable (TopDestination, Modifier) -> Unit
) {
    val reducedMotion = rememberReducedMotion()

    // Keep visited tabs warm in composition so switching is instantaneous (<16ms)
    val mountedTabs = remember { mutableStateListOf(destination) }
    var previousDestination by remember { mutableStateOf<TopDestination?>(null) }
    var activeDestination by remember { mutableStateOf(destination) }
    var transitionDirection by remember { mutableIntStateOf(1) }

    // Pre-warm remaining tabs gently after initial frame settles so even the FIRST tap is instant
    LaunchedEffect(Unit) {
        delay(280L)
        TopDestination.entries.forEach { tab ->
            if (tab !in mountedTabs) {
                mountedTabs += tab
                delay(60L)
            }
        }
    }

    // Drives BOTH screen cross-slide AND the animated transition phase overlay purely on GPU layer
    val transitionProgress = remember { Animatable(1f) }
    var isTransitioning by remember { mutableStateOf(false) }

    LaunchedEffect(destination) {
        if (destination !in mountedTabs) mountedTabs += destination
        if (destination != activeDestination) {
            transitionDirection = if (destination.ordinal > activeDestination.ordinal) 1 else -1
            previousDestination = activeDestination
            activeDestination = destination
            if (reducedMotion) {
                transitionProgress.snapTo(1f)
                previousDestination = null
                isTransitioning = false
            } else {
                isTransitioning = true
                transitionProgress.snapTo(0f)
                transitionProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                )
                previousDestination = null
                isTransitioning = false
            }
        }
    }

    val stage: @Composable (Modifier) -> Unit = { stageModifier ->
        Box(modifier = stageModifier) {
            mountedTabs.forEach { tab ->
                key(tab) {
                    val isCurrent = tab == activeDestination
                    val isExiting = tab == previousDestination && isTransitioning
                    val z = when {
                        isCurrent -> 2f
                        isExiting -> 1f
                        else -> 0f
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(z)
                            .graphicsLayer {
                                val p = transitionProgress.value
                                when {
                                    isCurrent -> {
                                        alpha = if (isTransitioning) p else 1f
                                        translationX = if (isTransitioning) (1f - p) * size.width * 0.14f * transitionDirection else 0f
                                        val s = if (isTransitioning) 0.975f + 0.025f * p else 1f
                                        scaleX = s
                                        scaleY = s
                                    }
                                    isExiting -> {
                                        alpha = 1f - p
                                        translationX = -p * size.width * 0.10f * transitionDirection
                                        val s = 1f - 0.025f * p
                                        scaleX = s
                                        scaleY = s
                                    }
                                    else -> {
                                        alpha = 0f
                                        translationX = 0f
                                        scaleX = 1f
                                        scaleY = 1f
                                    }
                                }
                            }
                            .semantics {
                                if (!isCurrent) {
                                    @Suppress("DEPRECATION")
                                    invisibleToUser()
                                }
                            }
                    ) {
                        content(tab, Modifier.fillMaxSize())
                    }
                }
            }

            // Animated Transition Phase Overlay (reads transitionProgress ONLY in Draw/Layer phase!)
            if (!reducedMotion && isTransitioning) {
                NavigationPhaseOverlay(
                    destination = activeDestination,
                    progressProvider = { transitionProgress.value },
                    direction = transitionDirection,
                    modifier = Modifier.fillMaxSize().zIndex(5f)
                )
            }
        }
    }

    if (useRail) {
        Row(Modifier.fillMaxSize()) {
            NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Spacer(Modifier.height(12.dp))
                TopDestination.entries.forEach { dest ->
                    NavigationRailItem(
                        selected = destination == dest,
                        onClick = { onSelect(dest) },
                        icon = {
                            Box {
                                Icon(dest.icon, contentDescription = dest.label)
                                if (dest == TopDestination.FOCUS && pomodoroRunning) {
                                    RunningIndicatorDot(Modifier.align(Alignment.TopEnd))
                                }
                            }
                        },
                        label = { Text(dest.label) }
                    )
                }
            }
            stage(Modifier.weight(1f).fillMaxSize())
        }
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                TimecureExpressiveNavBar(
                    selectedDestination = destination,
                    pomodoroRunning = pomodoroRunning,
                    reducedMotion = reducedMotion,
                    onSelect = onSelect
                )
            }
        ) { innerPadding ->
            stage(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            )
        }
    }
}

/**
 * Visible animated transition phase rendered purely on GPU Draw/Layer phases (0 recompositions!):
 * - Soft directional tonal gradient sweep across the viewport
 * - Blooming destination phase badge gliding cleanly with directional parallax
 */
@Composable
private fun NavigationPhaseOverlay(
    destination: TopDestination,
    progressProvider: () -> Float,
    direction: Int,
    modifier: Modifier = Modifier
) {
    val accent = destinationAccentColor(destination)
    val container = destinationContainerColor(destination)
    val onContainer = destinationOnContainerColor(destination)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val progress = progressProvider()
            val envelope = sin(progress * PI).toFloat().coerceIn(0f, 1f)
            val sweepCenterX = if (direction > 0) {
                size.width * (1.12f - progress * 1.24f)
            } else {
                size.width * (-0.12f + progress * 1.24f)
            }
            val bandWidth = size.width * 0.58f
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        accent.copy(alpha = 0.10f * envelope),
                        Color.Transparent
                    ),
                    startX = sweepCenterX - bandWidth / 2f,
                    endX = sweepCenterX + bandWidth / 2f
                )
            )
        }

        Surface(
            color = container.copy(alpha = 0.96f),
            contentColor = onContainer,
            shape = CircleShape,
            tonalElevation = 4.dp,
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(top = 64.dp)
                .graphicsLayer {
                    val progress = progressProvider()
                    val envelope = sin(progress * PI).toFloat().coerceIn(0f, 1f)
                    alpha = envelope
                    val s = 0.88f + 0.14f * envelope
                    scaleX = s
                    scaleY = s
                    translationX = direction * (1f - progress) * 34f - direction * progress * 14f
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = destination.icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = destination.phaseTagline,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Custom M3 Expressive Navigation Bar with:
 * - Immediate tactile press-down spring scale + bounded M3 ripple + haptic tick on touch
 * - Spring-gliding active pill indicator that elastically stretches while moving across tabs
 * - Live pulsing dot on Focus when Pomodoro is running (zero-recomposition Canvas draw)
 */
@Composable
private fun TimecureExpressiveNavBar(
    selectedDestination: TopDestination,
    pomodoroRunning: Boolean,
    reducedMotion: Boolean,
    onSelect: (TopDestination) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val destinations = TopDestination.entries
    val selectedIndex = selectedDestination.ordinal

    val stretchAnim = remember { Animatable(0f) }
    var prevIndex by remember { mutableIntStateOf(selectedIndex) }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex != prevIndex && !reducedMotion) {
            val jump = abs(selectedIndex - prevIndex).coerceAtMost(3)
            prevIndex = selectedIndex
            stretchAnim.snapTo(jump * 9f)
            stretchAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        } else {
            prevIndex = selectedIndex
        }
    }

    val pillColor by animateColorAsState(
        targetValue = destinationContainerColor(selectedDestination),
        animationSpec = tween(200),
        label = "nav_pill_color"
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(74.dp)
                .padding(horizontal = 8.dp)
        ) {
            val slotWidth = maxWidth / destinations.size
            val basePillWidth = 64.dp
            val dynamicPillWidth = basePillWidth + stretchAnim.value.dp
            val targetOffset = slotWidth * selectedIndex + (slotWidth - dynamicPillWidth) / 2f

            val animatedOffset by animateDpAsState(
                targetValue = targetOffset,
                animationSpec = if (reducedMotion) tween(0) else spring(
                    dampingRatio = 0.74f,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "nav_pill_glide"
            )

            // Gliding M3 Active Pill Indicator
            Surface(
                color = pillColor,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .offset(x = animatedOffset, y = 10.dp)
                    .width(dynamicPillWidth)
                    .height(34.dp)
            ) {}

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                destinations.forEach { dest ->
                    val isSelected = dest == selectedDestination
                    val interaction = remember { MutableInteractionSource() }
                    val isPressed by interaction.collectIsPressedAsState()

                    val pressScale by animateFloatAsState(
                        targetValue = when {
                            reducedMotion -> 1f
                            isPressed -> 0.88f
                            isSelected -> 1.10f
                            else -> 0.96f
                        },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessHigh
                        ),
                        label = "nav_icon_press_scale"
                    )
                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) destinationOnContainerColor(dest)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(180),
                        label = "nav_icon_tint"
                    )
                    val labelTint by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(180),
                        label = "nav_label_tint"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                interactionSource = interaction,
                                indication = ripple(bounded = true, color = destinationAccentColor(dest)),
                                role = Role.Tab
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (!isSelected) onSelect(dest)
                            }
                            .semantics {
                                selected = isSelected
                                contentDescription = "${dest.label} tab"
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier.height(34.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = dest.icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer {
                                        scaleX = pressScale
                                        scaleY = pressScale
                                    }
                            )
                            if (dest == TopDestination.FOCUS && pomodoroRunning) {
                                RunningIndicatorDot(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-2).dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = dest.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = labelTint
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RunningIndicatorDot(modifier: Modifier = Modifier) {
    val reducedMotion = rememberReducedMotion()
    val alphaState: State<Float> = if (reducedMotion) {
        rememberUpdatedState(1f)
    } else {
        val inf = rememberInfiniteTransition(label = "pomo_dot")
        inf.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
            label = "pomo_dot_alpha"
        )
    }
    val dotColor = MaterialTheme.colorScheme.tertiary
    Canvas(modifier = modifier.size(8.dp)) {
        drawCircle(color = dotColor.copy(alpha = alphaState.value))
    }
}

@Composable
private fun destinationAccentColor(dest: TopDestination): Color = when (dest) {
    TopDestination.ALARMS -> MaterialTheme.colorScheme.primary
    TopDestination.FOCUS -> MaterialTheme.colorScheme.tertiary
    TopDestination.INSIGHTS -> MaterialTheme.colorScheme.secondary
    TopDestination.SETTINGS -> MaterialTheme.colorScheme.primary
}

@Composable
private fun destinationContainerColor(dest: TopDestination): Color = when (dest) {
    TopDestination.ALARMS -> MaterialTheme.colorScheme.primaryContainer
    TopDestination.FOCUS -> MaterialTheme.colorScheme.tertiaryContainer
    TopDestination.INSIGHTS -> MaterialTheme.colorScheme.secondaryContainer
    TopDestination.SETTINGS -> MaterialTheme.colorScheme.primaryContainer
}

@Composable
private fun destinationOnContainerColor(dest: TopDestination): Color = when (dest) {
    TopDestination.ALARMS -> MaterialTheme.colorScheme.onPrimaryContainer
    TopDestination.FOCUS -> MaterialTheme.colorScheme.onTertiaryContainer
    TopDestination.INSIGHTS -> MaterialTheme.colorScheme.onSecondaryContainer
    TopDestination.SETTINGS -> MaterialTheme.colorScheme.onPrimaryContainer
}
