package com.bettertick.ui.screens.custom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bettertick.data.model.Task
import com.bettertick.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Tasks for the blocks on a user-made tab. */
@HiltViewModel
class CustomTabViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {
    val tasks: StateFlow<List<Task>> = repository.observeAllTasks()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggle(task: Task, day: LocalDate) {
        viewModelScope.launch { runCatching { repository.toggleCompleteOn(task, day) } }
    }
}
