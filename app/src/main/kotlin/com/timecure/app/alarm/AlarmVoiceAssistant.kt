package com.timecure.app.alarm

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.timecure.app.TimecureApp
import com.timecure.app.model.Alarm
import com.timecure.app.tasks.LocationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Speaks spoken Time, Live Weather (via Open-Meteo), and Alarm Label reminders
 * both on demand from the Alarm Settings "Sample" buttons and in a periodic ducked
 * voice loop while an alarm is actively ringing.
 */
class AlarmVoiceAssistant(
    private val context: Context,
    private val soundManager: SoundManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .build()

    private var tts: TextToSpeech? = null
    @Volatile
    private var ttsReady = false
    private var pendingUtterance: Triple<String, Boolean, (() -> Unit)?>? = null
    private var loopJob: Job? = null

    private fun ensureTts() {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = tts ?: return@TextToSpeech
                engine.language = Locale.getDefault()
                engine.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        soundManager.setVoiceDucking(false)
                    }

                    @Deprecated("Deprecated in API 21")
                    override fun onError(utteranceId: String?) {
                        soundManager.setVoiceDucking(false)
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        soundManager.setVoiceDucking(false)
                    }
                })
                ttsReady = true
                pendingUtterance?.let { (txt, duck, doneCb) ->
                    pendingUtterance = null
                    speakText(txt, duck, doneCb)
                }
            } else {
                Log.w(TAG, "TTS initialization failed: status=$status")
            }
        }
    }

    fun speakText(text: String, duckAlarm: Boolean = false, onDone: (() -> Unit)? = null) {
        val clean = text.trim()
        if (clean.isBlank()) {
            onDone?.invoke()
            return
        }
        ensureTts()
        if (!ttsReady) {
            pendingUtterance = Triple(clean, duckAlarm, onDone)
            return
        }
        if (duckAlarm) {
            soundManager.setVoiceDucking(true)
        }
        val uid = "tc_${System.currentTimeMillis()}"
        runCatching {
            tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, uid)
        }.onFailure {
            soundManager.setVoiceDucking(false)
            Log.w(TAG, "TTS speak failed: ${it.message}")
        }
        onDone?.invoke()
    }

    fun speakTimeSample() {
        val formatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        speakText("It is $formatted.")
    }

    fun speakWeatherSample(context: Context) {
        scope.launch {
            val sentence = fetchLiveWeatherSentence(context)
            speakText(sentence)
        }
    }

    fun speakLabelSample(label: String, goal: String, wakePrompt: String) {
        val parts = listOf(label, goal, wakePrompt)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        val message = if (parts.isEmpty()) "Time to wake up and win your morning."
        else parts.take(2).joinToString(". ")
        speakText(message)
    }

    fun startRingingLoop(context: Context, alarm: Alarm) {
        val anyEnabled = alarm.timeReminderEnabled ||
            alarm.weatherReminderEnabled ||
            alarm.labelReminderEnabled
        if (!anyEnabled) return

        loopJob?.cancel()
        loopJob = scope.launch {
            val weatherSentence = if (alarm.weatherReminderEnabled) {
                fetchLiveWeatherSentence(context)
            } else null

            delay(3_500L)
            while (isActive && soundManager.isRinging) {
                val segments = buildList {
                    if (alarm.timeReminderEnabled) {
                        val nowStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                        add("The time is $nowStr.")
                    }
                    if (!weatherSentence.isNullOrBlank()) {
                        add(weatherSentence)
                    }
                    if (alarm.labelReminderEnabled) {
                        val lbl = listOf(alarm.label, alarm.goal)
                            .map { it.trim() }
                            .firstOrNull { it.isNotBlank() }
                        if (lbl != null) add("Reminder: $lbl.")
                    }
                }
                if (segments.isNotEmpty() && soundManager.isRinging) {
                    speakText(segments.joinToString(" "), duckAlarm = true)
                }
                delay(22_000L)
            }
        }
    }

    fun stop() {
        loopJob?.cancel()
        loopJob = null
        pendingUtterance = null
        runCatching { tts?.stop() }
        soundManager.setVoiceDucking(false)
    }

    suspend fun fetchLiveWeatherSentence(context: Context): String = withContext(Dispatchers.IO) {
        val fix = LocationHelper.bestLastKnownLocation(context)
        val (lat, lon) = if (fix != null) {
            fix.latitude to fix.longitude
        } else {
            val firstPlace = runCatching {
                (context.applicationContext as TimecureApp).container.smartTaskRepository
                    .getAllPlaces()
                    .firstOrNull()
            }.getOrNull()
            if (firstPlace != null) firstPlace.latitude to firstPlace.longitude
            else return@withContext "Weather unavailable offline — check outside before heading out."
        }

        runCatching {
            val url = String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code",
                lat,
                lon
            )
            val request = Request.Builder().url(url).get().build()
            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val body = resp.body?.string().orEmpty()
                val current = JSONObject(body).getJSONObject("current")
                val tempC = current.getDouble("temperature_2m").roundToInt()
                val code = current.getInt("weather_code")
                val desc = describeWmoWeatherCode(code)
                "Currently $tempC degrees Celsius and $desc."
            }
        }.getOrElse {
            "Weather unavailable offline — check outside before heading out."
        }
    }

    companion object {
        private const val TAG = "AlarmVoiceAssistant"

        fun describeWmoWeatherCode(code: Int): String = when (code) {
            0 -> "clear sky"
            1, 2 -> "partly cloudy"
            3 -> "overcast"
            45, 48 -> "foggy"
            51, 53, 55, 56, 57 -> "light drizzle"
            61, 63, 65, 66, 67, 80, 81, 82 -> "rainy"
            71, 73, 75, 77, 85, 86 -> "snowy"
            95, 96, 99 -> "thunderstorms"
            else -> "mild conditions"
        }
    }
}
