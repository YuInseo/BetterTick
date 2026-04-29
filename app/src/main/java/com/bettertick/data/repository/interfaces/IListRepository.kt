package com.bettertick.data.repository.interfaces

import com.bettertick.data.model.TaskList
import kotlinx.coroutines.flow.Flow

interface IListRepository {
    fun observeLists(): Flow<List<TaskList>>
    suspend fun addList(list: TaskList): String
    suspend fun updateList(list: TaskList)
    suspend fun deleteList(listId: String)
}
