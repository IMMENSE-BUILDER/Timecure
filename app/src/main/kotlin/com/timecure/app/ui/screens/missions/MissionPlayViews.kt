package com.timecure.app.ui.screens.missions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.timecure.app.alarm.ItemVerifyResult
import com.timecure.app.alarm.MissionAiHelper
import com.timecure.app.model.StepUnit
import com.timecure.app.model.WakeMissionCatalog
import com.timecure.app.model.WakeMissionConfig
import com.timecure.app.model.WakeMissionType
import com.timecure.app.ui.components.rememberReducedMotion
import com.timecure.app.util.ImageEncoder
import com.timecure.app.util.Permissions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
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
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(30.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "Preview • ${config.type.displayName}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Surface(
                        onClick = onDismiss,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = CircleShape,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Close, contentDescription = "Close preview", modifier = Modifier.size(18.dp))
                        }
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

@Composable
private fun MissionStageCard(
    icon: ImageVector,
    title: String,
    badge: String,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        tonalElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            content()
        }
    }
}

// =========================================================================
// 1. PICTURE SCREEN — HOUSEHOLD ITEM HUNT (Camera Viewfinder + AI Vision)
// =========================================================================

@Composable
private fun ItemHuntPlayView(
    config: WakeMissionConfig,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val reducedMotion = rememberReducedMotion()

    val itemPool = remember(config.selectedItems) {
        config.selectedItems.ifEmpty { WakeMissionCatalog.DEFAULT_HOUSEHOLD_ITEMS }
    }
    var currentRound by remember { mutableIntStateOf(1) }
    val totalRounds = config.rounds.coerceIn(1, 3)

    var targetItem by remember(config) { mutableStateOf(itemPool.random()) }
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    var capturedPreview by remember { mutableStateOf<Bitmap?>(null) }
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
                capturedPreview = ImageEncoder.encodeUri(context, uri, maxDimension = 720)?.previewBitmap
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
                        capturedPreview = null
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

    val scanSweep by if (isVerifying && !reducedMotion) {
        rememberInfiniteTransition(label = "scan_line").animateFloat(
            initialValue = 0.08f,
            targetValue = 0.92f,
            animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "scan_y"
        )
    } else {
        remember { mutableStateOf(0.5f) }
    }

    MissionStageCard(
        icon = Icons.Default.Search,
        title = "Item Hunt",
        badge = "$currentRound / $totalRounds"
    ) {
        // Target object callout banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Find & snap:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Text(
                    text = targetItem,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        // Camera Viewfinder & Picture Preview Frame
        val bracketColor = when {
            verifyResult?.found == true -> Color(0xFF19A95B)
            verifyResult != null -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(216.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
            contentAlignment = Alignment.Center
        ) {
            val preview = capturedPreview
            if (preview != null) {
                Image(
                    bitmap = preview.asImageBitmap(),
                    contentDescription = "Captured photo for $targetItem",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Black.copy(alpha = 0.25f),
                                0.55f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = 0.65f)
                            )
                        )
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(20.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Text(
                        text = "Point camera cleanly at $targetItem",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "AI vision verifies the real object before unlocking",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Corner Viewfinder Brackets + Scanning Line
            Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                val strokePx = 3.dp.toPx()
                val corner = 22.dp.toPx()
                val w = size.width
                val h = size.height
                // Top-left
                drawLine(bracketColor, Offset(0f, corner), Offset(0f, 0f), strokePx, StrokeCap.Round)
                drawLine(bracketColor, Offset(0f, 0f), Offset(corner, 0f), strokePx, StrokeCap.Round)
                // Top-right
                drawLine(bracketColor, Offset(w - corner, 0f), Offset(w, 0f), strokePx, StrokeCap.Round)
                drawLine(bracketColor, Offset(w, 0f), Offset(w, corner), strokePx, StrokeCap.Round)
                // Bottom-left
                drawLine(bracketColor, Offset(0f, h - corner), Offset(0f, h), strokePx, StrokeCap.Round)
                drawLine(bracketColor, Offset(0f, h), Offset(corner, h), strokePx, StrokeCap.Round)
                // Bottom-right
                drawLine(bracketColor, Offset(w - corner, h), Offset(w, h), strokePx, StrokeCap.Round)
                drawLine(bracketColor, Offset(w, h - corner), Offset(w, h), strokePx, StrokeCap.Round)

                if (isVerifying) {
                    val y = h * scanSweep
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, bracketColor, Color.Transparent)
                        ),
                        start = Offset(6.dp.toPx(), y),
                        end = Offset(w - 6.dp.toPx(), y),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            if (isVerifying) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Checking for $targetItem...",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        verifyResult?.let { res ->
            Surface(
                color = if (res.found) Color(0xFF19A95B).copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    if (res.found) Color(0xFF19A95B).copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (res.found) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (res.found) Color(0xFF19A95B) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (res.found) "Verified: ${res.detected}" else "Saw: ${res.detected}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (res.found) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Text(
                        text = res.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (res.found) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.88f)
                    )
                    if (res.offlineFallback) {
                        Spacer(Modifier.height(4.dp))
                        FilledTonalButton(
                            onClick = {
                                if (currentRound >= totalRounds) onComplete()
                                else {
                                    currentRound++
                                    capturedPreview = null
                                    verifyResult = null
                                    targetItem = itemPool.random()
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
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
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (verifyResult != null && verifyResult?.found == false) "Retake Photo"
                else "Scan Item with Camera",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        TextButton(
            onClick = {
                capturedPreview = null
                verifyResult = null
                targetItem = itemPool.filter { it != targetItem }.ifEmpty { itemPool }.random()
            },
            enabled = !isVerifying
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Don't have it? Swap item")
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

    LaunchedEffect(currentRound, wordList) {
        while (true) {
            delay(750L)
            beatCursor = (beatCursor + 1) % wordList.size
            runCatching { toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 80) }
        }
    }

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

    MissionStageCard(
        icon = Icons.Default.Mic,
        title = "Say Words on Beat",
        badge = "Round $currentRound / $totalRounds"
    ) {
        if (!hasMic) {
            Button(
                onClick = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Allow Microphone to Speak", fontWeight = FontWeight.Bold)
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.8f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
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
                    val beatScale by animateFloatAsState(
                        targetValue = if (onBeat && !matched) 1.07f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "beat_scale"
                    )
                    Surface(
                        onClick = {
                            if (!recognitionAvailable) {
                                matchedIndices = matchedIndices + idx
                            }
                        },
                        color = bg,
                        contentColor = if (matched || onBeat) Color.White else MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape,
                        border = if (onBeat) BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary) else null,
                        modifier = Modifier.graphicsLayer {
                            scaleX = beatScale
                            scaleY = beatScale
                        }
                    ) {
                        Text(
                            text = word,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = CircleShape
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (heardText.isNotBlank()) "Heard: \"$heardText\""
                    else if (recognitionAvailable) "Speak highlighted words (${matchedIndices.size}/${wordList.size})"
                    else "Tap each word on beat (${matchedIndices.size}/${wordList.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

    MissionStageCard(
        icon = Icons.Default.GridView,
        title = "Color Memory",
        badge = "Round $currentRound / $totalRounds"
    ) {
        Surface(
            color = if (isMemorizePhase) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = CircleShape
        ) {
            Text(
                text = if (isMemorizePhase) "Memorize golden tiles..."
                else "Tap ${targets.size - foundTiles.size} remaining tile(s)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isMemorizePhase) MaterialTheme.colorScheme.onTertiaryContainer
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (r in 0 until gridDim) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (c in 0 until gridDim) {
                            val idx = r * gridDim + c
                            val isLit = (isMemorizePhase && idx in targets) || (idx in foundTiles)
                            val isWrong = wrongTile == idx
                            val color by animateColorAsState(
                                targetValue = when {
                                    isWrong -> MaterialTheme.colorScheme.error
                                    isLit -> Color(0xFFF3AA19)
                                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                label = "tile_color_$idx"
                            )
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
                                shape = RoundedCornerShape(12.dp),
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

    MissionStageCard(
        icon = Icons.Default.Keyboard,
        title = "Typing Drill",
        badge = "$currentRound / $totalRounds",
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = annotatedPrompt,
                style = MaterialTheme.typography.titleMedium.copy(lineHeight = 26.sp),
                modifier = Modifier.padding(16.dp)
            )
        }

        OutlinedTextField(
            value = typed,
            onValueChange = { incoming ->
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
            shape = RoundedCornerShape(16.dp),
            supportingText = {
                if (pasteNotice) Text("No copy-pasting — type manually!", color = MaterialTheme.colorScheme.error)
            },
            modifier = Modifier.fillMaxWidth()
        )
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
    val scope = rememberCoroutineScope()
    val reducedMotion = rememberReducedMotion()
    val totalRounds = config.rounds.coerceIn(1, 10)
    var currentRound by remember { mutableIntStateOf(1) }

    val problem = remember(currentRound, config.difficultyLevel) {
        generateMathProblem(config.difficultyLevel)
    }
    var typedAns by remember(currentRound) { mutableStateOf("") }
    var isError by remember(currentRound) { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }

    fun onWrongMath() {
        isError = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (!reducedMotion) {
            scope.launch {
                shakeOffset.snapTo(0f)
                listOf(-16f, 16f, -10f, 10f, 0f).forEach { target ->
                    shakeOffset.animateTo(target, tween(45))
                }
            }
        }
    }

    fun submitMath(guess: Int?) {
        if (guess == problem.answer) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            isError = false
            if (currentRound >= totalRounds) onComplete()
            else currentRound++
        } else {
            onWrongMath()
        }
    }

    MissionStageCard(
        icon = Icons.Default.Calculate,
        title = "Math Focus",
        badge = "$currentRound / $totalRounds • ${config.difficultyLabel()}",
        modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = problem.expression,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 22.dp, horizontal = 16.dp)
            )
        }

        if (config.difficultyLevel <= 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                problem.choices.forEach { ch ->
                    FilledTonalButton(
                        onClick = { submitMath(ch) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).height(56.dp)
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
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submitMath(typedAns.trim().toIntOrNull()) }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { submitMath(typedAns.trim().toIntOrNull()) },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Submit Answer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        if (isError) {
            Text(
                "Double-check your math and try again",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
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
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "step_arc"
    )
    val metersWalked = (stepsTaken / 1.35f).toInt()

    MissionStageCard(
        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
        title = "Step Out of Bed",
        badge = "$stepsTaken / $requiredSteps"
    ) {
        Text(
            text = if (config.stepUnit == StepUnit.STEPS)
                "Take $requiredSteps steps across the room"
            else
                "Walk ${config.stepTarget} meters ($requiredSteps steps)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier.size(188.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val outerR = size.minDimension / 2f - 4.dp.toPx()
                val ringR = outerR - 14.dp.toPx()
                val stroke = 11.dp.toPx()

                for (i in 0 until 60) {
                    val angle = (-90.0 + i * 6.0) * PI / 180.0
                    val major = i % 5 == 0
                    val tickLen = if (major) 7.dp.toPx() else 3.5.dp.toPx()
                    val r1 = outerR - tickLen
                    val lit = i <= (animatedProgress * 60).toInt() && animatedProgress > 0.005f
                    drawLine(
                        color = if (lit) primary else track.copy(alpha = 0.7f),
                        start = Offset((cx + r1 * cos(angle)).toFloat(), (cy + r1 * sin(angle)).toFloat()),
                        end = Offset((cx + outerR * cos(angle)).toFloat(), (cy + outerR * sin(angle)).toFloat()),
                        strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                val tl = Offset(cx - ringR, cy - ringR)
                drawCircle(color = track, radius = ringR, style = Stroke(stroke))
                drawArc(
                    color = primary,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = tl,
                    size = Size(ringR * 2f, ringR * 2f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "$remainingSteps",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Text(
                    text = "steps left",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = CircleShape
        ) {
            Text(
                text = "~$metersWalked m walked",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
            )
        }

        if (isPreview) {
            FilledTonalButton(
                onClick = { recordStep() },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Simulate +1 Step (Preview)")
            }
        }
    }
}
