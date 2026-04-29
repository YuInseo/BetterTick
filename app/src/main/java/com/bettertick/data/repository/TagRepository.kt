package com.bettertick.data.repository

import com.bettertick.data.firebase.FirestoreProvider
import com.bettertick.data.model.Tag
import com.google.firebase.firestore.ListenSource
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SnapshotListenOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(
    private val firestoreProvider: FirestoreProvider
) {
    private val cacheOptions = SnapshotListenOptions.Builder()
        .setSource(ListenSource.CACHE)
        .build()

    fun observeTags(): Flow<List<Tag>> = callbackFlow {
        val registration = firestoreProvider.tagsCollection()
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener(cacheOptions) { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.map { doc ->
                    doc.toObject(Tag::class.java)?.copy(id = doc.id) ?: Tag()
                } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    suspend fun addTag(tag: Tag): String {
        val docRef = firestoreProvider.tagsCollection().document()
        docRef.set(tag.copy(id = docRef.id))
        return docRef.id
    }

    suspend fun updateTag(tag: Tag) {
        firestoreProvider.tagsCollection().document(tag.id).set(tag)
    }

    suspend fun deleteTag(tagId: String) {
        firestoreProvider.tagsCollection().document(tagId).delete()
    }
}
