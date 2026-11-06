package com.example.system_mini_capstone.ui

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.system_mini_capstone.data.network.AuthRepository
import com.example.system_mini_capstone.data.network.AuthRepositoryImpl
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RecoveryStep {
    REQUEST_CODE,    // Step 1: Input registered email
    RESET_PASSWORD,  // Step 2: Input reset token/code & new password
    COMPLETED        // Step 3: Success state before looping back to Login
}

data class ForgotPasswordUiState(
    val email: String = "",
    val resetToken: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val step: RecoveryStep = RecoveryStep.REQUEST_CODE,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val emailError: String? = null,
    val tokenError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null
)

sealed interface ForgotPasswordEvent {
    data class CodeSent(val message: String) : ForgotPasswordEvent
    data object PasswordResetSuccess : ForgotPasswordEvent
}

class ForgotPasswordViewModel(
    application: Application,
    private val authRepository: AuthRepository = AuthRepositoryImpl()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ForgotPasswordEvent>()
    val eventFlow: SharedFlow<ForgotPasswordEvent> = _eventFlow.asSharedFlow()

    fun onEmailChanged(email: String) {
        _uiState.update {
            it.copy(
                email = email,
                emailError = null,
                errorMessage = null
            )
        }
    }

    fun onTokenChanged(token: String) {
        _uiState.update {
            it.copy(
                resetToken = token,
                tokenError = null,
                errorMessage = null
            )
        }
    }

    fun onNewPasswordChanged(password: String) {
        _uiState.update {
            it.copy(
                newPassword = password,
                passwordError = null,
                errorMessage = null
            )
        }
    }

    fun onConfirmPasswordChanged(password: String) {
        _uiState.update {
            it.copy(
                confirmPassword = password,
                confirmPasswordError = null,
                errorMessage = null
            )
        }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Step 1: Requests a password recovery link / token from the backend.
     */
    fun sendRecoveryRequest() {
        val email = _uiState.value.email.trim()

        if (email.isBlank()) {
            _uiState.update { it.copy(emailError = "Please enter your email address") }
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(emailError = "Please enter a valid email address") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = authRepository.requestPasswordReset(email)

            result.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        step = RecoveryStep.RESET_PASSWORD,
                        resetToken = response.resetToken ?: it.resetToken,
                        infoMessage = response.message
                    )
                }
                _eventFlow.emit(ForgotPasswordEvent.CodeSent(response.message))
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = exception.localizedMessage ?: "Failed to send reset code."
                    )
                }
            }
        }
    }

    /**
     * Step 2: Validates the token and sets the new password.
     */
    fun submitNewPassword() {
        val currentState = _uiState.value
        val email = currentState.email.trim()
        val token = currentState.resetToken.trim()
        val newPassword = currentState.newPassword
        val confirmPassword = currentState.confirmPassword

        var hasError = false
        var tokenErr: String? = null
        var passErr: String? = null
        var confirmErr: String? = null

        if (token.isBlank()) {
            tokenErr = "Reset code or token is required"
            hasError = true
        }

        if (newPassword.isBlank()) {
            passErr = "New password cannot be empty"
            hasError = true
        } else if (newPassword.length < 6) {
            passErr = "Password must be at least 6 characters"
            hasError = true
        }

        if (confirmPassword != newPassword) {
            confirmErr = "Passwords do not match"
            hasError = true
        }

        if (hasError) {
            _uiState.update {
                it.copy(
                    tokenError = tokenErr,
                    passwordError = passErr,
                    confirmPasswordError = confirmErr
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = authRepository.resetPassword(email, token, newPassword)

            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        step = RecoveryStep.COMPLETED,
                        infoMessage = "Password successfully reset! You can now sign in."
                    )
                }
                _eventFlow.emit(ForgotPasswordEvent.PasswordResetSuccess)
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = exception.localizedMessage ?: "Unable to reset password."
                    )
                }
            }
        }
    }
}
