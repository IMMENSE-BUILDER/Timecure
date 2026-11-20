package com.timecure.app.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.audiofx.LoudnessEnhancer
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
 * Plays the alarm on STREAM_ALARM at full stream volume, supporting:
 * - Bounded or trimmed MP3 segments
 * - User volume slider scaling (overridden to 100% + DSP boost on Extra Loud or re-rings)
 * - Gentle wake-up smooth exponential/linear ramp (e.g. 30s)
 * - Hardware LoudnessEnhancer DSP boost (+9 dB) for Extra Loud mode
 * - Smooth TTS voice reminder ducking while speaking time / weather / label
 */
class SoundManager(private val context: Context) {

    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var player: MediaPlayer? = null
    private var previewPlayer: MediaPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var previewEnhancer: LoudnessEnhancer? = null
    private var loopWatchJob: Job? = null
    private var gentleRampJob: Job? = null
    private var previewWatchJob: Job? = null
    private var extraLoudSampleJob: Job? = null
    private var focus: AudioFocusRequest? = null
    private var savedVolume = -1

    @Volatile
    private var currentTargetGain = 1f

    @Volatile
    private var currentRampGain = 1f

    @Volatile
    private var isDucked = false

    @Volatile
    private var previewBaseGain = 1f

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
        endMs: Long = 0L,
        volumePercent: Int = 100,
        gentleWakeSeconds: Int = 0,
        extraLoud: Boolean = false
    ) {
        stopPreview()
        if (isRinging) return
        isRinging = true
        isDucked = false

        val boost = extraLoud || rerings > 0
        currentTargetGain = if (boost) 1f else (volumePercent / 100f).coerceIn(0.08f, 1f)
        val effectiveGentleSecs = if (rerings > 0) 0 else gentleWakeSeconds.coerceAtLeast(0)
        currentRampGain = if (effectiveGentleSecs > 0) 0.08f else currentTargetGain

        runCatching {
            savedVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
            audio.setStreamVolume(AudioManager.STREAM_ALARM, audio.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
        }.onFailure { Log.w(TAG, "Could not force volume: ${it.message}") }

        runCatching { requestFocus() }.onFailure { Log.w(TAG, "Audio focus failed: ${it.message}") }

        startPlayback(toneUri, startMs, endMs, boost, effectiveGentleSecs)
        if (vibrate) startVibration(rerings)
    }

    private fun startPlayback(
        toneUri: String,
        startMs: Long,
        endMs: Long,
        boost: Boolean,
        gentleWakeSeconds: Int
    ) {
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
                val startGain = currentRampGain * if (isDucked) 0.25f else 1f
                mp.setVolume(startGain, startGain)
                if (boost) {
                    attachEnhancer(mp, isPreview = false)
                }
                mp.start()
                player = mp

                if (gentleWakeSeconds > 0) {
                    gentleRampJob?.cancel()
                    gentleRampJob = scope.launch {
                        val stepMs = 250L
                        val steps = ((gentleWakeSeconds * 1000L) / stepMs).coerceAtLeast(1L)
                        for (step in 1L..steps) {
                            if (!isActive || !isRinging || player !== mp) break
                            val t = step.toFloat() / steps.toFloat()
                            currentRampGain = 0.08f + (currentTargetGain - 0.08f) * (t * t)
                            applyCurrentAlarmVolume(mp)
                            delay(stepMs)
                        }
                        if (isActive && isRinging && player === mp) {
                            currentRampGain = currentTargetGain
                            applyCurrentAlarmVolume(mp)
                        }
                    }
                }

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

    private fun applyCurrentAlarmVolume(mp: MediaPlayer?) {
        val g = (currentRampGain * if (isDucked) 0.25f else 1f).coerceIn(0.03f, 1f)
        runCatching { mp?.setVolume(g, g) }
    }

    fun setVoiceDucking(ducked: Boolean) {
        isDucked = ducked
        applyCurrentAlarmVolume(player)
    }

    private fun attachEnhancer(mp: MediaPlayer, isPreview: Boolean) {
        runCatching {
            val enhancer = LoudnessEnhancer(mp.audioSessionId).apply {
                setTargetGain(900) // +9 dB clean hardware boost
                enabled = true
            }
            if (isPreview) {
                runCatching { previewEnhancer?.release() }
                previewEnhancer = enhancer
            } else {
                runCatching { loudnessEnhancer?.release() }
                loudnessEnhancer = enhancer
            }
        }.onFailure {
            Log.w(TAG, "LoudnessEnhancer unavailable on this audio HAL: ${it.message}")
        }
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
        isDucked = false
        gentleRampJob?.cancel()
        gentleRampJob = null
        loopWatchJob?.cancel()
        loopWatchJob = null
        runCatching { loudnessEnhancer?.release() }
        loudnessEnhancer = null
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

    /** Short non-destructive preview (supports [startMs..endMs], volume slider, DSP boost & progress bar). */
    @Synchronized
    fun previewTone(
        toneUri: String,
        startMs: Long = 0L,
        endMs: Long = 0L,
        volumePercent: Int = 100,
        extraLoud: Boolean = false,
        onPositionMs: ((Long) -> Unit)? = null,
        onProgressFraction: ((Float) -> Unit)? = null
    ): Boolean {
        stopPreview()
        if (isRinging) return false
        val uri = toneUri.takeIf { it.isNotBlank() }?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: return false
        val mp = MediaPlayer()
        previewBaseGain = if (extraLoud) 1f else (volumePercent / 100f).coerceIn(0.05f, 1f)
        return try {
            mp.setAudioAttributes(alarmAttributes)
            mp.setDataSource(context, uri)
            val bounded = endMs > startMs && endMs > 0L
            mp.isLooping = bounded
            mp.setOnCompletionListener {
                if (!bounded) {
                    onProgressFraction?.invoke(0f)
                    stopPreview()
                }
            }
            mp.prepare()
            mp.setVolume(previewBaseGain, previewBaseGain)
            if (extraLoud) {
                attachEnhancer(mp, isPreview = true)
            }
            if (bounded && startMs > 0L) mp.seekTo(startMs.toInt())
            mp.start()
            previewPlayer = mp
            if (bounded || onPositionMs != null || onProgressFraction != null) {
                previewWatchJob?.cancel()
                previewWatchJob = scope.launch {
                    while (isActive && previewPlayer === mp) {
                        runCatching {
                            if (mp.isPlaying) {
                                val pos = mp.currentPosition.toLong()
                                onPositionMs?.invoke(pos)
                                val span = if (bounded) (endMs - startMs).coerceAtLeast(1L)
                                else mp.duration.toLong().coerceAtLeast(1L)
                                val rel = if (bounded) (pos - startMs).coerceAtLeast(0L) else pos
                                onProgressFraction?.invoke((rel.toFloat() / span.toFloat()).coerceIn(0f, 1f))
                                if (bounded && pos >= endMs) {
                                    mp.seekTo(startMs.toInt())
                                }
                            }
                        }
                        delay(70L)
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
    fun updatePreviewVolume(volumePercent: Int, extraLoud: Boolean) {
        val mp = previewPlayer ?: return
        previewBaseGain = if (extraLoud) 1f else (volumePercent / 100f).coerceIn(0.05f, 1f)
        runCatching { mp.setVolume(previewBaseGain, previewBaseGain) }
        if (extraLoud) {
            if (previewEnhancer == null) attachEnhancer(mp, isPreview = true)
            else runCatching { previewEnhancer?.enabled = true }
        } else {
            runCatching { previewEnhancer?.enabled = false }
        }
    }

    @Synchronized
    fun previewExtraLoudSample(
        toneUri: String,
        startMs: Long = 0L,
        endMs: Long = 0L,
        onFinished: () -> Unit = {}
    ): Boolean {
        val started = previewTone(
            toneUri = toneUri,
            startMs = startMs,
            endMs = endMs,
            volumePercent = 100,
            extraLoud = true
        )
        if (!started) {
            onFinished()
            return false
        }
        extraLoudSampleJob?.cancel()
        extraLoudSampleJob = scope.launch {
            delay(3_500L)
            stopPreview()
            onFinished()
        }
        return true
    }

    @Synchronized
    fun stopPreview() {
        extraLoudSampleJob?.cancel()
        extraLoudSampleJob = null
        previewWatchJob?.cancel()
        previewWatchJob = null
        runCatching { previewEnhancer?.release() }
        previewEnhancer = null
        runCatching { previewPlayer?.let { if (it.isPlaying) it.stop(); it.release() } }
        previewPlayer = null
    }

    private companion object {
        const val TAG = "SoundManager"
    }
}
