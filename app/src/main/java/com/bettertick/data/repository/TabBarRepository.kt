package com.bettertick.data.repository

import com.bettertick.data.firebase.FirestoreProvider
import com.bettertick.data.model.TabBarConfig
import com.bettertick.data.model.defaultTabBarConfig
import com.google.firebase.firestore.ListenSource
import com.google.firebase.firestore.SnapshotListenOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TabBarRepository @Inject constructor(
    private val firestoreProvider: FirestoreProvider
) {
    private val cacheOptions = SnapshotListenOptions.Builder()
        .setSource(ListenSource.CACHE)
        .build()

    fun observeConfig(): Flow<TabBarConfig> = callbackFlow {
        val registration = firestoreProvider.settingsDocument("tabbar")
            .addSnapshotListener(cacheOptions) { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                // Missing doc / empty id list → fall back to defaults so the
                // nav never renders as an empty strip.
                val config = snapshot?.toObject(TabBarConfig::class.java)
                    ?.takeIf { it.enabledIds.isNotEmpty() }
                    ?: defaultTabBarConfig
                trySend(config)
            }
        awaitClose { registration.remove() }
    }

    suspend fun saveConfig(config: TabBarConfig) {
        firestoreProvider.settingsDocument("tabbar").set(config)
    }
}
