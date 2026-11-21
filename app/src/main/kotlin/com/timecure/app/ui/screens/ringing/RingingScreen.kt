package com.timecure.app.ui.screens.ringing

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timecure.app.alarm.AlarmScheduler
import com.timecure.app.model.Alarm
import com.timecure.app.model.ChallengeSource
import com.timecure.app.model.CognitiveChallenge
import com.timecure.app.ui.components.AnimatedCheckmarkBadge
import com.timecure.app.ui.components.BreathingAuraRing
import com.timecure.app.ui.components.StroopWordBanner
import com.timecure.app.ui.components.rememberReducedMotion
import com.timecure.app.ui.screens.addalarm.AlarmWallpaperBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Shared atmospheric radial lighting behind lock-screen alarm / task / wake-check takeovers.
 */
@Composable
internal fun RingingBackdropAtmosphere(
    primaryGlow: Color,
    secondaryGlow: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(primaryGlow.copy(alpha = 0.24f), Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 0.22f),
                radius = size.minDimension * 0.85f
            )
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(secondaryGlow.copy(alpha = 0.14f), Color.Transparent),
                center = Offset(size.width * 0.85f, size.height * 0.82f),
                radius = size.minDimension * 0.72f
            )
        )
    }
}

/**
 * Segmented capsule step bar replacing generic thin progress bars.
 */
