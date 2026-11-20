package com.timecure.app.ui.screens.addalarm

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.timecure.app.TimecureApp
import com.timecure.app.data.local.ChallengeGenerator
import com.timecure.app.model.Alarm
import com.timecure.app.model.ChallengeCategory
import com.timecure.app.model.ChallengeDifficulty
import com.timecure.app.model.WakeMissionConfig
import com.timecure.app.ui.components.safeTopBarWindowInsets
import com.timecure.app.ui.screens.missions.MissionConfigDialog
import com.timecure.app.ui.screens.missions.MissionPickerSheet
import com.timecure.app.ui.screens.missions.MissionPreviewDialog
import com.timecure.app.ui.screens.missions.missionIcon
import com.timecure.app.ui.screens.missions.missionTileBg
import com.timecure.app.ui.screens.missions.missionTileFg
import com.timecure.app.util.Permissions
import com.timecure.app.util.TimeFormat
import java.util.Calendar
import kotlin.math.roundToInt

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
    val haptic = LocalHapticFeedback.current
    val container = (context.applicationContext as TimecureApp).container
    val soundManager = container.soundManager
    val voiceAssistant = container.voiceAssistant

    DisposableEffect(Unit) {
        onDispose {
            soundManager.stopPreview()
            voiceAssistant.stop()
        }
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

    // Screenshot alarm sound & custom setting states
    var volumePercent by remember { mutableIntStateOf(initialAlarm?.volume?.coerceIn(10, 100) ?: 100) }
    var vibrate by remember { mutableStateOf(initialAlarm?.vibrate ?: true) }
    var gentleWakeSeconds by remember { mutableIntStateOf(initialAlarm?.gentleWakeSeconds ?: 30) }
    var timeReminderEnabled by remember { mutableStateOf(initialAlarm?.timeReminderEnabled ?: false) }
    var weatherReminderEnabled by remember { mutableStateOf(initialAlarm?.weatherReminderEnabled ?: false) }
    var labelReminderEnabled by remember { mutableStateOf(initialAlarm?.labelReminderEnabled ?: false) }
    var extraLoudEnabled by remember { mutableStateOf(initialAlarm?.extraLoudEnabled ?: false) }
    var snoozeEnabled by remember { mutableStateOf(initialAlarm?.snoozeEnabled ?: true) }
    var snoozeIntervalMinutes by remember { mutableIntStateOf(initialAlarm?.snoozeIntervalMinutes ?: 5) }
    var snoozeMaxCount by remember { mutableIntStateOf(initialAlarm?.snoozeMaxCount ?: 3) }
    var wallpaperUri by remember {
        mutableStateOf(initialAlarm?.wallpaperUri ?: AlarmWallpaperPreset.YESTERDAY_TOMORROW.id)
    }
    var wakeCheckEnabled by remember { mutableStateOf(initialAlarm?.wakeCheckEnabled ?: true) }
    var wakeCheckDelayMinutes by remember { mutableIntStateOf(initialAlarm?.wakeCheckDelayMinutes ?: 2) }
    var wakeCheckCountdownSeconds by remember { mutableIntStateOf(initialAlarm?.wakeCheckCountdownSeconds ?: 15) }
    var preventPowerOff by remember { mutableStateOf(initialAlarm?.preventPowerOff ?: false) }
    var wakeMissions by remember(initialAlarm) {
        mutableStateOf(initialAlarm?.wakeMissions ?: emptyList())
    }

    var isPreviewPlaying by remember { mutableStateOf(false) }
    var isExtraLoudSamplePlaying by remember { mutableStateOf(false) }
    var previewProgress by remember { mutableFloatStateOf(0f) }
    var showMp3Sheet by remember { mutableStateOf(false) }
    var showGentleDialog by remember { mutableStateOf(false) }
    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var showWakeCheckDialog by remember { mutableStateOf(false) }
    var showPreventPowerOffDialog by remember { mutableStateOf(false) }
    var showMissionPicker by remember { mutableStateOf(false) }
    var configuringMission by remember { mutableStateOf<WakeMissionConfig?>(null) }
    var configuringIndex by remember { mutableIntStateOf(-1) }
    var previewMission by remember { mutableStateOf<WakeMissionConfig?>(null) }
    var sampleSeed by remember { mutableIntStateOf(0) }

    val sampleQuestion = remember(category, difficulty, sampleSeed) {
        ChallengeGenerator.generate(category, difficulty)
    }

    val resolvedToneTitle = remember(toneUri, toneTitle) {
        when {
            toneTitle.isNotBlank() -> toneTitle
            toneUri.isBlank() -> "Alarm Clock (Default)"
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
        daysOfWeek = selectedDays,
        wakeCheckEnabled = wakeCheckEnabled,
        gentleWakeSeconds = gentleWakeSeconds,
        snoozeEnabled = snoozeEnabled,
        snoozeIntervalMinutes = snoozeIntervalMinutes,
        snoozeMaxCount = snoozeMaxCount,
        wakeCheckDelayMinutes = wakeCheckDelayMinutes,
        wakeCheckCountdownSeconds = wakeCheckCountdownSeconds,
        preventPowerOff = preventPowerOff
    )
    val ringsInText = TimeFormat.until(previewAlarm.computeNextTriggerMillis())

    val questionOptions = listOf(1, 2, 3, 5)
    val weekdaysSet = setOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY
    )

    val isExisting = initialAlarm != null && initialAlarm.id > 0L

    fun buildSavedAlarm(): Alarm = Alarm(
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
        volume = volumePercent,
        vibrate = vibrate,
        wakeCheckEnabled = wakeCheckEnabled,
        gentleWakeSeconds = gentleWakeSeconds,
        timeReminderEnabled = timeReminderEnabled,
        weatherReminderEnabled = weatherReminderEnabled,
        labelReminderEnabled = labelReminderEnabled,
        extraLoudEnabled = extraLoudEnabled,
        snoozeEnabled = snoozeEnabled,
        snoozeIntervalMinutes = snoozeIntervalMinutes,
        snoozeMaxCount = snoozeMaxCount,
        wallpaperUri = wallpaperUri,
        wakeMissionsJson = WakeMissionConfig.encodeList(wakeMissions),
        wakeCheckDelayMinutes = wakeCheckDelayMinutes,
        wakeCheckCountdownSeconds = wakeCheckCountdownSeconds,
        preventPowerOff = preventPowerOff
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                windowInsets = safeTopBarWindowInsets(),
                title = {
                    Column {
                        Text(if (!isExisting) "Wake-up alarm" else "Edit wake-up alarm")
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
                    val existingAlarm = initialAlarm?.takeIf { it.id > 0L }
                    if (existingAlarm != null && onDelete != null) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                soundManager.stopPreview()
                                voiceAssistant.stop()
                                onDelete(existingAlarm)
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete alarm",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(onClick = { useDialPicker = !useDialPicker }) {
                        Icon(
                            imageVector = if (useDialPicker) Icons.Default.Keyboard else Icons.Default.AccessTime,
                            contentDescription = if (useDialPicker) "Switch to keypad" else "Switch to clock dial"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 6.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            soundManager.stopPreview()
                            voiceAssistant.stop()
                            onSave(buildSavedAlarm())
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) {
                        Text("Save", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp)
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

            Spacer(Modifier.height(8.dp))

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
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedDays = if (selected) selectedDays - dayInt else selectedDays + dayInt
                        },
                        label = { Text(dayName.take(1)) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ---------- Wake-Up Missions (0..5), Wake-Up Check & Prevent Power-Off ----------
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Wake-up mission",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${wakeMissions.size}/5",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (slotIdx in 0 until 5) {
                            val mission = wakeMissions.getOrNull(slotIdx)
                            if (mission != null) {
                                Box(modifier = Modifier.padding(top = 6.dp, end = 4.dp)) {
                                    Surface(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            configuringIndex = slotIdx
                                            configuringMission = mission
                                        },
                                        color = missionTileBg(mission.type),
                                        contentColor = missionTileFg(mission.type),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.size(width = 74.dp, height = 82.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = missionIcon(mission.type),
                                                contentDescription = mission.type.displayName,
                                                modifier = Modifier.size(26.dp)
                                            )
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                text = mission.type.shortName,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            wakeMissions = wakeMissions.filterIndexed { i, _ -> i != slotIdx }
                                        },
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 6.dp, y = (-6).dp)
                                            .size(22.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove ${mission.type.displayName}",
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        configuringIndex = -1
                                        showMissionPicker = true
                                    },
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .size(width = 74.dp, height = 82.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Add wake-up mission",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showWakeCheckDialog = true
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Wake up check",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = previewAlarm.wakeCheckSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (wakeCheckEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showPreventPowerOffDialog = true
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Prevent power-off",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        val guardReady = Permissions.canDrawOverlays(context) &&
                            Permissions.isPowerOffAccessibilityEnabled(context)
                        Text(
                            text = when {
                                !preventPowerOff -> "Off"
                                guardReady -> "On"
                                else -> "Setup needed"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                !preventPowerOff -> MaterialTheme.colorScheme.onSurfaceVariant
                                guardReady -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ---------- Section 1: Alarm sound ----------
            Text(
                text = "Alarm sound",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    // Play button + Tone name + chevron
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                soundManager.stopPreview()
                                isPreviewPlaying = false
                                previewProgress = 0f
                                showMp3Sheet = true
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isExtraLoudSamplePlaying = false
                                if (isPreviewPlaying) {
                                    soundManager.stopPreview()
                                    isPreviewPlaying = false
                                    previewProgress = 0f
                                } else {
                                    isPreviewPlaying = soundManager.previewTone(
                                        toneUri = toneUri,
                                        startMs = toneStartMs,
                                        endMs = toneEndMs,
                                        volumePercent = volumePercent,
                                        extraLoud = extraLoudEnabled,
                                        onProgressFraction = { previewProgress = it }
                                    )
                                }
                            },
                            shape = CircleShape,
                            color = if (isPreviewPlaying) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = if (isPreviewPlaying) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isPreviewPlaying) "Stop preview" else "Preview alarm sound",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = resolvedToneTitle,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Tap to browse phone MP3s & trim",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Choose tone",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Audio progress indicator
                    LinearProgressIndicator(
                        progress = { previewProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )

                    // Volume slider + vibrate checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Alarm volume",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Slider(
                            value = volumePercent.toFloat(),
                            onValueChange = {
                                val nextVol = it.roundToInt().coerceIn(10, 100)
                                volumePercent = nextVol
                                if (isPreviewPlaying) {
                                    soundManager.updatePreviewVolume(nextVol, extraLoudEnabled)
                                }
                            },
                            valueRange = 10f..100f,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Vibration,
                            contentDescription = "Vibrate toggle",
                            tint = if (vibrate) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Checkbox(
                            checked = vibrate,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                vibrate = it
                            }
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    // Gentle wake-up
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showGentleDialog = true
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gentle wake-up",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = previewAlarm.gentleWakeSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Time reminder + Sample pill + Switch
                    SoundFeatureSwitchRow(
                        title = "Time reminder",
                        checked = timeReminderEnabled,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            timeReminderEnabled = it
                        },
                        onSampleClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            voiceAssistant.speakTimeSample()
                        }
                    )

                    // Weather reminder + Sample pill + Switch
                    SoundFeatureSwitchRow(
                        title = "Weather reminder",
                        checked = weatherReminderEnabled,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            weatherReminderEnabled = it
                        },
                        onSampleClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            voiceAssistant.speakWeatherSample(context)
                        }
                    )

                    // Label reminder + Sample pill + Switch
                    SoundFeatureSwitchRow(
                        title = "Label reminder",
                        checked = labelReminderEnabled,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            labelReminderEnabled = it
                        },
                        onSampleClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            voiceAssistant.speakLabelSample(label, goal, wakePrompt)
                        }
                    )

                    // Extra loud effect + Sample pill + Switch
                    SoundFeatureSwitchRow(
                        title = "Extra loud effect",
                        checked = extraLoudEnabled,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            extraLoudEnabled = it
                            if (isPreviewPlaying) {
                                soundManager.updatePreviewVolume(volumePercent, it)
                            }
                        },
                        sampleActive = isExtraLoudSamplePlaying,
                        onSampleClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isPreviewPlaying = false
                            previewProgress = 0f
                            if (isExtraLoudSamplePlaying) {
                                soundManager.stopPreview()
                                isExtraLoudSamplePlaying = false
                            } else {
                                isExtraLoudSamplePlaying = soundManager.previewExtraLoudSample(
                                    toneUri = toneUri,
                                    startMs = toneStartMs,
                                    endMs = toneEndMs,
                                    onFinished = { isExtraLoudSamplePlaying = false }
                                )
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ---------- Section 2: Custom setting ----------
            Text(
                text = "Custom setting",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    // Snooze row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showSnoozeDialog = true
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Snooze",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = previewAlarm.snoozeSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Alarm wallpaper row with portrait thumbnail
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showWallpaperDialog = true
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Alarm wallpaper",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Lock-screen quote card or gallery photo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        AlarmWallpaperThumbnail(
                            wallpaperUri = wallpaperUri,
                            modifier = Modifier.size(width = 56.dp, height = 78.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

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
                supportingText = { Text("Displayed when ringing & spoken when Label reminder is enabled") },
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

            Spacer(Modifier.height(22.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(18.dp))

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

    if (showGentleDialog) {
        GentleWakeDialog(
            selectedSeconds = gentleWakeSeconds,
            onSelect = {
                gentleWakeSeconds = it
                showGentleDialog = false
            },
            onDismiss = { showGentleDialog = false }
        )
    }

    if (showSnoozeDialog) {
        SnoozeSettingsDialog(
            enabled = snoozeEnabled,
            intervalMinutes = snoozeIntervalMinutes,
            maxCount = snoozeMaxCount,
            onConfirm = { en, interval, maxC ->
                snoozeEnabled = en
                snoozeIntervalMinutes = interval
                snoozeMaxCount = maxC
                showSnoozeDialog = false
            },
            onDismiss = { showSnoozeDialog = false }
        )
    }

    if (showWallpaperDialog) {
        AlarmWallpaperPickerDialog(
            currentUri = wallpaperUri,
            onSelect = {
                wallpaperUri = it
                showWallpaperDialog = false
            },
            onDismiss = { showWallpaperDialog = false }
        )
    }

    if (showMissionPicker) {
        MissionPickerSheet(
            onSelectType = { type ->
                showMissionPicker = false
                configuringIndex = -1
                configuringMission = WakeMissionConfig(type = type)
            },
            onDismiss = { showMissionPicker = false }
        )
    }

    configuringMission?.let { activeConfig ->
        MissionConfigDialog(
            initialConfig = activeConfig,
            goal = goal.ifBlank { defaultGoal },
            onPreview = { previewMission = it },
            onSave = { savedConfig ->
                wakeMissions = if (configuringIndex in wakeMissions.indices) {
                    wakeMissions.mapIndexed { idx, old -> if (idx == configuringIndex) savedConfig else old }
                } else {
                    (wakeMissions + savedConfig).take(5)
                }
                configuringMission = null
                configuringIndex = -1
            },
            onDismiss = {
                configuringMission = null
                configuringIndex = -1
            }
        )
    }

    previewMission?.let { activePreview ->
        MissionPreviewDialog(
            config = activePreview,
            goal = goal.ifBlank { defaultGoal },
            onDismiss = { previewMission = null }
        )
    }

    if (showWakeCheckDialog) {
        WakeUpCheckConfigDialog(
            enabled = wakeCheckEnabled,
            delayMinutes = wakeCheckDelayMinutes,
            countdownSeconds = wakeCheckCountdownSeconds,
            onConfirm = { en, delayMin, countdownSec ->
                wakeCheckEnabled = en
                wakeCheckDelayMinutes = delayMin
                wakeCheckCountdownSeconds = countdownSec
                showWakeCheckDialog = false
            },
            onDismiss = { showWakeCheckDialog = false }
        )
    }

    if (showPreventPowerOffDialog) {
        PreventPowerOffDialog(
            enabled = preventPowerOff,
            onEnabledChange = { preventPowerOff = it },
            onDismiss = { showPreventPowerOffDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WakeUpCheckConfigDialog(
    enabled: Boolean,
    delayMinutes: Int,
    countdownSeconds: Int,
    onConfirm: (Boolean, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var draftEnabled by remember(enabled) { mutableStateOf(enabled) }
    var draftDelay by remember(delayMinutes) { mutableIntStateOf(delayMinutes) }
    var draftCountdown by remember(countdownSeconds) { mutableIntStateOf(countdownSeconds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wake up check") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "After dismissing this alarm, a countdown pop-up (\"3, 2, 1\") asks if you're awake. Pressing Awake silences all alarms for this task.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable wake up check", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = draftEnabled, onCheckedChange = { draftEnabled = it })
                }

                if (draftEnabled) {
                    Text("Check after dismissing", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 2, 3, 5, 10, 15).forEach { mins ->
                            FilterChip(
                                selected = draftDelay == mins,
                                onClick = { draftDelay = mins },
                                label = { Text("${mins}m") }
                            )
                        }
                    }

                    Text("Countdown response window", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(10, 15, 20, 30, 45, 60).forEach { secs ->
                            FilterChip(
                                selected = draftCountdown == secs,
                                onClick = { draftCountdown = secs },
                                label = { Text("${secs}s") }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(draftEnabled, draftDelay, draftCountdown) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SoundFeatureSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onSampleClick: () -> Unit,
    sampleActive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.width(10.dp))
        Surface(
            onClick = onSampleClick,
            shape = CircleShape,
            color = if (sampleActive) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (sampleActive) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (sampleActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = "Sample $title",
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (sampleActive) "Playing" else "Sample",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun GentleWakeDialog(
    selectedSeconds: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        0 to "Off",
        15 to "15 seconds",
        30 to "30 seconds",
        60 to "1 minute",
        120 to "2 minutes",
        300 to "5 minutes"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gentle wake-up") },
        text = {
            Column {
                Text(
                    text = "Gradually ramp volume from quiet to your alarm level.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                options.forEach { (secs, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(secs) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedSeconds == secs, onClick = { onSelect(secs) })
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SnoozeSettingsDialog(
    enabled: Boolean,
    intervalMinutes: Int,
    maxCount: Int,
    onConfirm: (Boolean, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var draftEnabled by remember(enabled) { mutableStateOf(enabled) }
    var draftInterval by remember(intervalMinutes) { mutableIntStateOf(intervalMinutes) }
    var draftMax by remember(maxCount) { mutableIntStateOf(maxCount) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Snooze settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Allow snooze", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = draftEnabled, onCheckedChange = { draftEnabled = it })
                }

                if (draftEnabled) {
                    Text("Snooze interval", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 3, 5, 10, 15, 30).forEach { mins ->
                            FilterChip(
                                selected = draftInterval == mins,
                                onClick = { draftInterval = mins },
                                label = { Text("${mins}m") }
                            )
                        }
                    }

                    Text("Max snoozes", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1 to "1", 2 to "2", 3 to "3", 5 to "5", 0 to "Unlimited").forEach { (c, lbl) ->
                            FilterChip(
                                selected = draftMax == c,
                                onClick = { draftMax = c },
                                label = { Text(lbl) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(draftEnabled, draftInterval, draftMax) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
