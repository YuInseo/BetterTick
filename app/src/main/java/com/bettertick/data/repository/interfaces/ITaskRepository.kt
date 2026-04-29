package com.bettertick.data.repository.interfaces

import com.bettertick.data.model.Task
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface ITaskRepository {
    fun observeAllTasks(): Flow<List<Task>>
    fun observeTasksByList(listId: String): Flow<List<Task>>
    fun observeIncompleteTasks(): Flow<List<Task>>
    fun observeTasksForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Task>>
    suspend fun addTask(task: Task): String
    suspend fun updateTask(task: Task)
    suspend fun toggleComplete(taskId: String, isCompleted: Boolean)
    suspend fun deleteTask(taskId: String)
}
