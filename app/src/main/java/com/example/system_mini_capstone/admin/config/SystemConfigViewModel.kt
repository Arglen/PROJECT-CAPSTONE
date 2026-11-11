package com.example.system_mini_capstone.admin.config

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SystemConfigUiState(
    val config: FullSystemConfig = FullSystemConfig(),
    val selectedSection: Int = 0,
    val isDirty: Boolean = false,
    val isSaving: Boolean = false,
    val statusMessage: String? = null
) {
    val isUrlValid: Boolean
        get() {
            val url = config.apiConfig.baseUrl
            return (url.startsWith("http://") || url.startsWith("https://")) && url.endsWith("/") && !url.contains(" ")
        }

    val urlError: String?
        get() {
            if (isUrlValid) return null
            val url = config.apiConfig.baseUrl
            return when {
                url.isBlank() -> "URL cannot be blank"
                !url.startsWith("http://") && !url.startsWith("https://") -> "Must start with http:// or https://"
                !url.endsWith("/") -> "Must end with '/'"
                url.contains(" ") -> "Cannot contain spaces"
                else -> "Invalid URL format"
            }
        }
}

class SystemConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val configManager = SystemConfigManager(application)

    private val _uiState = MutableStateFlow(SystemConfigUiState())
    val uiState: StateFlow<SystemConfigUiState> = _uiState.asStateFlow()

    init {
        loadConfiguration()
    }

    fun loadConfiguration() {
        val loaded = configManager.loadConfig()
        _uiState.update {
            it.copy(
                config = loaded,
                isDirty = false
            )
        }
    }

    fun selectSection(sectionIndex: Int) {
        _uiState.update { it.copy(selectedSection = sectionIndex) }
    }

    // 1. Gesture Vision Modifiers
    fun updateDetectionConfidence(value: Float) {
        _uiState.update {
            it.copy(
                config = it.config.copy(gestureConfig = it.config.gestureConfig.copy(minDetectionConfidence = value)),
                isDirty = true
            )
        }
    }

    fun updateTrackingConfidence(value: Float) {
        _uiState.update {
            it.copy(
                config = it.config.copy(gestureConfig = it.config.gestureConfig.copy(minTrackingConfidence = value)),
                isDirty = true
            )
        }
    }

    fun updatePresenceConfidence(value: Float) {
        _uiState.update {
            it.copy(
                config = it.config.copy(gestureConfig = it.config.gestureConfig.copy(minPresenceConfidence = value)),
                isDirty = true
            )
        }
    }

    fun updateDebounceFrames(frames: Int) {
        _uiState.update {
            it.copy(
                config = it.config.copy(gestureConfig = it.config.gestureConfig.copy(debounceFrameCount = frames)),
                isDirty = true
            )
        }
    }

    fun updateDetectTwoHands(detectTwo: Boolean) {
        _uiState.update {
            it.copy(
                config = it.config.copy(gestureConfig = it.config.gestureConfig.copy(detectTwoHands = detectTwo)),
                isDirty = true
            )
        }
    }

    // 2. API Endpoints Modifiers
    fun updateApiBaseUrl(url: String) {
        _uiState.update {
            it.copy(
                config = it.config.copy(apiConfig = it.config.apiConfig.copy(baseUrl = url)),
                isDirty = true
            )
        }
    }

    fun updateApiKey(key: String) {
        _uiState.update {
            it.copy(
                config = it.config.copy(apiConfig = it.config.apiConfig.copy(apiKey = key)),
                isDirty = true
            )
        }
    }

    fun updateApiEnvironment(env: ApiEnvironment) {
        _uiState.update {
            it.copy(
                config = it.config.copy(
                    apiConfig = it.config.apiConfig.copy(
                        environment = env,
                        baseUrl = env.defaultUrl
                    )
                ),
                isDirty = true
            )
        }
    }

    fun updateApiTimeout(timeoutSeconds: Int) {
        _uiState.update {
            it.copy(
                config = it.config.copy(apiConfig = it.config.apiConfig.copy(timeoutSeconds = timeoutSeconds)),
                isDirty = true
            )
        }
    }

    // 3. Feature Flags Modifiers
    fun updateAutoPronounceTts(enabled: Boolean) {
        _uiState.update {
            it.copy(
                config = it.config.copy(featureToggles = it.config.featureToggles.copy(autoPronounceTts = enabled)),
                isDirty = true
            )
        }
    }

    fun updateDrawSkeleton(enabled: Boolean) {
        _uiState.update {
            it.copy(
                config = it.config.copy(featureToggles = it.config.featureToggles.copy(drawHandLandmarkSkeleton = enabled)),
                isDirty = true
            )
        }
    }

    fun updateExperimentalGestures(enabled: Boolean) {
        _uiState.update {
            it.copy(
                config = it.config.copy(featureToggles = it.config.featureToggles.copy(enableExperimentalGestures = enabled)),
                isDirty = true
            )
        }
    }

    fun updateOfflineMl(enabled: Boolean) {
        _uiState.update {
            it.copy(
                config = it.config.copy(featureToggles = it.config.featureToggles.copy(offlineMlFallback = enabled)),
                isDirty = true
            )
        }
    }

    // 4. Cloud & Privacy Modifiers
    fun updateSyncFrequency(freq: SyncFrequency) {
        _uiState.update {
            it.copy(
                config = it.config.copy(privacyConfig = it.config.privacyConfig.copy(cloudSyncFrequency = freq)),
                isDirty = true
            )
        }
    }

    fun updateFrameStoragePolicy(policy: FrameStoragePolicy) {
        _uiState.update {
            it.copy(
                config = it.config.copy(privacyConfig = it.config.privacyConfig.copy(frameRecordingPolicy = policy)),
                isDirty = true
            )
        }
    }

    fun updatePurgeDays(days: Int) {
        _uiState.update {
            it.copy(
                config = it.config.copy(privacyConfig = it.config.privacyConfig.copy(dataPurgeDays = days)),
                isDirty = true
            )
        }
    }

    fun updateE2EEncryption(enabled: Boolean) {
        _uiState.update {
            it.copy(
                config = it.config.copy(privacyConfig = it.config.privacyConfig.copy(enforceE2EEncryption = enabled)),
                isDirty = true
            )
        }
    }

    fun updateAnonymizeSession(enabled: Boolean) {
        _uiState.update {
            it.copy(
                config = it.config.copy(privacyConfig = it.config.privacyConfig.copy(anonymizeSessionTelemetry = enabled)),
                isDirty = true
            )
        }
    }

    // Actions
    fun saveChanges() {
        val state = _uiState.value
        if (state.isSaving || !state.isDirty || !state.isUrlValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val current = _uiState.value.config
            configManager.saveConfig(current)
            _uiState.update {
                it.copy(
                    isDirty = false,
                    isSaving = false,
                    statusMessage = "Configuration applied & committed successfully!"
                )
            }
        }
    }

    fun restoreFactoryDefaults() {
        val reset = configManager.resetToDefaults()
        _uiState.update {
            it.copy(
                config = reset,
                isDirty = false,
                statusMessage = "Restored factory defaults."
            )
        }
    }

    fun dismissStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
