package com.timecure.app.ui.screens.addalarm

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.timecure.app.TimecureApp
import com.timecure.app.data.local.ChallengeGenerator
import com.timecure.app.model.Alarm
import com.timecure.app.model.ChallengeCategory
import com.timecure.app.model.ChallengeDifficulty
import com.timecure.app.ui.components.safeTopBarWindowInsets
import com.timecure.app.util.TimeFormat
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditAlarmScreen(
    initialAlarm: Alarm?,
    defaultGoal: String,
    defaultDifficulty: ChallengeDifficulty,
    onSave: (Alarm) -> Unit,
    onDelete: ((Alarm) -> Unit)? = null,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val soundManager = (context.applicationContext as TimecureApp).container.soundManager

    DisposableEffect(Unit) {
        onDispose { soundManager.stopPreview() }
    }

    val initialCal = remember(initialAlarm) {
        Calendar.getInstance().apply {
            if (initialAlarm != null) {
                set(Calendar.HOUR_OF_DAY, initialAlarm.hour)
                set(Calendar.MINUTE, initialAlarm.minute)
            } else {
                set(Calendar.HOUR_OF_DAY, 7)
                set(Calendar.MINUTE, 0)
            }
        }
    }

    val timeState = rememberTimePickerState(
        initialHour = initialCal.get(Calendar.HOUR_OF_DAY),
        initialMinute = initialCal.get(Calendar.MINUTE),
        is24Hour = false
    )

    var useDialPicker by remember { mutableStateOf(false) }
    var label by remember { mutableStateOf(initialAlarm?.label ?: "") }
    var goal by remember { mutableStateOf(initialAlarm?.goal ?: defaultGoal) }
    var wakePrompt by remember {
        mutableStateOf(
            initialAlarm?.wakePrompt?.takeIf { it.isNotBlank() }
                ?: "Get up for college — show up sharp and on time."
        )
    }
    var selectedDays by remember {
        mutableStateOf(
            initialAlarm?.daysOfWeek ?: setOf(
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                Calendar.THURSDAY, Calendar.FRIDAY
            )
        )
    }
    var difficulty by remember { mutableStateOf(initialAlarm?.difficulty ?: defaultDifficulty) }
    var category by remember { mutableStateOf(initialAlarm?.category ?: ChallengeCategory.MIXED) }
    var questionsRequired by remember { mutableIntStateOf(initialAlarm?.questionsRequired ?: 2) }
    var toneUri by remember { mutableStateOf(initialAlarm?.toneUri ?: "") }
    var toneTitle by remember { mutableStateOf(initialAlarm?.toneTitle ?: "") }
    var toneStartMs by remember { mutableLongStateOf(initialAlarm?.toneStartMs ?: 0L) }
    var toneEndMs by remember { mutableLongStateOf(initialAlarm?.toneEndMs ?: 0L) }
    var vibrate by remember { mutableStateOf(initialAlarm?.vibrate ?: true) }
    var wakeCheckEnabled by remember { mutableStateOf(initialAlarm?.wakeCheckEnabled ?: true) }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var showMp3Sheet by remember { mutableStateOf(false) }
    var sampleSeed by remember { mutableIntStateOf(0) }

    val sampleQuestion = remember(category, difficulty, sampleSeed) {
        ChallengeGenerator.generate(category, difficulty)
    }

    val resolvedToneTitle = remember(toneUri, toneTitle) {
        when {
            toneTitle.isNotBlank() -> toneTitle
            toneUri.isBlank() -> "Default alarm sound"
            else -> runCatching {
                RingtoneManager.getRingtone(context, Uri.parse(toneUri))?.getTitle(context)
            }.getOrNull() ?: "Custom audio track"
        }
    }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            toneUri = uri?.toString().orEmpty()
            toneTitle = uri?.let {
                runCatching { RingtoneManager.getRingtone(context, it)?.getTitle(context) }.getOrNull()
            }.orEmpty()
            toneStartMs = 0L
            toneEndMs = 0L
        }
    }

    val previewAlarm = Alarm(
        hour = timeState.hour,
        minute = timeState.minute,
        daysOfWeek = selectedDays
    )
    val ringsInText = TimeFormat.until(previewAlarm.computeNextTriggerMillis())

    val questionOptions = listOf(1, 2, 3, 5)
    val weekdaysSet = setOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY
    )

    val isExisting = initialAlarm != null && initialAlarm.id > 0L

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                windowInsets = safeTopBarWindowInsets(),
                title = {
                    Column {
                        Text(if (!isExisting) "New alarm" else "Edit alarm")
                        Text(
                            text = "Rings $ringsInText",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { useDialPicker = !useDialPicker }) {
                        Icon(
                            imageVector = if (useDialPicker) Icons.Default.Keyboard else Icons.Default.AccessTime,
                            contentDescription = if (useDialPicker) "Switch to keypad" else "Switch to clock dial"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedContent(targetState = useDialPicker, label = "time_picker_mode") { dial ->
                    if (dial) TimePicker(state = timeState)
                    else TimeInput(state = timeState, modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Repeat days + quick presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Repeat", style = MaterialTheme.typography.titleMedium)
                Row {
                    TextButton(onClick = { selectedDays = weekdaysSet }) { Text("Weekdays") }
                    TextButton(onClick = { selectedDays = Alarm.WEEK_ORDER.map { it.first }.toSet() }) { Text("Daily") }
                    TextButton(onClick = { selectedDays = emptySet() }) { Text("Once") }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Alarm.WEEK_ORDER.forEach { (dayInt, dayName) ->
                    val selected = dayInt in selectedDays
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedDays = if (selected) selectedDays - dayInt else selectedDays + dayInt
                        },
                        label = { Text(dayName.take(1)) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Personal Routine & Wake-Up Prompts
            Text("Personalization & Wake Prompt", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Alarm.PERSONAL_PROMPTS.forEach { (presetGoal, presetLabel, presetPrompt) ->
                    SuggestionChip(
                        onClick = {
                            goal = presetGoal
                            label = presetLabel
                            wakePrompt = presetPrompt
                        },
                        label = { Text(presetGoal) }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = goal,
                onValueChange = { goal = it },
                label = { Text("Morning goal") },
                placeholder = { Text("e.g. Get up for college") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = wakePrompt,
                onValueChange = { wakePrompt = it },
                label = { Text("Personal lock-screen wake prompt") },
                supportingText = { Text("Displayed prominently when the alarm rings & passed to your active AI model") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Short label (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(20.dp))

            // Cognitive Challenge Settings
            Text("Cognitive wake-up challenge", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = category.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ChallengeCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat.shortName) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "SAMPLE PREVIEW (${sampleQuestion.category.shortName.uppercase()})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = sampleQuestion.question.replace("\n", " "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Answer: ${sampleQuestion.answer}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { sampleSeed++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Shuffle sample question")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("Difficulty", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val diffs = ChallengeDifficulty.entries
                diffs.forEachIndexed { index, d ->
                    SegmentedButton(
                        selected = difficulty == d,
                        onClick = { difficulty = d },
                        shape = SegmentedButtonDefaults.itemShape(index, diffs.size)
                    ) { Text(d.displayName) }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("Questions to unlock", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                questionOptions.forEachIndexed { index, count ->
                    SegmentedButton(
                        selected = questionsRequired == count,
                        onClick = { questionsRequired = count },
                        shape = SegmentedButtonDefaults.itemShape(index, questionOptions.size)
                    ) { Text("$count") }
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))

            // MP3 / Trimmed Alarm Tone Picker
            ListItem(
                leadingContent = {
                    Icon(
                        imageVector = if (toneUri.isNotBlank()) Icons.Default.ContentCut else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                headlineContent = { Text("Alarm tone (MP3 + Trimmer)") },
                supportingContent = { Text("$resolvedToneTitle • Tap to browse phone MP3s & trim") },
                trailingContent = {
                    IconButton(
                        onClick = {
                            if (isPreviewPlaying) {
                                soundManager.stopPreview()
                                isPreviewPlaying = false
                            } else {
                                isPreviewPlaying = soundManager.previewTone(toneUri, toneStartMs, toneEndMs)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isPreviewPlaying) "Stop preview" else "Preview alarm tone"
                        )
                    }
                },
                modifier = Modifier.clickable {
                    soundManager.stopPreview()
                    isPreviewPlaying = false
                    showMp3Sheet = true
                }
            )

            ListItem(
                headlineContent = { Text("Aggressive vibration") },
                supportingContent = { Text("Escalating haptic waveform alongside max-volume audio") },
                trailingContent = { Switch(checked = vibrate, onCheckedChange = { vibrate = it }) }
            )

            ListItem(
                headlineContent = { Text("5-minute anti-relapse check") },
                supportingContent = { Text("Re-checks 5 min after you wake up so you cannot fall back asleep") },
                trailingContent = { Switch(checked = wakeCheckEnabled, onCheckedChange = { wakeCheckEnabled = it }) }
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    soundManager.stopPreview()
                    onSave(
                        Alarm(
                            id = initialAlarm?.id ?: 0L,
                            hour = timeState.hour,
                            minute = timeState.minute,
                            daysOfWeek = selectedDays,
                            label = label.trim(),
                            goal = goal.trim().ifBlank { defaultGoal },
                            wakePrompt = wakePrompt.trim(),
                            difficulty = difficulty,
                            category = category,
                            questionsRequired = questionsRequired,
                            toneUri = toneUri,
                            toneTitle = toneTitle,
                            toneStartMs = toneStartMs,
                            toneEndMs = toneEndMs,
                            isEnabled = true,
                            volume = 100,
                            vibrate = vibrate,
                            wakeCheckEnabled = wakeCheckEnabled
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Save alarm", style = MaterialTheme.typography.titleMedium)
            }

            val existingAlarm = initialAlarm?.takeIf { it.id > 0L }
            if (existingAlarm != null && onDelete != null) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        soundManager.stopPreview()
                        onDelete(existingAlarm)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Delete alarm", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showMp3Sheet) {
        Mp3PickerAndTrimmerSheet(
            onTrimConfirmed = { trim ->
                toneUri = trim.uri
                toneTitle = trim.title
                toneStartMs = trim.startMs
                toneEndMs = trim.endMs
                showMp3Sheet = false
            },
            onPickSystemRingtone = {
                showMp3Sheet = false
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Choose system alarm tone")
                    if (toneUri.isNotBlank()) {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(toneUri))
                    }
                }
                ringtoneLauncher.launch(intent)
            },
            onDismiss = { showMp3Sheet = false }
        )
    }
}
