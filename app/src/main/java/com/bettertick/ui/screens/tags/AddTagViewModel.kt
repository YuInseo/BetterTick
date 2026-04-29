package com.bettertick.ui.screens.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bettertick.data.model.Tag
import com.bettertick.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AddTagViewModel @Inject constructor(
    private val tagRepository: TagRepository
) : ViewModel() {

    val tags: StateFlow<List<Tag>> = tagRepository.observeTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun createTag(tag: Tag) {
        tagRepository.addTag(tag)
    }

    suspend fun updateTag(tag: Tag) {
        tagRepository.updateTag(tag)
    }

    suspend fun deleteTag(tagId: String) {
        tagRepository.deleteTag(tagId)
    }
}
