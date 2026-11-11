package com.example.system_mini_capstone

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.system_mini_capstone.ui.SplashNavigationState
import com.example.system_mini_capstone.ui.SplashScreen
import com.example.system_mini_capstone.ui.SplashViewModel
import com.example.system_mini_capstone.ui.theme.System_mini_capstoneTheme
import kotlinx.coroutines.launch

/**
 * Entry point activity of the application.
 * Utilizes androidx.core:core-splashscreen to eliminate white flashes and double splashes.
 */
class SplashActivity : ComponentActivity() {

    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install Core SplashScreen before super.onCreate()
        installSplashScreen().setKeepOnScreenCondition {
            viewModel.navigationState.value is SplashNavigationState.Loading
        }

        super.onCreate(savedInstanceState)

        setContent {
            System_mini_capstoneTheme {
                SplashScreen(modifier = Modifier.fillMaxSize())
            }
        }

        observeNavigation()
    }

    private fun observeNavigation() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationState.collect { state ->
                    when (state) {
                        SplashNavigationState.Loading -> {
                            // Keep displaying splash animation
                        }
                        SplashNavigationState.NavigateToDashboard -> {
                            navigateToDashboard()
                        }
                        SplashNavigationState.NavigateToLogin -> {
                            navigateToLogin()
                        }
                    }
                }
            }
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
