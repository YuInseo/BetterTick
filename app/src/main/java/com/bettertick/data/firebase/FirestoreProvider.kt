package com.bettertick.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.PersistentCacheSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreProvider @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    init {
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder()
                        .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                        .build()
                )
                .build()
            firestore.firestoreSettings = settings
        } catch (_: IllegalStateException) {
            // Settings already applied or Firestore already in use
        }
    }

    private fun userId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User not authenticated – check auth state before accessing Firestore")
    }

    val isAuthenticated: Boolean get() = auth.currentUser != null

    private fun userDoc() = firestore.collection("users").document(userId())

    fun tasksCollection(): CollectionReference = userDoc().collection("tasks")
    fun listsCollection(): CollectionReference = userDoc().collection("lists")
    fun tagsCollection(): CollectionReference = userDoc().collection("tags")
    fun habitsCollection(): CollectionReference = userDoc().collection("habits")
    fun habitLogsCollection(): CollectionReference = userDoc().collection("habitLogs")
    fun focusSessionsCollection(): CollectionReference = userDoc().collection("focusSessions")
    fun focusCategoriesCollection(): CollectionReference = userDoc().collection("focusCategories")
    fun diaryCollection(): CollectionReference = userDoc().collection("diary")

    /** Settings live as single docs under users/{uid}/settings, e.g. "matrix". */
    fun settingsDocument(name: String) = userDoc().collection("settings").document(name)

    fun userDocument() = userDoc()
}
