package com.bettertick.ui.screens.tasks

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bettertick.data.model.Tag
import com.bettertick.data.model.Task
import com.bettertick.data.model.TaskList
import com.bettertick.data.repository.ListRepository
import com.bettertick.data.repository.TagRepository
import com.bettertick.data.repository.TaskRepository
import com.bettertick.widget.calendar.ReminderWidget
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

/** Everything the quick-add sheet can set on a new task. */
data class QuickAddDraft(
    val title: String,
    val notes: String = "",
    /** null = 날짜 없음. */
    val date: LocalDate? = LocalDate.now(),
    /** null = 종일 (all-day). */
    val time: LocalTime? = null,
    val durationMinutes: Int = 60,
    val repeatRule: String? = null,
    val repeatEnd: String? = null,
    val priority: Int = 0,
    val tagIds: List<String> = emptyList(),
    /** Tag names typed as #tag that may not exist yet. */
    val newTagNames: List<String> = emptyList(),
    /** null = use the screen's context list. */
    val listId: String? = null
)

@HiltViewModel
class QuickAddViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskRepository: TaskRepository,
    private val listRepository: ListRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    val lists: StateFlow<List<TaskList>> = listRepository.observeLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<Tag>> = tagRepository.observeTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun createTag(name: String): String {
        val trimmed = name.trim().removePrefix("#")
        if (trimmed.isBlank()) return ""
        tags.value.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }?.let { return it.id }
        return tagRepository.addTag(Tag(name = trimmed))
    }

    fun addTask(
        title: String,
        date: LocalDate? = LocalDate.now(),
        listId: String = "",
        kanbanColumn: String = ""
    ) = addDraft(QuickAddDraft(title = title, date = date), contextListId = listId, kanbanColumn = kanbanColumn)

    fun addDraft(
        draft: QuickAddDraft,
        contextListId: String = "",
        kanbanColumn: String = ""
    ) {
        if (draft.title.isBlank()) return
        viewModelScope.launch {
            // A date without a time is an all-day task (midnight), like
            // TickTick; a picked time keeps that exact time.
            val dueTimestamp = draft.date?.let { d ->
                Timestamp(Date.from(d.atTime(draft.time ?: LocalTime.MIDNIGHT).atZone(ZoneId.systemDefault()).toInstant()))
            }
            val createdTagIds = draft.newTagNames.map { createTag(it) }.filter { it.isNotBlank() }
            val task = Task(
                title = draft.title.trim(),
                notes = draft.notes.trim(),
                dueDate = dueTimestamp,
                durationMinutes = draft.durationMinutes,
                repeatRule = draft.repeatRule,
                repeatEnd = draft.repeatEnd,
                priority = draft.priority,
                tagIds = (draft.tagIds + createdTagIds).distinct(),
                listId = draft.listId ?: contextListId,
                kanbanColumn = kanbanColumn,
                sortOrder = System.currentTimeMillis()
            )
            taskRepository.addTask(task)
            runCatching { ReminderWidget().updateAll(context) }
        }
    }
}
