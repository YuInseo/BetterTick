package com.bettertick.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.bettertick.data.firebase.DataSeeder
import com.bettertick.data.firebase.FirestoreProvider
import com.bettertick.data.model.User
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestoreProvider: FirestoreProvider,
    private val dataSeeder: DataSeeder
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    companion object {
        private const val WEB_CLIENT_ID =
            "739238902578-qh2378342mrb8vngipu919d2e7r66p55.apps.googleusercontent.com"
    }

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun signUp(email: String, password: String, displayName: String) {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: return
        createUserAndSeedData(uid, email, displayName)
    }

    suspend fun signInWithGoogle(context: Context) {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(context, request)
        val credential = result.credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()

            if (authResult.additionalUserInfo?.isNewUser == true) {
                val user = authResult.user ?: return
                createUserAndSeedData(
                    uid = user.uid,
                    email = user.email ?: "",
                    displayName = user.displayName ?: "",
                    photoUrl = user.photoUrl?.toString() ?: ""
                )
            }
        }
    }

    private suspend fun createUserAndSeedData(
        uid: String,
        email: String,
        displayName: String,
        photoUrl: String = ""
    ) {
        val user = User(
            id = uid,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl,
            createdAt = Timestamp.now()
        )
        firestoreProvider.userDocument().set(user).await()
        dataSeeder.seedDefaultData()
    }

    fun signOut() {
        auth.signOut()
    }

    /** Updates the current user's Firebase Auth display name and Firestore doc. */
    suspend fun updateDisplayName(newName: String) {
        val user = auth.currentUser ?: return
        val request = userProfileChangeRequest {
            displayName = newName
        }
        user.updateProfile(request).await()
        runCatching {
            firestoreProvider.userDocument().update("displayName", newName).await()
        }
    }

    /**
     * Deletes the current user's Firebase Auth account. May fail with
     * FirebaseAuthRecentLoginRequiredException — caller handles by asking the
     * user to sign in again first.
     */
    suspend fun deleteAccount() {
        val user = auth.currentUser ?: return
        user.delete().await()
    }

    /** Signs out of all devices by revoking the refresh token (approximation). */
    suspend fun signOutOfAllDevices() {
        // Firebase doesn't expose a revoke-all-refresh-tokens API from client.
        // Best-effort: sign out locally. Other devices will be signed out on
        // next token refresh only if admin SDK is used server-side.
        auth.signOut()
    }
}
