package com.bettertick.ui.screens.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bettertick.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow<Boolean?>(null)
    val isLoggedIn: StateFlow<Boolean?> = _isLoggedIn.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val userName: String
        get() = authRepository.currentUser?.email?.substringBefore('@') ?: ""

    val userEmail: String
        get() = authRepository.currentUser?.email ?: ""

    val userDisplayName: String
        get() = authRepository.currentUser?.displayName
            ?: authRepository.currentUser?.email?.substringBefore('@')
            ?: ""

    val userPhotoUrl: String?
        get() = authRepository.currentUser?.photoUrl?.toString()

    val isGoogleUser: Boolean
        get() = authRepository.currentUser?.providerData
            ?.any { it.providerId == "google.com" } == true

    init {
        _isLoggedIn.value = authRepository.currentUser != null
        viewModelScope.launch {
            authRepository.observeAuthState().collect { user ->
                _isLoggedIn.value = user != null
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            try {
                authRepository.signIn(email, password)
            } catch (e: Exception) {
                _authError.value = e.message ?: "Login failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signUp(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            try {
                authRepository.signUp(email, password, displayName)
            } catch (e: Exception) {
                _authError.value = e.message ?: "Registration failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            try {
                authRepository.signInWithGoogle(context)
            } catch (e: Exception) {
                _authError.value = e.message ?: "Google sign-in failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun refreshAuthState() {
        _isLoggedIn.value = authRepository.currentUser != null
    }

    fun clearError() {
        _authError.value = null
    }

    /** Returns true on success. */
    suspend fun updateDisplayName(newName: String): Boolean = runCatching {
        authRepository.updateDisplayName(newName.trim())
    }.isSuccess

    /** Returns null on success, error message on failure. */
    suspend fun deleteAccount(): String? = runCatching {
        authRepository.deleteAccount()
        null
    }.getOrElse { it.message ?: "계정 삭제 실패" }

    suspend fun signOutOfAllDevices() {
        authRepository.signOutOfAllDevices()
    }
}
