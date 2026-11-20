package com.timecure.app.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Plays the alarm on STREAM_ALARM at full volume, supporting both physically trimmed MP3 files
 * and bounded [startMs..endMs] looping segments. Every step is isolated so one failing call
 * can never leave the alarm silent.
 */
class SoundManager(private val context: Context) {

    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var player: MediaPlayer? = null
    private var previewPlayer: MediaPlayer? = null
    private var loopWatchJob: Job? = null
    private var previewWatchJob: Job? = null
    private var focus: AudioFocusRequest? = null
    private var savedVolume = -1

    @Volatile
    var isRinging = false
        private set

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private val alarmAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    @Synchronized
    fun start(
        toneUri: String,
        vibrate: Boolean,
        rerings: Int,
        startMs: Long = 0L,
        endMs: Long = 0L
    ) {
        stopPreview()
        if (isRinging) return
        isRinging = true

        runCatching {
            savedVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
            audio.setStreamVolume(AudioManager.STREAM_ALARM, audio.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
        }.onFailure { Log.w(TAG, "Could not force volume: ${it.message}") }

        runCatching { requestFocus() }.onFailure { Log.w(TAG, "Audio focus failed: ${it.message}") }

        startPlayback(toneUri, startMs, endMs)
        if (vibrate) startVibration(rerings)
    }

    private fun startPlayback(toneUri: String, startMs: Long, endMs: Long) {
        val candidates = buildList {
            if (toneUri.isNotBlank()) add(Uri.parse(toneUri) to true)
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.let { add(it to false) }
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)?.let { add(it to false) }
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.let { add(it to false) }
        }
        for ((uri, applyBounds) in candidates) {
            val mp = MediaPlayer()
            try {
                mp.setAudioAttributes(alarmAttributes)
                mp.setDataSource(context, uri)
                mp.isLooping = true
                mp.prepare()
                val bounded = applyBounds && endMs > startMs && endMs > 0L
                if (bounded && startMs > 0L) {
                    mp.seekTo(startMs.toInt())
                }
                mp.start()
                player = mp
                if (bounded) {
                    loopWatchJob?.cancel()
                    loopWatchJob = scope.launch {
                        while (isActive && isRinging) {
                            runCatching {
                                if (mp.isPlaying && mp.currentPosition >= endMs) {
                                    mp.seekTo(startMs.toInt())
                                }
                            }
                            delay(120L)
                        }
                    }
                }
                return
            } catch (e: Exception) {
                Log.w(TAG, "Tone $uri failed (${e.message}), trying next")
                runCatching { mp.release() }
            }
        }
        Log.e(TAG, "No playable alarm tone found")
    }

    private fun startVibration(rerings: Int) {
        val pattern = if (rerings == 0) longArrayOf(0, 600, 250, 600, 250, 600, 900)
        else longArrayOf(0, 1000, 120, 1000, 120, 1000, 300)
        runCatching {
            val effect = VibrationEffect.createWaveform(pattern, 0)
            @Suppress("DEPRECATION")
            vibrator?.vibrate(effect, alarmAttributes)
        }.onFailure { Log.w(TAG, "Vibration failed: ${it.message}") }
    }

    private fun requestFocus() {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
            .setAudioAttributes(alarmAttributes)
            .setOnAudioFocusChangeListener { /* never yield while alarm rings */ }
            .build()
        focus = request
        audio.requestAudioFocus(request)
    }

    @Synchronized
    fun stop() {
        if (!isRinging) return
        isRinging = false
        loopWatchJob?.cancel()
        loopWatchJob = null
        runCatching { player?.let { if (it.isPlaying) it.stop(); it.release() } }
        player = null
        runCatching { vibrator?.cancel() }
        runCatching {
            if (savedVolume >= 0) audio.setStreamVolume(AudioManager.STREAM_ALARM, savedVolume, 0)
        }
        savedVolume = -1
        runCatching { focus?.let { audio.abandonAudioFocusRequest(it) } }
        focus = null
    }

    /** Short non-destructive preview (supports [startMs..endMs] looping with live position callback). */
    @Synchronized
    fun previewTone(
        toneUri: String,
        startMs: Long = 0L,
        endMs: Long = 0L,
        onPositionMs: ((Long) -> Unit)? = null
    ): Boolean {
        stopPreview()
        if (isRinging) return false
        val uri = toneUri.takeIf { it.isNotBlank() }?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: return false
        val mp = MediaPlayer()
        return try {
            mp.setAudioAttributes(alarmAttributes)
            mp.setDataSource(context, uri)
            val bounded = endMs > startMs && endMs > 0L
            mp.isLooping = bounded
            mp.setOnCompletionListener { if (!bounded) stopPreview() }
            mp.prepare()
            if (bounded && startMs > 0L) mp.seekTo(startMs.toInt())
            mp.start()
            previewPlayer = mp
            if (bounded || onPositionMs != null) {
                previewWatchJob?.cancel()
                previewWatchJob = scope.launch {
                    while (isActive && previewPlayer === mp) {
                        runCatching {
                            if (mp.isPlaying) {
                                val pos = mp.currentPosition.toLong()
                                onPositionMs?.invoke(pos)
                                if (bounded && pos >= endMs) {
                                    mp.seekTo(startMs.toInt())
                                }
                            }
                        }
                        delay(80L)
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Preview failed: ${e.message}")
            runCatching { mp.release() }
            false
        }
    }

    @Synchronized
    fun stopPreview() {
        previewWatchJob?.cancel()
        previewWatchJob = null
        runCatching { previewPlayer?.let { if (it.isPlaying) it.stop(); it.release() } }
        previewPlayer = null
    }

    private companion object {
        const val TAG = "SoundManager"
    }
}
