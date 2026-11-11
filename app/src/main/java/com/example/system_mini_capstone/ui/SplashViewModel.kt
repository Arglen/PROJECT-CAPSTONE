package com.example.system_mini_capstone.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.system_mini_capstone.data.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

sealed interface SplashNavigationState {
    data object Loading : SplashNavigationState
    data object NavigateToDashboard : SplashNavigationState
    data object NavigateToLogin : SplashNavigationState
}

class SplashViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager by lazy { SessionManager(application) }

    private val _navigationState = MutableStateFlow<SplashNavigationState>(SplashNavigationState.Loading)
    val navigationState: StateFlow<SplashNavigationState> = _navigationState.asStateFlow()

    private var hasNavigated = false

    init {
        validateSession()
    }

    /**
     * Checks if a valid, unexpired token exists off the main thread.
     * Enforces a brief presentation delay (700ms) for smooth animation.
     */
    fun validateSession() {
        if (hasNavigated) return
        viewModelScope.launch {
            _navigationState.value = SplashNavigationState.Loading

            val isValid = withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                val sessionValid = try {
                    withTimeoutOrNull(3000L) {
                        sessionManager.isSessionValid()
                    } ?: false
                } catch (_: Exception) {
                    false
                }
                val elapsedTime = System.currentTimeMillis() - startTime
                val remainingDelay = (SPLASH_MIN_DURATION_MS - elapsedTime).coerceAtLeast(0)
                delay(remainingDelay)
                sessionValid
            }

            if (!hasNavigated) {
                hasNavigated = true
                _navigationState.value = if (isValid) {
                    SplashNavigationState.NavigateToDashboard
                } else {
                    SplashNavigationState.NavigateToLogin
                }
            }
        }
    }

    companion object {
        private const val SPLASH_MIN_DURATION_MS = 700L
    }
}
