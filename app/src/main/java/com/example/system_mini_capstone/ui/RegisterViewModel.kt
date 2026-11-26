package com.example.system_mini_capstone.ui

import android.app.Application
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.system_mini_capstone.data.SessionManager
import com.example.system_mini_capstone.data.network.AuthErrorMapper
import com.example.system_mini_capstone.data.network.GoogleAuthHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

data class RegisterUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val displayNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null
)

sealed interface RegisterEvent {
    data class Success(val email: String, val displayName: String) : RegisterEvent
    data class Error(val message: String) : RegisterEvent
}

class RegisterViewModel @JvmOverloads constructor(
    application: Application
) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<RegisterEvent>()
    val eventFlow: SharedFlow<RegisterEvent> = _eventFlow.asSharedFlow()

    fun onDisplayNameChanged(name: String) {
        _uiState.update { it.copy(displayName = name, displayNameError = null, errorMessage = null) }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, errorMessage = null) }
    }

    fun onConfirmPasswordChanged(password: String) {
        _uiState.update { it.copy(confirmPassword = password, confirmPasswordError = null, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun signInWithGoogle(context: android.content.Context) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = GoogleAuthHelper.signInWithGoogle(context, sessionManager)
            result.onSuccess { email ->
                _uiState.update { it.copy(isLoading = false) }
                _eventFlow.emit(RegisterEvent.Success(email = email, displayName = email.substringBefore("@")))
            }.onFailure { exception ->
                if (exception is androidx.credentials.exceptions.GetCredentialCancellationException) {
                    _uiState.update { it.copy(isLoading = false) }
                } else {
                    val errorMsg = exception.localizedMessage ?: "Google sign-in failed. Please try again."
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = errorMsg
                        )
                    }
                }
            }
        }
    }

    fun register() {
        if (_uiState.value.isLoading) return

        val state = _uiState.value
        val name = state.displayName.trim()
        val email = state.email.trim()
        val password = state.password
        val confirm = state.confirmPassword

        var hasError = false
        var nameErr: String? = null
        var emailErr: String? = null
        var passErr: String? = null
        var confirmErr: String? = null

        if (name.isBlank()) {
            nameErr = "Display name is required"
            hasError = true
        }

        if (email.isBlank()) {
            emailErr = "Email address is required"
            hasError = true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailErr = "Please enter a valid email address"
            hasError = true
        }

        if (password.isBlank()) {
            passErr = "Password is required"
            hasError = true
        } else if (password.length < 6) {
            passErr = "Choose a password with at least 6 characters."
            hasError = true
        }

        if (confirm.isBlank()) {
            confirmErr = "Please confirm your password"
            hasError = true
        } else if (password != confirm) {
            confirmErr = "Passwords do not match"
            hasError = true
        }

        if (hasError) {
            _uiState.update {
                it.copy(
                    displayNameError = nameErr,
                    emailError = emailErr,
                    passwordError = passErr,
                    confirmPasswordError = confirmErr
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // 1. Create Firebase Auth user
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user ?: throw Exception("Failed to create user account.")

                // 2. Update user profile display name
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                user.updateProfile(profileUpdates).await()

                // 3. Create Firestore user document wrapped in 5s timeout
                val userDoc = mapOf(
                    "uid" to user.uid,
                    "displayName" to name,
                    "email" to email,
                    "role" to "USER",
                    "createdAt" to System.currentTimeMillis()
                )
                try {
                    withTimeoutOrNull(5000) {
                        firestore.collection("users").document(user.uid).set(userDoc).await()
                    }
                } catch (e: Exception) {
                    Log.w("SignLinkAuth", "Firestore user document write timed out or failed: ${e.message}", e)
                }

                // 4. Save session with SessionManager using real uid, email, display name with role "USER"
                sessionManager.saveUserSession(user.uid, email, name, "USER")

                _uiState.update { it.copy(isLoading = false) }
                _eventFlow.emit(RegisterEvent.Success(email = email, displayName = name))
            } catch (e: Exception) {
                Log.e("SignLinkAuth", "Registration failed", e)
                val friendlyMessage = AuthErrorMapper.mapErrorWithDebug(e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = friendlyMessage
                    )
                }
                _eventFlow.emit(RegisterEvent.Error(friendlyMessage))
            }
        }
    }
}
