package com.example.system_mini_capstone.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.system_mini_capstone.data.SessionManager
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

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val usernameError: String? = null,
    val passwordError: String? = null
)

sealed interface LoginEvent {
    data class Success(val role: String, val email: String) : LoginEvent
    data class Error(val message: String) : LoginEvent
}

class LoginViewModel(
    application: Application,
    private val authRepository: AuthRepository = AuthRepositoryImpl()
) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<LoginEvent>()
    val eventFlow: SharedFlow<LoginEvent> = _eventFlow.asSharedFlow()

    fun onUsernameChanged(username: String) {
        _uiState.update {
            it.copy(
                username = username,
                usernameError = null,
                errorMessage = null
            )
        }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                passwordError = null,
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

    fun applyPreset(presetUser: String, presetPass: String) {
        _uiState.update {
            it.copy(
                username = presetUser,
                password = presetPass,
                usernameError = null,
                passwordError = null,
                errorMessage = null
            )
        }
    }

    fun login() {
        val currentState = _uiState.value
        val username = currentState.username.trim()
        val password = currentState.password

        var hasError = false
        var userErr: String? = null
        var passErr: String? = null

        if (username.isBlank()) {
            userErr = "Username or email is required"
            hasError = true
        }

        if (password.isBlank()) {
            passErr = "Password is required"
            hasError = true
        } else if (password.length < 6) {
            passErr = "Password must be at least 6 characters"
            hasError = true
        }

        if (hasError) {
            _uiState.update {
                it.copy(
                    usernameError = userErr,
                    passwordError = passErr
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = authRepository.login(username, password)

            result.onSuccess { authResponse ->
                // Securely store returned access token, refresh token, role, and details
                sessionManager.saveAuthSession(authResponse)

                val role = authResponse.user?.role ?: "USER"
                val email = authResponse.user?.email ?: username

                _uiState.update { it.copy(isLoading = false) }
                _eventFlow.emit(LoginEvent.Success(role = role, email = email))
            }.onFailure { exception ->
                val errorMsg = exception.localizedMessage ?: "Authentication failed. Please check credentials."
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = errorMsg
                    )
                }
                _eventFlow.emit(LoginEvent.Error(errorMsg))
            }
        }
    }
}
