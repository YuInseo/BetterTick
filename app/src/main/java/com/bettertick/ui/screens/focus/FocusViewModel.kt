package com.bettertick.ui.screens.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bettertick.data.model.FocusCategory
import com.bettertick.data.model.FocusSession
import com.bettertick.data.repository.FocusRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val sessionId: String? = null
)

@HiltViewModel
class FocusViewModel @Inject constructor(
    private val focusRepository: FocusRepository
) : ViewModel() {

    val categories: StateFlow<List<FocusCategory>> = focusRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todaySessions: StateFlow<List<FocusSession>> = focusRepository.observeTodaySessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null

    fun startSession(category: FocusCategory) {
        if (_timerState.value.isRunning) return

        viewModelScope.launch {
            val session = FocusSession(
                activityName = category.name,
                activityIcon = category.icon,
                activityColor = category.color,
                startedAt = Timestamp.now()
            )
            val sessionId = focusRepository.startSession(session)

            _timerState.value = TimerState(
                isRunning = true,
                isPaused = false,
                elapsedSeconds = 0,
                categoryName = category.name,
                categoryIcon = category.icon,
                categoryColor = category.color,
                sessionId = sessionId
            )

            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (!_timerState.value.isPaused) {
                    _timerState.value = _timerState.value.copy(
                        elapsedSeconds = _timerState.value.elapsedSeconds + 1
                    )
                }
            }
        }
    }

    fun pauseSession() {
        _timerState.value = _timerState.value.copy(isPaused = true)
    }

    fun resumeSession() {
        _timerState.value = _timerState.value.copy(isPaused = false)
    }

    fun stopSession() {
        val state = _timerState.value
        val sessionId = state.sessionId ?: return

        timerJob?.cancel()
        viewModelScope.launch {
            focusRepository.endSession(sessionId, state.elapsedSeconds)
            _timerState.value = TimerState()
        }
    }

    fun addCategory(name: String, icon: String = "", color: String = "#FF8C00") {
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

    fun getTodayTotalSeconds(categoryName: String): Long {
        return todaySessions.value
            .filter { it.activityName == categoryName && it.isCompleted }
            .sumOf { it.durationSeconds }
    }

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
