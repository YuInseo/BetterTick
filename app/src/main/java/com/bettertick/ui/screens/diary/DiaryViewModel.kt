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

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError = _saveError.asStateFlow()
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
        if (date == _selectedDate.value) return
        _selectedDate.value = date
        _currentEntry.value = _entriesMap.value[date.toString()]
        _saveError.value = null
        observeDate()
    }

    private fun observeDate() {
        dateJob?.cancel()
        _draftContent.value = draftRepository.getDraft(_selectedDate.value.toString())
        val date = _selectedDate.value
        dateJob = viewModelScope.launch {
            repository.observeEntryForDate(date.toString()).collect {
                if (_selectedDate.value == date) _currentEntry.value = it
            }
        }
    }

    fun saveDraft(content: String, mood: Int = 0, date: LocalDate = _selectedDate.value) {
        draftRepository.saveDraft(date.toString(), content, mood)
        if (_selectedDate.value == date) _draftContent.value = content
    }

    fun draftMood(): Int = draftRepository.getDraftMood(_selectedDate.value.toString())

    fun deleteDraft() {
        draftRepository.deleteDraft(_selectedDate.value.toString())
        _draftContent.value = null
    }

    fun saveEntry(content: String, mood: Int, date: LocalDate = _selectedDate.value) {
        // Capture the date and existing identity while still on the editing day.
        val existing = _currentEntry.value?.takeIf { it.dateStr == date.toString() }
            ?: _entriesMap.value[date.toString()]
        val entry = diarySaveSnapshot(date, content, mood, existing)
        saveDraft(content, mood, date)
        _saveError.value = null
        viewModelScope.launch {
            try {
                repository.saveEntry(entry)
                // A newer draft typed during this save must survive.
                if (draftRepository.getDraft(entry.dateStr) == entry.content &&
                    draftRepository.getDraftMood(entry.dateStr) == entry.mood) {
                    draftRepository.deleteDraft(entry.dateStr)
                    if (_selectedDate.value == date) _draftContent.value = null
                }
                txtExporter.export(entry)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_selectedDate.value == date) _saveError.value = "저장하지 못했어요. 내용은 임시저장되어 있어요. 다시 시도해 주세요."
            }
        }
    }

    fun deleteCurrentEntry() {
        val date = _selectedDate.value
        val id = _currentEntry.value?.id?.takeIf { it.isNotBlank() } ?: return
        viewModelScope.launch {
            repository.deleteEntry(id)
            draftRepository.deleteDraft(date.toString())
            if (_selectedDate.value == date) _draftContent.value = null
        }
    }
}
