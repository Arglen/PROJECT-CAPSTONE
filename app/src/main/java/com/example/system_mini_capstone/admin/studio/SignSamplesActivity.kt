package com.example.system_mini_capstone.admin.studio

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.system_mini_capstone.DashboardActivity
import com.example.system_mini_capstone.data.SessionManager
import com.example.system_mini_capstone.translator.HandLandmarkerHelper
import com.example.system_mini_capstone.translator.HandSkeletonOverlay
import com.example.system_mini_capstone.ui.theme.SignLinkColors
import com.example.system_mini_capstone.ui.theme.System_mini_capstoneTheme

class SignSamplesActivity : ComponentActivity() {

    private val viewModel: SignSamplesViewModel by viewModels()
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
            errorMessage = "Camera permission is required for sign recording."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Admin Security Guard
        if (SessionManager(this).getUserRole() != "ADMIN") {
            Log.w("AdminGuard", "Unauthorized access attempt to SignSamplesActivity. Redirecting.")
            val intent = Intent(this, DashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }

        setContent {
            System_mini_capstoneTheme {
                val uiState by viewModel.uiState.collectAsState()
                var deleteTargetLabel by remember { mutableStateOf<String?>(null) }
                var hasCameraPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    )
                }
                val hasMultipleCameras = landmarkerHelper?.hasMultipleCameras() == true

                if (deleteTargetLabel != null) {
                    val lbl = deleteTargetLabel!!
                    AlertDialog(
                        onDismissRequest = { deleteTargetLabel = null },
                        title = { Text("Delete All Samples") },
                        text = { Text("Are you sure you want to delete all recorded samples for sign '$lbl'?") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.deleteSamplesForLabel(lbl)
                                    deleteTargetLabel = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { deleteTargetLabel = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                SignSamplesScreen(
                    uiState = uiState,
                    hasCameraPermission = hasCameraPermission,
                    hasMultipleCameras = hasMultipleCameras,
                    errorMessage = errorMessage,
                    isMirrored = landmarkerHelper?.isMirrored() == true,
                    onBack = { finish() },
                    onUpdateLabel = viewModel::updateLabel,
                    onStartRecording = viewModel::startRecording,
                    onCancelRecording = viewModel::cancelRecording,
                    onRetrySave = viewModel::retrySave,
                    onDeleteLabel = { label -> deleteTargetLabel = label },
                    onSwitchCamera = {
                        previewViewRef?.let { landmarkerHelper?.switchCamera(this, it) }
                        viewModel.setLensFacing(landmarkerHelper?.isMirrored() == true)
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
                                onHandsDetected = { hands ->
                                    viewModel.onHandsReceived(hands)
                                },
                                onError = { err ->
                                    errorMessage = err
                                }
                            )
                        }
                        viewModel.setLensFacing(landmarkerHelper?.isMirrored() == true)
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
fun SignSamplesScreen(
    uiState: SignSamplesUiState,
    hasCameraPermission: Boolean,
    hasMultipleCameras: Boolean,
    errorMessage: String?,
    isMirrored: Boolean,
    onBack: () -> Unit,
    onUpdateLabel: (String) -> Unit,
    onStartRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onRetrySave: () -> Unit,
    onDeleteLabel: (String) -> Unit,
    onSwitchCamera: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onTryAgain: () -> Unit,
    onPreviewViewCreated: (androidx.camera.view.PreviewView) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign Samples Studio", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SignLinkColors.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Camera Preview Box
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.DarkGray)
                ) {
                    if (hasCameraPermission && errorMessage == null) {
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
                        Box(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = errorMessage ?: "Camera permission is required.",
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                if (!hasCameraPermission) {
                                    Button(onClick = onRequestPermission) { Text("Grant Permission") }
                                } else {
                                    Button(onClick = onTryAgain) { Text("Try Again") }
                                }
                            }
                        }
                    }

                    // Landmark Skeleton Overlay
                    if (hasCameraPermission && errorMessage == null) {
                        HandSkeletonOverlay(
                            hands = uiState.simulatedHands,
                            isMirrored = isMirrored,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Status Overlay
                    Box(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
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
                                    .background(if (uiState.statusMessage.contains("Recording") || uiState.statusMessage.contains("Saved")) Color.Green else Color.Yellow)
                            )
                            Text(
                                text = uiState.statusMessage.uppercase(),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Switch Camera Button
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
                }
            }

            // Recorder Control Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Record Hand Shape Samples",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = uiState.labelInput,
                            onValueChange = onUpdateLabel,
                            label = { Text("Sign Label (e.g. HELLO, A, 1)") },
                            singleLine = true,
                            isError = uiState.labelError != null,
                            supportingText = {
                                uiState.labelError?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isRecording && !uiState.isCountingDown
                        )

                        if (uiState.isCountingDown) {
                            Text(
                                text = "Starting in ${uiState.countdownSeconds}...",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        } else if (uiState.isRecording) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Capturing samples...", style = MaterialTheme.typography.bodyMedium)
                                    Text("${uiState.recordedCount} / ${uiState.targetCount}", fontWeight = FontWeight.Bold)
                                }
                                LinearProgressIndicator(
                                    progress = { uiState.recordedCount.toFloat() / uiState.targetCount.toFloat() },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                )
                            }

                            Button(
                                onClick = onCancelRecording,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Cancel Recording")
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = onStartRecording,
                                    enabled = !uiState.isSaving,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Text("Record 40 Samples")
                                }

                                if (uiState.saveError) {
                                    Button(
                                        onClick = onRetrySave,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text("Tap to Retry")
                                    }
                                }
                            }
                        }

                        if (uiState.isSaving) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Saving samples to cloud...")
                            }
                        }
                    }
                }
            }

            // Sample Summary List
            item {
                Text(
                    text = "Recorded Sign Samples Database",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.isLoadingSummaries) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.labelSummaries.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No sign samples recorded yet.\nEnter a label above and tap Record.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(uiState.labelSummaries, key = { it.label }) { summary ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = summary.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${summary.count} samples recorded",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDeleteLabel(summary.label) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete samples for ${summary.label}",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
