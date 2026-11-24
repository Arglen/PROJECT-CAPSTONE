package com.example.system_mini_capstone.translator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import com.example.system_mini_capstone.SignLinkBaseActivity
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.system_mini_capstone.DashboardActivity
import com.example.system_mini_capstone.admin.lcms.LessonListActivity
import com.example.system_mini_capstone.ui.components.SignLinkBackButton
import com.example.system_mini_capstone.ui.components.SignLinkBottomBar
import com.example.system_mini_capstone.ui.components.SignLinkCard
import com.example.system_mini_capstone.ui.components.SignLinkPrimaryButton
import com.example.system_mini_capstone.ui.components.SignLinkSecondaryButton
import com.example.system_mini_capstone.ui.theme.SignLinkColors
import com.example.system_mini_capstone.ui.theme.System_mini_capstoneTheme

class TranslatorActivity : SignLinkBaseActivity() {

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
            errorMessage = "Allow camera access to start."
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
                    onTabSelected = { index ->
                        when (index) {
                            0 -> {
                                startActivity(Intent(this@TranslatorActivity, DashboardActivity::class.java))
                                finish()
                            }
                            1 -> {} // Camera
                            2 -> {
                                startActivity(Intent(this@TranslatorActivity, LessonListActivity::class.java))
                                finish()
                            }
                            3 -> Toast.makeText(this@TranslatorActivity, "Saved signs coming in Stage F6", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onToggleTts = viewModel::toggleTts,
                    onToggleSkeleton = viewModel::toggleSkeleton,
                    onToggleRecognition = {
                        viewModel.toggleRecognition(!uiState.isRecognizing)
                    },
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

@Composable
fun TranslatorScreen(
    uiState: TranslatorUiState,
    hasCameraPermission: Boolean,
    hasMultipleCameras: Boolean,
    errorMessage: String?,
    isMirrored: Boolean,
    onBack: () -> Unit,
    onTabSelected: (Int) -> Unit,
    onToggleTts: (Boolean) -> Unit,
    onToggleSkeleton: (Boolean) -> Unit,
    onToggleRecognition: () -> Unit,
    onClearHistory: () -> Unit,
    onSwitchCamera: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onTryAgain: () -> Unit,
    onPreviewViewCreated: (androidx.camera.view.PreviewView) -> Unit
) {
    val isRunning = uiState.isRecognizing && hasCameraPermission && errorMessage == null

    Scaffold(
        bottomBar = {
            SignLinkBottomBar(
                selectedIndex = 1, // Camera
                onTabSelected = onTabSelected
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SignLinkColors.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header: Back button, Title & Subtitle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SignLinkBackButton(onClick = onBack)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Your camera companion",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = SignLinkColors.onBackground
                        )
                        Text(
                            text = "Point your camera at your hand and sign slowly.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SignLinkColors.secondaryText
                        )
                    }
                }
            }

            // Camera Card (Navy #172033, 28dp radius, ~312dp tall)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(312.dp)
                        .shadow(elevation = 4.dp, shape = RoundedCornerShape(28.dp), clip = false)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF172033))
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
                        // Camera Off / Permission Missing View
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(56.dp),
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Your camera is off",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = errorMessage ?: "Allow camera access to start.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (!hasCameraPermission) {
                                    Button(
                                        onClick = onRequestPermission,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SignLinkColors.primary)
                                    ) {
                                        Text("Grant Permission")
                                    }
                                } else {
                                    Button(
                                        onClick = onTryAgain,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SignLinkColors.primary)
                                    ) {
                                        Text("Try Again")
                                    }
                                }
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

                    // LIVE / CAMERA OFF pill (Top Left)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = if (isRunning) "LIVE" else "CAMERA OFF",
                                color = if (isRunning) Color.Green else Color.Yellow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Flip Camera Button (40dp dark translucent circle, Top Right)
                    if (hasMultipleCameras && hasCameraPermission && errorMessage == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            IconButton(
                                onClick = onSwitchCamera,
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cameraswitch,
                                    contentDescription = "Switch camera",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Tip Card (White, 24dp radius)
            item {
                SignLinkCard {
                    Text(
                        text = "Good light. Hands in view.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SignLinkColors.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sign slowly, with your upper body visible.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SignLinkColors.secondaryText
                    )
                }
            }

            // Recognition Result Card (White, 24dp radius)
            item {
                SignLinkCard {
                    // Status Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SignLinkColors.successTint
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SignLinkColors.successGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (uiState.currentSign != "No hand detected" && uiState.currentSign != "Detecting...") "Recognized - ASL" else "Listening...",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SignLinkColors.successGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sign name very large (48sp bold navy, centered)
                    Text(
                        text = uiState.currentSign,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp,
                        color = SignLinkColors.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Confidence: ${(uiState.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SignLinkColors.secondaryText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = SignLinkColors.outline)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Recognition can vary with lighting, framing and movement.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SignLinkColors.secondaryText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Buttons & Controls under card
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SignLinkPrimaryButton(
                        text = if (uiState.isRecognizing) "Stop recognition" else "Start recognition",
                        onClick = onToggleRecognition
                    )
                    SignLinkSecondaryButton(
                        text = "Try again",
                        onClick = onClearHistory
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

                        TextButton(onClick = onClearHistory) {
                            Text("Clear History", color = SignLinkColors.error)
                        }
                    }
                }
            }

            // Footer line
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Private by design - no video is recorded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SignLinkColors.secondaryText,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
