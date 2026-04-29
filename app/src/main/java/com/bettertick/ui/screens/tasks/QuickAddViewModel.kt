package com.bettertick.ui.screens.tasks

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bettertick.data.model.Task
import com.bettertick.data.repository.TaskRepository
import com.bettertick.widget.calendar.ReminderWidget
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class QuickAddViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskRepository: TaskRepository
) : ViewModel() {

    fun addTask(
        title: String,
        date: LocalDate? = LocalDate.now(),
        listId: String = "",
        kanbanColumn: String = ""
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            // Today keeps the precise current time so a same-day task sorts
            // correctly against existing dueDate-based ordering. A future or
            // past date is anchored at midnight local time. A null date leaves
            // dueDate unset — used by kanban inserts where "no date" is the
            // expected default.
            val dueTimestamp = when {
                date == null -> null
                date == LocalDate.now() -> Timestamp(Date())
                else -> Timestamp(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()))
            }
            val task = Task(
                title = title,
                dueDate = dueTimestamp,
                listId = listId,
                kanbanColumn = kanbanColumn,
                sortOrder = System.currentTimeMillis()
            )
            taskRepository.addTask(task)
            runCatching { ReminderWidget().updateAll(context) }
        }
    }
}
