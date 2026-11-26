package com.example.system_mini_capstone.translator

import android.content.Context
import android.content.SharedPreferences
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
import com.example.system_mini_capstone.ml.HandResult
import com.example.system_mini_capstone.ml.toLogicalHandedness
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * CameraX and MediaPipe Vision Pipeline Helper supporting up to 2 hands detection,
 * front/back camera switching, dynamic mirroring, and correct logical handedness mapping.
 */
class HandLandmarkerHelper(
    private val context: Context,
    private val onHandsDetected: (List<HandResult>) -> Unit,
    private val onCameraSwitched: () -> Unit = {},
    private val onError: (String) -> Unit = {}
) {
    private var handLandmarker: HandLandmarker? = null
    private var backgroundExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var currentLensFacing: Int = prefs.getInt(KEY_LENS_FACING, CameraSelector.LENS_FACING_FRONT)
    private var isSwitching = false

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
                .setNumHands(2) // Up to 2 hands supported
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
            Log.i(TAG, "MediaPipe HandLandmarker initialized for dual-hand detection")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaPipe HandLandmarker: ${e.message}", e)
            onError("Failed to load hand model: ${e.message}")
            handLandmarker = null
        }
    }

    fun isMirrored(): Boolean = isMirrored(currentLensFacing)

    fun hasMultipleCameras(): Boolean {
        val provider = cameraProvider ?: try { ProcessCameraProvider.getInstance(context).get() } catch (_: Exception) { return false }
        val hasFront = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
        val hasBack = provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
        return hasFront && hasBack
    }

    fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        if (isSwitching) return
        isSwitching = true

        val provider = cameraProvider
        if (provider == null) {
            isSwitching = false
            return
        }

        val targetLensFacing = if (currentLensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }

        val targetSelector = CameraSelector.Builder().requireLensFacing(targetLensFacing).build()
        if (!provider.hasCamera(targetSelector)) {
            isSwitching = false
            onError("Selected camera not available on this device.")
            return
        }

        currentLensFacing = targetLensFacing
        prefs.edit().putInt(KEY_LENS_FACING, currentLensFacing).apply()

        try {
            provider.unbindAll()
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

            provider.bindToLifecycle(lifecycleOwner, targetSelector, preview, imageAnalysis)
            onCameraSwitched()
            Log.i(TAG, "Switched camera successfully to lens facing: $currentLensFacing")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to switch camera, falling back: ${e.message}", e)
            currentLensFacing = if (targetLensFacing == CameraSelector.LENS_FACING_FRONT) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
            prefs.edit().putInt(KEY_LENS_FACING, currentLensFacing).apply()
            val fallbackSelector = CameraSelector.Builder().requireLensFacing(currentLensFacing).build()
            try {
                provider.unbindAll()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    .also { analysis -> analysis.setAnalyzer(backgroundExecutor) { proxy -> analyzeImage(proxy) } }
                provider.bindToLifecycle(lifecycleOwner, fallbackSelector, preview, imageAnalysis)
                onError("Failed to start chosen camera. Switched back.")
            } catch (_: Exception) {
                onError("Camera failed to start.")
            }
        } finally {
            isSwitching = false
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

                val selectorBuilder = CameraSelector.Builder().requireLensFacing(currentLensFacing)
                val cameraSelector = try {
                    selectorBuilder.build()
                } catch (_: Exception) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }

                val finalSelector = if (cameraProvider?.hasCamera(cameraSelector) == true) {
                    cameraSelector
                } else {
                    currentLensFacing = CameraSelector.LENS_FACING_FRONT
                    prefs.edit().putInt(KEY_LENS_FACING, currentLensFacing).apply()
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    finalSelector,
                    preview,
                    imageAnalysis
                )
                Log.i(TAG, "CameraX use cases bound successfully with lens facing: $currentLensFacing")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind CameraX use cases: ${e.message}", e)
                onError("Camera in use or unavailable. Tap 'Try again'.")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun analyzeImage(imageProxy: ImageProxy) {
        if (handLandmarker == null) {
            imageProxy.close()
            onHandsDetected(emptyList())
            return
        }

        try {
            val bitmap = imageProxy.toBitmap()
            if (bitmap != null) {
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                val rotatedBitmap = rotateBitmap(bitmap, rotationDegrees.toFloat())
                val mirrored = isMirrored()
                val finalBitmap = if (mirrored) {
                    flipBitmap(rotatedBitmap)
                } else {
                    rotatedBitmap
                }

                val mpImage = BitmapImageBuilder(finalBitmap).build()
                val frameTime = SystemClock.uptimeMillis()
                handLandmarker?.detectAsync(mpImage, frameTime)
            } else {
                onHandsDetected(emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image frame for hand landmarks: ${e.message}", e)
            onHandsDetected(emptyList())
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
        val handResults = mutableListOf<HandResult>()
        val landmarks = result.landmarks()
        val handednessList = result.handedness()
        val mirrored = isMirrored()

        if (!landmarks.isNullOrEmpty()) {
            for (i in landmarks.indices) {
                val hand = landmarks[i]
                val landmarksList = mutableListOf<HandLandmark>()
                for (point in hand) {
                    val px = if (mirrored) 1f - point.x() else point.x()
                    landmarksList.add(HandLandmark(px, point.y(), point.z()))
                }

                val rawCategory = if (!handednessList.isNullOrEmpty() && i < handednessList.size) {
                    handednessList[i].firstOrNull()
                } else {
                    null
                }
                val rawLabel = rawCategory?.categoryName() ?: "Right"
                val rawScore = rawCategory?.score() ?: 1.0f

                val logicalHandedness = toLogicalHandedness(rawLabel, mirrored, rawScore)
                handResults.add(HandResult(logicalHandedness, rawScore, landmarksList))
            }
        }
        onHandsDetected(handResults)
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
        private const val PREFS_NAME = "signlink_camera_prefs"
        private const val KEY_LENS_FACING = "key_lens_facing"

        fun isMirrored(lensFacing: Int): Boolean {
            return lensFacing == CameraSelector.LENS_FACING_FRONT
        }
    }
}
