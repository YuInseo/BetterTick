package com.bettertick.ui.screens.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bettertick.data.export.DiaryTxtExporter
import com.bettertick.data.model.DiaryEntry
import com.bettertick.data.repository.DiaryDraftRepository
import com.bettertick.data.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val repository: DiaryRepository,
    private val draftRepository: DiaryDraftRepository,
    private val txtExporter: DiaryTxtExporter
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _currentEntry = MutableStateFlow<DiaryEntry?>(null)
    val currentEntry: StateFlow<DiaryEntry?> = _currentEntry.asStateFlow()

    private val _entriesMap = MutableStateFlow<Map<String, DiaryEntry>>(emptyMap())
    val entriesMap: StateFlow<Map<String, DiaryEntry>> = _entriesMap.asStateFlow()

    private val _draftContent = MutableStateFlow<String?>(null)
    val draftContent: StateFlow<String?> = _draftContent.asStateFlow()

    private var dateJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observeAllEntries().collect { list ->
                _entriesMap.value = list.associateBy { it.dateStr }
            }
        }
        observeDate()
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        observeDate()
    }

    private fun observeDate() {
        dateJob?.cancel()
        _draftContent.value = draftRepository.getDraft(_selectedDate.value.toString())
        dateJob = viewModelScope.launch {
            repository.observeEntryForDate(_selectedDate.value.toString()).collect {
                _currentEntry.value = it
            }
        }
    }

    fun saveDraft(content: String) {
        draftRepository.saveDraft(_selectedDate.value.toString(), content)
        _draftContent.value = content
    }

    fun deleteDraft() {
        draftRepository.deleteDraft(_selectedDate.value.toString())
        _draftContent.value = null
    }

    fun saveEntry(content: String, mood: Int) {
        viewModelScope.launch {
            val existing = _currentEntry.value
            val entry = if (existing != null) {
                existing.copy(content = content, mood = mood)
            } else {
                DiaryEntry(dateStr = _selectedDate.value.toString(), content = content, mood = mood)
            }
            repository.saveEntry(entry)
            draftRepository.deleteDraft(_selectedDate.value.toString())
            _draftContent.value = null
            txtExporter.export(entry)
        }
    }

    fun deleteCurrentEntry() {
        viewModelScope.launch {
            _currentEntry.value?.id?.takeIf { it.isNotBlank() }?.let {
                repository.deleteEntry(it)
                draftRepository.deleteDraft(_selectedDate.value.toString())
                _draftContent.value = null
            }
        }
    }
}
