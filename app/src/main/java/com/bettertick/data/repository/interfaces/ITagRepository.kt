package com.bettertick.data.repository.interfaces

import com.bettertick.data.model.Tag
import kotlinx.coroutines.flow.Flow

interface ITagRepository {
    fun observeTags(): Flow<List<Tag>>
    suspend fun addTag(tag: Tag): String
    suspend fun updateTag(tag: Tag)
    suspend fun deleteTag(tagId: String)
}
