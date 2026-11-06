package com.example.system_mini_capstone.translator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.system_mini_capstone.ui.theme.System_mini_capstoneTheme

class TranslatorActivity : ComponentActivity() {

    private val viewModel: TranslatorViewModel by viewModels()
    private var landmarkerHelper: HandLandmarkerHelper? = null

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            android.util.Log.w("TranslatorActivity", "Camera permission denied by user; falling back to simulated pipeline")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            System_mini_capstoneTheme {
                val uiState by viewModel.uiState.collectAsState()

                TranslatorScreen(
                    uiState = uiState,
                    onBack = { finish() },
                    onToggleTts = viewModel::toggleTts,
                    onToggleSkeleton = viewModel::toggleSkeleton,
                    onToggleRecognition = viewModel::toggleRecognition,
                    onClearHistory = viewModel::clearHistory,
                    onPreviewViewCreated = { previewView ->
                        if (landmarkerHelper == null) {
                            landmarkerHelper = HandLandmarkerHelper(applicationContext) { landmarks ->
                                viewModel.processLandmarks(landmarks)
                            }
                        }
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            landmarkerHelper?.bindCameraUseCases(this, previewView)
                        }
                    }
                )
            }
        }
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
    onBack: () -> Unit,
    onToggleTts: (Boolean) -> Unit,
    onToggleSkeleton: (Boolean) -> Unit,
    onToggleRecognition: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onPreviewViewCreated: (androidx.camera.view.PreviewView) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SignLink Real-Time Translator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onClearHistory) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear History")
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

                // Status Indicator Overlay
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
                                .background(if (uiState.isRecognizing) Color.Green else Color.Red)
                        )
                        Text(
                            text = if (uiState.isRecognizing) "CAM & MEDIAPIPE LIVE" else "PAUSED",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Landmark Skeleton Overlay Canvas
                if (uiState.isDrawSkeletonEnabled) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val landmarks = uiState.simulatedLandmarks
                        if (landmarks.size >= 21) {
                            val w = size.width
                            val h = size.height

                            val connections = listOf(
                                0 to 1, 1 to 2, 2 to 3, 3 to 4,       // Thumb
                                0 to 5, 5 to 6, 6 to 7, 7 to 8,       // Index
                                5 to 9, 9 to 10, 10 to 11, 11 to 12,  // Middle
                                9 to 13, 13 to 14, 14 to 15, 15 to 16,// Ring
                                13 to 17, 17 to 18, 18 to 19, 19 to 20, // Pinky
                                0 to 17                               // Palm base
                            )

                            for (conn in connections) {
                                val p1 = landmarks[conn.first]
                                val p2 = landmarks[conn.second]
                                drawLine(
                                    color = Color.Green,
                                    start = Offset(p1.x * w, p1.y * h),
                                    end = Offset(p2.x * w, p2.y * h),
                                    strokeWidth = 3f,
                                    cap = StrokeCap.Round
                                )
                            }

                            for (lm in landmarks) {
                                drawCircle(
                                    color = Color.Cyan,
                                    radius = 6f,
                                    center = Offset(lm.x * w, lm.y * h)
                                )
                            }
                        }
                    }
                }

                // Real-Time Sign Text Overlay Badge
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = uiState.currentSign,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Confidence: ${(uiState.confidence * 100).toInt()}%",
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Accessibility & TTS Controls Row
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Accessibility & Speech Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto-Vocalize (TTS) Output")
                        Switch(
                            checked = uiState.isTtsEnabled,
                            onCheckedChange = onToggleTts
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Render Hand Skeleton Overlay")
                        Switch(
                            checked = uiState.isDrawSkeletonEnabled,
                            onCheckedChange = onToggleSkeleton
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Real-Time Tracking Stream")
                        Switch(
                            checked = uiState.isRecognizing,
                            onCheckedChange = onToggleRecognition
                        )
                    }
                }
            }

            // Translation History Transcript
            Text("Live Translation Transcript", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.translationHistory.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No signs recognized yet. Point camera at sign gestures...", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.translationHistory) { record ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
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
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = record.timestamp,
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${(record.confidence * 100).toInt()}%",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
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
