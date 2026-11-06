package com.example.system_mini_capstone.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.system_mini_capstone.data.SessionManager
import com.example.system_mini_capstone.data.network.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class DashboardNavTab {
    OVERVIEW,
    TRANSLATOR,
    ADMIN_PANEL,
    SETTINGS
}

data class DashboardFeatureItem(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val isRestrictedToAdmin: Boolean = false,
    val routeAction: String
)

data class DashboardUiState(
    val userEmail: String = "",
    val username: String = "",
    val userRole: UserRole = UserRole.USER,
    val tokenPreview: String = "",
    val activeTab: DashboardNavTab = DashboardNavTab.OVERVIEW,
    val accessibleFeatures: List<DashboardFeatureItem> = emptyList(),
    val showLogoutDialog: Boolean = false,
    val infoNotice: String? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadSessionData()
    }

    fun loadSessionData() {
        val role = UserRole.fromString(sessionManager.getUserRole())
        val email = sessionManager.getUserEmail() ?: "user@signlink.app"
        val username = sessionManager.getUsername() ?: email.substringBefore("@")
        val token = sessionManager.getAuthToken()?.take(22) ?: "None"

        val allFeatures = listOf(
            DashboardFeatureItem(
                id = "realtime_translator",
                title = "Real-Time Camera Translator",
                description = "Continuous 21-hand landmark detection via CameraX & MediaPipe with Text-to-Speech audio.",
                iconEmoji = "🤟",
                isRestrictedToAdmin = false,
                routeAction = "action_launch_translator"
            ),
            DashboardFeatureItem(
                id = "gesture_dictionary",
                title = "Sign Gesture Reference",
                description = "Browse standard ASL alphabets, greetings, and registered phrase landmarks.",
                iconEmoji = "📖",
                isRestrictedToAdmin = false,
                routeAction = "action_dictionary"
            ),
            DashboardFeatureItem(
                id = "tts_settings",
                title = "Voice & Speech Engine",
                description = "Configure speech rate, pitch, language accents, and auto-speak sensitivity.",
                iconEmoji = "🗣️",
                isRestrictedToAdmin = false,
                routeAction = "action_tts_settings"
            ),
            DashboardFeatureItem(
                id = "admin_user_mgmt",
                title = "User & Access Management",
                description = "Manage system accounts, assign RBAC permissions, and revoke active sessions.",
                iconEmoji = "👥",
                isRestrictedToAdmin = true,
                routeAction = "action_admin_users"
            ),
            DashboardFeatureItem(
                id = "admin_model_telemetry",
                title = "ML Model Telemetry & Benchmarks",
                description = "Live frame rate (FPS), MediaPipe inference latency, and detection confidence thresholds.",
                iconEmoji = "⚡",
                isRestrictedToAdmin = true,
                routeAction = "action_admin_telemetry"
            ),
            DashboardFeatureItem(
                id = "admin_gesture_studio",
                title = "Sign Customization Studio",
                description = "Calibrate and train new hand landmark vectors into the gesture recognition dictionary.",
                iconEmoji = "⚙️",
                isRestrictedToAdmin = true,
                routeAction = "action_admin_studio"
            ),
            DashboardFeatureItem(
                id = "admin_maintenance",
                title = "Support & Maintenance Utilities",
                description = "Server health telemetry (CPU/RAM/Uptime), automated system backups, and point-in-time recovery.",
                iconEmoji = "🛠️",
                isRestrictedToAdmin = true,
                routeAction = "action_admin_maintenance"
            )
        )

        // Filter features by Role-Based Access Control (RBAC)
        val filteredFeatures = if (role == UserRole.ADMIN) {
            allFeatures
        } else {
            allFeatures.filter { !it.isRestrictedToAdmin }
        }

        _uiState.update {
            it.copy(
                userEmail = email,
                username = username,
                userRole = role,
                tokenPreview = token,
                accessibleFeatures = filteredFeatures
            )
        }
    }

    fun selectTab(tab: DashboardNavTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun promptLogout() {
        _uiState.update { it.copy(showLogoutDialog = true) }
    }

    fun dismissLogout() {
        _uiState.update { it.copy(showLogoutDialog = false) }
    }

    fun triggerAction(action: String) {
        _uiState.update {
            it.copy(infoNotice = "Triggered module: $action")
        }
    }

    fun dismissNotice() {
        _uiState.update { it.copy(infoNotice = null) }
    }

    fun logout(): Boolean {
        sessionManager.clearSession()
        return true
    }
}