@Composable
internal fun StepPillBar(
    currentStep: Int,
    totalSteps: Int,
    isAllSolved: Boolean,
    modifier: Modifier = Modifier
) {
    if (totalSteps <= 1) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (idx in 1..totalSteps) {
            val done = isAllSolved || idx < currentStep
            val active = !isAllSolved && idx == currentStep
            val barColor = when {
                done -> MaterialTheme.colorScheme.primary
                active -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f)
            }
            val height by animateDpAsState(
                targetValue = if (active) 7.dp else 5.dp,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "step_pill_h"
            )
            Box(
                modifier = Modifier
                    .weight(if (active) 1.45f else 1f)
                    .height(height)
                    .clip(CircleShape)
                    .background(barColor)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RingingScreen(
    alarm: Alarm?,
    challenge: CognitiveChallenge?,
    currentStep: Int,
    totalSteps: Int,
    rerings: Int,
    wakeCheck: Boolean,
    practiceMode: Boolean,
    isTestSimulation: Boolean,
    isLoadingChallenge: Boolean,
    isAllSolved: Boolean,
    elapsedSeconds: Int,
    wrongAttempts: Int,
    userName: String,
    snoozesUsed: Int = 0,
    onSnooze: (() -> Unit)? = null,
    onSubmitAnswer: (String) -> Boolean,
    onDone: () -> Unit,
    onLaunchFocusSprint: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val reducedMotion = rememberReducedMotion()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val imeVisible = WindowInsets.isImeVisible

    var userAnswer by remember(challenge?.id) { mutableStateOf("") }
    var isError by remember(challenge?.id) { mutableStateOf(false) }
    var errorMessage by remember(challenge?.id) { mutableStateOf("") }
    var showHint by remember(challenge?.id) { mutableStateOf(false) }
    var backNotice by remember { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }

    BackHandler(enabled = !practiceMode && !isAllSolved && !isTestSimulation) {
        backNotice = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            delay(3500)
            backNotice = false
        }
    }

    fun triggerWrongFeedback(msg: String) {
        isError = true
        errorMessage = msg
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (!reducedMotion) {
            scope.launch {
                shakeOffset.snapTo(0f)
                listOf(-18f, 18f, -12f, 12f, -6f, 0f).forEach { target ->
                    shakeOffset.animateTo(target, tween(45))
                }
            }
        }
    }

    fun submit(answerText: String) {
        if (answerText.isBlank()) {
            triggerWrongFeedback("Type an answer to wake your brain")
            return
        }
        val ok = onSubmitAnswer(answerText)
        if (!ok) {
            triggerWrongFeedback("Not quite — double-check and try again")
        } else {
            isError = false
            userAnswer = ""
        }
    }

    val goal = alarm?.goal?.takeIf { it.isNotBlank() } ?: "Win the morning"
    val timeLabel = alarm?.let { "${it.formattedTime} ${it.amPm}" } ?: "Timecure"
    val dateLabel = remember {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    }
    val elapsedText = String.format(Locale.getDefault(), "%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60)
    val nextDelay = AlarmScheduler.reringDelayMinutes(rerings)
    val primaryGlow = if (rerings > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (!practiceMode) {
            AlarmWallpaperBackdrop(
                wallpaperUri = alarm?.wallpaperUri.orEmpty(),
                modifier = Modifier.fillMaxSize()
            )
        }
        RingingBackdropAtmosphere(
            primaryGlow = primaryGlow,
            secondaryGlow = MaterialTheme.colorScheme.tertiary
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .displayCutoutPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(top = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sleek Top HUD Bar
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
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                .semantics { liveRegion = LiveRegionMode.Polite },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = primaryGlow
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = elapsedText,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    when {
                        practiceMode -> TextButton(onClick = onDone) { Text("Exit Dojo") }
                        isTestSimulation -> Surface(
                            onClick = onDone,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Stop test alarm", modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Stop Test", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        else -> Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Step ${currentStep.coerceAtMost(totalSteps)} of $totalSteps",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                if (totalSteps > 1) {
                    Spacer(Modifier.height(12.dp))
                    StepPillBar(
                        currentStep = currentStep,
                        totalSteps = totalSteps,
                        isAllSolved = isAllSolved
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Dramatic Collapsible Hero Clock + Goal Pill
                AnimatedVisibility(
                    visible = !imeVisible || isAllSolved,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (rerings > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = CircleShape,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "ESCALATED RE-RING #$rerings • MAX VOLUME",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        } else if (wakeCheck) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "ANTI-RELAPSE CHECK • CONFIRM AWAKE",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        } else if (practiceMode) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "BRAIN WARM-UP DOJO",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(if (practiceMode) 112.dp else 192.dp)
                        ) {
                            if (!isAllSolved) {
                                BreathingAuraRing(
                                    primaryColor = primaryGlow,
                                    accentColor = MaterialTheme.colorScheme.tertiary,
                                    urgent = rerings > 0,
                                    reducedMotion = reducedMotion,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (!practiceMode) {
                                    Text(
                                        text = dateLabel.uppercase(Locale.getDefault()),
                                        style = MaterialTheme.typography.labelSmall,
                                        letterSpacing = 1.1.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = if (practiceMode) "Dojo" else alarm?.formattedTime ?: "Test",
                                        style = if (practiceMode) {
                                            MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
                                        } else {
                                            MaterialTheme.typography.displayLarge.copy(
                                                fontSize = 58.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = (-1.5).sp
                                            )
                                        },
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!practiceMode && alarm != null) {
                                        Text(
                                            text = alarm.amPm,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryGlow,
                                            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        // Goal Capsule
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(15.dp)
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

                        val promptText = alarm?.wakePrompt?.takeIf { it.isNotBlank() }
                        if (promptText != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = promptText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                    }
                }

                // Compact Header When Keyboard Is Open
                AnimatedVisibility(visible = imeVisible && !isAllSolved) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (practiceMode) "Brain Warm-Up Dojo" else timeLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = goal,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }

                // Main Challenge Card OR Victory Card
                AnimatedContent(
                    targetState = isAllSolved,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
                    label = "ringing_content"
                ) { solved ->
                    if (solved) {
                        VictoryCard(
                            goal = goal,
                            userName = userName,
                            elapsedSeconds = elapsedSeconds,
                            wrongAttempts = wrongAttempts,
                            wakeCheckArmed = !practiceMode && !isTestSimulation && !wakeCheck && (alarm?.wakeCheckEnabled == true),
                            practiceMode = practiceMode,
                            reducedMotion = reducedMotion,
                            onDone = onDone,
                            onLaunchFocusSprint = onLaunchFocusSprint?.let { launch -> { launch(goal) } }
                        )
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                            shape = RoundedCornerShape(28.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                            tonalElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                        ) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (isLoadingChallenge || challenge == null) {
                                    Spacer(Modifier.height(20.dp))
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(34.dp),
                                        strokeWidth = 3.dp
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        "Preparing your cognitive challenge...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(20.dp))
                                } else {
                                    // Refined Challenge Metadata Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = CircleShape,
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = if (challenge.source == ChallengeSource.AI)
                                                            Icons.Default.AutoAwesome else Icons.Default.Memory,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = if (challenge.source == ChallengeSource.AI)
                                                    "Timecure AI" else challenge.category.displayName,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = challenge.difficulty.displayName,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(18.dp))

                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.75f),
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = challenge.question,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                lineHeight = 30.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)
                                        )
                                    }

                                    Spacer(Modifier.height(16.dp))

                                    val stroop = challenge.stroop
                                    if (stroop != null) {
                                        StroopWordBanner(stroop = stroop)
                                        Spacer(Modifier.height(16.dp))
                                        if (isError) {
                                            Text(
                                                text = errorMessage,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(bottom = 10.dp)
                                            )
                                        }
                                        val chunked = stroop.options.chunked(2)
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            chunked.forEach { rowOptions ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    rowOptions.forEach { option ->
                                                        FilledTonalButton(
                                                            onClick = { submit(option) },
                                                            shape = RoundedCornerShape(16.dp),
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(56.dp)
                                                                .semantics { contentDescription = "Answer $option" }
                                                        ) {
                                                            Text(
                                                                text = option,
                                                                style = MaterialTheme.typography.titleSmall,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        OutlinedTextField(
                                            value = userAnswer,
                                            onValueChange = {
                                                userAnswer = it
                                                isError = false
                                            },
                                            label = { Text("Your answer") },
                                            isError = isError,
                                            shape = RoundedCornerShape(16.dp),
                                            supportingText = {
                                                if (isError) {
                                                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                                                }
                                            },
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = if (challenge.wantsNumericKeyboard)
                                                    KeyboardType.Number else KeyboardType.Text,
                                                imeAction = ImeAction.Done
                                            ),
                                            keyboardActions = KeyboardActions(onDone = { submit(userAnswer) }),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(Modifier.height(12.dp))

                                        Button(
                                            onClick = { submit(userAnswer) },
                                            shape = RoundedCornerShape(18.dp),
                                            modifier = Modifier.fillMaxWidth().height(56.dp)
                                        ) {
                                            Text(
                                                "Submit answer",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    if (challenge.hint.isNotBlank()) {
                                        Spacer(Modifier.height(10.dp))
                                        if (showHint) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Default.Lightbulb,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(
                                                        text = challenge.hint,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                                    )
                                                }
                                            }
                                        } else {
                                            TextButton(onClick = { showHint = true }) {
                                                Icon(
                                                    Icons.Default.Lightbulb,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text("Need a hint?")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                AnimatedVisibility(visible = backNotice) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        Text(
                            text = "No shortcuts — solve the challenge to silence. Force-closing schedules an escalated re-ring in $nextDelay min.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                val canSnooze = !practiceMode &&
                    !isAllSolved &&
                    !wakeCheck &&
                    !isTestSimulation &&
                    onSnooze != null &&
                    alarm?.snoozeEnabled == true &&
                    (alarm.snoozeMaxCount == 0 || snoozesUsed < alarm.snoozeMaxCount)

                if (canSnooze) {
                    val snoozeLabel = if (alarm.snoozeMaxCount <= 0) {
                        "Snooze ${alarm.snoozeIntervalMinutes} min"
                    } else {
                        val left = (alarm.snoozeMaxCount - snoozesUsed).coerceAtLeast(1)
                        "Snooze ${alarm.snoozeIntervalMinutes} min ($left left)"
                    }
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSnooze()
                        },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(bottom = 2.dp)
                    ) {
                        Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(snoozeLabel, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (!practiceMode && !isAllSolved) {
                    Text(
                        text = if (isTestSimulation)
                            "Test simulation • Max volume & hardware key lock active"
                        else
                            "Dismissing without solving re-rings in $nextDelay min at harder difficulty",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun VictoryCard(
    goal: String,
    userName: String,
    elapsedSeconds: Int,
    wrongAttempts: Int,
    wakeCheckArmed: Boolean,
    practiceMode: Boolean,
    reducedMotion: Boolean,
    onDone: () -> Unit,
    onLaunchFocusSprint: (() -> Unit)? = null
) {
    val greeting = if (userName.isNotBlank()) "You're awake, $userName." else "Prefrontal cortex online."
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedCheckmarkBadge(reducedMotion = reducedMotion)
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (practiceMode) "Warm-up complete" else greeting,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${elapsedSeconds}s",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Solve time",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (wrongAttempts == 0) "Flawless" else "$wrongAttempts retry",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (wrongAttempts == 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Precision",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "LOCKED IN FOR TODAY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = goal,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (wakeCheckArmed) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Wake-up check armed — stay out of bed.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(20.dp))

            if (!practiceMode && onLaunchFocusSprint != null) {
                FilledTonalButton(
                    onClick = onLaunchFocusSprint,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Launch 25m Focus Sprint", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(10.dp))
            }
            Button(
                onClick = onDone,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(
                    text = if (practiceMode) "Back to Insights" else "Start my day",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
