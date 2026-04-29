package com.bettertick.ui.screens.matrix

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bettertick.data.model.MatrixConfig
import com.bettertick.data.model.QuadrantConfig
import com.bettertick.data.model.Tag
import com.bettertick.data.model.Task
import com.bettertick.data.model.TaskList
import com.bettertick.data.model.defaultMatrix
import com.bettertick.data.repository.ListRepository
import com.bettertick.data.repository.MatrixRepository
import com.bettertick.data.repository.TagRepository
import com.bettertick.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class MatrixViewModel @Inject constructor(
    private val matrixRepository: MatrixRepository,
    private val taskRepository: TaskRepository,
    listRepository: ListRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    val config: StateFlow<MatrixConfig> = matrixRepository.observeConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultMatrix)

    val tasks: StateFlow<List<Task>> = taskRepository.observeAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lists: StateFlow<List<TaskList>> = listRepository.observeLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<Tag>> = tagRepository.observeTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun tasksFor(quadrant: QuadrantConfig): List<Task> =
        filterTasks(tasks.value, quadrant, config.value.hideCompleted, config.value.todayOnly)

    fun saveConfig(newConfig: MatrixConfig) {
        viewModelScope.launch { matrixRepository.saveConfig(newConfig) }
    }

    fun saveQuadrant(updated: QuadrantConfig) {
        val current = config.value
        val next = current.copy(
            quadrants = current.quadrants.map { if (it.id == updated.id) updated else it }
        )
        saveConfig(next)
    }

    fun toggleHideCompleted() {
        saveConfig(config.value.copy(hideCompleted = !config.value.hideCompleted))
    }

    fun toggleTodayOnly() {
        saveConfig(config.value.copy(todayOnly = !config.value.todayOnly))
    }

    fun toggleComplete(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch { taskRepository.toggleComplete(taskId, isCompleted) }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch { taskRepository.updateTask(task) }
    }

    /** Quick-add from the matrix — stamps the new task with whichever facets
     *  the target quadrant filters on so it lands in that quadrant on the
     *  very next render. */
    fun createTaskInQuadrant(title: String, notes: String, quadrant: QuadrantConfig) {
        val trimmed = title.trim()
        if (trimmed.isBlank()) return
        val priority = if (quadrant.priority in 0..3) quadrant.priority else 0
        val listId = quadrant.listIds.firstOrNull() ?: ""
        val tagIds = if (QuadrantConfig.TagMode.from(quadrant.tagMode) ==
            QuadrantConfig.TagMode.Has) quadrant.tagIds else emptyList()
        val dueDate = when (QuadrantConfig.DateMode.from(quadrant.dateMode)) {
            // A Today-filtered quadrant would reject a null dueDate, so stamp
            // "today at current time" to keep the new task visible.
            QuadrantConfig.DateMode.Today -> com.google.firebase.Timestamp.now()
            else -> null
        }
        viewModelScope.launch {
            taskRepository.addTask(
                Task(
                    title = trimmed,
                    notes = notes.trim(),
                    priority = priority,
                    listId = listId,
                    tagIds = tagIds,
                    dueDate = dueDate,
                    sortOrder = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun createTag(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return ""
        tags.value.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
            ?.let { return it.id }
        return tagRepository.addTag(com.bettertick.data.model.Tag(name = trimmed))
    }

    /** Drag-and-drop moves a task between quadrants. We patch whichever task
     *  fields the destination quadrant filters on (priority / listId / tags)
     *  so the task matches its new quadrant when the matrix re-evaluates.
     *  Also strips tags from the source quadrant if it was a Has filter, so a
     *  move between tag-based quadrants doesn't leave the task matching both. */
    fun moveTaskToQuadrant(task: Task, source: QuadrantConfig, destination: QuadrantConfig) {
        if (source.id == destination.id) return
        val patched = applyQuadrantFacetsToTask(task, source, destination)
        android.util.Log.d(
            "MatrixDrag",
            "move id=${task.id} src=${source.id} dst=${destination.id} " +
                "destPriority=${destination.priority} destDateMode=${destination.dateMode} " +
                "destTypeMode=${destination.typeMode} destListIds=${destination.listIds} " +
                "destTagMode=${destination.tagMode} destTagIds=${destination.tagIds}"
        )
        android.util.Log.d(
            "MatrixDrag",
            "patched priority=${task.priority}->${patched.priority} " +
                "list=${task.listId}->${patched.listId} " +
                "tags=${task.tagIds}->${patched.tagIds} " +
                "due=${task.dueDate}->${patched.dueDate} same=${patched == task}"
        )
        if (patched == task) return
        viewModelScope.launch {
            try {
                taskRepository.updateTask(patched)
                android.util.Log.d("MatrixDrag", "updateTask OK id=${patched.id}")
            } catch (t: Throwable) {
                android.util.Log.e("MatrixDrag", "updateTask FAILED", t)
            }
        }
    }
}

/** Pure filter — keeping it top-level so tests (and Compose previews) can drive
 *  it without a VM instance. */
fun filterTasks(
    all: List<Task>,
    quadrant: QuadrantConfig,
    hideCompleted: Boolean,
    todayOnly: Boolean = false
): List<Task> {
    val today = LocalDate.now()
    return all.asSequence()
        .filter { if (hideCompleted) !it.isCompleted else true }
        .filter { !it.isAbandoned }
        .filter { t ->
            if (!todayOnly) return@filter true
            val d = t.dueDate?.toDate()?.toInstant()
                ?.atZone(ZoneId.systemDefault())?.toLocalDate()
            d == today
        }
        .filter { t ->
            quadrant.listIds.isEmpty() || t.listId in quadrant.listIds
        }
        .filter { t ->
            when (QuadrantConfig.TagMode.from(quadrant.tagMode)) {
                QuadrantConfig.TagMode.Any -> true
                QuadrantConfig.TagMode.Has ->
                    quadrant.tagIds.isEmpty() || t.tagIds.any { it in quadrant.tagIds }
                QuadrantConfig.TagMode.Lacks ->
                    quadrant.tagIds.isEmpty() || t.tagIds.none { it in quadrant.tagIds }
            }
        }
        .filter { t ->
            val localDate = t.dueDate?.toDate()?.toInstant()
                ?.atZone(ZoneId.systemDefault())?.toLocalDate()
            when (QuadrantConfig.DateMode.from(quadrant.dateMode)) {
                QuadrantConfig.DateMode.All -> true
                QuadrantConfig.DateMode.Today -> localDate == today
                QuadrantConfig.DateMode.Overdue -> localDate != null && localDate.isBefore(today)
                QuadrantConfig.DateMode.NoDate -> localDate == null
            }
        }
        .filter { t ->
            if (quadrant.priority < 0) true else t.priority == quadrant.priority
        }
        .filter { t ->
            when (QuadrantConfig.TypeMode.from(quadrant.typeMode)) {
                QuadrantConfig.TypeMode.All -> true
                // 과제 = task with a due date, 노트 = task without — the
                // simplest mapping that uses fields the model already has.
                QuadrantConfig.TypeMode.Task -> t.dueDate != null
                QuadrantConfig.TypeMode.Note -> t.dueDate == null
            }
        }
        .toList()
}

/** Rewrites a task so it matches [destination]'s filter spec, undoing the
 *  [source] facets that would otherwise keep it matching its old quadrant.
 *  Patches priority, listId, tagIds, dueDate (when destination's date or
 *  type mode requires one), so the task actually shows up in the destination
 *  quadrant after the drop — previously a destination requiring e.g. a due
 *  date silently swallowed the task because only priority/listId/tags were
 *  written. */
fun applyQuadrantFacetsToTask(
    task: Task,
    source: QuadrantConfig,
    destination: QuadrantConfig
): Task {
    var listId = task.listId
    var tagIds = task.tagIds
    var priority = task.priority
    var dueDate = task.dueDate

    // Priority — apply destination unless it's "any" (-1). This is the main
    // drag-and-drop knob, matching the classic priority-per-quadrant layout.
    if (destination.priority in 0..3) priority = destination.priority

    // listId — if destination pins exactly one list, move the task there.
    // Multi-list quadrants stay ambiguous so we don't guess.
    if (destination.listIds.size == 1) listId = destination.listIds.first()

    // Tags — first drop any tags pulled in by a source "Has" filter so the
    // task stops matching the old quadrant, then add destination tags if the
    // destination is also a "Has" filter.
    if (QuadrantConfig.TagMode.from(source.tagMode) == QuadrantConfig.TagMode.Has &&
        source.tagIds.isNotEmpty()) {
        tagIds = tagIds.filterNot { it in source.tagIds }
    }
    if (QuadrantConfig.TagMode.from(destination.tagMode) == QuadrantConfig.TagMode.Has &&
        destination.tagIds.isNotEmpty()) {
        // Union — preserve existing tags while guaranteeing a match against
        // at least one of the destination tag ids.
        val additions = destination.tagIds.filterNot { it in tagIds }
        tagIds = tagIds + additions
    }

    // Date — coerce the dueDate to satisfy the destination's dateMode/
    // typeMode. Without this, dropping into e.g. a "Today" quadrant when the
    // task has a stale date leaves it filtered out, so the task appears to
    // vanish on release.
    val today = LocalDate.now()
    val current = dueDate?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
    when (QuadrantConfig.DateMode.from(destination.dateMode)) {
        QuadrantConfig.DateMode.Today -> if (current != today) {
            dueDate = com.google.firebase.Timestamp(
                java.util.Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant())
            )
        }
        QuadrantConfig.DateMode.Overdue -> if (current == null || !current.isBefore(today)) {
            val yesterday = today.minusDays(1)
            dueDate = com.google.firebase.Timestamp(
                java.util.Date.from(yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant())
            )
        }
        QuadrantConfig.DateMode.NoDate -> dueDate = null
        QuadrantConfig.DateMode.All -> Unit
    }
    when (QuadrantConfig.TypeMode.from(destination.typeMode)) {
        QuadrantConfig.TypeMode.Task -> if (dueDate == null) {
            dueDate = com.google.firebase.Timestamp(
                java.util.Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant())
            )
        }
        QuadrantConfig.TypeMode.Note -> dueDate = null
        QuadrantConfig.TypeMode.All -> Unit
    }

    return task.copy(
        listId = listId,
        tagIds = tagIds,
        priority = priority,
        dueDate = dueDate
    )
}
