package com.bettertick.data.repository

import com.bettertick.data.firebase.FirestoreProvider
import com.bettertick.data.model.DiaryEntry
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenSource
import com.google.firebase.firestore.SnapshotListenOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiaryRepository @Inject constructor(
    private val firestoreProvider: FirestoreProvider
) {
    private val cacheOptions = SnapshotListenOptions.Builder()
        .setSource(ListenSource.CACHE)
        .build()

    fun observeAllEntries(): Flow<List<DiaryEntry>> = callbackFlow {
        val reg = firestoreProvider.diaryCollection()
            .addSnapshotListener(cacheOptions) { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull { doc ->
                    doc.toObject(DiaryEntry::class.java)?.copy(id = doc.id)
                } ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    fun observeEntryForDate(dateStr: String): Flow<DiaryEntry?> = callbackFlow {
        val reg = firestoreProvider.diaryCollection()
            .whereEqualTo("dateStr", dateStr)
            .addSnapshotListener(cacheOptions) { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val entry = snap?.documents?.firstOrNull()?.let { doc ->
                    doc.toObject(DiaryEntry::class.java)?.copy(id = doc.id)
                }
                trySend(entry)
            }
        awaitClose { reg.remove() }
    }

    suspend fun saveEntry(entry: DiaryEntry) {
        if (entry.id.isBlank()) {
            val ref = firestoreProvider.diaryCollection().document()
            ref.set(entry.copy(id = ref.id))
        } else {
            firestoreProvider.diaryCollection().document(entry.id)
                .set(entry.copy(updatedAt = Timestamp.now()))
        }
    }

    suspend fun deleteEntry(entryId: String) {
        firestoreProvider.diaryCollection().document(entryId).delete()
    }
}
