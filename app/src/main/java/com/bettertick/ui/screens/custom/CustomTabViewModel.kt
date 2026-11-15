package com.bettertick.ui.screens.custom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bettertick.data.model.DiaryEntry
import com.bettertick.data.model.Task
import com.bettertick.data.model.TaskList
import com.bettertick.data.model.completedOn
import com.bettertick.data.model.isShiftTask
import com.bettertick.data.model.occursOn
import com.bettertick.ui.screens.tasks.MoneyEntry
import com.bettertick.ui.screens.tasks.MoneyPrefs
import com.bettertick.data.repository.DiaryRepository
import com.bettertick.data.repository.HabitRepository
import com.bettertick.data.repository.ListRepository
import com.bettertick.data.repository.TaskRepository
import com.bettertick.ui.screens.habits.isDueOn
import com.bettertick.ui.screens.tasks.HabitTodayItem
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

/** Data for the blocks on a user-made tab. */
@HiltViewModel
class CustomTabViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val habitRepository: HabitRepository,
    listRepository: ListRepository,
    diaryRepository: DiaryRepository
) : ViewModel() {
    private val today = LocalDate.now()

    val tasks: StateFlow<List<Task>> = repository.observeAllTasks()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lists: StateFlow<List<TaskList>> = listRepository.observeLists()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habits: StateFlow<List<HabitTodayItem>> = combine(
        habitRepository.observeHabits(),
        habitRepository.observeHabitLogs(today.toString(), today.toString())
    ) { habits, logs ->
        val done = logs.filter { it.isCompleted }.map { it.habitId }.toSet()
        habits.filter { !it.isArchived && it.isDueOn(today) }.map { HabitTodayItem(it, it.id in done) }
    }.catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val diary: StateFlow<DiaryEntry?> = diaryRepository.observeEntryForDate(today.toString())
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun toggle(task: Task, day: LocalDate) {
        viewModelScope.launch { runCatching { repository.toggleCompleteOn(task, day) } }
    }

    fun toggleHabit(habitId: String) {
        viewModelScope.launch { runCatching { habitRepository.toggleHabitLog(habitId, today.toString()) } }
    }

    /** 빠른 추가: a task for today (in [listId] when the block has one). */
    fun addToday(title: String, listId: String = "") {
        if (title.isBlank()) return
        viewModelScope.launch {
            runCatching {
                repository.addTask(Task(
                    title = title.trim(),
                    listId = listId,
                    dueDate = Timestamp(Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant())),
                    sortOrder = System.currentTimeMillis()
                ))
            }
        }
    }

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    /** Short messages for a toast: macro results and 알림 steps. */
    val messages: SharedFlow<String> = _messages

    /**
     * Runs a macro's steps in order. Text fields can use {변수}; values are read
     * from [ctx] as it was when the button was pressed. A 화면 이동 step runs last.
     */
    fun runMacro(tabId: String, config: MacroConfig, ctx: DashContext, onNavigate: (String) -> Unit) {
        viewModelScope.launch {
            var navigateTo: String? = null
            var done = 0
            var toasted = false
            val now = java.time.LocalTime.now()
            for (step in config.steps) {
                val ok = runCatching {
                    when (step.type) {
                        MacroType.ADD_TASK -> {
                            val title = DashVars.fill(step.text, ctx).trim()
                            require(title.isNotBlank())
                            val day = ctx.today.plusDays(step.amount)
                            val time = runCatching { java.time.LocalTime.parse(step.time) }.getOrNull()
                            val at = (if (time != null) day.atTime(time) else day.atStartOfDay()).atZone(ZoneId.systemDefault())
                            repository.addTask(Task(title = title, listId = step.target,
                                dueDate = Timestamp(Date.from(at.toInstant())), sortOrder = System.currentTimeMillis()))
                        }
                        MacroType.COMPLETE_TODAY -> ctx.tasks.filter {
                            !it.isShiftTask() && it.parentId.isBlank() && !it.isAbandoned &&
                                it.occursOn(ctx.today) && !it.completedOn(ctx.today)
                        }.forEach { repository.toggleCompleteOn(it, ctx.today) }
                        MacroType.CHECK_HABITS -> ctx.habits.filter { !it.done }
                            .forEach { habitRepository.toggleHabitLog(it.habit.id, ctx.today.toString()) }
                        MacroType.ADD_MONEY -> {
                            require(step.amount > 0)
                            MoneyPrefs.saveEntry(MoneyEntry(date = ctx.today, amount = step.amount, income = step.flag,
                                category = step.target.ifBlank { "기타" }, memo = DashVars.fill(step.text, ctx)))
                        }
                        MacroType.APPEND_MEMO -> {
                            val tab = CustomTabsPrefs.tab(tabId) ?: error("no tab")
                            val memo = tab.blocks.firstOrNull { it.id == step.target && it.type == DashBlockType.MEMO }
                                ?: tab.blocks.firstOrNull { it.type == DashBlockType.MEMO }
                                ?: error("no memo block")
                            val line = "%02d:%02d ".format(now.hour, now.minute) + DashVars.fill(step.text, ctx)
                            val text = if (memo.text.isBlank()) line else memo.text.trimEnd() + "\n" + line
                            CustomTabsPrefs.update(tab.copy(blocks = tab.blocks.map { if (it.id == memo.id) it.copy(text = text) else it }))
                        }
                        MacroType.TOAST -> { _messages.tryEmit(DashVars.fill(step.text, ctx)); toasted = true }
                        MacroType.NAVIGATE -> { navigateTo = step.target }
                        else -> error("unknown step")
                    }
                    Unit
                }.isSuccess
                if (ok) done++
            }
            if (!toasted) {
                val failed = config.steps.size - done
                _messages.tryEmit(if (failed == 0) "${config.label} 완료" else "${config.label}: ${failed}단계 실패")
            }
            navigateTo?.takeIf { it.isNotBlank() }?.let(onNavigate)
        }
    }
}
