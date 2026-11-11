package com.example.system_mini_capstone.admin.config

/**
 * Camera gesture detection sensitivity thresholds for MediaPipe Hand Landmarker.
 */
data class CameraGestureConfig(
    val minDetectionConfidence: Float = 0.60f,
    val minTrackingConfidence: Float = 0.60f,
    val minPresenceConfidence: Float = 0.60f,
    val debounceFrameCount: Int = 6,
    val detectTwoHands: Boolean = false
)

enum class ApiEnvironment(val label: String, val defaultUrl: String) {
    PRODUCTION("Production", "https://api.signlink.app/"),
    STAGING("Staging", "https://staging-api.signlink.app/"),
    LOCAL_DEV("Local", "http://10.0.2.2:8080/")
}

/**
 * Backend API endpoints, environment targets, and authorization keys.
 */
data class BackendApiConfig(
    val baseUrl: String = "https://api.signlink.app/",
    val apiKey: String = "sl_live_9f83b271a04ec5629",
    val environment: ApiEnvironment = ApiEnvironment.PRODUCTION,
    val timeoutSeconds: Int = 15
)

/**
 * Dynamic feature toggles enabled across mobile application instances.
 */
data class FeatureTogglesConfig(
    val autoPronounceTts: Boolean = true,
    val drawHandLandmarkSkeleton: Boolean = true,
    val enableExperimentalGestures: Boolean = false,
    val offlineMlFallback: Boolean = true
)

enum class SyncFrequency(val label: String) {
    REALTIME("Real-time Continuous"),
    HOURLY("Hourly Batch"),
    WIFI_ONLY("Wi-Fi Only (Recommended)"),
    DISABLED("Disabled (Air-gapped)")
}

enum class FrameStoragePolicy(val label: String) {
    NEVER("Never Store Frames (Ephemeral RAM only)"),
    LANDMARKS_ONLY("Store 21-Vector Coordinates Only"),
    WITH_CONSENT("Store Anonymized Samples (User Opt-in)")
}

/**
 * Cloud storage compliance, retention policies, and privacy parameters.
 */
data class CloudPrivacyConfig(
    val cloudSyncFrequency: SyncFrequency = SyncFrequency.WIFI_ONLY,
    val frameRecordingPolicy: FrameStoragePolicy = FrameStoragePolicy.NEVER,
    val dataPurgeDays: Int = 30,
    val enforceE2EEncryption: Boolean = true,
    val anonymizeSessionTelemetry: Boolean = true
)

/**
 * Composite container for all administrative configuration domains.
 */
data class FullSystemConfig(
    val gestureConfig: CameraGestureConfig = CameraGestureConfig(),
    val apiConfig: BackendApiConfig = BackendApiConfig(),
    val featureToggles: FeatureTogglesConfig = FeatureTogglesConfig(),
    val privacyConfig: CloudPrivacyConfig = CloudPrivacyConfig()
)
