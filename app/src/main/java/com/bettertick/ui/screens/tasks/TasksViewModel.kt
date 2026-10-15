package com.bettertick.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bettertick.data.model.Tag
import com.bettertick.data.model.Task
import com.bettertick.data.model.TaskList
import com.bettertick.data.repository.ListRepository
import com.bettertick.data.repository.TagRepository
import com.bettertick.data.repository.TaskRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val listRepository: ListRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _currentFilter = MutableStateFlow<TaskFilter>(TaskFilter.Today)
    val currentFilter: StateFlow<TaskFilter> = _currentFilter.asStateFlow()

    // Which kanban column is active in the current list's kanban view. Lives
    // on the VM so the FAB can drop a newly-added task into the same column
    // the user is looking at.
    private val _selectedKanbanColumn = MutableStateFlow("")
    val selectedKanbanColumn: StateFlow<String> = _selectedKanbanColumn.asStateFlow()

    fun setSelectedKanbanColumn(column: String) {
        _selectedKanbanColumn.value = column
    }

    val lists: StateFlow<List<TaskList>> = listRepository.observeLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<Tag>> = tagRepository.observeTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Unfiltered task stream — drives the drawer's count badges so they
     *  stay correct regardless of which filter the screen is showing. */
    val allTasks: StateFlow<List<Task>> = taskRepository.observeAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<Task>> = _currentFilter.flatMapLatest { filter ->
        when (filter) {
            // Both Today and Inbox are driven off the same unfiltered stream
            // — the screen-level bucket logic does all the date-based
            // splitting anyway, and sharing the same source avoids a
            // separate `whereEqualTo("isCompleted", false)` query that was
            // returning 0 rows because Kotlin's Firestore mapper serializes
            // `isCompleted` under a different field name than the raw query
            // expects.
            is TaskFilter.ByList -> taskRepository.observeTasksByList(filter.listId)
            // Smart lists / tags: filter the shared stream (see SmartLists.kt).
            else -> kotlinx.coroutines.flow.combine(taskRepository.observeAllTasks(), lists) { all, ls ->
                val today = LocalDate.now()
                all.filter { filter.matches(it, ls, today) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: TaskFilter) {
        _currentFilter.value = filter
    }

    val filterTitle: String
        get() = _currentFilter.value.title

    fun addTask(title: String, dueDate: LocalDate? = null) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val timestamp = dueDate?.let {
                Timestamp(Date.from(it.atStartOfDay(ZoneId.systemDefault()).toInstant()))
            }
            val listId = when (val filter = _currentFilter.value) {
                is TaskFilter.ByList -> filter.listId
                else -> ""
            }
            val task = Task(
                title = title,
                listId = listId,
                dueDate = timestamp ?: Timestamp.now(),
                sortOrder = System.currentTimeMillis()
            )
            taskRepository.addTask(task)
        }
    }

    /** 상세 ⋯ > 복제. */
    fun duplicateTask(task: Task) {
        viewModelScope.launch {
            taskRepository.addTask(
                task.copy(
                    id = "",
                    isCompleted = false,
                    completedAt = null,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now(),
                    sortOrder = task.sortOrder + 1
                )
            )
        }
    }

    fun toggleComplete(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            taskRepository.toggleComplete(taskId, isCompleted)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }

    fun togglePinned(listId: String, pinned: Boolean) {
        viewModelScope.launch {
            listRepository.setPinned(listId, pinned)
        }
    }

    fun deleteList(listId: String) {
        viewModelScope.launch {
            // If the deleted list was the active filter, fall back to Today so
            // TasksScreen doesn't stay on a ghost reference.
            if ((_currentFilter.value as? TaskFilter.ByList)?.listId == listId) {
                _currentFilter.value = TaskFilter.Today
            }
            listRepository.deleteList(listId)
        }
    }

    fun updateList(list: TaskList) {
        viewModelScope.launch {
            listRepository.updateList(list)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
        }
    }

    suspend fun createTag(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return ""
        // Dedupe against the existing list so repeated "create" from the
        // picker (e.g. Korean IME autocomplete triggering twice) doesn't spam
        // Firestore with duplicates.
        tags.value.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
            ?.let { return it.id }
        return tagRepository.addTag(Tag(name = trimmed))
    }
}
