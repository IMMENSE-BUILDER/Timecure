package com.timecure.app.ui.screens.missions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.timecure.app.model.StepUnit
import com.timecure.app.model.WakeMissionCatalog
import com.timecure.app.model.WakeMissionConfig
import com.timecure.app.model.WakeMissionType
import com.timecure.app.util.Permissions
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MissionConfigDialog(
    initialConfig: WakeMissionConfig,
    goal: String,
    onPreview: (WakeMissionConfig) -> Unit,
    onSave: (WakeMissionConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var difficultyLevel by remember(initialConfig) { mutableIntStateOf(initialConfig.difficultyLevel) }
    var rounds by remember(initialConfig) { mutableIntStateOf(initialConfig.rounds) }
    var stepTarget by remember(initialConfig) { mutableIntStateOf(initialConfig.stepTarget) }
    var stepUnit by remember(initialConfig) { mutableStateOf(initialConfig.stepUnit) }
    var selectedItems by remember(initialConfig) { mutableStateOf(initialConfig.selectedItems) }
    var selectedTopics by remember(initialConfig) { mutableStateOf(initialConfig.selectedTopics) }
    var useAiPhrase by remember(initialConfig) { mutableStateOf(initialConfig.useAiPhrase) }

    var showItemsPicker by remember { mutableStateOf(false) }
    var showTopicsPicker by remember { mutableStateOf(false) }
    var permNonce by remember { mutableIntStateOf(0) }

    val requiredPermission: String? = when (initialConfig.type) {
        WakeMissionType.ITEM_HUNT -> Manifest.permission.CAMERA
        WakeMissionType.SAY_WORD_ON_BEAT -> Manifest.permission.RECORD_AUDIO
        WakeMissionType.STEP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Manifest.permission.ACTIVITY_RECOGNITION
        } else null
        else -> null
    }

    val hasRequiredPermission = remember(permNonce, initialConfig.type) {
        when (initialConfig.type) {
            WakeMissionType.ITEM_HUNT -> Permissions.hasCamera(context)
            WakeMissionType.SAY_WORD_ON_BEAT -> Permissions.hasRecordAudio(context)
            WakeMissionType.STEP -> Permissions.hasActivityRecognition(context)
            else -> true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permNonce++
        if (granted) {
            onSave(
                WakeMissionConfig(
                    type = initialConfig.type,
                    difficultyLevel = difficultyLevel,
                    rounds = rounds,
                    stepTarget = stepTarget,
                    stepUnit = stepUnit,
                    selectedItems = selectedItems,
                    selectedTopics = selectedTopics,
                    useAiPhrase = useAiPhrase
                )
            )
        }
    }

    fun buildCurrentConfig() = WakeMissionConfig(
        type = initialConfig.type,
        difficultyLevel = difficultyLevel,
        rounds = rounds,
        stepTarget = stepTarget,
        stepUnit = stepUnit,
        selectedItems = selectedItems.ifEmpty { WakeMissionCatalog.DEFAULT_HOUSEHOLD_ITEMS },
        selectedTopics = selectedTopics.ifEmpty { WakeMissionCatalog.DEFAULT_BEAT_TOPICS },
        useAiPhrase = useAiPhrase
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = initialConfig.type.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 470.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (initialConfig.type) {
                        WakeMissionType.ITEM_HUNT -> {
                            Text(
                                text = "Find the item on screen to turn off the alarm",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Surface(
                                color = Color(0xFF151920),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth().height(168.dp)
                            ) {
                                Box(modifier = Modifier.padding(14.dp)) {
                                    Column(
                                        modifier = Modifier.align(Alignment.Center),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Surface(
                                            color = Color(0xFFEBC844),
                                            contentColor = Color(0xFF1A1916),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Cup",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                            )
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Surface(
                                            color = Color(0xFF39C16C).copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(2.dp, Color(0xFF39C16C))
                                        ) {
                                            Text(
                                                text = "AI Vision Scan Verified",
                                                color = Color(0xFF8FF5B0),
                                                style = MaterialTheme.typography.labelMedium,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                    if (!hasRequiredPermission) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = CircleShape,
                                            modifier = Modifier.align(Alignment.BottomStart)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Camera needed", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                            Text(
                                text = "Don't have it? Pick again for a new item!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Surface(
                                onClick = { showItemsPicker = true },
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Select items", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                    Text(
                                        "${selectedItems.size} items",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            RoundsSelectorCard(
                                label = "items",
                                rounds = rounds,
                                maxRounds = 3,
                                onChange = { rounds = it }
                            )
                        }

                        WakeMissionType.SAY_WORD_ON_BEAT -> {
                            Text(
                                text = "Say the word on beat to turn off the alarm",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Surface(
                                color = Color(0xFF172755),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.padding(14.dp)) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 26.dp)
                                    ) {
                                        Text(
                                            text = "Say the words on beat",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val sampleWords = listOf("wake", "up", "right", "now", "do", "not", "be", "late")
                                        sampleWords.chunked(4).forEachIndexed { rowIdx, chunk ->
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                chunk.forEachIndexed { colIdx, w ->
                                                    val lit = rowIdx == 0 && colIdx == 1
                                                    Surface(
                                                        color = if (lit) Color(0xFFD7D7D7) else Color(0xFF28438A),
                                                        contentColor = if (lit) Color(0xFF121826) else Color.White,
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text(
                                                            text = w,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (!hasRequiredPermission) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = CircleShape,
                                            modifier = Modifier.align(Alignment.BottomStart)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Microphone needed", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "This mission plays sound effects",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                onClick = { showTopicsPicker = true },
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Select topics", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                    Text(
                                        "${selectedTopics.size} topics",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            RoundsSelectorCard(
                                label = "times",
                                rounds = rounds,
                                maxRounds = 3,
                                onChange = { rounds = it }
                            )
                        }

                        WakeMissionType.FIND_COLOR_TILES -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ExampleBadge()
                                Spacer(Modifier.height(10.dp))
                                val highlighted = remember { setOf(0, 5, 10, 13, 15) }
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    for (r in 0 until 4) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                            for (c in 0 until 4) {
                                                val idx = r * 4 + c
                                                Surface(
                                                    color = if (idx in highlighted) Color(0xFFF3AA19)
                                                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.size(28.dp)
                                                ) {}
                                            }
                                        }
                                    }
                                }
                            }
                            DifficultySliderCard(
                                level = difficultyLevel,
                                minLabel = "Easy",
                                maxLabel = "Hard",
                                labelFor = { l -> WakeMissionCatalog.DIFFICULTY_LABELS.getOrElse(l) { "Easy" } },
                                onChange = { difficultyLevel = it }
                            )
                            RoundsSelectorCard(
                                label = "rounds",
                                rounds = rounds,
                                maxRounds = 6,
                                onChange = { rounds = it }
                            )
                        }

                        WakeMissionType.TYPING -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ExampleBadge()
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "\"${WakeMissionCatalog.pickOfflineTypingPhrase(difficultyLevel, goal)}\"",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("AI-generated phrase", style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            text = "Customizes phrase to your morning goal",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(checked = useAiPhrase, onCheckedChange = { useAiPhrase = it })
                                }
                            }
                            DifficultySliderCard(
                                level = difficultyLevel,
                                minLabel = "Short",
                                maxLabel = "Long",
                                labelFor = { l -> WakeMissionCatalog.DIFFICULTY_LABELS.getOrElse(l) { "Easy" } },
                                onChange = { difficultyLevel = it }
                            )
                            RoundsSelectorCard(
                                label = "times",
                                rounds = rounds,
                                maxRounds = 3,
                                onChange = { rounds = it }
                            )
                        }

                        WakeMissionType.MATH -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ExampleBadge()
                                val sampleEq = when (difficultyLevel) {
                                    0 -> "2 + 9 ="
                                    1 -> "18 + 27 ="
                                    2 -> "(14 × 3) + 8 ="
                                    3 -> "(23 × 7) - 19 ="
                                    else -> "(18 × 12) + 47 ="
                                }
                                Text(
                                    text = sampleEq,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("20", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("30", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
                                    }
                                    Surface(
                                        color = Color(0xFF19A95B),
                                        contentColor = Color.White,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                            DifficultySliderCard(
                                level = difficultyLevel,
                                minLabel = "Easy",
                                maxLabel = "Hard",
                                labelFor = { l -> WakeMissionCatalog.DIFFICULTY_LABELS.getOrElse(l) { "Very easy" } },
                                onChange = { difficultyLevel = it }
                            )
                            RoundsSelectorCard(
                                label = "times",
                                rounds = rounds,
                                maxRounds = 10,
                                onChange = { rounds = it }
                            )
                        }

                        WakeMissionType.STEP -> {
                            Text(
                                text = "Step out of bed and get walking — it really works!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.DirectionsWalk,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(42.dp)
                                    )
                                    Text(
                                        text = "14   13   12",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 4.sp
                                    )
                                    Text(
                                        text = if (stepUnit == StepUnit.STEPS)
                                            "Requires $stepTarget physical steps to turn off alarm"
                                        else
                                            "Requires ~${WakeMissionCatalog.stepsRequiredFor(stepTarget, stepUnit)} steps (${stepTarget}m walk) to turn off alarm",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                val units = StepUnit.entries
                                units.forEachIndexed { idx, u ->
                                    SegmentedButton(
                                        selected = stepUnit == u,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            stepUnit = u
                                            stepTarget = if (u == StepUnit.STEPS) 20 else 100
                                        },
                                        shape = SegmentedButtonDefaults.itemShape(idx, units.size)
                                    ) { Text(u.label) }
                                }
                            }

                            val presets = if (stepUnit == StepUnit.STEPS) {
                                listOf(10, 20, 30, 40, 50, 80, 100)
                            } else {
                                listOf(30, 50, 100, 150, 200)
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                presets.forEach { preset ->
                                    FilterChip(
                                        selected = stepTarget == preset,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            stepTarget = preset
                                        },
                                        label = { Text("$preset ${if (stepUnit == StepUnit.STEPS) "steps" else "m"}") }
                                    )
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            val dec = if (stepUnit == StepUnit.STEPS) 5 else 10
                                            stepTarget = (stepTarget - dec).coerceAtLeast(10)
                                        }
                                    ) { Icon(Icons.Default.Remove, contentDescription = "Decrease") }
                                    Text(
                                        text = "$stepTarget ${stepUnit.label}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = {
                                            val inc = if (stepUnit == StepUnit.STEPS) 5 else 10
                                            stepTarget = (stepTarget + inc).coerceAtMost(500)
                                        }
                                    ) { Icon(Icons.Default.Add, contentDescription = "Increase") }
                                }
                            }
                        }
                    }
                }

                // Bottom Preview + Complete / Allow Permission Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPreview(buildCurrentConfig())
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("Preview", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (!hasRequiredPermission && requiredPermission != null) {
                                permissionLauncher.launch(requiredPermission)
                            } else {
                                onSave(buildCurrentConfig())
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1.5f).height(50.dp)
                    ) {
                        Text(
                            text = if (!hasRequiredPermission && requiredPermission != null) "Allow permission"
                            else "Complete",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showItemsPicker) {
        MultiSelectChecklistDialog(
            title = "Select Household Items",
            allOptions = WakeMissionCatalog.DEFAULT_HOUSEHOLD_ITEMS,
            selected = selectedItems,
            onConfirm = {
                selectedItems = it.ifEmpty { listOf("Cup") }
                showItemsPicker = false
            },
            onDismiss = { showItemsPicker = false }
        )
    }

    if (showTopicsPicker) {
        MultiSelectChecklistDialog(
            title = "Select Beat Topics",
            allOptions = WakeMissionCatalog.DEFAULT_BEAT_TOPICS,
            selected = selectedTopics,
            onConfirm = {
                selectedTopics = it.ifEmpty { listOf("Morning Motivation") }
                showTopicsPicker = false
            },
            onDismiss = { showTopicsPicker = false }
        )
    }
}

@Composable
private fun ExampleBadge() {
    Surface(
        color = Color(0xFF0A6BFF),
        contentColor = Color.White,
        shape = CircleShape
    ) {
        Text(
            text = "Example",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun DifficultySliderCard(
    level: Int,
    minLabel: String,
    maxLabel: String,
    labelFor: (Int) -> String,
    onChange: (Int) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = labelFor(level),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Slider(
                value = level.toFloat(),
                onValueChange = { onChange(it.roundToInt().coerceIn(0, 4)) },
                valueRange = 0f..4f,
                steps = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(minLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(maxLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RoundsSelectorCard(
    label: String,
    rounds: Int,
    maxRounds: Int,
    onChange: (Int) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onChange((rounds - 1).coerceAtLeast(1)) }) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease count")
            }
            Text(
                text = "$rounds  $label",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { onChange((rounds + 1).coerceAtMost(maxRounds)) }) {
                Icon(Icons.Default.Add, contentDescription = "Increase count")
            }
        }
    }
}

@Composable
private fun MultiSelectChecklistDialog(
    title: String,
    allOptions: List<String>,
    selected: List<String>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember(selected) { mutableStateOf(selected.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                TextButton(
                    onClick = {
                        draft = if (draft.size == allOptions.size) emptySet() else allOptions.toSet()
                    }
                ) {
                    Text(if (draft.size == allOptions.size) "Clear" else "Select all")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                allOptions.forEachIndexed { idx, opt ->
                    val checked = opt in draft
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                draft = if (checked) draft - opt else draft + opt
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                draft = if (checked) draft - opt else draft + opt
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(opt, style = MaterialTheme.typography.bodyLarge)
                    }
                    if (idx < allOptions.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(allOptions.filter { it in draft }) },
                enabled = draft.isNotEmpty()
            ) {
                Text("Save (${draft.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
