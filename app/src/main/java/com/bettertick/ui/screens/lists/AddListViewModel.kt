package com.bettertick.ui.screens.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bettertick.data.model.TaskList
import com.bettertick.data.repository.ListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AddListViewModel @Inject constructor(
    private val listRepository: ListRepository
) : ViewModel() {

    // Surfaced so the form can block duplicate names before hitting Firestore.
    val lists: StateFlow<List<TaskList>> = listRepository.observeLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun createList(list: TaskList) {
        listRepository.addList(list)
    }

    suspend fun updateList(list: TaskList) {
        listRepository.updateList(list)
    }
}
