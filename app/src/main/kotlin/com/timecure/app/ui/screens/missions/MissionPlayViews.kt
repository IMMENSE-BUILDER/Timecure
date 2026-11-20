package com.timecure.app.ui.screens.missions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.timecure.app.TimecureApp
import com.timecure.app.alarm.ItemVerifyResult
import com.timecure.app.alarm.MissionAiHelper
import com.timecure.app.model.StepUnit
import com.timecure.app.model.WakeMissionCatalog
import com.timecure.app.model.WakeMissionConfig
import com.timecure.app.model.WakeMissionType
import com.timecure.app.util.ImageEncoder
import com.timecure.app.util.Permissions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

@Composable
fun MissionPreviewDialog(
    config: WakeMissionConfig,
    goal: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Preview • ${config.type.displayName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close preview")
                    }
                }

                MissionPlayView(
                    config = config,
                    goal = goal,
                    isPreview = true,
                    onMissionComplete = onDismiss
                )
            }
        }
    }
}

@Composable
fun MissionPlayView(
    config: WakeMissionConfig,
    goal: String,
    isPreview: Boolean,
    onMissionComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        when (config.type) {
            WakeMissionType.ITEM_HUNT -> ItemHuntPlayView(config, onMissionComplete)
            WakeMissionType.SAY_WORD_ON_BEAT -> SayWordOnBeatPlayView(config, onMissionComplete)
            WakeMissionType.FIND_COLOR_TILES -> FindColorTilesPlayView(config, onMissionComplete)
            WakeMissionType.TYPING -> TypingMissionPlayView(config, goal, onMissionComplete)
            WakeMissionType.MATH -> MathMissionPlayView(config, onMissionComplete)
            WakeMissionType.STEP -> StepMissionPlayView(config, isPreview, onMissionComplete)
        }
    }
}

// =========================================================================
// 1. HOUSEHOLD ITEM HUNT (AI Vision)
// =========================================================================

