package com.bettertick.data.repository.interfaces

import com.bettertick.data.model.FocusCategory
import com.bettertick.data.model.FocusSession
import kotlinx.coroutines.flow.Flow

interface IFocusRepository {
    fun observeCategories(): Flow<List<FocusCategory>>
    fun observeTodaySessions(): Flow<List<FocusSession>>
    suspend fun addCategory(category: FocusCategory): String
    suspend fun startSession(session: FocusSession): String
    suspend fun endSession(sessionId: String, durationSeconds: Long)
    suspend fun deleteCategory(categoryId: String)
}
