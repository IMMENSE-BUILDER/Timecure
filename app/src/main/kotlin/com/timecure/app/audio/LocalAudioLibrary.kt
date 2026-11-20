package com.timecure.app.audio

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AudioTrackItem(
    val uri: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val isMp3: Boolean
)

object LocalAudioLibrary {

    /**
     * Scans MediaStore across the phone for all MP3 and audio files (duration >= 1s).
     */
    suspend fun scanDeviceAudio(context: Context): List<AudioTrackItem> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<AudioTrackItem>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.MIME_TYPE
        )
        val selection = "${MediaStore.Audio.Media.DURATION} >= 1000"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        runCatching {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val displayName = cursor.getString(nameCol).orEmpty()
                    val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() }
                        ?: displayName.substringBeforeLast('.')
                    val artist = cursor.getString(artistCol)?.takeIf { it.isNotBlank() && it != "<unknown>" }
                        ?: "Local audio"
                    val duration = cursor.getLong(durCol)
                    val mime = cursor.getString(mimeCol).orEmpty()
                    val isMp3 = mime.contains("mpeg", ignoreCase = true) ||
                        displayName.endsWith(".mp3", ignoreCase = true)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    )
                    tracks += AudioTrackItem(
                        uri = contentUri.toString(),
                        title = title,
                        artist = artist,
                        durationMs = duration,
                        isMp3 = isMp3
                    )
                }
            }
        }
        // Prioritize MP3 files first while still keeping all phone audio accessible
        tracks.sortedWith(compareByDescending<AudioTrackItem> { it.isMp3 }.thenBy { it.title.lowercase() })
    }

    /**
     * Resolves title and duration for an arbitrary audio URI picked via the system file browser.
     */
    suspend fun inspectUri(context: Context, uri: Uri): AudioTrackItem = withContext(Dispatchers.IO) {
        var displayName = "Custom MP3"
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) displayName = c.getString(0) ?: displayName
            }
        }
        var durationMs = 30_000L
        var artist = "External file"
        val retriever = MediaMetadataRetriever()
        runCatching {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
                if (it > 0) durationMs = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }?.let {
                artist = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }?.let {
                displayName = it
            }
        }
        runCatching { retriever.release() }

        AudioTrackItem(
            uri = uri.toString(),
            title = displayName.substringBeforeLast('.'),
            artist = artist,
            durationMs = durationMs,
            isMp3 = displayName.endsWith(".mp3", ignoreCase = true) ||
                (context.contentResolver.getType(uri)?.contains("mpeg", ignoreCase = true) == true)
        )
    }

    fun formatDuration(ms: Long): String {
        val totalSecs = (ms / 1000L).coerceAtLeast(0L)
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return "%d:%02d".format(mins, secs)
    }
}
