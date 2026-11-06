package com.example.system_mini_capstone.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.system_mini_capstone.admin.config.ApiEnvironment
import com.example.system_mini_capstone.admin.config.BackendApiConfig
import com.example.system_mini_capstone.admin.config.CameraGestureConfig
import com.example.system_mini_capstone.admin.config.CloudPrivacyConfig
import com.example.system_mini_capstone.admin.config.FeatureTogglesConfig
import com.example.system_mini_capstone.admin.config.FrameStoragePolicy
import com.example.system_mini_capstone.admin.config.SyncFrequency
import com.example.system_mini_capstone.admin.config.SystemConfigUiState
import com.example.system_mini_capstone.admin.config.SystemConfigViewModel
import com.example.system_mini_capstone.ui.theme.System_mini_capstoneTheme

/**
 * Administrative configuration panel for tuning camera vision thresholds,
 * configuring backend API endpoints, dynamic feature flags, and cloud privacy policies.
 */
class SystemConfigActivity : ComponentActivity() {

    private val viewModel: SystemConfigViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            System_mini_capstoneTheme {
                val uiState by viewModel.uiState.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(uiState.statusMessage) {
                    uiState.statusMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.dismissStatus()
                    }
                }

                SystemConfigScreen(
                    uiState = uiState,
                    snackbarHostState = snackbarHostState,
                    onSelectSection = viewModel::selectSection,
                    onUpdateDetectionConfidence = viewModel::updateDetectionConfidence,
                    onUpdateTrackingConfidence = viewModel::updateTrackingConfidence,
                    onUpdatePresenceConfidence = viewModel::updatePresenceConfidence,
                    onUpdateDebounceFrames = viewModel::updateDebounceFrames,
                    onUpdateDetectTwoHands = viewModel::updateDetectTwoHands,
                    onUpdateApiBaseUrl = viewModel::updateApiBaseUrl,
                    onUpdateApiKey = viewModel::updateApiKey,
                    onUpdateApiEnvironment = viewModel::updateApiEnvironment,
                    onUpdateApiTimeout = viewModel::updateApiTimeout,
                    onUpdateAutoPronounceTts = viewModel::updateAutoPronounceTts,
                    onUpdateDrawSkeleton = viewModel::updateDrawSkeleton,
                    onUpdateExperimentalGestures = viewModel::updateExperimentalGestures,
                    onUpdateOfflineMl = viewModel::updateOfflineMl,
                    onUpdateSyncFrequency = viewModel::updateSyncFrequency,
                    onUpdateFrameStoragePolicy = viewModel::updateFrameStoragePolicy,
                    onUpdatePurgeDays = viewModel::updatePurgeDays,
                    onUpdateE2EEncryption = viewModel::updateE2EEncryption,
                    onUpdateAnonymizeSession = viewModel::updateAnonymizeSession,
                    onSaveChanges = viewModel::saveChanges,
                    onRestoreDefaults = viewModel::restoreFactoryDefaults,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemConfigScreen(
    uiState: SystemConfigUiState,
    snackbarHostState: SnackbarHostState,
    onSelectSection: (Int) -> Unit,
    onUpdateDetectionConfidence: (Float) -> Unit,
    onUpdateTrackingConfidence: (Float) -> Unit,
    onUpdatePresenceConfidence: (Float) -> Unit,
    onUpdateDebounceFrames: (Int) -> Unit,
    onUpdateDetectTwoHands: (Boolean) -> Unit,
    onUpdateApiBaseUrl: (String) -> Unit,
    onUpdateApiKey: (String) -> Unit,
    onUpdateApiEnvironment: (ApiEnvironment) -> Unit,
    onUpdateApiTimeout: (Int) -> Unit,
    onUpdateAutoPronounceTts: (Boolean) -> Unit,
    onUpdateDrawSkeleton: (Boolean) -> Unit,
    onUpdateExperimentalGestures: (Boolean) -> Unit,
    onUpdateOfflineMl: (Boolean) -> Unit,
    onUpdateSyncFrequency: (SyncFrequency) -> Unit,
    onUpdateFrameStoragePolicy: (FrameStoragePolicy) -> Unit,
    onUpdatePurgeDays: (Int) -> Unit,
    onUpdateE2EEncryption: (Boolean) -> Unit,
    onUpdateAnonymizeSession: (Boolean) -> Unit,
    onSaveChanges: () -> Unit,
    onRestoreDefaults: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Restore Factory Defaults") },
            text = { Text("Are you sure you want to reset all vision thresholds, API endpoints, and feature flags to factory default settings?") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        onRestoreDefaults()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset All")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("System Configuration", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    TextButton(onClick = { showResetDialog = true }) {
                        Text("Reset", color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = onSaveChanges,
                        enabled = uiState.isDirty,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(if (uiState.isDirty) "Save*" else "Saved")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val sections = listOf(
                "📷 Vision Sensitivity",
                "🌐 Backend & API",
                "🎛️ Feature Flags",
                "☁️ Cloud & Privacy"
            )

            ScrollableTabRow(
                selectedTabIndex = uiState.selectedSection,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                sections.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.selectedSection == index,
                        onClick = { onSelectSection(index) },
                        text = { Text(title) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                when (uiState.selectedSection) {
                    0 -> {
                        item {
                            VisionSensitivitySection(
                                config = uiState.config.gestureConfig,
                                onUpdateDetection = onUpdateDetectionConfidence,
                                onUpdateTracking = onUpdateTrackingConfidence,
                                onUpdatePresence = onUpdatePresenceConfidence,
                                onUpdateDebounce = onUpdateDebounceFrames,
                                onUpdateTwoHands = onUpdateDetectTwoHands
                            )
                        }
                    }
                    1 -> {
                        item {
                            BackendApiSection(
                                config = uiState.config.apiConfig,
                                onUpdateUrl = onUpdateApiBaseUrl,
                                onUpdateKey = onUpdateApiKey,
                                onUpdateEnv = onUpdateApiEnvironment,
                                onUpdateTimeout = onUpdateApiTimeout
                            )
                        }
                    }
                    2 -> {
                        item {
                            FeatureTogglesSection(
                                config = uiState.config.featureToggles,
                                onUpdateTts = onUpdateAutoPronounceTts,
                                onUpdateSkeleton = onUpdateDrawSkeleton,
                                onUpdateExperimental = onUpdateExperimentalGestures,
                                onUpdateOffline = onUpdateOfflineMl
                            )
                        }
                    }
                    3 -> {
                        item {
                            CloudPrivacySection(
                                config = uiState.config.privacyConfig,
                                onUpdateSync = onUpdateSyncFrequency,
                                onUpdatePolicy = onUpdateFrameStoragePolicy,
                                onUpdatePurgeDays = onUpdatePurgeDays,
                                onUpdateE2ee = onUpdateE2EEncryption,
                                onUpdateAnonymize = onUpdateAnonymizeSession
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onSaveChanges,
                        enabled = uiState.isDirty,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Apply & Commit Configuration Changes")
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// SECTION 1: VISION SENSITIVITY
// -----------------------------------------------------------------------------------------
@Composable
fun VisionSensitivitySection(
    config: CameraGestureConfig,
    onUpdateDetection: (Float) -> Unit,
    onUpdateTracking: (Float) -> Unit,
    onUpdatePresence: (Float) -> Unit,
    onUpdateDebounce: (Int) -> Unit,
    onUpdateTwoHands: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "MediaPipe Hand Landmarker Thresholds",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Min Detection Confidence
            SliderSettingItem(
                title = "Min Hand Detection Confidence",
                subtitle = "Initial anchor box detection score required to locate hands in frame.",
                value = config.minDetectionConfidence,
                valueRange = 0.10f..0.95f,
                displayValue = "${(config.minDetectionConfidence * 100).toInt()}%",
                onValueChange = onUpdateDetection
            )

            // Min Tracking Confidence
            SliderSettingItem(
                title = "Min Landmark Tracking Confidence",
                subtitle = "Confidence required to track 21 coordinates continuously between frames.",
                value = config.minTrackingConfidence,
                valueRange = 0.10f..0.95f,
                displayValue = "${(config.minTrackingConfidence * 100).toInt()}%",
                onValueChange = onUpdateTracking
            )

            // Min Presence Confidence
            SliderSettingItem(
                title = "Min Hand Presence Confidence",
                subtitle = "Ensures partial shadows or background edges are not classified as hands.",
                value = config.minPresenceConfidence,
                valueRange = 0.10f..0.95f,
                displayValue = "${(config.minPresenceConfidence * 100).toInt()}%",
                onValueChange = onUpdatePresence
            )

            // Debounce Consecutive Frames
            SliderSettingItem(
                title = "Gesture Anti-Flicker Debounce",
                subtitle = "Requires consecutive matching frames before speech / output triggers.",
                value = config.debounceFrameCount.toFloat(),
                valueRange = 2f..15f,
                displayValue = "${config.debounceFrameCount} frames",
                onValueChange = { onUpdateDebounce(it.toInt()) }
            )

            // Two hands toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Simultaneous Dual-Hand Tracking",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Track up to 2 hands (42 landmarks) for two-handed ASL signs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = config.detectTwoHands, onCheckedChange = onUpdateTwoHands)
            }
        }
    }
}

@Composable
fun SliderSettingItem(
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = displayValue,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

// -----------------------------------------------------------------------------------------
// SECTION 2: BACKEND & API CONFIGURATION
// -----------------------------------------------------------------------------------------
@Composable
fun BackendApiSection(
    config: BackendApiConfig,
    onUpdateUrl: (String) -> Unit,
    onUpdateKey: (String) -> Unit,
    onUpdateEnv: (ApiEnvironment) -> Unit,
    onUpdateTimeout: (Int) -> Unit
) {
    var isKeyVisible by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Target API Environment",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ApiEnvironment.entries.forEach { env ->
                    FilterChip(
                        selected = config.environment == env,
                        onClick = { onUpdateEnv(env) },
                        label = { Text(env.label, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedTextField(
                value = config.baseUrl,
                onValueChange = onUpdateUrl,
                label = { Text("Base API Endpoint URL") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = config.apiKey,
                onValueChange = onUpdateKey,
                label = { Text("Client API Key") },
                singleLine = true,
                visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                        Text(if (isKeyVisible) "👁" else "🔒", fontSize = 18.sp)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            SliderSettingItem(
                title = "HTTP Request Timeout",
                subtitle = "Maximum allowable round-trip duration for cloud sync requests.",
                value = config.timeoutSeconds.toFloat(),
                valueRange = 5f..60f,
                displayValue = "${config.timeoutSeconds} sec",
                onValueChange = { onUpdateTimeout(it.toInt()) }
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// SECTION 3: FEATURE TOGGLES
// -----------------------------------------------------------------------------------------
@Composable
fun FeatureTogglesSection(
    config: FeatureTogglesConfig,
    onUpdateTts: (Boolean) -> Unit,
    onUpdateSkeleton: (Boolean) -> Unit,
    onUpdateExperimental: (Boolean) -> Unit,
    onUpdateOffline: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Dynamic Feature Toggles",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            ToggleRowItem(
                title = "Continuous Text-to-Speech (TTS)",
                subtitle = "Automatically speaks recognized signs out loud when gesture stabilizes.",
                checked = config.autoPronounceTts,
                onCheckedChange = onUpdateTts
            )

            ToggleRowItem(
                title = "Live Landmark Skeleton Overlay",
                subtitle = "Renders real-time 21-node green joints and connections over camera preview.",
                checked = config.drawHandLandmarkSkeleton,
                onCheckedChange = onUpdateSkeleton
            )

            ToggleRowItem(
                title = "Experimental Dynamic Gestures",
                subtitle = "Enables multi-frame motion trajectories (e.g. ASL Letter J, Z).",
                checked = config.enableExperimentalGestures,
                onCheckedChange = onUpdateExperimental
            )

            ToggleRowItem(
                title = "Offline ML Inference Fallback",
                subtitle = "Allows continuous local CPU/GPU detection without internet connectivity.",
                checked = config.offlineMlFallback,
                onCheckedChange = onUpdateOffline
            )
        }
    }
}

@Composable
fun ToggleRowItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// -----------------------------------------------------------------------------------------
// SECTION 4: CLOUD & PRIVACY PARAMETERS
// -----------------------------------------------------------------------------------------
@Composable
fun CloudPrivacySection(
    config: CloudPrivacyConfig,
    onUpdateSync: (SyncFrequency) -> Unit,
    onUpdatePolicy: (FrameStoragePolicy) -> Unit,
    onUpdatePurgeDays: (Int) -> Unit,
    onUpdateE2ee: (Boolean) -> Unit,
    onUpdateAnonymize: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Cloud Telemetry & Privacy Policies",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Telemetry Sync Frequency",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SyncFrequency.entries.forEach { freq ->
                    FilterChip(
                        selected = config.cloudSyncFrequency == freq,
                        onClick = { onUpdateSync(freq) },
                        label = { Text(freq.label) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Camera Frame Storage Policy",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FrameStoragePolicy.entries.forEach { policy ->
                    FilterChip(
                        selected = config.frameRecordingPolicy == policy,
                        onClick = { onUpdatePolicy(policy) },
                        label = { Text(policy.label) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            SliderSettingItem(
                title = "Local Cache Log Purge",
                subtitle = "Automatically clean stored inference history older than N days.",
                value = config.dataPurgeDays.toFloat(),
                valueRange = 7f..90f,
                displayValue = "${config.dataPurgeDays} days",
                onValueChange = { onUpdatePurgeDays(it.toInt()) }
            )

            ToggleRowItem(
                title = "Enforce End-to-End Encryption (E2EE)",
                subtitle = "Encrypt all telemetry payloads before transmitting to cloud endpoints.",
                checked = config.enforceE2EEncryption,
                onCheckedChange = onUpdateE2ee
            )

            ToggleRowItem(
                title = "Strict Anonymization",
                subtitle = "Completely strip device IDs and IP addresses from analytics payloads.",
                checked = config.anonymizeSessionTelemetry,
                onCheckedChange = onUpdateAnonymize
            )
        }
    }
}
