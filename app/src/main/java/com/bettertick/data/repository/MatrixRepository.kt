package com.bettertick.data.repository

import com.bettertick.data.firebase.FirestoreProvider
import com.bettertick.data.model.MatrixConfig
import com.bettertick.data.model.defaultMatrix
import com.google.firebase.firestore.ListenSource
import com.google.firebase.firestore.SnapshotListenOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatrixRepository @Inject constructor(
    private val firestoreProvider: FirestoreProvider
) {
    private val cacheOptions = SnapshotListenOptions.Builder()
        .setSource(ListenSource.CACHE)
        .build()

    fun observeConfig(): Flow<MatrixConfig> = callbackFlow {
        val registration = firestoreProvider.settingsDocument("matrix")
            .addSnapshotListener(cacheOptions) { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                // Missing doc → ship defaults so the UI still renders a four-quadrant
                // layout on first launch.
                val config = snapshot?.toObject(MatrixConfig::class.java)
                    ?.takeIf { it.quadrants.isNotEmpty() }
                    ?: defaultMatrix
                trySend(config)
            }
        awaitClose { registration.remove() }
    }

    suspend fun saveConfig(config: MatrixConfig) {
        firestoreProvider.settingsDocument("matrix").set(config)
    }
}
