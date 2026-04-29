package com.bettertick.data.repository

import com.bettertick.data.firebase.FirestoreProvider
import com.bettertick.data.model.TaskList
import com.google.firebase.firestore.ListenSource
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SnapshotListenOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListRepository @Inject constructor(
    private val firestoreProvider: FirestoreProvider
) {
    private val cacheOptions = SnapshotListenOptions.Builder()
        .setSource(ListenSource.CACHE)
        .build()

    fun observeLists(): Flow<List<TaskList>> = callbackFlow {
        val registration = firestoreProvider.listsCollection()
            .orderBy("sortOrder", Query.Direction.ASCENDING)
            .addSnapshotListener(cacheOptions) { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.map { doc ->
                    doc.toObject(TaskList::class.java)?.copy(id = doc.id) ?: TaskList()
                } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    suspend fun addList(list: TaskList): String {
        val docRef = firestoreProvider.listsCollection().document()
        docRef.set(list.copy(id = docRef.id))
        return docRef.id
    }

    suspend fun updateList(list: TaskList) {
        firestoreProvider.listsCollection().document(list.id).set(list)
    }

    suspend fun setPinned(listId: String, pinned: Boolean) {
        firestoreProvider.listsCollection().document(listId)
            .update("isPinned", pinned)
    }

    suspend fun deleteList(listId: String) {
        firestoreProvider.listsCollection().document(listId).delete()
    }
}
