package com.example.system_mini_capstone.admin.config

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages persistent storage and retrieval of administrative system configuration parameters.
 */
class SystemConfigManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadConfig(): FullSystemConfig {
        val gestureConfig = CameraGestureConfig(
            minDetectionConfidence = prefs.getFloat(KEY_GESTURE_MIN_DETECTION, 0.60f),
            minTrackingConfidence = prefs.getFloat(KEY_GESTURE_MIN_TRACKING, 0.60f),
            minPresenceConfidence = prefs.getFloat(KEY_GESTURE_MIN_PRESENCE, 0.60f),
            debounceFrameCount = prefs.getInt(KEY_GESTURE_DEBOUNCE_FRAMES, 6),
            detectTwoHands = prefs.getBoolean(KEY_GESTURE_TWO_HANDS, false)
        )

        val envName = prefs.getString(KEY_API_ENV, ApiEnvironment.PRODUCTION.name) ?: ApiEnvironment.PRODUCTION.name
        val environment = try {
            ApiEnvironment.valueOf(envName)
        } catch (e: Exception) {
            ApiEnvironment.PRODUCTION
        }

        val apiConfig = BackendApiConfig(
            baseUrl = prefs.getString(KEY_API_BASE_URL, environment.defaultUrl) ?: environment.defaultUrl,
            apiKey = prefs.getString(KEY_API_KEY, "sl_live_9f83b271a04ec5629") ?: "sl_live_9f83b271a04ec5629",
            environment = environment,
            timeoutSeconds = prefs.getInt(KEY_API_TIMEOUT, 15)
        )

        val featureToggles = FeatureTogglesConfig(
            autoPronounceTts = prefs.getBoolean(KEY_TOGGLE_TTS, true),
            drawHandLandmarkSkeleton = prefs.getBoolean(KEY_TOGGLE_SKELETON, true),
            enableExperimentalGestures = prefs.getBoolean(KEY_TOGGLE_EXPERIMENTAL, false),
            offlineMlFallback = prefs.getBoolean(KEY_TOGGLE_OFFLINE_ML, true)
        )

        val syncFreqName = prefs.getString(KEY_PRIVACY_SYNC_FREQ, SyncFrequency.WIFI_ONLY.name) ?: SyncFrequency.WIFI_ONLY.name
        val syncFrequency = try {
            SyncFrequency.valueOf(syncFreqName)
        } catch (e: Exception) {
            SyncFrequency.WIFI_ONLY
        }

        val policyName = prefs.getString(KEY_PRIVACY_FRAME_POLICY, FrameStoragePolicy.NEVER.name) ?: FrameStoragePolicy.NEVER.name
        val framePolicy = try {
            FrameStoragePolicy.valueOf(policyName)
        } catch (e: Exception) {
            FrameStoragePolicy.NEVER
        }

        val privacyConfig = CloudPrivacyConfig(
            cloudSyncFrequency = syncFrequency,
            frameRecordingPolicy = framePolicy,
            dataPurgeDays = prefs.getInt(KEY_PRIVACY_PURGE_DAYS, 30),
            enforceE2EEncryption = prefs.getBoolean(KEY_PRIVACY_E2EE, true),
            anonymizeSessionTelemetry = prefs.getBoolean(KEY_PRIVACY_ANONYMIZE, true)
        )

        return FullSystemConfig(
            gestureConfig = gestureConfig,
            apiConfig = apiConfig,
            featureToggles = featureToggles,
            privacyConfig = privacyConfig
        )
    }

    fun saveConfig(config: FullSystemConfig) {
        prefs.edit().apply {
            // 1. Gesture Vision
            putFloat(KEY_GESTURE_MIN_DETECTION, config.gestureConfig.minDetectionConfidence)
            putFloat(KEY_GESTURE_MIN_TRACKING, config.gestureConfig.minTrackingConfidence)
            putFloat(KEY_GESTURE_MIN_PRESENCE, config.gestureConfig.minPresenceConfidence)
            putInt(KEY_GESTURE_DEBOUNCE_FRAMES, config.gestureConfig.debounceFrameCount)
            putBoolean(KEY_GESTURE_TWO_HANDS, config.gestureConfig.detectTwoHands)

            // 2. API Endpoints
            putString(KEY_API_BASE_URL, config.apiConfig.baseUrl)
            putString(KEY_API_KEY, config.apiConfig.apiKey)
            putString(KEY_API_ENV, config.apiConfig.environment.name)
            putInt(KEY_API_TIMEOUT, config.apiConfig.timeoutSeconds)

            // 3. Feature Flags
            putBoolean(KEY_TOGGLE_TTS, config.featureToggles.autoPronounceTts)
            putBoolean(KEY_TOGGLE_SKELETON, config.featureToggles.drawHandLandmarkSkeleton)
            putBoolean(KEY_TOGGLE_EXPERIMENTAL, config.featureToggles.enableExperimentalGestures)
            putBoolean(KEY_TOGGLE_OFFLINE_ML, config.featureToggles.offlineMlFallback)

            // 4. Cloud & Privacy
            putString(KEY_PRIVACY_SYNC_FREQ, config.privacyConfig.cloudSyncFrequency.name)
            putString(KEY_PRIVACY_FRAME_POLICY, config.privacyConfig.frameRecordingPolicy.name)
            putInt(KEY_PRIVACY_PURGE_DAYS, config.privacyConfig.dataPurgeDays)
            putBoolean(KEY_PRIVACY_E2EE, config.privacyConfig.enforceE2EEncryption)
            putBoolean(KEY_PRIVACY_ANONYMIZE, config.privacyConfig.anonymizeSessionTelemetry)

            apply()
        }
    }

    fun resetToDefaults(): FullSystemConfig {
        val defaultConfig = FullSystemConfig()
        saveConfig(defaultConfig)
        return defaultConfig
    }

    companion object {
        private const val PREFS_NAME = "signlink_system_config_prefs"

        private const val KEY_GESTURE_MIN_DETECTION = "key_gesture_min_detection"
        private const val KEY_GESTURE_MIN_TRACKING = "key_gesture_min_tracking"
        private const val KEY_GESTURE_MIN_PRESENCE = "key_gesture_min_presence"
        private const val KEY_GESTURE_DEBOUNCE_FRAMES = "key_gesture_debounce_frames"
        private const val KEY_GESTURE_TWO_HANDS = "key_gesture_two_hands"

        private const val KEY_API_BASE_URL = "key_api_base_url"
        private const val KEY_API_KEY = "key_api_key"
        private const val KEY_API_ENV = "key_api_env"
        private const val KEY_API_TIMEOUT = "key_api_timeout"

        private const val KEY_TOGGLE_TTS = "key_toggle_tts"
        private const val KEY_TOGGLE_SKELETON = "key_toggle_skeleton"
        private const val KEY_TOGGLE_EXPERIMENTAL = "key_toggle_experimental"
        private const val KEY_TOGGLE_OFFLINE_ML = "key_toggle_offline_ml"

        private const val KEY_PRIVACY_SYNC_FREQ = "key_privacy_sync_freq"
        private const val KEY_PRIVACY_FRAME_POLICY = "key_privacy_frame_policy"
        private const val KEY_PRIVACY_PURGE_DAYS = "key_privacy_purge_days"
        private const val KEY_PRIVACY_E2EE = "key_privacy_e2ee"
        private const val KEY_PRIVACY_ANONYMIZE = "key_privacy_anonymize"
    }
}
