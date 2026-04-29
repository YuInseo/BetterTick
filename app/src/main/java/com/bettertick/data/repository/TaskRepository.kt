package com.bettertick.data.repository

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.bettertick.data.firebase.FirestoreProvider
import com.bettertick.data.model.Task
import com.bettertick.widget.calendar.ReminderWidget
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenSource
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SnapshotListenOptions
import com.google.firebase.firestore.Source
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first task repository.
 *
 * READ strategy:
 * - Uses SnapshotListener with ListenSource.CACHE to read ONLY from local cache.
 * - No server reads are triggered by listeners → zero read costs while browsing.
 * - A single DEFAULT listener on the root collection syncs data in background.
 *
 * WRITE strategy:
 * - All writes go to local cache first (instant UI update).
 * - Firestore queues writes and syncs to server when online.
 * - No await() on writes for UI responsiveness (fire-and-forget locally).
 */
@Singleton
class TaskRepository @Inject constructor(
    private val firestoreProvider: FirestoreProvider,
    @ApplicationContext private val appContext: Context
) {
    // Cache-only listen options — reads ONLY from local disk, never hits server
    private val cacheOptions = SnapshotListenOptions.Builder()
        .setSource(ListenSource.CACHE)
        .build()

    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun refreshWidget() {
        widgetScope.launch {
            runCatching { ReminderWidget().updateAll(appContext) }
        }
    }

    fun observeAllTasks(): Flow<List<Task>> = callbackFlow {
        val registration = firestoreProvider.tasksCollection()
            .orderBy("sortOrder", Query.Direction.ASCENDING)
            .addSnapshotListener(cacheOptions) { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.map { doc ->
                    doc.toObject(Task::class.java)?.copy(id = doc.id) ?: Task()
                } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    fun observeTasksByList(listId: String): Flow<List<Task>> = callbackFlow {
        val registration = firestoreProvider.tasksCollection()
            .whereEqualTo("listId", listId)
            .orderBy("sortOrder", Query.Direction.ASCENDING)
            .addSnapshotListener(cacheOptions) { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.map { doc ->
                    doc.toObject(Task::class.java)?.copy(id = doc.id) ?: Task()
                } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    fun observeIncompleteTasks(): Flow<List<Task>> = callbackFlow {
        val registration = firestoreProvider.tasksCollection()
            .orderBy("sortOrder", Query.Direction.ASCENDING)
            .addSnapshotListener(cacheOptions) { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.map { doc ->
                    doc.toObject(Task::class.java)?.copy(id = doc.id) ?: Task()
                } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    fun observeTasksForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Task>> = callbackFlow {
        val startTs = Timestamp(Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
        val endTs = Timestamp(Date.from(endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()))

        val registration = firestoreProvider.tasksCollection()
            .whereGreaterThanOrEqualTo("dueDate", startTs)
            .whereLessThan("dueDate", endTs)
            .addSnapshotListener(cacheOptions) { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.map { doc ->
                    doc.toObject(Task::class.java)?.copy(id = doc.id) ?: Task()
                } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    /**
     * Background sync listener — the ONLY listener that talks to the server.
     * Call this once at app startup to keep local cache fresh.
     * Uses default source (server + cache) with metadata changes.
     */
    fun startBackgroundSync(): Flow<Boolean> = callbackFlow {
        val registration = firestoreProvider.tasksCollection()
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, _ ->
                val isFromCache = snapshot?.metadata?.isFromCache ?: true
                trySend(isFromCache)
            }
        awaitClose { registration.remove() }
    }

    // --- WRITES: all go to local cache first, sync automatically ---

    suspend fun addTask(task: Task): String {
        val docRef = firestoreProvider.tasksCollection().document()
        docRef.set(task.copy(id = docRef.id))  // no .await() → instant local write
        refreshWidget()
        return docRef.id
    }

    suspend fun updateTask(task: Task) {
        firestoreProvider.tasksCollection().document(task.id)
            .set(task.copy(updatedAt = Timestamp.now()))
        refreshWidget()
    }

    suspend fun toggleComplete(taskId: String, isCompleted: Boolean) {
        val updates = mapOf(
            "isCompleted" to isCompleted,
            "completed" to isCompleted,
            "completedAt" to if (isCompleted) Timestamp.now() else null,
            "updatedAt" to Timestamp.now()
        )
        val msg = StringBuilder()
        try {
            // .await() 제거 — set은 fire-and-forget으로 호출만 하고 즉시 반환.
            // Firestore의 local cache 쓰기는 동기적으로 일어나야 하므로 listener도
            // 곧바로 fire되어야 정상.
            firestoreProvider.tasksCollection().document(taskId)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
            msg.append("set issued OK\n")

            // 200ms 후 cache 상태 읽어서 진짜로 반영됐는지 확인
            kotlinx.coroutines.delay(250)
            val snap = firestoreProvider.tasksCollection().document(taskId)
                .get(Source.CACHE).await()
            val cachedIsCompleted = snap.getBoolean("isCompleted")
            val cachedCompleted = snap.getBoolean("completed")
            msg.append("cache: isCompleted=$cachedIsCompleted, completed=$cachedCompleted\n")
            msg.append("hasPendingWrites=${snap.metadata.hasPendingWrites()}")
            android.util.Log.d("TaskRepository", "toggleComplete diag $taskId → $msg")
        } catch (e: Exception) {
            msg.append("EXCEPTION: ${e.javaClass.simpleName}: ${e.message?.take(120)}")
            android.util.Log.e("TaskRepository", "toggleComplete FAILED $taskId", e)
        }
        kotlinx.coroutines.withContext(Dispatchers.Main) {
            android.widget.Toast.makeText(
                appContext,
                msg.toString(),
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
        refreshWidget()
    }

    suspend fun setAbandoned(taskId: String, isAbandoned: Boolean) {
        val updates = mapOf(
            "isAbandoned" to isAbandoned,
            "abandoned" to isAbandoned,
            "abandonedAt" to if (isAbandoned) Timestamp.now() else null,
            "updatedAt" to Timestamp.now()
        )
        try {
            firestoreProvider.tasksCollection().document(taskId)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
            android.util.Log.d("TaskRepository", "setAbandoned OK $taskId → $isAbandoned")
        } catch (e: Exception) {
            android.util.Log.e("TaskRepository", "setAbandoned FAILED $taskId", e)
            throw e
        }
        refreshWidget()
    }

    suspend fun setDueDate(taskId: String, dueDate: Timestamp?) {
        firestoreProvider.tasksCollection().document(taskId).update(
            mapOf(
                "dueDate" to dueDate,
                "updatedAt" to Timestamp.now()
            )
        )
        refreshWidget()
    }

    suspend fun deleteTask(taskId: String) {
        firestoreProvider.tasksCollection().document(taskId).delete()
        refreshWidget()
    }

    /**
     * Skip a single occurrence of a recurring task. Appends the ISO date
     * to the task's `exceptions` list so [Task.occursOn] stops returning
     * true for that date while leaving the series intact.
     */
    suspend fun skipOccurrence(taskId: String, date: LocalDate) {
        val doc = firestoreProvider.tasksCollection().document(taskId)
        val snapshot = doc.get(Source.CACHE).await()
        val existing = snapshot.toObject(Task::class.java) ?: return
        val iso = date.toString()
        if (iso in existing.exceptions) return
        doc.update(
            mapOf(
                "exceptions" to existing.exceptions + iso,
                "updatedAt" to Timestamp.now()
            )
        )
        refreshWidget()
    }

    /**
     * Move a single occurrence of a recurring task to [newDate]. The source
     * date is added to `exceptions`, and a standalone (non-recurring) copy
     * is created at the target date so the moved instance renders there
     * without dragging the whole series with it.
     */
    suspend fun moveOccurrence(taskId: String, sourceDate: LocalDate, newDate: LocalDate) {
        val doc = firestoreProvider.tasksCollection().document(taskId)
        val snapshot = doc.get(Source.CACHE).await()
        val original = snapshot.toObject(Task::class.java) ?: return

        // Preserve the original time-of-day when shifting the occurrence so
        // the moved copy keeps its alarm/start time.
        val time = original.dueDate?.toDate()?.toInstant()
            ?.atZone(ZoneId.systemDefault())?.toLocalTime()
            ?: java.time.LocalTime.MIDNIGHT
        val movedDue = Timestamp(
            Date.from(newDate.atTime(time).atZone(ZoneId.systemDefault()).toInstant())
        )

        // Skip the source occurrence on the series.
        val iso = sourceDate.toString()
        if (iso !in original.exceptions) {
            doc.update(
                mapOf(
                    "exceptions" to original.exceptions + iso,
                    "updatedAt" to Timestamp.now()
                )
            )
        }

        // Drop a detached copy at the target date — no repeat, no exceptions.
        val newDoc = firestoreProvider.tasksCollection().document()
        val detached = original.copy(
            id = newDoc.id,
            dueDate = movedDue,
            repeatRule = null,
            repeatEnd = null,
            exceptions = emptyList(),
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        newDoc.set(detached)
        refreshWidget()
    }
}
