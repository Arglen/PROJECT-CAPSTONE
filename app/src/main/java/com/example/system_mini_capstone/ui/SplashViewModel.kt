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

sealed interface SplashNavigationState {
    data object Loading : SplashNavigationState
    data object NavigateToDashboard : SplashNavigationState
    data object NavigateToLogin : SplashNavigationState
}

class SplashViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)

    private val _navigationState = MutableStateFlow<SplashNavigationState>(SplashNavigationState.Loading)
    val navigationState: StateFlow<SplashNavigationState> = _navigationState.asStateFlow()

    init {
        validateSession()
    }

    /**
     * Checks if a valid, unexpired token exists.
     * Enforces a brief presentation delay for branding continuity.
     */
    fun validateSession() {
        viewModelScope.launch {
            _navigationState.value = SplashNavigationState.Loading

            val isValid = withContext(Dispatchers.IO) {
                // Minimum splash screen display time for smooth visual transition
                val startTime = System.currentTimeMillis()
                val sessionValid = sessionManager.isSessionValid()
                val elapsedTime = System.currentTimeMillis() - startTime
                val remainingDelay = (SPLASH_MIN_DURATION_MS - elapsedTime).coerceAtLeast(0)
                delay(remainingDelay)
                sessionValid
            }

            _navigationState.value = if (isValid) {
                SplashNavigationState.NavigateToDashboard
            } else {
                SplashNavigationState.NavigateToLogin
            }
        }
    }

    companion object {
        private const val SPLASH_MIN_DURATION_MS = 1200L
    }
}
