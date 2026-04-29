package com.bettertick.data.repository.interfaces

import com.bettertick.data.model.Habit
import com.bettertick.data.model.HabitLog
import kotlinx.coroutines.flow.Flow

interface IHabitRepository {
    fun observeHabits(): Flow<List<Habit>>
    fun observeHabitLogs(startDate: String, endDate: String): Flow<List<HabitLog>>
    suspend fun addHabit(habit: Habit): String
    suspend fun toggleHabitLog(habitId: String, date: String)
    suspend fun deleteHabit(habitId: String)
}
