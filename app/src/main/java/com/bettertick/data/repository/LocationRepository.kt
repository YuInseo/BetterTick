package com.bettertick.data.repository

import android.content.Context
import com.bettertick.data.firebase.FirestoreProvider
import com.bettertick.data.model.FavoritePlace
import com.bettertick.data.model.LocationRecord
import com.google.firebase.firestore.ListenSource
import com.google.firebase.firestore.SnapshotListenOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val firestoreProvider: FirestoreProvider,
    @ApplicationContext context: Context
) {
    private val cacheOptions = SnapshotListenOptions.Builder()
        .setSource(ListenSource.CACHE)
        .build()

    // Firestore Spark(무료) 한도(쓰기 2만/일)를 절대 넘지 않도록, 고빈도인 '경로
    // 점' 쓰기를 하루 안전선으로 제한한다. 장소(깃발)·즐겨찾기는 양이 적어 제한 X.
    private val budgetPrefs = context.getSharedPreferences("loc_write_budget", Context.MODE_PRIVATE)

    /** 오늘 경로 점 쓰기가 한도 내인지 확인하고, 허용 시 카운트를 1 올린다. */
    @Synchronized
    private fun allowPathWrite(): Boolean {
        val today = LocalDate.now().toString()
        val day = budgetPrefs.getString("day", "")
        val count = if (day == today) budgetPrefs.getInt("count", 0) else 0
        if (count >= DAILY_PATH_WRITE_CAP) return false
        budgetPrefs.edit().putString("day", today).putInt("count", count + 1).apply()
        return true
    }

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

    /** 위치 기록이 하나라도 있는 날짜(dateStr) 집합. 캐시 기반(서버 읽기 없음). */
    fun observeRecordedDates(): Flow<Set<String>> = callbackFlow {
        if (!firestoreProvider.isAuthenticated) { trySend(emptySet()); awaitClose { }; return@callbackFlow }
        val reg = firestoreProvider.locationRecordsCollection()
            .addSnapshotListener(cacheOptions) { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull { it.getString("dateStr") }?.toSet() ?: emptySet())
            }
        awaitClose { reg.remove() }
    }

    suspend fun addRecord(record: LocationRecord) {
        if (!firestoreProvider.isAuthenticated) return
        // 고빈도 경로 점은 일일 한도 내에서만 쓴다(무료 쓰기 2만/일 보호).
        // 장소(깃발) 기록은 양이 적고 중요하므로 항상 통과.
        if (!record.isPlace && !allowPathWrite()) return
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

    suspend fun removeFavorite(id: String) {
        if (!firestoreProvider.isAuthenticated || id.isBlank()) return
        firestoreProvider.favoritePlacesCollection().document(id).delete().await()
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

    companion object {
        // 하루 경로 점 쓰기 안전 상한. Firestore 무료 한도(2만/일)보다 훨씬 낮게
        // 잡아, 장소·즐겨찾기 등 다른 쓰기를 더해도 절대 한도를 넘지 않게 한다.
        // 8천 점이면 20m 간격 기준 ~160km 경로라 실사용엔 충분.
        private const val DAILY_PATH_WRITE_CAP = 8000
    }
}
