package com.bettertick.data.repository.interfaces

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface IAuthRepository {
    val currentUser: FirebaseUser?
    fun observeAuthState(): Flow<FirebaseUser?>
    suspend fun signIn(email: String, password: String)
    suspend fun signUp(email: String, password: String, displayName: String)
    fun signOut()
}
