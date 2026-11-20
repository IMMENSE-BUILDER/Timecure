package com.timecure.app.ui.screens.tasks

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.timecure.app.model.SavedPlace
import com.timecure.app.model.SmartTask
import com.timecure.app.model.TaskAlertMode
import com.timecure.app.model.TaskTriggerType
import com.timecure.app.tasks.LocationFix
import com.timecure.app.tasks.LocationGeoMath
import com.timecure.app.ui.components.rememberReducedMotion
import com.timecure.app.ui.components.safeTopBarWindowInsets
import com.timecure.app.util.Permissions

private enum class TaskFilter(val label: String) {
    ALL("All"),
    LOCATION("Location"),
    TIME("Time")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TasksScreen(
    tasks: List<SmartTask>,
    places: List<SavedPlace>,
    radarActive: Boolean,
    currentLocation: LocationFix?,
    onAddTask: () -> Unit,
    onEditTask: (SmartTask) -> Unit,
    onToggleTaskEnabled: (SmartTask, Boolean) -> Unit,
    onToggleTaskCompleted: (SmartTask, Boolean) -> Unit,
    onTestFireTask: (SmartTask) -> Unit,
    onSavePlace: (SavedPlace) -> Unit,
    onDeletePlace: (SavedPlace) -> Unit,
    onRefreshLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val reducedMotion = rememberReducedMotion()

    var filter by remember { mutableStateOf(TaskFilter.ALL) }
    var editingPlace by remember { mutableStateOf<SavedPlace?>(null) }
    var showPlaceDialog by remember { mutableStateOf(false) }
    var permRefreshNonce by remember { mutableIntStateOf(0) }

    val hasAnyLocationPerm = remember(permRefreshNonce) { Permissions.hasAnyLocation(context) }
    val hasBackgroundLocationPerm = remember(permRefreshNonce) { Permissions.hasBackgroundLocation(context) }
    val hasActiveLocationTasks = remember(tasks) {
        tasks.any { it.triggerType == TaskTriggerType.LOCATION && it.isEnabled && !it.isCompleted }
    }

    val fgPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permRefreshNonce++
        onRefreshLocation()
    }
    val bgPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        permRefreshNonce++
        onRefreshLocation()
    }

    val placesById = remember(places) { places.associateBy { it.id } }
    val filteredTasks = remember(tasks, filter) {
        when (filter) {
            TaskFilter.ALL -> tasks
            TaskFilter.LOCATION -> tasks.filter { it.triggerType == TaskTriggerType.LOCATION }
            TaskFilter.TIME -> tasks.filter { it.triggerType == TaskTriggerType.TIME }
        }
    }
    val watchedPlaceCount = remember(tasks) {
        tasks.filter {
            it.triggerType == TaskTriggerType.LOCATION && it.isEnabled && !it.isCompleted && it.placeId != null
        }.mapNotNull { it.placeId }.distinct().size
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tasks") },
                windowInsets = safeTopBarWindowInsets(),
                actions = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            editingPlace = null
                            showPlaceDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddLocationAlt,
                            contentDescription = "Mark new place"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAddTask()
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Task") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Permission Banner for Foreground / Background Location
            if (!hasAnyLocationPerm || (hasActiveLocationTasks && !hasBackgroundLocationPerm)) {
                item(key = "perm_banner") {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (!hasAnyLocationPerm)
                                        "Enable Location permission to mark places & auto-trigger on arrival"
                                    else
                                        "Allow Background Location ('Allow all the time') so location tasks fire with screen off",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!hasAnyLocationPerm) {
                                    FilledTonalButton(
                                        onClick = {
                                            fgPermLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        }
                                    ) { Text("Allow Location") }
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    FilledTonalButton(
                                        onClick = {
                                            bgPermLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                        }
                                    ) { Text("Allow Always") }
                                }
                                TextButton(
                                    onClick = {
                                        runCatching { context.startActivity(Permissions.appDetailsSettings(context)) }
                                    }
                                ) { Text("App Settings") }
                            }
                        }
                    }
                }
            }

            // Live Radar Status Strip
            item(key = "radar_status") {
                RadarStatusPill(
                    radarActive = radarActive,
                    watchedPlaceCount = watchedPlaceCount,
                    totalTasks = tasks.count { !it.isCompleted },
                    reducedMotion = reducedMotion,
                    onRefreshGps = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (Permissions.hasAnyLocation(context)) {
                            onRefreshLocation()
                        } else {
                            fgPermLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }
                )
            }

            // Marked Places Carousel (College, Restaurant, Home, etc.)
            item(key = "places_carousel") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Marked places",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                editingPlace = null
                                showPlaceDialog = true
                            }
                        ) {
                            Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Mark place")
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(places, key = { "place_${it.id}" }) { place ->
                            PlaceChipCard(
                                place = place,
                                currentLocation = currentLocation,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    editingPlace = place
                                    showPlaceDialog = true
                                }
                            )
                        }
                        item(key = "mark_place_tile") {
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    editingPlace = null
                                    showPlaceDialog = true
                                },
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .width(142.dp)
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AddLocationAlt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Mark new spot",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "College, Restaurant, Home...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Filter Segmented Bar
            item(key = "task_filters") {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val entries = TaskFilter.entries
                    entries.forEachIndexed { idx, entry ->
                        SegmentedButton(
                            selected = filter == entry,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                filter = entry
                            },
                            shape = SegmentedButtonDefaults.itemShape(idx, entries.size)
                        ) { Text(entry.label) }
                    }
                }
            }

            if (filteredTasks.isEmpty()) {
                item(key = "empty_tasks") {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.TaskAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "No tasks yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Mark a location like your College or favorite Restaurant (100m–200m zone), or schedule a time reminder. Timecure notifies or rings whenever you arrive or on time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        editingPlace = null
                                        showPlaceDialog = true
                                    }
                                ) { Text("Mark Place") }
                                FilledTonalButton(onClick = onAddTask) { Text("Add Task") }
                            }
                        }
                    }
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
                    val linkedPlace = task.placeId?.let { placesById[it] }
                    SmartTaskRow(
                        task = task,
                        place = linkedPlace,
                        currentLocation = currentLocation,
                        reducedMotion = reducedMotion,
                        onEdit = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onEditTask(task)
                        },
                        onToggleEnabled = { enabled ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleTaskEnabled(task, enabled)
                        },
                        onToggleCompleted = { done ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleTaskCompleted(task, done)
                        },
                        onTestFire = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTestFireTask(task)
                        }
                    )
                }
            }
        }
    }

    if (showPlaceDialog) {
        MarkPlaceDialog(
            initialPlace = editingPlace,
            onSave = { place ->
                showPlaceDialog = false
                onSavePlace(place)
            },
            onDelete = editingPlace?.let { existing ->
                {
                    showPlaceDialog = false
                    onDeletePlace(existing)
                }
            },
            onDismiss = { showPlaceDialog = false }
        )
    }
}

