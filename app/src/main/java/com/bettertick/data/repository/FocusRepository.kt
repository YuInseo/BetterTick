package com.bettertick.data.repository

import com.bettertick.data.firebase.FirestoreProvider
import com.bettertick.data.model.FocusCategory
import com.bettertick.data.model.FocusSession
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenSource
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SnapshotListenOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FocusRepository @Inject constructor(
    private val firestoreProvider: FirestoreProvider
) {
    private val cacheOptions = SnapshotListenOptions.Builder()
        .setSource(ListenSource.CACHE)
        .build()

    fun observeCategories(): Flow<List<FocusCategory>> = callbackFlow {
        val registration = firestoreProvider.focusCategoriesCollection()
            .orderBy("sortOrder", Query.Direction.ASCENDING)
            .addSnapshotListener(cacheOptions) { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.map { doc ->
                    doc.toObject(FocusCategory::class.java)?.copy(id = doc.id) ?: FocusCategory()
                } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    fun observeTodaySessions(): Flow<List<FocusSession>> = callbackFlow {
        val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val startTimestamp = Timestamp(Date.from(todayStart))

        val registration = firestoreProvider.focusSessionsCollection()
            .whereGreaterThanOrEqualTo("startedAt", startTimestamp)
            .orderBy("startedAt", Query.Direction.DESCENDING)
            .addSnapshotListener(cacheOptions) { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.map { doc ->
                    doc.toObject(FocusSession::class.java)?.copy(id = doc.id) ?: FocusSession()
                } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    /**
     * Sessions started anytime in the current Sunday-Saturday week. Used by
     * the FocusDistributionWidget to render the weekly stacked-bar chart.
     */
    fun observeThisWeekSessions(): Flow<List<FocusSession>> = callbackFlow {
        val today = LocalDate.now()
        val weekStart = today.with(java.time.DayOfWeek.SUNDAY)
            .let { sun -> if (sun.isAfter(today)) sun.minusWeeks(1) else sun }
        val startInstant = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val startTimestamp = Timestamp(Date.from(startInstant))

        val registration = firestoreProvider.focusSessionsCollection()
            .whereGreaterThanOrEqualTo("startedAt", startTimestamp)
            .orderBy("startedAt", Query.Direction.DESCENDING)
            .addSnapshotListener(cacheOptions) { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.map { doc ->
                    doc.toObject(FocusSession::class.java)?.copy(id = doc.id) ?: FocusSession()
                } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    /**
     * All completed + in-flight sessions, newest first. Used by the stats
     * screen which aggregates across multiple time windows (today, week,
     * month, year) — running a per-window query for each would multiply
     * listeners; one stream keeps the math local.
     */
    fun observeAllSessions(): Flow<List<FocusSession>> = callbackFlow {
        val registration = firestoreProvider.focusSessionsCollection()
            .orderBy("startedAt", Query.Direction.DESCENDING)
            .addSnapshotListener(cacheOptions) { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.map { doc ->
                    doc.toObject(FocusSession::class.java)?.copy(id = doc.id) ?: FocusSession()
                } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    suspend fun addCategory(category: FocusCategory): String {
        val docRef = firestoreProvider.focusCategoriesCollection().document()
        docRef.set(category.copy(id = docRef.id))
        return docRef.id
    }

    suspend fun startSession(session: FocusSession): String {
        val docRef = firestoreProvider.focusSessionsCollection().document()
        docRef.set(session.copy(id = docRef.id))
        return docRef.id
    }

    suspend fun endSession(sessionId: String, durationSeconds: Long) {
        firestoreProvider.focusSessionsCollection().document(sessionId).update(
            mapOf(
                "endedAt" to Timestamp.now(),
                "durationSeconds" to durationSeconds,
                // Firebase Kotlin 매퍼가 isCompleted → completed로 직렬화함
                "completed" to true
            )
        )
    }

    suspend fun deleteCategory(categoryId: String) {
        firestoreProvider.focusCategoriesCollection().document(categoryId).delete()
    }
}
