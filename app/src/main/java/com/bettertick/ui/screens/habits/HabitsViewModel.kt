package com.bettertick.ui.screens.habits

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bettertick.data.model.Habit
import com.bettertick.data.model.HabitLog
import com.bettertick.data.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habitRepository: HabitRepository
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val habits: StateFlow<List<Habit>> = habitRepository.observeHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedHabits: StateFlow<List<Habit>> = habitRepository.observeArchivedHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _detailDateRange = MutableStateFlow<Pair<String, String>?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val detailLogs: StateFlow<Map<String, Set<String>>> = _detailDateRange.flatMapLatest { range ->
        if (range == null) flowOf(emptyMap())
        else habitRepository.observeHabitLogs(range.first, range.second).map { logs ->
            logs.filter { it.isCompleted }
                .groupBy { it.habitId }
                .mapValues { (_, list) -> list.map { it.date }.toSet() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Full records (mood + note) keyed by "habitId|date" — used by the
    // date-tap dialog in the detail screen.
    @OptIn(ExperimentalCoroutinesApi::class)
    val detailLogRecords: StateFlow<Map<String, HabitLog>> = _detailDateRange.flatMapLatest { range ->
        if (range == null) flowOf(emptyMap())
        else habitRepository.observeHabitLogs(range.first, range.second).map { logs ->
            logs.associateBy { "${it.habitId}|${it.date}" }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _weekStart = MutableStateFlow(getWeekStart(LocalDate.now()))
    val weekStart: StateFlow<LocalDate> = _weekStart.asStateFlow()

    val weekDates: StateFlow<List<LocalDate>> = _weekStart.map { start ->
        (0L..6L).map { start.plusDays(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), getWeekDates(LocalDate.now()))

    @OptIn(ExperimentalCoroutinesApi::class)
    val weekLogs: StateFlow<Map<String, Set<String>>> = _weekStart.flatMapLatest { start ->
        val end = start.plusDays(6)
        habitRepository.observeHabitLogs(
            start.format(dateFormatter),
            end.format(dateFormatter)
        ).map { logs ->
            logs.filter { it.isCompleted }
                .groupBy { it.habitId }
                .mapValues { (_, logList) -> logList.map { it.date }.toSet() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Full HabitLog records for the current week — used by the list-row
    // swipe-left dialog to pre-populate mood/note for today.
    @OptIn(ExperimentalCoroutinesApi::class)
    val weekLogRecords: StateFlow<Map<String, HabitLog>> = _weekStart.flatMapLatest { start ->
        val end = start.plusDays(6)
        habitRepository.observeHabitLogs(
            start.format(dateFormatter),
            end.format(dateFormatter)
        ).map { logs -> logs.associateBy { "${it.habitId}|${it.date}" } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        val newWeekStart = getWeekStart(date)
        if (newWeekStart != _weekStart.value) {
            _weekStart.value = newWeekStart
        }
    }

    fun previousWeek() {
        _weekStart.value = _weekStart.value.minusWeeks(1)
    }

    fun nextWeek() {
        _weekStart.value = _weekStart.value.plusWeeks(1)
    }

    fun toggleHabit(habitId: String, date: LocalDate) {
        viewModelScope.launch {
            habitRepository.toggleHabitLog(habitId, date.format(dateFormatter))
        }
    }

    fun saveHabitLog(habitId: String, date: LocalDate, isCompleted: Boolean, mood: Int, note: String) {
        viewModelScope.launch {
            habitRepository.saveHabitLog(
                habitId = habitId,
                date = date.format(dateFormatter),
                isCompleted = isCompleted,
                mood = mood,
                note = note
            )
        }
    }

    fun addHabit(name: String, icon: String = "", color: String = "#FF8C00") {
        if (name.isBlank()) return
        viewModelScope.launch {
            val habit = Habit(
                name = name,
                icon = icon,
                color = color,
                sortOrder = System.currentTimeMillis()
            )
            habitRepository.addHabit(habit)
        }
    }

    fun addHabitFromDraft(draft: HabitDraft) {
        Log.d("HabitsVM", "addHabitFromDraft: name=${draft.name}, icon=${draft.icon}, color=${draft.color}")
        if (draft.name.isBlank()) {
            Log.w("HabitsVM", "addHabitFromDraft: blank name, aborting")
            return
        }
        viewModelScope.launch {
            val habit = Habit(
                name = draft.name,
                description = draft.description,
                icon = draft.icon,
                color = draft.color,
                frequency = draft.frequency,
                targetDays = draft.targetDays,
                weeklyCount = draft.weeklyCount,
                intervalDays = draft.intervalDays,
                reminders = draft.reminders,
                goalType = draft.goalType,
                startDate = draft.startDate,
                targetDayCount = draft.targetDayCount,
                group = draft.group,
                autoShowLog = draft.autoShowLog,
                sortOrder = System.currentTimeMillis()
            )
            try {
                val id = habitRepository.addHabit(habit)
                Log.d("HabitsVM", "addHabitFromDraft: saved with id=$id")
            } catch (e: Exception) {
                Log.e("HabitsVM", "addHabitFromDraft: FAILED", e)
            }
        }
    }

    fun archiveHabit(habitId: String) {
        viewModelScope.launch { habitRepository.archiveHabit(habitId) }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch { habitRepository.deleteHabit(habitId) }
    }

    fun setDetailDateRange(start: String, end: String) {
        _detailDateRange.value = start to end
    }

    fun isHabitCompleted(habitId: String, date: LocalDate): Boolean {
        return weekLogs.value[habitId]?.contains(date.format(dateFormatter)) == true
    }

    private fun getWeekStart(date: LocalDate): LocalDate {
        // Week starts on Monday
        return date.minusDays((date.dayOfWeek.value - 1).toLong())
    }

    private fun getWeekDates(date: LocalDate): List<LocalDate> {
        val start = getWeekStart(date)
        return (0L..6L).map { start.plusDays(it) }
    }
}
