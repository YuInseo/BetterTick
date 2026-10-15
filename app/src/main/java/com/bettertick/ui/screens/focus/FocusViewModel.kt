package com.bettertick.ui.screens.focus

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bettertick.data.model.FocusCategory
import com.bettertick.data.model.FocusSession
import com.bettertick.data.repository.FocusRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimerState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedSeconds: Long = 0,
    val categoryName: String = "",
    val categoryIcon: String = "",
    val categoryColor: String = "#FF8C00",
    val sessionId: String? = null,
    val mode: FocusMode = FocusMode.POMO,
    val phase: FocusPhase = FocusPhase.FOCUS,
    /** Countdown length in seconds; 0 for the stopwatch. */
    val targetSeconds: Long = 0,
    /** Set when a phase ended and the next one waits for the user. */
    val awaitingNext: FocusPhase? = null
) {
    val remainingSeconds: Long get() = (targetSeconds - elapsedSeconds).coerceAtLeast(0)
    val progress: Float get() = if (targetSeconds <= 0) 0f else (elapsedSeconds.toFloat() / targetSeconds).coerceIn(0f, 1f)
}

@HiltViewModel
class FocusViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val focusRepository: FocusRepository
) : ViewModel() {

    val categories: StateFlow<List<FocusCategory>> = focusRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todaySessions: StateFlow<List<FocusSession>> = focusRepository.observeTodaySessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null
    private var currentCategory: FocusCategory? = null

    // Wall-clock bookkeeping so the timer stays correct while the app is in
    // the background (a 1s delay loop alone drifts / stops when suspended).
    private var phaseStartMs = 0L
    private var pausedAccumMs = 0L
    private var pausedAtMs = 0L

    fun startSession(category: FocusCategory) {
        if (_timerState.value.isRunning) return
        currentCategory = category
        startPhase(FocusPhase.FOCUS)
    }

    private fun startPhase(phase: FocusPhase) {
        val category = currentCategory ?: FocusCategory(name = "포커스")
        val mode = FocusPrefs.mode
        val target = if (mode == FocusMode.POMO) FocusPrefs.minutesFor(phase) * 60L else 0L
        phaseStartMs = System.currentTimeMillis()
        pausedAccumMs = 0L
        pausedAtMs = 0L
        _timerState.value = TimerState(
            isRunning = true,
            categoryName = category.name,
            categoryIcon = category.icon,
            categoryColor = category.color,
            mode = mode,
            phase = phase,
            targetSeconds = target
        )
        // Only focus phases are recorded as sessions (statistics / calendar).
        if (phase == FocusPhase.FOCUS) {
            viewModelScope.launch {
                val id = focusRepository.startSession(
                    FocusSession(
                        activityName = category.name,
                        activityIcon = category.icon,
                        activityColor = category.color,
                        startedAt = Timestamp.now()
                    )
                )
                _timerState.value = _timerState.value.copy(sessionId = id)
            }
        }
        startTicker()
    }

    private fun startTicker() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(500)
                val s = _timerState.value
                if (!s.isRunning || s.awaitingNext != null) continue
                if (s.isPaused) continue
                val elapsed = (System.currentTimeMillis() - phaseStartMs - pausedAccumMs) / 1000
                _timerState.value = s.copy(elapsedSeconds = elapsed)
                if (s.targetSeconds > 0 && elapsed >= s.targetSeconds) onPhaseComplete()
            }
        }
    }

    private fun onPhaseComplete() {
        val s = _timerState.value
        alert()
        when (s.phase) {
            FocusPhase.FOCUS -> {
                s.sessionId?.let { id ->
                    viewModelScope.launch { focusRepository.endSession(id, s.targetSeconds) }
                }
                FocusPrefs.recordPomo()
                val next = if (FocusPrefs.pomosToday % FocusPrefs.longBreakInterval.coerceAtLeast(1) == 0)
                    FocusPhase.LONG_BREAK else FocusPhase.SHORT_BREAK
                if (FocusPrefs.autoStartBreak) startPhase(next)
                else _timerState.value = s.copy(sessionId = null, elapsedSeconds = s.targetSeconds, awaitingNext = next)
            }
            FocusPhase.SHORT_BREAK, FocusPhase.LONG_BREAK -> {
                if (FocusPrefs.autoStartNextPomo) startPhase(FocusPhase.FOCUS)
                else _timerState.value = s.copy(elapsedSeconds = s.targetSeconds, awaitingNext = FocusPhase.FOCUS)
            }
        }
    }

    /** Starts the phase the timer is waiting on (휴식 시작 / 집중 시작). */
    fun startNext() {
        val next = _timerState.value.awaitingNext ?: return
        startPhase(next)
    }

    /** 휴식 건너뛰기. */
    fun skipBreak() {
        val s = _timerState.value
        if (s.phase == FocusPhase.FOCUS && s.awaitingNext == null) return
        startPhase(FocusPhase.FOCUS)
    }

    private fun alert() {
        if (FocusPrefs.endVibrate) runCatching {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
            } else {
                @Suppress("DEPRECATION") v?.vibrate(600)
            }
        }
        if (FocusPrefs.endSound) runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 600)
            viewModelScope.launch { delay(900); tone.release() }
        }
    }

    fun pauseSession() {
        if (_timerState.value.isPaused) return
        pausedAtMs = System.currentTimeMillis()
        _timerState.value = _timerState.value.copy(isPaused = true)
    }

    fun resumeSession() {
        if (!_timerState.value.isPaused) return
        pausedAccumMs += System.currentTimeMillis() - pausedAtMs
        _timerState.value = _timerState.value.copy(isPaused = false)
    }

    fun stopSession() {
        val state = _timerState.value
        timerJob?.cancel()
        val sessionId = state.sessionId
        if (sessionId != null && state.phase == FocusPhase.FOCUS && state.awaitingNext == null) {
            viewModelScope.launch { focusRepository.endSession(sessionId, state.elapsedSeconds) }
        }
        _timerState.value = TimerState()
    }

    fun addCategory(name: String, icon: String = "", color: String = "#4772FA") {
        viewModelScope.launch {
            val category = FocusCategory(
                name = name,
                icon = icon,
                color = color,
                sortOrder = System.currentTimeMillis()
            )
            focusRepository.addCategory(category)
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch { focusRepository.deleteCategory(categoryId) }
    }

    fun getTodayTotalSeconds(categoryName: String): Long {
        return todaySessions.value
            .filter { it.activityName == categoryName && it.isCompleted }
            .sumOf { it.durationSeconds }
    }

    /** Seconds shown on the clock: remaining for 뽀모도로, elapsed for 스톱워치. */
    fun displaySeconds(s: TimerState): Long = if (s.targetSeconds > 0) s.remainingSeconds else s.elapsedSeconds

    fun formatTime(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format("%d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
