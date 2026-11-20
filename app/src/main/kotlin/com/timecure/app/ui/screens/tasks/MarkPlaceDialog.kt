package com.timecure.app.ui.screens.tasks

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.timecure.app.model.PlaceTag
import com.timecure.app.model.SavedPlace
import com.timecure.app.tasks.GeocodedPlace
import com.timecure.app.tasks.LocationHelper
import com.timecure.app.util.Permissions
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

fun placeTagIcon(tag: PlaceTag): ImageVector = when (tag) {
    PlaceTag.COLLEGE -> Icons.Default.School
    PlaceTag.RESTAURANT -> Icons.Default.Restaurant
    PlaceTag.HOME -> Icons.Default.Home
    PlaceTag.WORK -> Icons.Default.Work
    PlaceTag.GYM -> Icons.Default.FitnessCenter
    PlaceTag.SHOP -> Icons.Default.ShoppingBag
    PlaceTag.CUSTOM -> Icons.Default.LocationOn
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarkPlaceDialog(
    initialPlace: SavedPlace?,
    onSave: (SavedPlace) -> Unit,
    onDelete: ((SavedPlace) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var name by remember(initialPlace) { mutableStateOf(initialPlace?.name ?: "") }
    var tag by remember(initialPlace) { mutableStateOf(initialPlace?.tag ?: PlaceTag.COLLEGE) }
    var addressLabel by remember(initialPlace) { mutableStateOf(initialPlace?.addressLabel ?: "") }
    var latText by remember(initialPlace) {
        mutableStateOf(
            initialPlace?.latitude?.let { String.format(Locale.US, "%.6f", it) } ?: ""
        )
    }
    var lonText by remember(initialPlace) {
        mutableStateOf(
            initialPlace?.longitude?.let { String.format(Locale.US, "%.6f", it) } ?: ""
        )
    }
    var radiusMeters by remember(initialPlace) {
        mutableIntStateOf((initialPlace?.radiusMeters ?: 150).coerceIn(50, 500))
    }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<GeocodedPlace>>(emptyList()) }
    var isLocating by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var showManualCoords by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun captureCurrentGps() {
        isLocating = true
        statusMessage = null
        scope.launch {
            val fix = LocationHelper.fetchCurrentLocation(context)
            if (fix != null) {
                latText = String.format(Locale.US, "%.6f", fix.latitude)
                lonText = String.format(Locale.US, "%.6f", fix.longitude)
                val label = LocationHelper.reverseGeocode(context, fix.latitude, fix.longitude)
                addressLabel = label
                if (name.isBlank()) name = tag.displayName
                statusMessage = "GPS locked (±${fix.accuracyMeters.roundToInt()} m): $label"
            } else {
                statusMessage = "Unable to lock GPS right now. Try turning on Location or searching by address."
            }
            isLocating = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            captureCurrentGps()
        } else {
            statusMessage = "Location permission needed to mark your current GPS spot."
        }
    }

    val parsedLat = latText.toDoubleOrNull()
    val parsedLon = lonText.toDoubleOrNull()
    val coordsValid = parsedLat != null && parsedLon != null &&
        parsedLat in -90.0..90.0 && parsedLon in -180.0..180.0
    val canSave = name.trim().isNotEmpty() && coordsValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (initialPlace == null) "Mark a Place" else "Edit Place")
                if (initialPlace != null && onDelete != null) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDelete(initialPlace)
                        }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete place",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Place name (e.g. College, Olive Restaurant)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Mark as", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PlaceTag.entries.forEach { t ->
                        FilterChip(
                            selected = tag == t,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val prevDefault = tag.displayName
                                tag = t
                                if (name.isBlank() || name == prevDefault) {
                                    name = t.displayName
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    placeTagIcon(t),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(t.displayName) }
                        )
                    }
                }

                // 1-Tap GPS Pin
                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (Permissions.hasAnyLocation(context)) {
                            captureCurrentGps()
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    enabled = !isLocating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Locking GPS coordinates...")
                    } else {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Mark Current GPS Location")
                    }
                }

                // Search Address / Landmark
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Or search address / restaurant") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (searchQuery.isNotBlank() && !isSearching) {
                                isSearching = true
                                statusMessage = null
                                scope.launch {
                                    val found = LocationHelper.searchAddress(context, searchQuery)
                                    searchResults = found
                                    if (found.isEmpty()) {
                                        statusMessage = "No address matches found — try including city name or use GPS."
                                    }
                                    isSearching = false
                                }
                            }
                        },
                        enabled = searchQuery.isNotBlank() && !isSearching
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = "Search address")
                        }
                    }
                }

                if (searchResults.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            searchResults.forEachIndexed { idx, candidate ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            latText = String.format(Locale.US, "%.6f", candidate.latitude)
                                            lonText = String.format(Locale.US, "%.6f", candidate.longitude)
                                            addressLabel = candidate.label
                                            if (name.isBlank()) name = searchQuery.trim()
                                            searchResults = emptyList()
                                            statusMessage = "Pinned: ${candidate.label}"
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = candidate.label,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (idx < searchResults.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }

                statusMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Current Pinned Coordinates Preview + Interactive Manual Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (coordsValid) "Zone pinned (${addressLabel.ifBlank { "$latText, $lonText" }})"
                        else "No coordinates pinned yet",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (coordsValid) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showManualCoords = !showManualCoords }) {
                        Text(if (showManualCoords) "Hide coords" else "Manual")
                    }
                }

                if (showManualCoords) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = latText,
                            onValueChange = { latText = it },
                            label = { Text("Latitude") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lonText,
                            onValueChange = { lonText = it },
                            label = { Text("Longitude") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Interactive Geofence Zone Preview
                GeofencePreviewCard(
                    placeName = name.ifBlank { tag.displayName },
                    radiusMeters = radiusMeters
                )

                Text(
                    "Coverage radius: $radiusMeters m",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(100, 150, 200, 300, 500).forEach { preset ->
                        FilterChip(
                            selected = radiusMeters == preset,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                radiusMeters = preset
                            },
                            label = { Text("${preset}m") }
                        )
                    }
                }
                Slider(
                    value = radiusMeters.toFloat(),
                    onValueChange = { radiusMeters = (it.roundToInt() / 10) * 10 },
                    valueRange = 50f..500f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = parsedLat ?: return@Button
                    val lon = parsedLon ?: return@Button
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSave(
                        SavedPlace(
                            id = initialPlace?.id ?: 0L,
                            name = name.trim(),
                            tag = tag,
                            addressLabel = addressLabel.ifBlank {
                                String.format(Locale.US, "%.4f, %.4f", lat, lon)
                            },
                            latitude = lat,
                            longitude = lon,
                            radiusMeters = radiusMeters,
                            createdAtMillis = initialPlace?.createdAtMillis ?: System.currentTimeMillis()
                        )
                    )
                },
                enabled = canSave
            ) {
                Text("Save Place")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun GeofencePreviewCard(
    placeName: String,
    radiusMeters: Int
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val container = MaterialTheme.colorScheme.surfaceContainerLow

    Surface(
        color = container,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(68.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(64.dp)) {
                    val ratio = ((radiusMeters - 50f) / 450f).coerceIn(0f, 1f)
                    val outerRadius = (size.minDimension / 2f) * (0.48f + 0.48f * ratio)
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        color = primary.copy(alpha = 0.16f),
                        radius = outerRadius,
                        center = center
                    )
                    drawCircle(
                        color = primary.copy(alpha = 0.85f),
                        radius = outerRadius,
                        center = center,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        )
                    )
                    drawCircle(
                        color = tertiary,
                        radius = 5.dp.toPx(),
                        center = center
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "$placeName • ${radiusMeters}m Zone",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Triggers automatically whenever you enter or step into this ${radiusMeters}m perimeter.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
