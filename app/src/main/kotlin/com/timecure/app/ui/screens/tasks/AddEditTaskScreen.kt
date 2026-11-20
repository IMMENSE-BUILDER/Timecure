package com.timecure.app.ui.screens.tasks

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.timecure.app.model.Alarm
import com.timecure.app.model.GeofenceTriggerMode
import com.timecure.app.model.SavedPlace
import com.timecure.app.model.SmartTask
import com.timecure.app.model.TaskAlertMode
import com.timecure.app.model.TaskTriggerType
import com.timecure.app.tasks.LocationFix
import com.timecure.app.tasks.LocationGeoMath
import com.timecure.app.ui.components.safeTopBarWindowInsets
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTaskScreen(
    initialTask: SmartTask?,
    places: List<SavedPlace>,
    currentLocation: LocationFix?,
    onSaveTask: (SmartTask) -> Unit,
    onDeleteTask: ((SmartTask) -> Unit)? = null,
    onSaveNewPlace: suspend (SavedPlace) -> SavedPlace,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var title by remember(initialTask) { mutableStateOf(initialTask?.title ?: "") }
    var notes by remember(initialTask) { mutableStateOf(initialTask?.notes ?: "") }
    var triggerType by remember(initialTask) {
        mutableStateOf(initialTask?.triggerType ?: TaskTriggerType.LOCATION)
    }
    var alertMode by remember(initialTask) {
        mutableStateOf(initialTask?.alertMode ?: TaskAlertMode.NOTIFICATION)
    }
    var selectedPlaceId by remember(initialTask) {
        mutableStateOf(initialTask?.placeId ?: places.firstOrNull()?.id)
    }
    var radiusMeters by remember(initialTask) {
        mutableIntStateOf(
            initialTask?.radiusMeters
                ?: places.firstOrNull { it.id == selectedPlaceId }?.radiusMeters
                ?: 150
        )
    }
    var geofenceMode by remember(initialTask) {
        mutableStateOf(initialTask?.geofenceMode ?: GeofenceTriggerMode.ARRIVE)
    }
    var repeatEveryTime by remember(initialTask) {
        mutableStateOf(initialTask?.repeatEveryTime ?: false)
    }

    val initialCal = remember(initialTask) {
        Calendar.getInstance().apply {
            if (initialTask != null) {
                set(Calendar.HOUR_OF_DAY, initialTask.hour)
                set(Calendar.MINUTE, initialTask.minute)
            } else {
                add(Calendar.HOUR_OF_DAY, 1)
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
    var selectedDays by remember(initialTask) {
        mutableStateOf(initialTask?.daysOfWeek ?: emptySet())
    }

    var showMarkPlaceDialog by remember { mutableStateOf(false) }

    val selectedPlace = places.firstOrNull { it.id == selectedPlaceId }
    val canSave = title.trim().isNotEmpty() &&
        (triggerType == TaskTriggerType.TIME || selectedPlace != null)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (initialTask == null) "New Smart Task" else "Edit Smart Task") },
                windowInsets = safeTopBarWindowInsets(),
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (initialTask != null && onDeleteTask != null) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDeleteTask(initialTask)
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete task")
                        }
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("What needs to happen?") },
                placeholder = { Text("e.g. Check in at lab, Pay bill at restaurant") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // Trigger Type Switcher
            Column {
                Text("Trigger by", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val types = TaskTriggerType.entries
                    types.forEachIndexed { idx, t ->
                        SegmentedButton(
                            selected = triggerType == t,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                triggerType = t
                            },
                            icon = {
                                Icon(
                                    imageVector = if (t == TaskTriggerType.LOCATION)
                                        Icons.Default.LocationOn else Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            shape = SegmentedButtonDefaults.itemShape(idx, types.size)
                        ) {
                            Text(t.displayName)
                        }
                    }
                }
            }

            if (triggerType == TaskTriggerType.LOCATION) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Marked place", style = MaterialTheme.typography.labelLarge)
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showMarkPlaceDialog = true
                            }
                        ) {
                            Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Mark new place")
                        }
                    }

                    if (places.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "No places marked yet",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Initially mark places like your College or Restaurant with a 100m–200m zone so Timecure recognizes them automatically.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FilledTonalButton(
                                    onClick = { showMarkPlaceDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.AddLocationAlt, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Mark a Place Now")
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            places.forEach { place ->
                                val isSelected = place.id == selectedPlaceId
                                val distLabel = currentLocation?.let { loc ->
                                    val meters = LocationGeoMath.distanceMeters(
                                        loc.latitude, loc.longitude,
                                        place.latitude, place.longitude
                                    )
                                    "${LocationGeoMath.formatDistance(meters)} away"
                                }
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedPlaceId = place.id
                                        radiusMeters = place.radiusMeters
                                    },
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = placeTagIcon(place.tag),
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = place.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = buildString {
                                                    append("${place.tag.displayName} • ${place.radiusMeters}m")
                                                    if (place.addressLabel.isNotBlank()) {
                                                        append(" • ${place.addressLabel}")
                                                    }
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (distLabel != null) {
                                            Text(
                                                text = distLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.width(8.dp))
                                        }
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = "Selected place",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Text("Trigger zone radius: $radiusMeters m", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(100, 150, 200, 300, 500).forEach { r ->
                            FilterChip(
                                selected = radiusMeters == r,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    radiusMeters = r
                                },
                                label = { Text("${r} m") }
                            )
                        }
                    }

                    Text("Trigger condition", style = MaterialTheme.typography.labelLarge)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val modes = GeofenceTriggerMode.entries
                        modes.forEachIndexed { idx, gm ->
                            SegmentedButton(
                                selected = geofenceMode == gm,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    geofenceMode = gm
                                },
                                shape = SegmentedButtonDefaults.itemShape(idx, modes.size)
                            ) {
                                Text(gm.displayName)
                            }
                        }
                    }

                    ListItem(
                        headlineContent = { Text("Repeat on every visit") },
                        supportingContent = {
                            Text("Keep re-arming each time you return to this place (15m cooldown)")
                        },
                        trailingContent = {
                            Switch(
                                checked = repeatEveryTime,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    repeatEveryTime = it
                                }
                            )
                        }
                    )
                }
            } else {
                // TIME TRIGGER
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { useDialPicker = !useDialPicker }) {
                            Icon(
                                imageVector = if (useDialPicker) Icons.Default.Keyboard else Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (useDialPicker) "Type time" else "Clock dial")
                        }
                    }
                    AnimatedContent(targetState = useDialPicker, label = "task_time_picker") { dial ->
                        if (dial) TimePicker(state = timeState) else TimeInput(state = timeState)
                    }
                }

                Column {
                    Text("Repeat schedule", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Alarm.WEEK_ORDER.forEach { (dayConst, shortLabel) ->
                            val selected = dayConst in selectedDays
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedDays = if (selected) selectedDays - dayConst else selectedDays + dayConst
                                },
                                label = { Text(shortLabel) }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Alert Mode Selector
            Column {
                Text("How should Timecure alert you?", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val alerts = TaskAlertMode.entries
                    alerts.forEachIndexed { idx, mode ->
                        SegmentedButton(
                            selected = alertMode == mode,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                alertMode = mode
                            },
                            icon = {
                                Icon(
                                    imageVector = if (mode == TaskAlertMode.ALARM)
                                        Icons.Default.Alarm else Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            shape = SegmentedButtonDefaults.itemShape(idx, alerts.size)
                        ) {
                            Text(mode.displayName)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (alertMode == TaskAlertMode.ALARM)
                        "Rings loud at alarm volume + pops full-screen takeover until you dismiss or mark done."
                    else
                        "Posts a high-priority heads-up notification with quick Done & Snooze buttons.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSaveTask(
                            SmartTask(
                                id = initialTask?.id ?: 0L,
                                title = title.trim(),
                                notes = notes.trim(),
                                triggerType = triggerType,
                                alertMode = alertMode,
                                isEnabled = initialTask?.isEnabled ?: true,
                                isCompleted = initialTask?.isCompleted ?: false,
                                repeatEveryTime = repeatEveryTime,
                                hour = timeState.hour,
                                minute = timeState.minute,
                                daysOfWeek = selectedDays,
                                placeId = if (triggerType == TaskTriggerType.LOCATION) selectedPlaceId else null,
                                radiusMeters = radiusMeters,
                                geofenceMode = geofenceMode,
                                lastInside = initialTask?.lastInside ?: false,
                                lastTriggeredMillis = initialTask?.lastTriggeredMillis ?: 0L,
                                createdAtMillis = initialTask?.createdAtMillis ?: System.currentTimeMillis()
                            )
                        )
                    },
                    enabled = canSave,
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text("Save Task")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showMarkPlaceDialog) {
        MarkPlaceDialog(
            initialPlace = null,
            onSave = { newPlace ->
                showMarkPlaceDialog = false
                scope.launch {
                    val saved = onSaveNewPlace(newPlace)
                    selectedPlaceId = saved.id
                    radiusMeters = saved.radiusMeters
                }
            },
            onDismiss = { showMarkPlaceDialog = false }
        )
    }
}
