package com.bettertick.data.firebase

import com.bettertick.data.model.FocusCategory
import com.bettertick.data.model.Habit
import com.bettertick.data.model.TaskList
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore 컬렉션 구조:
 *
 * users/{userId}                    - 유저 프로필
 * users/{userId}/tasks/{taskId}     - 할 일
 * users/{userId}/lists/{listId}     - 할 일 목록 (카테고리)
 * users/{userId}/tags/{tagId}       - 태그
 * users/{userId}/habits/{habitId}   - 습관 정의
 * users/{userId}/habitLogs/{logId}  - 습관 완료 기록
 * users/{userId}/focusSessions/{id} - 포커스 타이머 기록
 * users/{userId}/focusCategories/{id} - 포커스 활동 카테고리
 */
@Singleton
class DataSeeder @Inject constructor(
    private val firestoreProvider: FirestoreProvider
) {
    /**
     * 회원가입 후 초기 데이터를 시드합니다.
     * - 기본 목록: 기본함
     * - 기본 포커스 카테고리: 명상, 공부, 휴식
     * - 기본 습관: 운동, 독서
     */
    suspend fun seedDefaultData() {
        seedDefaultLists()
        seedDefaultFocusCategories()
        seedDefaultHabits()
    }

    private suspend fun seedDefaultLists() {
        val existing = firestoreProvider.listsCollection().get().await()
        if (existing.isEmpty) {
            val defaults = listOf(
                TaskList(name = "기본함", icon = "inbox", isDefault = true, sortOrder = 0),
                TaskList(name = "Work", icon = "work", color = "#4FC3F7", sortOrder = 1),
                TaskList(name = "공부", icon = "school", color = "#FFB74D", sortOrder = 2)
            )
            defaults.forEach { list ->
                firestoreProvider.listsCollection().add(list).await()
            }
        }
    }

    private suspend fun seedDefaultFocusCategories() {
        val existing = firestoreProvider.focusCategoriesCollection().get().await()
        if (existing.isEmpty) {
            val defaults = listOf(
                FocusCategory(name = "명상", icon = "😊", color = "#81C784", sortOrder = 0),
                FocusCategory(name = "공부", icon = "📘", color = "#4FC3F7", sortOrder = 1),
                FocusCategory(name = "휴식", icon = "😊", color = "#81C784", sortOrder = 2),
                FocusCategory(name = "수학", icon = "✏️", color = "#FFB74D", sortOrder = 3),
                FocusCategory(name = "토익 공부", icon = "😊", color = "#81C784", sortOrder = 4),
                FocusCategory(name = "일본어 공부", icon = "📘", color = "#4FC3F7", sortOrder = 5)
            )
            defaults.forEach { cat ->
                firestoreProvider.focusCategoriesCollection().add(cat).await()
            }
        }
    }

    private suspend fun seedDefaultHabits() {
        val existing = firestoreProvider.habitsCollection().get().await()
        if (existing.isEmpty) {
            val defaults = listOf(
                Habit(name = "운동", icon = "💪", color = "#FF8C00", sortOrder = 0),
                Habit(name = "독서", icon = "📚", color = "#4FC3F7", sortOrder = 1),
                Habit(name = "명상", icon = "🧘", color = "#81C784", sortOrder = 2)
            )
            defaults.forEach { habit ->
                firestoreProvider.habitsCollection().add(habit).await()
            }
        }
    }
}
