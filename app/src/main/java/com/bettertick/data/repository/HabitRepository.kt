package com.bettertick.data.repository

import android.util.Log
import com.bettertick.data.firebase.FirestoreProvider
import com.bettertick.data.model.Habit
import com.bettertick.data.model.HabitLog
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepository @Inject constructor(
    private val firestoreProvider: FirestoreProvider
) {

    fun observeHabits(): Flow<List<Habit>> = callbackFlow {
        Log.d("HabitRepo", "observeHabits: registering listener")
        // No .whereEqualTo + .orderBy to avoid the composite-index requirement.
        // We filter and sort in memory — the list is always tiny.
        val registration = firestoreProvider.habitsCollection()
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HabitRepo", "observeHabits: listener error", error)
                    close(error); return@addSnapshotListener
                }
                val raw = snapshot?.documents.orEmpty()
                val parsed = raw.mapNotNull { doc ->
                    runCatching { doc.toObject(Habit::class.java)?.copy(id = doc.id) }
                        .onFailure { Log.e("HabitRepo", "observeHabits: parse failed for ${doc.id}", it) }
                        .getOrNull()
                }
                val filtered = parsed.filter { !it.isArchived }.sortedBy { it.sortOrder }
                Log.d("HabitRepo", "observeHabits: raw=${raw.size} parsed=${parsed.size} active=${filtered.size} pending=${snapshot?.metadata?.hasPendingWrites()}")
                trySend(filtered)
            }
        awaitClose {
            Log.d("HabitRepo", "observeHabits: unregistering listener")
            registration.remove()
        }
    }

    fun observeHabitLogs(startDate: String, endDate: String): Flow<List<HabitLog>> = callbackFlow {
        // Regular listener (no ListenSource.CACHE). Cache-only listeners can
        // miss local pending writes on some SDK versions, which made habit
        // toggles appear to do nothing offline. The default source still
        // serves cache immediately and never blocks the UI.
        val registration = firestoreProvider.habitLogsCollection()
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("HabitRepo", "observeHabitLogs listener error", error)
                    close(error); return@addSnapshotListener
                }
                val logs = snapshot?.documents?.map { doc ->
                    doc.toObject(HabitLog::class.java)?.copy(id = doc.id) ?: HabitLog()
                } ?: emptyList()
                Log.d("HabitRepo", "observeHabitLogs: emit ${logs.size} logs range=$startDate..$endDate pending=${snapshot?.metadata?.hasPendingWrites()}")
                trySend(logs)
            }
        awaitClose { registration.remove() }
    }

    suspend fun addHabit(habit: Habit): String {
        val docRef = firestoreProvider.habitsCollection().document()
        docRef.set(habit.copy(id = docRef.id)).await()
        return docRef.id
    }

    /** Upsert a rich habit log (status + mood + note). If the record has
     *  neither a completion nor any mood/note, the doc is deleted so the
     *  calendar stays clean. */
    suspend fun saveHabitLog(
        habitId: String,
        date: String,
        isCompleted: Boolean,
        mood: Int,
        note: String
    ) {
        val existing = firestoreProvider.habitLogsCollection()
            .whereEqualTo("habitId", habitId)
            .whereEqualTo("date", date)
            .get(Source.CACHE).await()
        val ref = existing.documents.firstOrNull()?.reference
        val isEmpty = !isCompleted && mood < 0 && note.isBlank()
        if (isEmpty) {
            ref?.delete()
            return
        }
        val log = HabitLog(
            habitId = habitId,
            date = date,
            isCompleted = isCompleted,
            completedAt = if (isCompleted) Timestamp.now() else null,
            mood = mood,
            note = note
        )
        if (ref != null) ref.set(log.copy(id = ref.id))
        else firestoreProvider.habitLogsCollection().add(log)
    }

    suspend fun toggleHabitLog(habitId: String, date: String) {
        try {
            val existing = firestoreProvider.habitLogsCollection()
                .whereEqualTo("habitId", habitId)
                .whereEqualTo("date", date)
                .get(Source.CACHE).await()

            Log.d("HabitRepo", "toggleHabitLog: habit=$habitId date=$date existing=${existing.size()}")
            if (existing.isEmpty) {
                val log = HabitLog(habitId = habitId, date = date, isCompleted = true, completedAt = Timestamp.now())
                // Do not .await() — offline writes hang waiting for server ack.
                // The local cache is updated synchronously; the cache listener fires.
                val ref = firestoreProvider.habitLogsCollection().add(log)
                Log.d("HabitRepo", "toggleHabitLog: ADDED id=${ref.result?.id ?: "pending"}")
            } else {
                val doc = existing.documents.firstOrNull()
                doc?.reference?.delete()
                Log.d("HabitRepo", "toggleHabitLog: DELETED id=${doc?.id}")
            }
        } catch (e: Exception) {
            Log.e("HabitRepo", "toggleHabitLog FAILED", e)
        }
    }

    fun observeArchivedHabits(): Flow<List<Habit>> = callbackFlow {
        val registration = firestoreProvider.habitsCollection()
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val parsed = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    runCatching { doc.toObject(Habit::class.java)?.copy(id = doc.id) }.getOrNull()
                }
                trySend(parsed.filter { it.isArchived }.sortedBy { it.sortOrder })
            }
        awaitClose { registration.remove() }
    }

    suspend fun archiveHabit(habitId: String) {
        firestoreProvider.habitsCollection().document(habitId)
            .update("isArchived", true)
    }

    suspend fun deleteHabit(habitId: String) {
        firestoreProvider.habitsCollection().document(habitId).delete()
    }
}
