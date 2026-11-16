package com.example.system_mini_capstone.translator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.system_mini_capstone.BuildConfig
import com.example.system_mini_capstone.ui.theme.System_mini_capstoneTheme

class TranslatorActivity : ComponentActivity() {

    private val viewModel: TranslatorViewModel by viewModels()
    private var landmarkerHelper: HandLandmarkerHelper? = null
    private var previewViewRef: androidx.camera.view.PreviewView? = null
    private var errorMessage by mutableStateOf<String?>(null)

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            errorMessage = null
            previewViewRef?.let { bindCamera(it) }
        } else {
            errorMessage = "Camera permission is required for real-time sign detection."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            System_mini_capstoneTheme {
                val uiState by viewModel.uiState.collectAsState()
                var showClearDialog by remember { mutableStateOf(false) }
                var hasCameraPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    )
                }
                val hasMultipleCameras = landmarkerHelper?.hasMultipleCameras() == true

                if (showClearDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearDialog = false },
                        title = { Text("Clear Transcript") },
                        text = { Text("Are you sure you want to clear the translation history?") },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.clearHistory()
                                showClearDialog = false
                            }) {
                                Text("Clear")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                TranslatorScreen(
                    uiState = uiState,
                    hasCameraPermission = hasCameraPermission,
                    hasMultipleCameras = hasMultipleCameras,
                    errorMessage = errorMessage,
                    isMirrored = landmarkerHelper?.isMirrored() == true,
                    onBack = { finish() },
                    onToggleTts = viewModel::toggleTts,
                    onToggleSkeleton = viewModel::toggleSkeleton,
                    onToggleRecognition = viewModel::toggleRecognition,
                    onClearHistory = { showClearDialog = true },
                    onSwitchCamera = {
                        previewViewRef?.let { landmarkerHelper?.switchCamera(this, it) }
                    },
                    onRequestPermission = {
                        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        }
                        startActivity(intent)
                    },
                    onTryAgain = {
                        errorMessage = null
                        previewViewRef?.let { bindCamera(it) }
                    },
                    onPreviewViewCreated = { previewView ->
                        previewViewRef = previewView
                        if (landmarkerHelper == null) {
                            landmarkerHelper = HandLandmarkerHelper(
                                context = applicationContext,
                                onLandmarksDetected = { landmarks ->
                                    viewModel.processLandmarks(landmarks)
                                },
                                onCameraSwitched = {
                                    viewModel.resetState()
                                },
                                onError = { err ->
                                    errorMessage = err
                                }
                            )
                        }
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            hasCameraPermission = true
                            bindCamera(previewView)
                        } else {
                            hasCameraPermission = false
                            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                )
            }
        }
    }

    private fun bindCamera(previewView: androidx.camera.view.PreviewView) {
        landmarkerHelper?.bindCameraUseCases(this, previewView)
    }

    override fun onDestroy() {
        super.onDestroy()
        landmarkerHelper?.clear()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen(
    uiState: TranslatorUiState,
    hasCameraPermission: Boolean,
    hasMultipleCameras: Boolean,
    errorMessage: String?,
    isMirrored: Boolean,
    onBack: () -> Unit,
    onToggleTts: (Boolean) -> Unit,
    onToggleSkeleton: (Boolean) -> Unit,
    onToggleRecognition: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onSwitchCamera: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onTryAgain: () -> Unit,
    onPreviewViewCreated: (androidx.camera.view.PreviewView) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SignLink Real-Time Translator",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onClearHistory) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear History")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CameraX Preview & MediaPipe Vision Pipeline Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.DarkGray)
            ) {
                if (hasCameraPermission && errorMessage == null) {
                    // Live CameraX Preview View
                    AndroidView(
                        factory = { ctx ->
                            androidx.camera.view.PreviewView(ctx).also { previewView ->
                                previewView.scaleType = androidx.camera.view.PreviewView.ScaleType.FILL_CENTER
                                onPreviewViewCreated(previewView)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Permission Denied or Camera Error View
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = errorMessage ?: "Camera permission is required for real-time sign detection.",
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            if (!hasCameraPermission) {
                                Button(onClick = onRequestPermission) {
                                    Text("Grant Permission")
                                }
                                TextButton(onClick = onOpenSettings) {
                                    Text("Open App Settings", color = Color.Cyan)
                                }
                            } else {
                                Button(onClick = onTryAgain) {
                                    Text("Try Again")
                                }
                            }
                        }
                    }
                }

                // Truthful Status Indicator & Sample Status Overlay (Top Start)
                Box(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.statusMessage.contains("Hand detected")) Color.Green else Color.Yellow)
                            )
                            Text(
                                text = uiState.statusMessage.uppercase(),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Sample Load Status Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = uiState.sampleLoadStatus,
                                color = Color.LightGray,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Hidden Debug Line (DEBUG builds only)
                        if (BuildConfig.DEBUG && uiState.debugMatchInfo.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.Black.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = "DEBUG: ${uiState.debugMatchInfo}",
                                    color = Color.Green,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Switch Camera Button (Top Center)
                if (hasMultipleCameras && hasCameraPermission && errorMessage == null) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        IconButton(
                            onClick = onSwitchCamera,
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch camera",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Landmark Skeleton Overlay
                if (uiState.isDrawSkeletonEnabled && hasCameraPermission && errorMessage == null) {
                    HandSkeletonOverlay(
                        landmarks = uiState.simulatedLandmarks,
                        isMirrored = isMirrored,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Real-Time Sign Text Overlay Badge (Positioned at Top-End)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        tonalElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "TRANSLATED SIGN",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = uiState.currentSign,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Controls & Translation History Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live Session History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = uiState.isTtsEnabled,
                                onClick = { onToggleTts(!uiState.isTtsEnabled) },
                                label = { Text("TTS Audio") }
                            )
                            FilterChip(
                                selected = uiState.isDrawSkeletonEnabled,
                                onClick = { onToggleSkeleton(!uiState.isDrawSkeletonEnabled) },
                                label = { Text("Bones") }
                            )
                        }
                    }

                    HorizontalDivider()

                    if (uiState.translationHistory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Show your hand clearly to the camera\nto begin translation...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.translationHistory, key = { it.id }) { record ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 2.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = record.signText,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Confidence: ${(record.confidence * 100).toInt()}%",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = record.timestamp,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