@Composable
private fun RadarStatusPill(
    radarActive: Boolean,
    watchedPlaceCount: Int,
    totalTasks: Int,
    reducedMotion: Boolean,
    onRefreshGps: () -> Unit
) {
    val dotAlphaState: State<Float> = if (radarActive && !reducedMotion) {
        val inf = rememberInfiniteTransition(label = "radar_dot")
        inf.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "radar_alpha"
        )
    } else {
        rememberUpdatedState(1f)
    }
    val dotColor = if (radarActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline

    Surface(
        color = if (radarActive) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(10.dp)) {
                drawCircle(color = dotColor.copy(alpha = dotAlphaState.value))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (radarActive)
                        "Location Radar Active • Watching $watchedPlaceCount ${if (watchedPlaceCount == 1) "place" else "places"}"
                    else
                        "$totalTasks active ${if (totalTasks == 1) "task" else "tasks"} scheduled",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (radarActive) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (radarActive)
                        "Triggers alerts automatically when entering marked perimeters"
                    else
                        "Add a location task to arm continuous background zone radar",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (radarActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRefreshGps) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh current GPS position"
                )
            }
        }
    }
}

@Composable
private fun PlaceChipCard(
    place: SavedPlace,
    currentLocation: LocationFix?,
    onClick: () -> Unit
) {
    val distanceLabel = remember(place, currentLocation) {
        currentLocation?.let { loc ->
            val m = LocationGeoMath.distanceMeters(
                loc.latitude, loc.longitude,
                place.latitude, place.longitude
            )
            LocationGeoMath.formatDistance(m)
        } ?: "${place.radiusMeters}m zone"
    }
    val isInsideNow = remember(place, currentLocation) {
        currentLocation?.let { loc ->
            LocationGeoMath.distanceMeters(
                loc.latitude, loc.longitude,
                place.latitude, place.longitude
            ) <= place.radiusMeters
        } ?: false
    }

    Surface(
        onClick = onClick,
        color = if (isInsideNow) MaterialTheme.colorScheme.tertiaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (isInsideNow) 1.5.dp else 1.dp,
            color = if (isInsideNow) MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .width(156.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = placeTagIcon(place.tag),
                    contentDescription = null,
                    tint = if (isInsideNow) MaterialTheme.colorScheme.onTertiaryContainer
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = CircleShape
                ) {
                    Text(
                        text = if (isInsideNow) "Inside" else distanceLabel,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = place.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${place.tag.displayName} • ${place.radiusMeters}m",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SmartTaskRow(
    task: SmartTask,
    place: SavedPlace?,
    currentLocation: LocationFix?,
    reducedMotion: Boolean,
    onEdit: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleCompleted: (Boolean) -> Unit,
    onTestFire: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "task_card_press"
    )

    val liveDistText = remember(task, place, currentLocation) {
        if (task.triggerType == TaskTriggerType.LOCATION && place != null && currentLocation != null) {
            val d = LocationGeoMath.distanceMeters(
                currentLocation.latitude, currentLocation.longitude,
                place.latitude, place.longitude
            )
            "${LocationGeoMath.formatDistance(d)} away"
        } else null
    }

    Surface(
        onClick = onEdit,
        interactionSource = interaction,
        color = if (task.isCompleted) MaterialTheme.colorScheme.surfaceContainerLowest
        else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                alpha = if (task.isCompleted) 0.62f else 1f
            }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = onToggleCompleted
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    if (task.notes.isNotBlank()) {
                        Text(
                            text = task.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Switch(
                    checked = task.isEnabled && !task.isCompleted,
                    onCheckedChange = onToggleEnabled,
                    enabled = !task.isCompleted
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AssistChip(
                    onClick = onEdit,
                    leadingIcon = {
                        Icon(
                            imageVector = if (task.triggerType == TaskTriggerType.LOCATION)
                                Icons.Default.LocationOn else Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        val triggerSummary = if (task.triggerType == TaskTriggerType.LOCATION) {
                            val placeName = place?.name ?: "Unlinked place"
                            "${task.geofenceMode.displayName}: $placeName (${task.radiusMeters}m)"
                        } else {
                            "${task.formattedTime()} • ${task.daysSummary}"
                        }
                        Text(triggerSummary)
                    }
                )

                AssistChip(
                    onClick = onEdit,
                    leadingIcon = {
                        Icon(
                            imageVector = if (task.alertMode == TaskAlertMode.ALARM)
                                Icons.Default.Alarm else Icons.Default.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text(task.alertMode.displayName) }
                )

                if (liveDistText != null) {
                    AssistChip(
                        onClick = onEdit,
                        leadingIcon = {
                            Icon(
                                Icons.Default.MyLocation,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(liveDistText) }
                    )
                }

                AssistChip(
                    onClick = onTestFire,
                    leadingIcon = {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Test") }
                )
            }
        }
    }
}
