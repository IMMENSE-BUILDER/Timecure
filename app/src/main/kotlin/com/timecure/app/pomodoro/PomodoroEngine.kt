package com.timecure.app.pomodoro

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import com.timecure.app.data.local.PreferencesManager
import com.timecure.app.data.repository.PomodoroRepository
import com.timecure.app.model.PomodoroPhase
import com.timecure.app.model.PomodoroPreset
import com.timecure.app.model.PomodoroState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PomodoroEngine(
    private val context: Context,
    private val repository: PomodoroRepository,
    private val prefs: PreferencesManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null
    private var endElapsedMillis: Long = 0L

    private val _state = MutableStateFlow(
        PomodoroState(activeGoal = prefs.defaultGoal)
    )
    val state: StateFlow<PomodoroState> = _state.asStateFlow()

    fun setGoal(goal: String) {
        _state.update { it.copy(activeGoal = goal) }
    }

    fun selectPreset(preset: PomodoroPreset) {
        pause(stopService = true)
        val secs = preset.durationSeconds(PomodoroPhase.FOCUS)
        _state.update {
            it.copy(
                preset = preset,
                phase = PomodoroPhase.FOCUS,
                remainingSeconds = secs,
                totalSeconds = secs,
                isRunning = false,
                phaseTransitionNonce = it.phaseTransitionNonce + 1,
                lastCompletedPhase = null
            )
        }
    }

    fun selectPhase(phase: PomodoroPhase) {
        pause(stopService = true)
        val s = _state.value
        val secs = s.preset.durationSeconds(phase)
        _state.update {
            it.copy(
                phase = phase,
                remainingSeconds = secs,
                totalSeconds = secs,
                isRunning = false,
                phaseTransitionNonce = it.phaseTransitionNonce + 1,
                lastCompletedPhase = null
            )
        }
    }

    /** Used by the Alarm Victory handoff: pre-load goal, pick Classic/Sprint, and launch immediately. */
    fun launchSprintForGoal(goal: String, preset: PomodoroPreset = PomodoroPreset.CLASSIC) {
        pause(stopService = false)
        val secs = preset.durationSeconds(PomodoroPhase.FOCUS)
        _state.value = PomodoroState(
            preset = preset,
            phase = PomodoroPhase.FOCUS,
            remainingSeconds = secs,
            totalSeconds = secs,
            isRunning = false,
            completedFocusCycles = _state.value.completedFocusCycles,
            activeGoal = goal.ifBlank { prefs.defaultGoal },
            phaseTransitionNonce = _state.value.phaseTransitionNonce + 1
        )
        start()
    }

    fun toggleRun() {
        if (_state.value.isRunning) pause(stopService = false) else start()
    }

    fun start() {
        val current = _state.value
        if (current.isRunning) return
        val remaining = if (current.remainingSeconds <= 0) current.totalSeconds else current.remainingSeconds
        endElapsedMillis = SystemClock.elapsedRealtime() + remaining * 1000L

        _state.update { it.copy(remainingSeconds = remaining, isRunning = true) }
        notifyService(PomodoroService.ACTION_START_OR_UPDATE)

        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive && _state.value.isRunning) {
                val leftMillis = endElapsedMillis - SystemClock.elapsedRealtime()
                val leftSecs = ((leftMillis + 999L) / 1000L).toInt().coerceAtLeast(0)
                if (leftSecs != _state.value.remainingSeconds) {
                    _state.update { it.copy(remainingSeconds = leftSecs) }
                    notifyService(PomodoroService.ACTION_START_OR_UPDATE)
                }
                if (leftMillis <= 0L) {
                    completeCurrentPhase(autoContinue = false)
                    break
                }
                delay(250L)
            }
        }
    }

    fun pause(stopService: Boolean = false) {
        tickJob?.cancel()
        tickJob = null
        if (_state.value.isRunning) {
            val leftSecs = ((endElapsedMillis - SystemClock.elapsedRealtime() + 999L) / 1000L)
                .toInt().coerceAtLeast(0)
            _state.update { it.copy(remainingSeconds = leftSecs, isRunning = false) }
        }
        if (stopService) {
            stopServiceNotification()
        } else {
            notifyService(PomodoroService.ACTION_START_OR_UPDATE)
        }
    }

    fun reset() {
        pause(stopService = true)
        val s = _state.value
        val secs = s.preset.durationSeconds(s.phase)
        _state.update { it.copy(remainingSeconds = secs, totalSeconds = secs, isRunning = false) }
    }

    fun skipPhase() {
        tickJob?.cancel()
        tickJob = null
        val s = _state.value
        val (nextPhase, newCycles) = nextPhaseAfter(s.phase, s.completedFocusCycles, s.preset, countFocus = false)
        val nextSecs = s.preset.durationSeconds(nextPhase)
        _state.update {
            it.copy(
                phase = nextPhase,
                remainingSeconds = nextSecs,
                totalSeconds = nextSecs,
                isRunning = false,
                completedFocusCycles = newCycles,
                phaseTransitionNonce = it.phaseTransitionNonce + 1,
                lastCompletedPhase = null
            )
        }
        stopServiceNotification()
    }

    private fun completeCurrentPhase(autoContinue: Boolean) {
        val s = _state.value
        val finished = s.phase
        playPhaseChime()

        if (finished == PomodoroPhase.FOCUS) {
            scope.launch {
                repository.recordSession(
                    durationMinutes = s.preset.focusMinutes,
                    goal = s.activeGoal,
                    preset = s.preset.displayName
                )
            }
        }

        val (nextPhase, newCycles) = nextPhaseAfter(finished, s.completedFocusCycles, s.preset, countFocus = true)
        val nextSecs = s.preset.durationSeconds(nextPhase)
        _state.update {
            it.copy(
                phase = nextPhase,
                remainingSeconds = nextSecs,
                totalSeconds = nextSecs,
                isRunning = false,
                completedFocusCycles = newCycles,
                phaseTransitionNonce = it.phaseTransitionNonce + 1,
                lastCompletedPhase = finished
            )
        }
        if (autoContinue) start() else stopServiceNotification()
    }

    private fun playPhaseChime() {
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(context, uri)?.play()
        }
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 180, 120, 260), -1))
        }
    }

    private fun notifyService(action: String) {
        runCatching {
            val intent = Intent(context, PomodoroService::class.java).apply { this.action = action }
            ContextCompat.startForegroundService(context, intent)
        }
    }

    private fun stopServiceNotification() {
        runCatching { context.stopService(Intent(context, PomodoroService::class.java)) }
    }

    companion object {
        /**
         * Pure transition helper (unit-tested):
         * FOCUS -> SHORT_BREAK, or LONG_BREAK every [PomodoroPreset.cyclesBeforeLongBreak] focus rounds.
         * Any break -> FOCUS.
         */
        fun nextPhaseAfter(
            current: PomodoroPhase,
            completedFocusCycles: Int,
            preset: PomodoroPreset,
            countFocus: Boolean
        ): Pair<PomodoroPhase, Int> {
            return when (current) {
                PomodoroPhase.FOCUS -> {
                    val nextCount = if (countFocus) completedFocusCycles + 1 else completedFocusCycles
                    val nextPhase = if (countFocus && nextCount % preset.cyclesBeforeLongBreak == 0) {
                        PomodoroPhase.LONG_BREAK
                    } else {
                        PomodoroPhase.SHORT_BREAK
                    }
                    nextPhase to nextCount
                }
                PomodoroPhase.SHORT_BREAK, PomodoroPhase.LONG_BREAK -> {
                    PomodoroPhase.FOCUS to completedFocusCycles
                }
            }
        }
    }
}
