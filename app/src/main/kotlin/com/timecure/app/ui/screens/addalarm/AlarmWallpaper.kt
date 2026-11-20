package com.timecure.app.ui.screens.addalarm

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class AlarmWallpaperPreset(
    val id: String,
    val title: String,
    val quote: String,
    val colors: List<Color>,
    val quoteHighlightBg: Color?,
    val quoteTextColor: Color
) {
    YESTERDAY_TOMORROW(
        id = "preset:yesterday_tomorrow",
        title = "Yesterday, Tomorrow",
        quote = "Yesterday, you said\ntomorrow.",
        colors = listOf(Color(0xFFE7DFD8), Color(0xFFD2C7BF)),
        quoteHighlightBg = Color(0xFFE5DF45),
        quoteTextColor = Color(0xFF1A1916)
    ),
    SUNRISE_DAWN(
        id = "preset:sunrise_dawn",
        title = "Sunrise Horizon",
        quote = "Own the first hour.",
        colors = listOf(Color(0xFF28183A), Color(0xFF8C3B2B), Color(0xFFF3A952)),
        quoteHighlightBg = null,
        quoteTextColor = Color(0xFFFFF7EE)
    ),
    MIDNIGHT_FOCUS(
        id = "preset:midnight_focus",
        title = "Midnight Focus",
        quote = "Discipline beats mood.",
        colors = listOf(Color(0xFF0A1320), Color(0xFF123458), Color(0xFF1E4E79)),
        quoteHighlightBg = null,
        quoteTextColor = Color(0xFFD6E9FF)
    ),
    BOTANICAL_CALM(
        id = "preset:botanical_calm",
        title = "Botanical Dawn",
        quote = "Breathe. Rise. Execute.",
        colors = listOf(Color(0xFF0C241E), Color(0xFF0B5242), Color(0xFF429E82)),
        quoteHighlightBg = null,
        quoteTextColor = Color(0xFFE4FFF6)
    ),
    NONE(
        id = "preset:none",
        title = "System Theme",
        quote = "",
        colors = listOf(Color(0xFF262A32), Color(0xFF1E2229)),
        quoteHighlightBg = null,
        quoteTextColor = Color(0xFFCBD5E1)
    );

    companion object {
        fun fromId(raw: String?): AlarmWallpaperPreset? =
            entries.firstOrNull { it.id == raw }
    }
}

suspend fun importWallpaperFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = context.contentResolver
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOpts) }
        val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight).coerceAtLeast(1)
        var sample = 1
        while (maxDim / sample > 1440) {
            sample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: return@withContext null

        val dir = File(context.filesDir, "wallpapers").apply { mkdirs() }
        val outFile = File(dir, "wp_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, fos)
        }
        bitmap.recycle()
        "file://${outFile.absolutePath}"
    }.getOrNull()
}

@Composable
private fun rememberLocalWallpaperBitmap(wallpaperUri: String): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = wallpaperUri) {
        value = if (wallpaperUri.startsWith("file://")) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val path = wallpaperUri.removePrefix("file://")
                    BitmapFactory.decodeFile(path)?.asImageBitmap()
                }.getOrNull()
            }
        } else null
    }
    return bitmap
}

@Composable
fun AlarmWallpaperThumbnail(
    wallpaperUri: String,
    modifier: Modifier = Modifier
) {
    val preset = remember(wallpaperUri) { AlarmWallpaperPreset.fromId(wallpaperUri) }
    val photo = rememberLocalWallpaperBitmap(wallpaperUri)
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = preset?.colors ?: AlarmWallpaperPreset.NONE.colors
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = "Selected alarm wallpaper",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (preset != null) {
            if (preset.quote.isNotBlank()) {
                val highlight = preset.quoteHighlightBg
                if (highlight != null) {
                    Surface(
                        color = highlight,
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.padding(horizontal = 5.dp)
                    ) {
                        Text(
                            text = preset.quote,
                            fontSize = 6.5.sp,
                            lineHeight = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = preset.quoteTextColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Text(
                        text = preset.quote,
                        fontSize = 7.sp,
                        lineHeight = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = preset.quoteTextColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            } else {
                Text(
                    text = "Default",
                    fontSize = 9.sp,
                    color = preset.quoteTextColor
                )
            }
        }
    }
}

@Composable
fun AlarmWallpaperBackdrop(
    wallpaperUri: String,
    modifier: Modifier = Modifier
) {
    val preset = remember(wallpaperUri) { AlarmWallpaperPreset.fromId(wallpaperUri) }
    val photo = rememberLocalWallpaperBitmap(wallpaperUri)

    if (preset == AlarmWallpaperPreset.NONE && photo == null) return

    Box(modifier = modifier) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            )
        } else if (preset != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(preset.colors))
            )
            if (preset.quote.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 42.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        color = preset.quoteHighlightBg ?: Color.Black.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = preset.quote.replace("\n", " "),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                            color = if (preset.quoteHighlightBg != null) preset.quoteTextColor
                            else Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.68f))
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlarmWallpaperPickerDialog(
    currentUri: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val imported = importWallpaperFromUri(context, uri)
                if (imported != null) {
                    onSelect(imported)
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alarm Wallpaper") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = { galleryPicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Choose photo from gallery")
                }

                if (currentUri.startsWith("file://")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AlarmWallpaperThumbnail(
                            wallpaperUri = currentUri,
                            modifier = Modifier.size(width = 56.dp, height = 78.dp)
                        )
                        Text(
                            text = "Custom photo active",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text("Built-in wake-up cards", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AlarmWallpaperPreset.entries.forEach { preset ->
                        val selected = currentUri == preset.id
                        Surface(
                            onClick = { onSelect(preset.id) },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(width = 76.dp, height = 104.dp)) {
                                    AlarmWallpaperThumbnail(
                                        wallpaperUri = preset.id,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (selected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = preset.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
