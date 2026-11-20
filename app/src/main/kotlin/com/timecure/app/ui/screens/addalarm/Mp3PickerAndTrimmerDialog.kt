package com.timecure.app.ui.screens.addalarm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.timecure.app.TimecureApp
import com.timecure.app.audio.AudioTrackItem
import com.timecure.app.audio.LocalAudioLibrary
import com.timecure.app.audio.Mp3Trimmer
import com.timecure.app.audio.TrimOutput
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Mp3PickerAndTrimmerSheet(
    onTrimConfirmed: (TrimOutput) -> Unit,
    onPickSystemRingtone: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val soundManager = (context.applicationContext as TimecureApp).container.soundManager
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    var tracks by remember { mutableStateOf<List<AudioTrackItem>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTrack by remember { mutableStateOf<AudioTrackItem?>(null) }

    DisposableEffect(Unit) {
        onDispose { soundManager.stopPreview() }
    }

    fun scanAllAudio() {
        scope.launch {
            isScanning = true
            tracks = LocalAudioLibrary.scanDeviceAudio(context)
            isScanning = false
        }
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) scanAllAudio()
    }

    // SAF file picker lets user grab ANY MP3 in Downloads/WhatsApp/Telegram even without MediaStore
    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            scope.launch {
                selectedTrack = LocalAudioLibrary.inspectUri(context, uri)
            }
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) scanAllAudio()
    }

    ModalBottomSheet(
        onDismissRequest = {
            soundManager.stopPreview()
            onDismiss()
        },
        sheetState = sheetState
    ) {
        val editing = selectedTrack
        if (editing == null) {
            // STEP 1: Browse all MP3s on device
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                Text("Select MP3 alarm tone", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Pick any MP3 on your phone, then trim the exact segment you want.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { filePickerLauncher.launch(arrayOf("audio/mpeg", "audio/*")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Browse MP3")
                    }
                    OutlinedButton(
                        onClick = {
                            soundManager.stopPreview()
                            onPickSystemRingtone()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("System Tones")
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (!hasPermission) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Allow audio library access to list every MP3 on your phone automatically (or tap Browse MP3 above).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Button(onClick = { permLauncher.launch(audioPermission) }) {
                                Text("Scan phone MP3 files")
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                } else {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        placeholder = { Text("Search ${tracks.size} audio files...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    if (isScanning) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    } else {
                        val filtered = remember(tracks, searchQuery) {
                            if (searchQuery.isBlank()) tracks
                            else tracks.filter {
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                    it.artist.contains(searchQuery, ignoreCase = true)
                            }
                        }
                        if (filtered.isEmpty()) {
                            Text(
                                text = "No audio files matched. Tap \"Browse MP3\" to open any folder on your phone.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                                items(filtered, key = { it.uri }) { track ->
                                    ListItem(
                                        leadingContent = {
                                            Icon(
                                                imageVector = Icons.Default.AudioFile,
                                                contentDescription = null,
                                                tint = if (track.isMp3) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        headlineContent = {
                                            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        },
                                        supportingContent = {
                                            Text(
                                                "${track.artist}  •  ${LocalAudioLibrary.formatDuration(track.durationMs)}" +
                                                    if (track.isMp3) "  •  MP3" else "",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        trailingContent = {
                                            Icon(
                                                Icons.Default.ContentCut,
                                                contentDescription = "Trim track",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        modifier = Modifier.clickable { selectedTrack = track }
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        } else {
            // STEP 2: Interactive Waveform Audio Trimmer
            AudioTrimmerView(
                track = editing,
                onBack = {
                    soundManager.stopPreview()
                    selectedTrack = null
                },
                onConfirmTrim = { startMs, endMs ->
                    soundManager.stopPreview()
                    scope.launch {
                        val result = Mp3Trimmer.trimAndCache(
                            context = context,
                            sourceUri = Uri.parse(editing.uri),
                            displayTitle = editing.title,
                            startMs = startMs,
                            endMs = endMs
                        )
                        onTrimConfirmed(result)
                    }
                }
            )
        }
    }
}

@Composable
private fun AudioTrimmerView(
    track: AudioTrackItem,
    onBack: () -> Unit,
    onConfirmTrim: (startMs: Long, endMs: Long) -> Unit
) {
    val context = LocalContext.current
    val soundManager = (context.applicationContext as TimecureApp).container.soundManager
    val totalMs = track.durationMs.coerceAtLeast(3000L)
    val minClipMs = 3000L.coerceAtMost(totalMs)

    var range by remember(track.uri) {
        val defaultEnd = totalMs.coerceAtMost(30_000L).toFloat()
        mutableStateOf(0f..defaultEnd)
    }
    var isPlaying by remember { mutableStateOf(false) }
    var playheadMs by remember { mutableLongStateOf(0L) }
    var isSaving by remember { mutableStateOf(false) }

    val startMs = range.start.toLong().coerceIn(0L, totalMs)
    val endMs = range.endInclusive.toLong().coerceIn((startMs + minClipMs).coerceAtMost(totalMs), totalMs)
    val clipSecs = ((endMs - startMs) / 1000L).coerceAtLeast(1L)

    fun startLoopPreview(s: Long, e: Long) {
        playheadMs = s
        isPlaying = soundManager.previewTone(
            toneUri = track.uri,
            startMs = s,
            endMs = e,
            onPositionMs = { pos -> playheadMs = pos }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to MP3 list")
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Trim alarm clip • ${LocalAudioLibrary.formatDuration(startMs)} to ${LocalAudioLibrary.formatDuration(endMs)} (${clipSecs}s)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Expressive Canvas Waveform with Active Selection Highlight & Moving Playhead
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            val activeColor = MaterialTheme.colorScheme.primary
            val dimColor = MaterialTheme.colorScheme.surfaceContainerHighest
            val playheadColor = MaterialTheme.colorScheme.tertiary
            val seed = remember(track.uri) { track.uri.hashCode() }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                val barCount = 48
                val gap = 3.dp.toPx()
                val barW = ((size.width - gap * (barCount - 1)) / barCount).coerceAtLeast(2.dp.toPx())
                val startFrac = startMs.toFloat() / totalMs.toFloat()
                val endFrac = endMs.toFloat() / totalMs.toFloat()

                for (i in 0 until barCount) {
                    val frac = i.toFloat() / barCount.toFloat()
                    val wave = 0.25f + 0.72f * abs(sin((i * 0.65 + (seed % 17)) * 0.85)).toFloat()
                    val h = size.height * wave
                    val left = i * (barW + gap)
                    val top = (size.height - h) / 2f
                    val inside = frac in startFrac..endFrac
                    drawRoundRect(
                        color = if (inside) activeColor else dimColor,
                        topLeft = Offset(left, top),
                        size = Size(barW, h),
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                    )
                }

                if (isPlaying) {
                    val headFrac = (playheadMs.toFloat() / totalMs.toFloat()).coerceIn(startFrac, endFrac)
                    val hx = size.width * headFrac
                    drawLine(
                        color = playheadColor,
                        start = Offset(hx, 0f),
                        end = Offset(hx, size.height),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        RangeSlider(
            value = range,
            onValueChange = { next ->
                val clampedEnd = next.endInclusive.coerceAtLeast(next.start + minClipMs)
                range = next.start.coerceAtLeast(0f)..clampedEnd.coerceAtMost(totalMs.toFloat())
            },
            onValueChangeFinished = {
                if (isPlaying) startLoopPreview(startMs, endMs)
            },
            valueRange = 0f..totalMs.toFloat(),
            modifier = Modifier.fillMaxWidth()
        )

        // Fine 1-second nudges + quick clip lengths
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    val ns = (startMs - 1000L).coerceAtLeast(0L)
                    range = ns.toFloat()..endMs.toFloat()
                    if (isPlaying) startLoopPreview(ns, endMs)
                }) { Text("-1s") }
                Text(LocalAudioLibrary.formatDuration(startMs), style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = {
                    val ns = (startMs + 1000L).coerceAtMost(endMs - minClipMs)
                    range = ns.toFloat()..endMs.toFloat()
                    if (isPlaying) startLoopPreview(ns, endMs)
                }) { Text("+1s") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    val ne = (endMs - 1000L).coerceAtLeast(startMs + minClipMs)
                    range = startMs.toFloat()..ne.toFloat()
                    if (isPlaying) startLoopPreview(startMs, ne)
                }) { Text("-1s") }
                Text(LocalAudioLibrary.formatDuration(endMs), style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = {
                    val ne = (endMs + 1000L).coerceAtMost(totalMs)
                    range = startMs.toFloat()..ne.toFloat()
                    if (isPlaying) startLoopPreview(startMs, ne)
                }) { Text("+1s") }
            }
        }

        // Quick duration preset chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(15_000L to "15s", 30_000L to "30s", 45_000L to "45s", totalMs to "Full").forEach { (len, label) ->
                SuggestionChip(
                    onClick = {
                        val targetEnd = (startMs + len).coerceAtMost(totalMs)
                        val targetStart = if (len == totalMs) 0L else startMs
                        range = targetStart.toFloat()..targetEnd.toFloat()
                        if (isPlaying) startLoopPreview(targetStart, targetEnd)
                    },
                    label = { Text(label) }
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = {
                    if (isPlaying) {
                        soundManager.stopPreview()
                        isPlaying = false
                    } else {
                        startLoopPreview(startMs, endMs)
                    }
                },
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(if (isPlaying) "Stop" else "Preview")
            }

            Button(
                onClick = {
                    if (!isSaving) {
                        isSaving = true
                        onConfirmTrim(startMs, endMs)
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.weight(1.4f).height(50.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Trim & Use")
                }
            }
        }
    }
}
