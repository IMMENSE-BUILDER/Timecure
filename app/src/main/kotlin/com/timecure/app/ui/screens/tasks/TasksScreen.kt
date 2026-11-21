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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.ui.geometry.Offset
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

@OptIn(ExperimentalMaterial3Api::class)
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
    val dotPulseState: State<Float> = if (radarActive && !reducedMotion) {
        val inf = rememberInfiniteTransition(label = "radar_dot")
        inf.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Restart),
            label = "radar_wave"
        )
    } else {
        rememberUpdatedState(0.5f)
    }
    val dotColor = if (radarActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline

    Surface(
        color = if (radarActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
        else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (radarActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(34.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    if (radarActive) {
                        val t = dotPulseState.value
                        drawCircle(
                            color = dotColor.copy(alpha = (1f - t) * 0.45f),
                            radius = size.minDimension * (0.24f + 0.25f * t),
                            center = c
                        )
                    }
                    drawCircle(
                        color = dotColor.copy(alpha = 0.2f),
                        radius = size.minDimension * 0.34f,
                        center = c
                    )
                    drawCircle(
                        color = dotColor,
                        radius = size.minDimension * 0.17f,
                        center = c
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (radarActive)
                        "Location Radar Active • $watchedPlaceCount ${if (watchedPlaceCount == 1) "place" else "places"}"
                    else
                        "$totalTasks active ${if (totalTasks == 1) "task" else "tasks"} scheduled",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (radarActive) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (radarActive)
                        "Fires automatically when entering marked zones"
                    else
                        "Add a location task to arm continuous zone radar",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (radarActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                onClick = onRefreshGps,
                color = if (radarActive) MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = CircleShape,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh current GPS position",
                        modifier = Modifier.size(18.dp),
                        tint = if (radarActive) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
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
        } ?: "${place.radiusMeters}m"
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
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = if (isInsideNow) 1.5.dp else 1.dp,
            color = if (isInsideNow) MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
        )
    ) {
        Column(
            modifier = Modifier
                .width(164.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (isInsideNow) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = placeTagIcon(place.tag),
                            contentDescription = null,
                            tint = if (isInsideNow) MaterialTheme.colorScheme.onTertiaryContainer
                            else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
                Surface(
                    color = if (isInsideNow) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = CircleShape
                ) {
                    Text(
                        text = if (isInsideNow) "Inside" else distanceLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isInsideNow) MaterialTheme.colorScheme.onTertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${place.tag.displayName} • ${place.radiusMeters}m zone",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isInsideNow) MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

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
        targetValue = if (isPressed && !reducedMotion) 0.975f else 1f,
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
            LocationGeoMath.formatDistance(d)
        } else null
    }

    val isLocation = task.triggerType == TaskTriggerType.LOCATION
    val triggerSummary = if (isLocation) {
        val placeName = place?.name ?: "Unlinked place"
        "${task.geofenceMode.displayName}: $placeName (${task.radiusMeters}m)"
    } else {
        "${task.formattedTime()} • ${task.daysSummary}"
    }

    Surface(
        onClick = onEdit,
        interactionSource = interaction,
        color = if (task.isCompleted) MaterialTheme.colorScheme.surfaceContainerLowest
        else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (task.isCompleted) 0.35f else 0.65f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                alpha = if (task.isCompleted) 0.62f else 1f
            }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { onToggleCompleted(!task.isCompleted) },
                    shape = CircleShape,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = if (task.isCompleted) null else BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.size(26.dp)
                ) {
                    if (task.isCompleted) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Mark incomplete",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (task.notes.isNotBlank()) {
                        Text(
                            text = task.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = task.isEnabled && !task.isCompleted,
                    onCheckedChange = onToggleEnabled,
                    enabled = !task.isCompleted
                )
            }

            // Clean inline metadata footer row (zero chip soup)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (isLocation) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f)
                        else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                        shape = CircleShape,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isLocation) Icons.Default.LocationOn else Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (isLocation) MaterialTheme.colorScheme.onTertiaryContainer
                                else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = triggerSummary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isLocation) MaterialTheme.colorScheme.onTertiaryContainer
                                else MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (task.alertMode == TaskAlertMode.ALARM)
                                Icons.Default.Alarm else Icons.Default.NotificationsActive,
                            contentDescription = task.alertMode.displayName,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        if (liveDistText != null) {
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = liveDistText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                Surface(
                    onClick = onTestFire,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Test task trigger",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}
