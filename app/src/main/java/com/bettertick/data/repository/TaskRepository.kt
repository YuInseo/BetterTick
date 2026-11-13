package com.bettertick.data.repository

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.bettertick.data.firebase.FirestoreProvider
import com.bettertick.data.model.Task
import com.bettertick.data.model.followingSchedule
import com.bettertick.data.model.followsSchedule
import com.bettertick.data.model.isSubtask
import com.bettertick.data.model.occursOn
import com.bettertick.data.model.unlinkedOn
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
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

    // Optimistic UI overrides — toggleComplete/setAbandoned가 Firestore round-trip
    // 결과에 의존하지 않고 즉시 시각 반영되도록 한다. snapshot listener의 cache
    // 처리가 늦거나 Kotlin 매퍼가 필드명을 헷갈려도 UI는 무조건 업데이트됨.
    // app 재시작하면 Firestore 캐시(offline-first)에서 다시 채워지므로 영구.
    private val _optimisticCompleted = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val _optimisticAbandoned = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    private fun List<Task>.applyOverrides(
        completed: Map<String, Boolean>,
        abandoned: Map<String, Boolean>
    ): List<Task> = map { t ->
        var v = t
        completed[t.id]?.let { v = v.copy(isCompleted = it) }
        abandoned[t.id]?.let { v = v.copy(isAbandoned = it) }
        v
    }

    /**
     * Firestore's mapper turns Kotlin `isCompleted` into a "completed" field on
     * write but can't map it back onto the `val`, so Boolean `is*` flags read
     * as false. Read them from the document directly (either field name).
     */
    private fun com.google.firebase.firestore.DocumentSnapshot.toTask(): Task? =
        toObject(Task::class.java)?.copy(
            id = id,
            isCompleted = getBoolean("completed") ?: getBoolean("isCompleted") ?: false,
            isAbandoned = getBoolean("abandoned") ?: getBoolean("isAbandoned") ?: false,
            isPinned = getBoolean("pinned") ?: getBoolean("isPinned") ?: false
        )

    private fun refreshWidget() {
        widgetScope.launch {
            runCatching { ReminderWidget().updateAll(appContext) }
        }
    }

    fun observeAllTasks(): Flow<List<Task>> = combine(
        rawObserveAllTasks(),
        _optimisticCompleted,
        _optimisticAbandoned
    ) { tasks, completed, abandoned -> tasks.applyOverrides(completed, abandoned) }

    private fun rawObserveAllTasks(): Flow<List<Task>> = callbackFlow {
        val registration = firestoreProvider.tasksCollection()
            .orderBy("sortOrder", Query.Direction.ASCENDING)
            .addSnapshotListener(cacheOptions) { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.map { doc ->
                    doc.toTask() ?: Task()
                } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    fun observeTasksByList(listId: String): Flow<List<Task>> = combine(
        rawObserveTasksByList(listId),
        _optimisticCompleted,
        _optimisticAbandoned
    ) { tasks, completed, abandoned -> tasks.applyOverrides(completed, abandoned) }

    private fun rawObserveTasksByList(listId: String): Flow<List<Task>> = callbackFlow {
        val registration = firestoreProvider.tasksCollection()
            .whereEqualTo("listId", listId)
            .orderBy("sortOrder", Query.Direction.ASCENDING)
            .addSnapshotListener(cacheOptions) { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.map { doc ->
                    doc.toTask() ?: Task()
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
                    doc.toTask() ?: Task()
                } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    fun observeTasksForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Task>> = combine(
        rawObserveTasksForDateRange(startDate, endDate),
        _optimisticCompleted,
        _optimisticAbandoned
    ) { tasks, completed, abandoned -> tasks.applyOverrides(completed, abandoned) }

    private fun rawObserveTasksForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Task>> = callbackFlow {
        val startTs = Timestamp(Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
        val endTs = Timestamp(Date.from(endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()))

        val registration = firestoreProvider.tasksCollection()
            .whereGreaterThanOrEqualTo("dueDate", startTs)
            .whereLessThan("dueDate", endTs)
            .addSnapshotListener(cacheOptions) { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.map { doc ->
                    doc.toTask() ?: Task()
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
        // A subtask's day / repeat always come from its parent; an edit to
        // it keeps only its own time and length.
        val toWrite = if (task.isSubtask) {
            cachedTask(task.parentId)?.let { task.followingSchedule(it) } ?: task
        } else task
        firestoreProvider.tasksCollection().document(task.id)
            .set(toWrite.copy(updatedAt = Timestamp.now()))
        if (!task.isSubtask) syncSubtasks(toWrite)
        refreshWidget()
    }

    // --- 하위 할일 ---

    private suspend fun cachedTask(id: String): Task? = runCatching {
        firestoreProvider.tasksCollection().document(id).get(Source.CACHE).await().toTask()
    }.getOrNull()

    private suspend fun cachedSubtasks(parentId: String): List<Task> = runCatching {
        firestoreProvider.tasksCollection().whereEqualTo("parentId", parentId)
            .get(Source.CACHE).await().documents.mapNotNull { it.toTask() }
    }.getOrDefault(emptyList())

    /** Re-copies [parent]'s schedule onto each of its subtasks that drifted. */
    private suspend fun syncSubtasks(parent: Task) {
        if (parent.id.isBlank()) return
        cachedSubtasks(parent.id).forEach { child ->
            if (!child.followsSchedule(parent)) {
                firestoreProvider.tasksCollection().document(child.id)
                    .set(child.followingSchedule(parent).copy(updatedAt = Timestamp.now()))
            }
        }
    }

    private suspend fun syncSubtasks(parentId: String) {
        cachedTask(parentId)?.takeIf { !it.isSubtask }?.let { syncSubtasks(it) }
    }

    /** Nests [child] under [parent]: it now repeats with the parent, at its own time. */
    suspend fun nestTask(child: Task, parent: Task) {
        if (child.id.isBlank() || parent.id.isBlank() || child.id == parent.id) return
        firestoreProvider.tasksCollection().document(child.id)
            .set(child.followingSchedule(parent, fresh = true).copy(updatedAt = Timestamp.now()))
        refreshWidget()
    }

    /** Takes [child] out of its parent; it stays on [day] as a one-off task. */
    suspend fun unnestTask(child: Task, day: LocalDate) {
        if (child.id.isBlank()) return
        firestoreProvider.tasksCollection().document(child.id)
            .set(child.unlinkedOn(day).copy(updatedAt = Timestamp.now()))
        refreshWidget()
    }

    /**
     * 완료 / 취소됨 / 내일로 미루기 / 되돌리기. [shownDate] is the day the task
     * was tapped on (calendar day or widget day) — a recurring task only
     * moves that one occurrence.
     */
    suspend fun applyStatus(task: Task, action: com.bettertick.data.model.TaskStatusAction, shownDate: LocalDate?) {
        when (action) {
            com.bettertick.data.model.TaskStatusAction.COMPLETE -> {
                if (task.isAbandoned) setAbandoned(task.id, false)
                toggleComplete(task.id, true)
            }
            com.bettertick.data.model.TaskStatusAction.REOPEN -> {
                if (task.isAbandoned) setAbandoned(task.id, false)
                if (task.isCompleted) toggleComplete(task.id, false)
            }
            com.bettertick.data.model.TaskStatusAction.CANCEL -> {
                if (task.isCompleted) toggleComplete(task.id, false)
                setAbandoned(task.id, true)
            }
            com.bettertick.data.model.TaskStatusAction.POSTPONE -> postponeToTomorrow(task, shownDate)
        }
    }

    /** 내일로 미루기: the day after the shown day (or after today, if that day already passed). */
    private suspend fun postponeToTomorrow(task: Task, shownDate: LocalDate?) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val due = task.dueDate?.toDate()?.toInstant()?.atZone(zone)
        val from = shownDate ?: due?.toLocalDate() ?: today
        val target = (if (from.isBefore(today)) today else from).plusDays(1)
        if (!task.repeatRule.isNullOrBlank()) {
            moveOccurrence(task.id, from, target)
            return
        }
        val time = due?.toLocalTime() ?: java.time.LocalTime.MIDNIGHT
        setDueDate(task.id, Timestamp(Date.from(target.atTime(time).atZone(zone).toInstant())))
        refreshWidget()
    }

    /** One server round-trip so the local cache (and every cache listener)
     *  picks up edits made on other devices while the app was closed. */
    suspend fun refreshFromServer() {
        kotlinx.coroutines.withTimeoutOrNull(10_000) {
            firestoreProvider.tasksCollection().get(Source.SERVER).await()
        }
    }

    suspend fun toggleComplete(taskId: String, isCompleted: Boolean) {
        // 1) 즉시 optimistic 반영 → UI 곧바로 토글
        _optimisticCompleted.update { it + (taskId to isCompleted) }
        // 2) 양 필드명에 동시 write (Kotlin is* Boolean 매퍼 필드명 SDK별 차이 우회)
        val updates = mapOf(
            "isCompleted" to isCompleted,
            "completed" to isCompleted,
            "completedAt" to if (isCompleted) Timestamp.now() else null,
            "updatedAt" to Timestamp.now()
        )
        runCatching {
            firestoreProvider.tasksCollection().document(taskId)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
        }.onFailure { e ->
            android.util.Log.e("TaskRepository", "toggleComplete write failed $taskId", e)
        }
        refreshWidget()
    }

    suspend fun setAbandoned(taskId: String, isAbandoned: Boolean) {
        _optimisticAbandoned.update { it + (taskId to isAbandoned) }
        val updates = mapOf(
            "isAbandoned" to isAbandoned,
            "abandoned" to isAbandoned,
            "abandonedAt" to if (isAbandoned) Timestamp.now() else null,
            "updatedAt" to Timestamp.now()
        )
        runCatching {
            firestoreProvider.tasksCollection().document(taskId)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
        }.onFailure { e ->
            android.util.Log.e("TaskRepository", "setAbandoned write failed $taskId", e)
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
        syncSubtasks(taskId)
        refreshWidget()
    }

    /** The task deleted last and when (epoch ms) — lets the UI offer 실행 취소. */
    private val _recentlyDeleted = kotlinx.coroutines.flow.MutableStateFlow<Pair<Task, Long>?>(null)
    val recentlyDeleted: kotlinx.coroutines.flow.StateFlow<Pair<Task, Long>?> = _recentlyDeleted
    /** Subtasks freed by the last delete, so 실행 취소 can nest them again. */
    private var freedSubtaskIds: List<String> = emptyList()

    suspend fun deleteTask(taskId: String) {
        val doc = firestoreProvider.tasksCollection().document(taskId)
        val existing = runCatching { doc.get(Source.CACHE).await().toTask() }.getOrNull()
        // Its subtasks become standalone and keep the schedule they had.
        val freed = cachedSubtasks(taskId)
        freed.forEach { child ->
            firestoreProvider.tasksCollection().document(child.id)
                .update(mapOf("parentId" to "", "updatedAt" to Timestamp.now()))
        }
        doc.delete()
        if (existing != null) {
            freedSubtaskIds = freed.map { it.id }
            _recentlyDeleted.value = existing to System.currentTimeMillis()
        }
        refreshWidget()
    }

    /** Puts back the task [deleteTask] removed last, under its old id. */
    suspend fun undoDelete() {
        val task = _recentlyDeleted.value?.first ?: return
        _recentlyDeleted.value = null
        if (task.id.isBlank()) return
        firestoreProvider.tasksCollection().document(task.id).set(task)
        freedSubtaskIds.forEach { id ->
            firestoreProvider.tasksCollection().document(id)
                .update(mapOf("parentId" to task.id, "updatedAt" to Timestamp.now()))
        }
        freedSubtaskIds = emptyList()
        refreshWidget()
    }

    fun clearRecentlyDeleted() {
        _recentlyDeleted.value = null
    }

    /**
     * Skip a single occurrence of a recurring task. Appends the ISO date
     * to the task's `exceptions` list so [Task.occursOn] stops returning
     * true for that date while leaving the series intact.
     */
    suspend fun skipOccurrence(taskId: String, date: LocalDate) {
        val doc = firestoreProvider.tasksCollection().document(taskId)
        val snapshot = doc.get(Source.CACHE).await()
        val existing = snapshot.toTask() ?: return
        val iso = date.toString()
        if (iso in existing.exceptions) return
        doc.update(
            mapOf(
                "exceptions" to existing.exceptions + iso,
                "updatedAt" to Timestamp.now()
            )
        )
        // Skipping a parent's day skips its subtasks that day too.
        syncSubtasks(existing.copy(exceptions = existing.exceptions + iso))
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
        val original = snapshot.toTask() ?: return

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
            parentId = "",
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        newDoc.set(detached)
        // Moving a parent's day moves that day of each subtask along with it.
        if (!original.isSubtask) {
            cachedSubtasks(taskId).filter { it.occursOn(sourceDate) }.forEach { child ->
                moveOccurrence(child.id, sourceDate, newDate)
            }
        }
        refreshWidget()
    }
}
