package com.example.system_mini_capstone.translator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.system_mini_capstone.admin.config.SystemConfigManager
import com.example.system_mini_capstone.ml.HandLandmark
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * CameraX and MediaPipe Vision Pipeline Helper.
 * Configures CameraX ImageAnalysis to stream frame buffers continuously without lagging the UI thread
 * and pipes live frames into Google MediaPipe Vision HandLandmarker using saved System Config thresholds.
 */
class HandLandmarkerHelper(
    private val context: Context,
    private val onLandmarksDetected: (List<HandLandmark>) -> Unit,
    private val onError: (String) -> Unit = {}
) {
    private var handLandmarker: HandLandmarker? = null
    private var backgroundExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    init {
        setupHandLandmarker()
    }

    private fun setupHandLandmarker() {
        try {
            val gestureConfig = SystemConfigManager(context).loadConfig().gestureConfig

            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("hand_landmarker.task")
                .build()

            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumHands(if (gestureConfig.detectTwoHands) 2 else 1)
                .setMinHandDetectionConfidence(gestureConfig.minDetectionConfidence)
                .setMinHandPresenceConfidence(gestureConfig.minPresenceConfidence)
                .setMinTrackingConfidence(gestureConfig.minTrackingConfidence)
                .setResultListener { result: HandLandmarkerResult, _ ->
                    processLandmarkerResult(result)
                }
                .setErrorListener { error ->
                    Log.e(TAG, "MediaPipe HandLandmarker error: ${error.message}", error)
                    onError(error.message ?: "Hand detector error")
                }
                .build()

            handLandmarker = HandLandmarker.createFromOptions(context, options)
            Log.i(TAG, "MediaPipe HandLandmarker initialized successfully with saved system config thresholds")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaPipe HandLandmarker: ${e.message}", e)
            onError("Failed to load hand model: ${e.message}")
            handLandmarker = null
        }
    }

    fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(backgroundExecutor) { imageProxy ->
                            analyzeImage(imageProxy)
                        }
                    }

                val cameraSelector = when {
                    cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) == true -> {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    }
                    cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) == true -> {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    else -> {
                        onError("No camera available on device")
                        return@addListener
                    }
                }

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                Log.i(TAG, "CameraX use cases bound successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind CameraX use cases: ${e.message}", e)
                onError("Camera in use or unavailable. Tap 'Try again'.")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun analyzeImage(imageProxy: ImageProxy) {
        if (handLandmarker == null) {
            imageProxy.close()
            onLandmarksDetected(emptyList())
            return
        }

        try {
            val bitmap = imageProxy.toBitmap()
            if (bitmap != null) {
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                val rotatedBitmap = rotateBitmap(bitmap, rotationDegrees.toFloat())
                val isFrontCamera = cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) == true
                val finalBitmap = if (isFrontCamera) {
                    flipBitmap(rotatedBitmap)
                } else {
                    rotatedBitmap
                }

                val mpImage = BitmapImageBuilder(finalBitmap).build()
                val frameTime = SystemClock.uptimeMillis()
                handLandmarker?.detectAsync(mpImage, frameTime)
            } else {
                onLandmarksDetected(emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image frame for hand landmarks: ${e.message}", e)
            onLandmarksDetected(emptyList())
        } finally {
            imageProxy.close()
        }
    }

    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        if (angle == 0f) return source
        val matrix = Matrix().apply { postRotate(angle) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun flipBitmap(source: Bitmap): Bitmap {
        val matrix = Matrix().apply { preScale(-1f, 1f) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun processLandmarkerResult(result: HandLandmarkerResult) {
        val landmarksList = mutableListOf<HandLandmark>()
        val landmarks = result.landmarks()
        if (!landmarks.isNullOrEmpty()) {
            for (hand in landmarks) {
                for (point in hand) {
                    landmarksList.add(HandLandmark(point.x(), point.y(), point.z()))
                }
            }
        }
        onLandmarksDetected(landmarksList)
    }

    fun clear() {
        backgroundExecutor.shutdown()
        try {
            handLandmarker?.close()
            handLandmarker = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing HandLandmarker: ${e.message}")
        }
        cameraProvider?.unbindAll()
    }

    companion object {
        private const val TAG = "HandLandmarkerHelper"
    }
}
