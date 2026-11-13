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
                _authError.value = authErrorMessage(e, "로그인하지 못했습니다.")
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
                _authError.value = authErrorMessage(e, "가입하지 못했습니다.")
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
                // Backing out of the account picker isn't an error worth showing.
                _authError.value = if (e::class.java.simpleName.contains("Cancel")) null
                else authErrorMessage(e, "Google 로그인을 하지 못했습니다.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Sends a reset mail; null on success, else what went wrong. */
    suspend fun sendPasswordReset(email: String): String? = runCatching {
        authRepository.sendPasswordReset(email.trim())
        null
    }.getOrElse { authErrorMessage(it, "메일을 보내지 못했습니다.") }

    /** Firebase's English errors in plain Korean. */
    private fun authErrorMessage(e: Throwable, fallback: String): String = when (e) {
        is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "가입되지 않은 이메일입니다."
        is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> "비밀번호는 6자 이상이어야 합니다."
        is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "이메일 또는 비밀번호가 올바르지 않습니다."
        is com.google.firebase.auth.FirebaseAuthUserCollisionException -> "이미 가입된 이메일입니다."
        is com.google.firebase.FirebaseNetworkException -> "네트워크 연결을 확인해 주세요."
        is com.google.firebase.FirebaseTooManyRequestsException -> "시도가 너무 많습니다. 잠시 후 다시 해 주세요."
        else -> fallback
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
