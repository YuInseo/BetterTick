package com.bettertick.ui.screens.custom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bettertick.data.model.DiaryEntry
import com.bettertick.data.model.Task
import com.bettertick.data.model.TaskList
import com.bettertick.data.repository.DiaryRepository
import com.bettertick.data.repository.HabitRepository
import com.bettertick.data.repository.ListRepository
import com.bettertick.data.repository.TaskRepository
import com.bettertick.ui.screens.habits.isDueOn
import com.bettertick.ui.screens.tasks.HabitTodayItem
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    diaryRepository: DiaryRepository,
    private val tagRepository: com.bettertick.data.repository.TagRepository,
    private val tabBarRepository: com.bettertick.data.repository.TabBarRepository
) : ViewModel() {
    /** Puts a (copied / imported) tab on the tab bar, where the bar shows it. */
    fun addToTabBar(tabId: String) {
        viewModelScope.launch {
            runCatching {
                val config = tabBarRepository.observeConfig().first()
                if (tabId in config.enabledIds) return@runCatching
                val ids = config.enabledIds.filter { it != "more" }.toMutableList()
                ids.add((config.maxTabs - 2).coerceIn(0, ids.size), tabId)
                tabBarRepository.saveConfig(config.copy(enabledIds = ids))
            }
        }
    }

    val tags: StateFlow<List<com.bettertick.data.model.Tag>> = tagRepository.observeTags()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** A task for [date] (null = no date) in [listId]. */
    fun addTask(title: String, listId: String, date: LocalDate?) {
        if (title.isBlank()) return
        viewModelScope.launch {
            runCatching {
                repository.addTask(Task(title = title.trim(), listId = listId,
                    dueDate = date?.let { Timestamp(Date.from(it.atStartOfDay(ZoneId.systemDefault()).toInstant())) },
                    sortOrder = System.currentTimeMillis()))
            }
        }
    }

    fun updateTask(task: Task) { viewModelScope.launch { runCatching { repository.updateTask(task) } } }

    fun deleteTask(task: Task) { viewModelScope.launch { runCatching { repository.deleteTask(task.id) } } }

    fun toggleFromDetail(task: Task, day: LocalDate?) {
        viewModelScope.launch {
            runCatching {
                repository.applyStatus(task,
                    if (task.isCompleted || task.isAbandoned) com.bettertick.data.model.TaskStatusAction.REOPEN
                    else com.bettertick.data.model.TaskStatusAction.COMPLETE, day)
            }
        }
    }

    suspend fun createTag(name: String): String {
        val trimmed = name.trim().removePrefix("#")
        if (trimmed.isBlank()) return ""
        return tags.value.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }?.id
            ?: runCatching { tagRepository.addTag(com.bettertick.data.model.Tag(name = trimmed)) }.getOrDefault("")
    }

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

    /** 버튼으로 실행: same engine as the automatic events. */
    fun runMacro(context: android.content.Context, tabId: String, blockId: String, config: MacroConfig,
                 ctx: DashContext, onNavigate: (String) -> Unit) {
        MacroEngine.fire(context, tabId, blockId, config, "버튼", ctx, onNavigate)
    }
}
