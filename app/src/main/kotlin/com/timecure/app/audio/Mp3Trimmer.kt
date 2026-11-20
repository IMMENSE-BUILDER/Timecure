package com.timecure.app.audio

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

data class TrimOutput(
    val uri: String,
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val physicallyTrimmed: Boolean
)

/**
 * Pure-Kotlin frame-accurate MP3 trimmer + resilient local cache.
 * Trims MP3 tracks on exact MPEG Layer-III frame boundaries into app-private storage so the
 * alarm tone stays playable even if the original external file is moved or storage permissions change.
 * Non-MP3 audio files are cached locally with exact [startMs..endMs] loop bounds for SoundManager.
 */
object Mp3Trimmer {

    private const val TAG = "Mp3Trimmer"

    suspend fun trimAndCache(
        context: Context,
        sourceUri: Uri,
        displayTitle: String,
        startMs: Long,
        endMs: Long
    ): TrimOutput = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "trimmed_tones").apply { mkdirs() }
        val safeStart = startMs.coerceAtLeast(0L)
        val safeEnd = endMs.coerceAtLeast(safeStart + 1000L)

        val rawBytes = runCatching {
            context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
        }.getOrNull()

        if (rawBytes != null && rawBytes.isNotEmpty()) {
            val trimmedMp3 = trimMp3Bytes(rawBytes, safeStart, safeEnd)
            if (trimmedMp3 != null && trimmedMp3.size > 1024) {
                val outFile = File(dir, "tone_${System.currentTimeMillis()}.mp3")
                outFile.writeBytes(trimmedMp3)
                Log.i(TAG, "Physically trimmed MP3 to ${trimmedMp3.size} bytes: ${outFile.name}")
                return@withContext TrimOutput(
                    uri = Uri.fromFile(outFile).toString(),
                    title = "$displayTitle (${LocalAudioLibrary.formatDuration(safeStart)}–${LocalAudioLibrary.formatDuration(safeEnd)})",
                    startMs = 0L,
                    endMs = 0L,
                    physicallyTrimmed = true
                )
            }

            // Fallback for non-MP3 (M4A/OGG/WAV) or non-standard headers: cache whole file + loop bounds
            val copyFile = File(dir, "tone_${System.currentTimeMillis()}.audio")
            copyFile.writeBytes(rawBytes)
            Log.i(TAG, "Cached audio locally with software trim bounds [$safeStart..$safeEnd]")
            return@withContext TrimOutput(
                uri = Uri.fromFile(copyFile).toString(),
                title = "$displayTitle (${LocalAudioLibrary.formatDuration(safeStart)}–${LocalAudioLibrary.formatDuration(safeEnd)})",
                startMs = safeStart,
                endMs = safeEnd,
                physicallyTrimmed = false
            )
        }

        // Last resort: use original content URI directly with trim bounds
        TrimOutput(
            uri = sourceUri.toString(),
            title = displayTitle,
            startMs = safeStart,
            endMs = safeEnd,
            physicallyTrimmed = false
        )
    }

    /**
     * Scans MPEG-1 / MPEG-2 / MPEG-2.5 Layer III frames and copies every frame whose timestamp
     * falls inside [startMs, endMs]. Returns null if the input does not contain valid MP3 frames.
     */
    fun trimMp3Bytes(data: ByteArray, startMs: Long, endMs: Long): ByteArray? {
        if (data.size < 16) return null
        var offset = skipId3v2(data)
        val out = ByteArrayOutputStream()
        var currentTimeMs = 0.0
        var validFrames = 0
        var copiedFrames = 0

        while (offset + 4 <= data.size) {
            val b0 = data[offset].toInt() and 0xFF
            val b1 = data[offset + 1].toInt() and 0xFF
            // 11-bit frame sync: 11111111 111xxxxx
            if (b0 != 0xFF || (b1 and 0xE0) != 0xE0) {
                offset++
                continue
            }

            val b2 = data[offset + 2].toInt() and 0xFF
            val versionBits = (b1 ushr 3) and 0x03 // 0=2.5, 1=reserved, 2=2, 3=1
            val layerBits = (b1 ushr 1) and 0x03   // 1=Layer III
            val bitrateIdx = (b2 ushr 4) and 0x0F
            val sampleRateIdx = (b2 ushr 2) and 0x03
            val padding = (b2 ushr 1) and 0x01

            if (versionBits == 1 || layerBits != 1 || bitrateIdx == 0 || bitrateIdx == 15 || sampleRateIdx == 3) {
                offset++
                continue
            }

            val isMpeg1 = (versionBits == 3)
            val bitrateKbps = if (isMpeg1) MPEG1_L3_BITRATES[bitrateIdx] else MPEG2_L3_BITRATES[bitrateIdx]
            val sampleRate = when (versionBits) {
                3 -> MPEG1_SAMPLE_RATES[sampleRateIdx]
                2 -> MPEG2_SAMPLE_RATES[sampleRateIdx]
                else -> MPEG25_SAMPLE_RATES[sampleRateIdx]
            }
            if (bitrateKbps <= 0 || sampleRate <= 0) {
                offset++
                continue
            }

            val samplesPerFrame = if (isMpeg1) 1152 else 576
            val coef = if (isMpeg1) 144 else 72
            val frameLen = (coef * bitrateKbps * 1000) / sampleRate + padding
            if (frameLen < 24 || offset + frameLen > data.size) break

            val frameDurationMs = (samplesPerFrame * 1000.0) / sampleRate
            validFrames++

            if (currentTimeMs >= startMs && currentTimeMs <= endMs) {
                out.write(data, offset, frameLen)
                copiedFrames++
            }
            currentTimeMs += frameDurationMs
            offset += frameLen
            if (currentTimeMs > endMs && copiedFrames > 0) break
        }

        return if (validFrames >= 5 && copiedFrames >= 2) out.toByteArray() else null
    }

    private fun skipId3v2(data: ByteArray): Int {
        if (data.size >= 10 &&
            data[0] == 'I'.code.toByte() &&
            data[1] == 'D'.code.toByte() &&
            data[2] == '3'.code.toByte()
        ) {
            val size = ((data[6].toInt() and 0x7F) shl 21) or
                ((data[7].toInt() and 0x7F) shl 14) or
                ((data[8].toInt() and 0x7F) shl 7) or
                (data[9].toInt() and 0x7F)
            val end = 10 + size
            if (end in 10 until data.size) return end
        }
        return 0
    }

    private val MPEG1_L3_BITRATES = intArrayOf(0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0)
    private val MPEG2_L3_BITRATES = intArrayOf(0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0)
    private val MPEG1_SAMPLE_RATES = intArrayOf(44100, 48000, 32000, 0)
    private val MPEG2_SAMPLE_RATES = intArrayOf(22050, 24000, 16000, 0)
    private val MPEG25_SAMPLE_RATES = intArrayOf(11025, 12000, 8000, 0)
}