@Composable
private fun ItemHuntPlayView(
    config: WakeMissionConfig,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val itemPool = remember(config.selectedItems) {
        config.selectedItems.ifEmpty { WakeMissionCatalog.DEFAULT_HOUSEHOLD_ITEMS }
    }
    var currentRound by remember { mutableIntStateOf(1) }
    val totalRounds = config.rounds.coerceIn(1, 3)

    var targetItem by remember(config) { mutableStateOf(itemPool.random()) }
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var verifyResult by remember { mutableStateOf<ItemVerifyResult?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { saved ->
        val uri = pendingCaptureUri
        if (saved && uri != null) {
            isVerifying = true
            verifyResult = null
            scope.launch {
                val res = MissionAiHelper.verifyHouseholdItem(context, targetItem, uri)
                isVerifying = false
                verifyResult = res
                if (res.found) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    delay(900L)
                    if (currentRound >= totalRounds) {
                        onComplete()
                    } else {
                        currentRound++
                        verifyResult = null
                        targetItem = itemPool.filter { it != targetItem }.ifEmpty { itemPool }.random()
                    }
                }
            }
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = ImageEncoder.createCameraCaptureUri(context)
            pendingCaptureUri = uri
            cameraLauncher.launch(uri)
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "HOUSEHOLD ITEM HUNT • $currentRound / $totalRounds",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Find & scan:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = targetItem,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                )
            }

            if (isVerifying) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                Text("AI vision checking photo for $targetItem...", style = MaterialTheme.typography.bodySmall)
            }

            verifyResult?.let { res ->
                Surface(
                    color = if (res.found) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (res.found) "Verified: ${res.detected}" else "Saw: ${res.detected}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(res.reason, style = MaterialTheme.typography.bodySmall)
                        if (res.offlineFallback) {
                            Spacer(Modifier.height(6.dp))
                            TextButton(
                                onClick = {
                                    if (currentRound >= totalRounds) onComplete()
                                    else {
                                        currentRound++
                                        verifyResult = null
                                        targetItem = itemPool.random()
                                    }
                                }
                            ) {
                                Text("Confirm manually offline")
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (Permissions.hasCamera(context)) {
                        val uri = ImageEncoder.createCameraCaptureUri(context)
                        pendingCaptureUri = uri
                        cameraLauncher.launch(uri)
                    } else {
                        permLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                enabled = !isVerifying,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Scan Item with Camera")
            }

            TextButton(
                onClick = {
                    verifyResult = null
                    targetItem = itemPool.filter { it != targetItem }.ifEmpty { itemPool }.random()
                },
                enabled = !isVerifying
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Don't have it? Pick another item")
            }
        }
    }
}

// =========================================================================
// 2. SAY THE WORD ON BEAT (Microphone + Synchronized Beat)
// =========================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SayWordOnBeatPlayView(
    config: WakeMissionConfig,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val topics = remember(config.selectedTopics) {
        config.selectedTopics.ifEmpty { WakeMissionCatalog.DEFAULT_BEAT_TOPICS }
    }
    var currentRound by remember { mutableIntStateOf(1) }
    val totalRounds = config.rounds.coerceIn(1, 3)

    val wordList = remember(currentRound, topics) {
        val topic = topics[(currentRound - 1) % topics.size]
        WakeMissionCatalog.BEAT_WORD_SETS[topic]
            ?: listOf("wake", "up", "right", "now", "do", "not", "be", "late")
    }

    var matchedIndices by remember(currentRound, wordList) { mutableStateOf(emptySet<Int>()) }
    var beatCursor by remember(currentRound, wordList) { mutableIntStateOf(0) }
    var heardText by remember { mutableStateOf("") }
    var hasMic by remember { mutableStateOf(Permissions.hasRecordAudio(context)) }
    val recognitionAvailable = remember { SpeechRecognizer.isRecognitionAvailable(context) }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasMic = granted }

    val toneGen = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 65) }.getOrNull()
    }
    DisposableEffect(Unit) {
        onDispose { runCatching { toneGen?.release() } }
    }

    // Metronome beat loop highlighting each word cleanly
    LaunchedEffect(currentRound, wordList) {
        while (true) {
            delay(750L)
            beatCursor = (beatCursor + 1) % wordList.size
            runCatching { toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 80) }
        }
    }

    // Continuous SpeechRecognizer listener when Mic permission & service are available
    DisposableEffect(hasMic, recognitionAvailable, currentRound) {
        if (!hasMic || !recognitionAvailable) {
            return@DisposableEffect onDispose {}
        }
        val recognizer = runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull()
            ?: return@DisposableEffect onDispose {}
        var disposed = false

        fun startListening() {
            if (disposed) return
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            runCatching { recognizer.startListening(intent) }
        }

        fun consumeBundle(bundle: Bundle?) {
            val strings = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            if (strings.isNotEmpty()) {
                val combined = strings.joinToString(" ")
                heardText = strings.first()
                val updated = WakeMissionCatalog.matchSpokenWords(wordList, matchedIndices, combined)
                if (updated != matchedIndices) {
                    matchedIndices = updated
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                if (!disposed) startListening()
            }
            override fun onResults(results: Bundle?) {
                consumeBundle(results)
                if (!disposed) startListening()
            }
            override fun onPartialResults(partialResults: Bundle?) {
                consumeBundle(partialResults)
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        startListening()
        onDispose {
            disposed = true
            runCatching {
                recognizer.stopListening()
                recognizer.destroy()
            }
        }
    }

    // Check round completion whenever matchedIndices updates
    LaunchedEffect(matchedIndices, wordList) {
        val threshold = (wordList.size * 0.75f).toInt().coerceAtLeast(wordList.size - 1)
        if (matchedIndices.size >= threshold) {
            delay(500L)
            if (currentRound >= totalRounds) {
                onComplete()
            } else {
                currentRound++
            }
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "SAY WORDS ON BEAT • $currentRound / $totalRounds",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            if (!hasMic) {
                Button(
                    onClick = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Allow Microphone to Speak")
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                wordList.forEachIndexed { idx, word ->
                    val matched = idx in matchedIndices
                    val onBeat = idx == beatCursor
                    val bg by animateColorAsState(
                        targetValue = when {
                            matched -> Color(0xFF19A95B)
                            onBeat -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        label = "beat_tile"
                    )
                    Surface(
                        onClick = {
                            if (!recognitionAvailable) {
                                // Accessibility fallback ONLY when device has no SpeechRecognizer
                                matchedIndices = matchedIndices + idx
                            }
                        },
                        color = bg,
                        contentColor = if (matched || onBeat) Color.White else MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(10.dp),
                        border = if (onBeat) BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary) else null
                    ) {
                        Text(
                            text = word,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            if (heardText.isNotBlank()) {
                Text(
                    text = "Heard: \"$heardText\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = if (recognitionAvailable) "Speak each highlighted word aloud (${matchedIndices.size}/${wordList.size})"
                    else "Speech engine unavailable — tap each word on beat to clear",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// =========================================================================
// 3. FIND COLOR TILES (Memory Grid)
// =========================================================================

@Composable
private fun FindColorTilesPlayView(
    config: WakeMissionConfig,
    onComplete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val totalRounds = config.rounds.coerceIn(1, 6)
    var currentRound by remember { mutableIntStateOf(1) }

    val gridDim = when {
        config.difficultyLevel <= 1 -> 3
        config.difficultyLevel <= 3 -> 4
        else -> 5
    }
    val targetCount = (gridDim + 1 + config.difficultyLevel / 2).coerceAtMost(gridDim * gridDim / 2)

    val targets = remember(currentRound, gridDim, targetCount) {
        (0 until gridDim * gridDim).shuffled(Random(System.nanoTime())).take(targetCount).toSet()
    }
    var isMemorizePhase by remember(currentRound) { mutableStateOf(true) }
    var foundTiles by remember(currentRound) { mutableStateOf(emptySet<Int>()) }
    var wrongTile by remember(currentRound) { mutableStateOf<Int?>(null) }

    LaunchedEffect(currentRound) {
        isMemorizePhase = true
        delay(2_200L)
        isMemorizePhase = false
    }

    LaunchedEffect(wrongTile) {
        if (wrongTile != null) {
            delay(450L)
            wrongTile = null
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "FIND COLOR TILES • ROUND $currentRound / $totalRounds",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (isMemorizePhase) "Memorize the golden tiles!"
                else "Tap the ${targets.size} remembered tiles (${foundTiles.size}/${targets.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (r in 0 until gridDim) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (c in 0 until gridDim) {
                            val idx = r * gridDim + c
                            val isLit = (isMemorizePhase && idx in targets) || (idx in foundTiles)
                            val isWrong = wrongTile == idx
                            val color = when {
                                isWrong -> MaterialTheme.colorScheme.error
                                isLit -> Color(0xFFF3AA19)
                                else -> MaterialTheme.colorScheme.surfaceContainerHighest
                            }
                            Surface(
                                onClick = {
                                    if (isMemorizePhase || idx in foundTiles) return@Surface
                                    if (idx in targets) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val next = foundTiles + idx
                                        foundTiles = next
                                        if (next.size == targets.size) {
                                            if (currentRound >= totalRounds) onComplete()
                                            else currentRound++
                                        }
                                    } else {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        wrongTile = idx
                                    }
                                },
                                color = color,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(if (gridDim <= 4) 56.dp else 46.dp)
                            ) {}
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 4. TYPING MISSION (AI or Curated Motivational Sentence)
// =========================================================================

@Composable
private fun TypingMissionPlayView(
    config: WakeMissionConfig,
    goal: String,
    onComplete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val totalRounds = config.rounds.coerceIn(1, 3)
    var currentRound by remember { mutableIntStateOf(1) }
    var targetPhrase by remember(currentRound) {
        mutableStateOf(WakeMissionCatalog.pickOfflineTypingPhrase(config.difficultyLevel, goal))
    }
    var typed by remember(currentRound) { mutableStateOf("") }
    var pasteNotice by remember(currentRound) { mutableStateOf(false) }

    LaunchedEffect(currentRound, config.useAiPhrase) {
        if (config.useAiPhrase) {
            val aiPhrase = MissionAiHelper.generateTypingPhrase(config.difficultyLevel, goal)
            if (typed.isEmpty()) targetPhrase = aiPhrase
        }
    }

    val annotatedPrompt = remember(targetPhrase, typed) {
        buildAnnotatedString {
            targetPhrase.forEachIndexed { i, ch ->
                val color = when {
                    i >= typed.length -> Color.Unspecified
                    typed[i] == ch -> Color(0xFF19A95B)
                    else -> Color(0xFFE53935)
                }
                withStyle(SpanStyle(color = color, fontWeight = if (i < typed.length) FontWeight.Bold else FontWeight.Normal)) {
                    append(ch)
                }
            }
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "TYPING MISSION • $currentRound / $totalRounds",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = annotatedPrompt,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(14.dp)
                )
            }
            OutlinedTextField(
                value = typed,
                onValueChange = { incoming ->
                    // Prevent multi-char copy-paste shortcuts
                    if (abs(incoming.length - typed.length) > 3) {
                        pasteNotice = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } else {
                        pasteNotice = false
                        typed = incoming
                        if (incoming.trim().equals(targetPhrase.trim(), ignoreCase = false)) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (currentRound >= totalRounds) onComplete()
                            else currentRound++
                        }
                    }
                },
                label = { Text("Type exact sentence above") },
                supportingText = {
                    if (pasteNotice) Text("No copy-pasting — type manually!", color = MaterialTheme.colorScheme.error)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// =========================================================================
// 5. MATH MISSION (5 Customizable Difficulty Tiers)
// =========================================================================

@Composable
private fun MathMissionPlayView(
    config: WakeMissionConfig,
    onComplete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val totalRounds = config.rounds.coerceIn(1, 10)
    var currentRound by remember { mutableIntStateOf(1) }

    val problem = remember(currentRound, config.difficultyLevel) {
        generateMathProblem(config.difficultyLevel)
    }
    var typedAns by remember(currentRound) { mutableStateOf("") }
    var isError by remember(currentRound) { mutableStateOf(false) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "MATH TEST • $currentRound / $totalRounds (${config.difficultyLabel()})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = problem.expression,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )

            if (config.difficultyLevel <= 1) {
                // Quick-tap buttons for Very Easy & Easy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    problem.choices.forEach { ch ->
                        FilledTonalButton(
                            onClick = {
                                if (ch == problem.answer) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isError = false
                                    if (currentRound >= totalRounds) onComplete()
                                    else currentRound++
                                } else {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isError = true
                                }
                            },
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) {
                            Text("$ch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = typedAns,
                    onValueChange = {
                        typedAns = it
                        isError = false
                    },
                    label = { Text("Answer") },
                    isError = isError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (typedAns.trim().toIntOrNull() == problem.answer) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (currentRound >= totalRounds) onComplete()
                                else currentRound++
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isError = true
                            }
                        }
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (typedAns.trim().toIntOrNull() == problem.answer) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (currentRound >= totalRounds) onComplete()
                            else currentRound++
                        } else {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isError = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Submit Answer")
                }
            }

            if (isError) {
                Text("Try again — check your math!", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private data class MathProblem(val expression: String, val answer: Int, val choices: List<Int>)

private fun generateMathProblem(level: Int): MathProblem {
    val rng = Random(System.nanoTime())
    val (expr, ans) = when (level) {
        0 -> {
            val a = rng.nextInt(2, 10)
            val b = rng.nextInt(2, 10)
            "$a + $b =" to (a + b)
        }
        1 -> {
            val a = rng.nextInt(12, 45)
            val b = rng.nextInt(11, 45)
            "$a + $b =" to (a + b)
        }
        2 -> {
            val a = rng.nextInt(6, 15)
            val b = rng.nextInt(3, 9)
            val c = rng.nextInt(5, 22)
            "($a × $b) + $c =" to (a * b + c)
        }
        3 -> {
            val a = rng.nextInt(11, 25)
            val b = rng.nextInt(4, 12)
            val c = rng.nextInt(9, 30)
            "($a × $b) - $c =" to (a * b - c)
        }
        else -> {
            val a = rng.nextInt(12, 29)
            val b = rng.nextInt(7, 15)
            val c = rng.nextInt(18, 65)
            "($a × $b) + $c =" to (a * b + c)
        }
    }
    val distractors = mutableSetOf(ans)
    while (distractors.size < 3) {
        val delta = rng.nextInt(1, 10) * if (rng.nextBoolean()) 1 else -1
        if (ans + delta >= 0) distractors += (ans + delta)
    }
    return MathProblem(expr, ans, distractors.shuffled(rng))
}

// =========================================================================
// 6. STEP MISSION (Hardware Step Detector + Accelerometer Peak Fallback)
// =========================================================================

@Composable
private fun StepMissionPlayView(
    config: WakeMissionConfig,
    isPreview: Boolean,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val requiredSteps = remember(config.stepTarget, config.stepUnit) {
        WakeMissionCatalog.stepsRequiredFor(config.stepTarget, config.stepUnit)
    }
    var stepsTaken by remember(config) { mutableIntStateOf(0) }
    var completed by remember(config) { mutableStateOf(false) }

    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest

    fun recordStep() {
        if (completed) return
        stepsTaken++
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (stepsTaken >= requiredSteps) {
            completed = true
            onComplete()
        }
    }

    DisposableEffect(config) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepSensor = if (Permissions.hasActivityRecognition(context)) {
            sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        } else null
        val accelSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        var lastStepMs = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val now = System.currentTimeMillis()
                if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                    if (now - lastStepMs >= 260L) {
                        lastStepMs = now
                        recordStep()
                    }
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val mag = sqrt(x * x + y * y + z * z)
                    // Normal standing gravity ~9.81 m/s^2; genuine walking stride peaks > 12.1 m/s^2
                    if (mag > 12.1f && now - lastStepMs >= 380L) {
                        lastStepMs = now
                        recordStep()
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (stepSensor != null) {
            sm.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_UI)
        } else if (accelSensor != null) {
            sm.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sm.unregisterListener(listener)
        }
    }

    val remainingSteps = (requiredSteps - stepsTaken).coerceAtLeast(0)
    val progress = (stepsTaken.toFloat() / requiredSteps.coerceAtLeast(1)).coerceIn(0f, 1f)
    val metersWalked = (stepsTaken / 1.35f).toInt()

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "STEP MISSION",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (config.stepUnit == StepUnit.STEPS)
                    "Take $requiredSteps steps out of bed"
                else
                    "Walk ${config.stepTarget} meters ($requiredSteps steps)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier.size(164.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f - 10.dp.toPx()
                    val stroke = 11.dp.toPx()
                    val tl = Offset(size.width / 2f - r, size.height / 2f - r)
                    drawCircle(color = track, radius = r, style = Stroke(stroke))
                    drawArc(
                        color = primary,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = tl,
                        size = Size(r * 2f, r * 2f),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = "$remainingSteps",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Text(
                        text = "steps left",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp)
            )
            Text(
                text = "$stepsTaken / $requiredSteps steps (~$metersWalked m walked)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isPreview) {
                OutlinedButton(onClick = { recordStep() }) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Simulate +1 step (Preview)")
                }
            }
        }
    }
}
