package com.timecure.app.ui.screens.ringing

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.timecure.app.alarm.AlarmScheduler
import com.timecure.app.model.Alarm
import com.timecure.app.model.ChallengeSource
import com.timecure.app.model.CognitiveChallenge
import com.timecure.app.ui.components.AnimatedCheckmarkBadge
import com.timecure.app.ui.components.BreathingAuraRing
import com.timecure.app.ui.components.StroopWordBanner
import com.timecure.app.ui.components.rememberReducedMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

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

    // Block accidental back press while an actual alarm is ringing
    BackHandler(enabled = !practiceMode && !isAllSolved && !isTestSimulation) {
        backNotice = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            delay(3500)
            backNotice = false
        }
    }

    val progressTarget = if (isAllSolved) 1f else (currentStep - 1).toFloat() / totalSteps.coerceAtLeast(1)
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = if (reducedMotion) tween(0) else spring(stiffness = Spring.StiffnessMediumLow),
        label = "step_progress"
    )

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
    val elapsedText = String.format(Locale.getDefault(), "%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60)
    val nextDelay = AlarmScheduler.reringDelayMinutes(rerings)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: elapsed timer, mode badge, and exit button (only in Practice / Simulation)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = elapsedText,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                when {
                    practiceMode -> TextButton(onClick = onDone) { Text("Exit Dojo") }
                    isTestSimulation -> IconButton(onClick = onDone) {
                        Icon(Icons.Default.Close, contentDescription = "Stop test alarm")
                    }
                    else -> Text(
                        text = "Step ${currentStep.coerceAtMost(totalSteps)} of $totalSteps",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Multi-step progress bar
            if (totalSteps > 1) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
                Spacer(Modifier.height(12.dp))
            }

            // Collapsible hero clock + aura (collapses neatly when keyboard opens so question stays above fold)
            AnimatedVisibility(
                visible = !imeVisible || isAllSolved,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (rerings > 0) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    "Escalated re-ring #$rerings — max volume",
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                        Spacer(Modifier.height(6.dp))
                    } else if (wakeCheck) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "5-minute anti-relapse check — confirm you are up",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    } else if (practiceMode) {
                        Text(
                            text = "BRAIN WARM-UP DOJO",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(if (practiceMode) 110.dp else 184.dp)
                    ) {
                        if (!isAllSolved) {
                            BreathingAuraRing(
                                primaryColor = if (rerings > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                accentColor = MaterialTheme.colorScheme.tertiary,
                                urgent = rerings > 0,
                                reducedMotion = reducedMotion,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (practiceMode) "Dojo" else alarm?.formattedTime ?: "Test",
                                style = if (practiceMode) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            if (!practiceMode && alarm != null) {
                                Text(
                                    text = alarm.amPm,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = "Goal: $goal",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                    val promptText = alarm?.wakePrompt?.takeIf { it.isNotBlank() }
                    if (promptText != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = promptText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // Compact inline clock header when IME is open
            AnimatedVisibility(visible = imeVisible && !isAllSolved) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (practiceMode) "Brain Warm-Up Dojo" else "$timeLabel  •  $goal",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Main Challenge OR Victory state
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
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isLoadingChallenge || challenge == null) {
                                Spacer(Modifier.height(16.dp))
                                CircularProgressIndicator(modifier = Modifier.size(36.dp))
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Preparing your cognitive challenge...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(16.dp))
                            } else {
                                // Challenge metadata header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (challenge.source == ChallengeSource.AI)
                                                Icons.Default.AutoAwesome else Icons.Default.Memory,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (challenge.source == ChallengeSource.AI)
                                                "AI Coach" else challenge.category.displayName,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = challenge.difficulty.displayName,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                Text(
                                    text = challenge.question,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

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
                                    // 2x2 large 56dp M3 tap buttons for Stroop
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
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(54.dp)
                                                            .semantics { contentDescription = "Answer $option" }
                                                    ) {
                                                        Text(
                                                            text = option,
                                                            style = MaterialTheme.typography.titleSmall
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
                                        modifier = Modifier.fillMaxWidth().height(52.dp)
                                    ) {
                                        Text("Submit answer", style = MaterialTheme.typography.titleMedium)
                                    }
                                }

                                if (challenge.hint.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    if (showHint) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.tertiaryContainer,
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = challenge.hint,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.padding(12.dp)
                                            )
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
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
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

            if (!practiceMode && !isAllSolved) {
                Text(
                    text = if (isTestSimulation)
                        "Test simulation — volume is forced to max, hardware keys are locked."
                    else
                        "Closing without solving re-rings in $nextDelay minutes at harder difficulty.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun VictoryCard(
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
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
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
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Solved in ${elapsedSeconds}s" +
                    if (wrongAttempts == 0) " • Flawless!" else " • $wrongAttempts retry(s)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "LOCKED IN FOR TODAY",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = goal,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
            if (wakeCheckArmed) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "5-minute anti-relapse check armed — stay out of bed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(20.dp))
            if (!practiceMode && onLaunchFocusSprint != null) {
                FilledTonalButton(
                    onClick = onLaunchFocusSprint,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Launch 25m Focus Sprint")
                }
                Spacer(Modifier.height(10.dp))
            }
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(
                    text = if (practiceMode) "Back to Insights" else "Start my day",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
