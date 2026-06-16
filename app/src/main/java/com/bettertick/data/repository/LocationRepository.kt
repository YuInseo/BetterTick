package com.bettertick.data.repository

import com.bettertick.data.firebase.FirestoreProvider
import com.bettertick.data.model.FavoritePlace
import com.bettertick.data.model.LocationRecord
import com.google.firebase.firestore.ListenSource
import com.google.firebase.firestore.SnapshotListenOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val firestoreProvider: FirestoreProvider
) {
    private val cacheOptions = SnapshotListenOptions.Builder()
        .setSource(ListenSource.CACHE)
        .build()

    fun observeRecordsForDate(dateStr: String): Flow<List<LocationRecord>> = callbackFlow {
        if (!firestoreProvider.isAuthenticated) { trySend(emptyList()); awaitClose { }; return@callbackFlow }
        val reg = firestoreProvider.locationRecordsCollection()
            .whereEqualTo("dateStr", dateStr)
            .orderBy("timestamp")
            .addSnapshotListener(cacheOptions) { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull { doc ->
                    doc.toObject(LocationRecord::class.java)?.copy(id = doc.id)
                } ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    suspend fun addRecord(record: LocationRecord) {
        if (!firestoreProvider.isAuthenticated) return
        val ref = firestoreProvider.locationRecordsCollection().document()
        ref.set(record.copy(id = ref.id)).await()
    }

    fun observeFavorites(): Flow<List<FavoritePlace>> = callbackFlow {
        if (!firestoreProvider.isAuthenticated) { trySend(emptyList()); awaitClose { }; return@callbackFlow }
        val reg = firestoreProvider.favoritePlacesCollection()
            .addSnapshotListener(cacheOptions) { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull { doc ->
                    doc.toObject(FavoritePlace::class.java)?.copy(id = doc.id)
                } ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    suspend fun addFavorite(name: String, lat: Double, lng: Double) {
        if (!firestoreProvider.isAuthenticated) return
        val ref = firestoreProvider.favoritePlacesCollection().document()
        ref.set(FavoritePlace(id = ref.id, name = name, latitude = lat, longitude = lng)).await()
    }

    suspend fun getLastRecord(): LocationRecord? {
        if (!firestoreProvider.isAuthenticated) return null
        return firestoreProvider.locationRecordsCollection()
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.toObject(LocationRecord::class.java)
    }
}
