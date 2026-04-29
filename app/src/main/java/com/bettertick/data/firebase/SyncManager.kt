package com.bettertick.data.firebase

import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages a single background sync listener per collection.
 *
 * Strategy:
 * - All UI reads use ListenSource.CACHE (zero server reads)
 * - This manager starts ONE default listener per collection that
 *   syncs server changes to local cache in the background.
 * - Result: UI is always instant (cache), server sync happens silently.
 *
 * Call startSync() once after user login.
 * Call stopSync() on logout.
 */
@Singleton
class SyncManager @Inject constructor(
    private val firestoreProvider: FirestoreProvider
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val listeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()

    fun startSync() {
        if (listeners.isNotEmpty()) return // already syncing

        // Each listener keeps its collection's local cache fresh
        val collections = listOf(
            firestoreProvider.tasksCollection(),
            firestoreProvider.listsCollection(),
            firestoreProvider.tagsCollection(),
            firestoreProvider.habitsCollection(),
            firestoreProvider.habitLogsCollection(),
            firestoreProvider.focusSessionsCollection(),
            firestoreProvider.focusCategoriesCollection()
        )

        collections.forEach { collection ->
            val reg = collection.addSnapshotListener(MetadataChanges.INCLUDE) { _, _ ->
                // Just keep cache fresh, no action needed
            }
            listeners.add(reg)
        }

        // Also sync user document
        listeners.add(
            firestoreProvider.userDocument()
                .addSnapshotListener(MetadataChanges.INCLUDE) { _, _ -> }
        )
    }

    fun stopSync() {
        listeners.forEach { it.remove() }
        listeners.clear()
    }
}
