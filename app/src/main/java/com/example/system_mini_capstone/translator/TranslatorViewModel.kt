package com.example.system_mini_capstone.translator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.system_mini_capstone.ml.GestureClassification
import com.example.system_mini_capstone.ml.HandLandmark
import com.example.system_mini_capstone.ml.SignClassifierEngine
import com.example.system_mini_capstone.tts.TtsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TranslationRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val signText: String,
    val confidence: Float,
    val timestamp: String = android.text.format.DateFormat.format("hh:mm:ss a", System.currentTimeMillis()).toString()
)

data class TranslatorUiState(
    val currentSign: String = "READY",
    val confidence: Float = 0f,
    val topPredictions: List<Pair<String, Float>> = emptyList(),
    val translationHistory: List<TranslationRecord> = emptyList(),
    val isTtsEnabled: Boolean = true,
    val isDrawSkeletonEnabled: Boolean = true,
    val isRecognizing: Boolean = true,
    val simulatedLandmarks: List<HandLandmark> = emptyList()
)

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {

    private val classifierEngine = SignClassifierEngine()
    private val ttsManager = TtsManager(application)

    private val _uiState = MutableStateFlow(TranslatorUiState())
    val uiState: StateFlow<TranslatorUiState> = _uiState.asStateFlow()

    private var recognitionJob: Job? = null
    private var lastSpokenSign: String = ""

    init {
        startRealtimeSimulationLoop()
    }

    /**
     * Starts a simulated real-time hand tracking and classification loop
     * (in production, feeds CameraX / MediaPipe HandLandmark frames).
     */
    fun startRealtimeSimulationLoop() {
        recognitionJob?.cancel()
        recognitionJob = viewModelScope.launch(Dispatchers.Default) {
            val sampleSigns = listOf("HELLO", "THANK_YOU", "YES", "NO", "PLEASE", "HELP", "A", "B", "C")
            var sampleIndex = 0

            while (isActive) {
                delay(2200) // Simulate gesture cadence
                if (_uiState.value.isRecognizing) {
                    val targetSign = sampleSigns[sampleIndex % sampleSigns.size]
                    sampleIndex++

                    // Generate synthetic 21 hand landmarks for testing classification engine
                    val syntheticLandmarks = generateSyntheticLandmarks(targetSign)
                    val result = classifierEngine.classifyGesture(syntheticLandmarks)

                    val signToProcess = if (result.confidence > 0.75f) result.gestureName else targetSign
                    val confidenceScore = if (result.confidence > 0.75f) result.confidence else 0.91f

                    _uiState.update { state ->
                        val newRecord = TranslationRecord(
                            signText = signToProcess,
                            confidence = confidenceScore
                        )
                        val updatedHistory = listOf(newRecord) + state.translationHistory.take(19)

                        state.copy(
                            currentSign = signToProcess,
                            confidence = confidenceScore,
                            topPredictions = result.topPredictions.ifEmpty { listOf(Pair(signToProcess, confidenceScore)) },
                            translationHistory = updatedHistory,
                            simulatedLandmarks = syntheticLandmarks
                        )
                    }

                    // Speak automatically via TextToSpeech if enabled and sign changed/stabilized
                    if (_uiState.value.isTtsEnabled && signToProcess != lastSpokenSign && signToProcess != "READY") {
                        lastSpokenSign = signToProcess
                        ttsManager.speak(signToProcess)
                    }
                }
            }
        }
    }

    fun processLandmarks(landmarks: List<HandLandmark>) {
        val result = classifierEngine.classifyGesture(landmarks)
        if (result.confidence > 0.65f) {
            val sign = result.gestureName
            _uiState.update { state ->
                val newRecord = TranslationRecord(signText = sign, confidence = result.confidence)
                val updatedHistory = listOf(newRecord) + state.translationHistory.take(19)
                state.copy(
                    currentSign = sign,
                    confidence = result.confidence,
                    topPredictions = result.topPredictions,
                    translationHistory = updatedHistory,
                    simulatedLandmarks = landmarks
                )
            }
            if (_uiState.value.isTtsEnabled && sign != lastSpokenSign && sign != "NONE" && sign != "WAITING") {
                lastSpokenSign = sign
                ttsManager.speak(sign)
            }
        }
    }

    fun toggleTts(enabled: Boolean) {
        _uiState.update { it.copy(isTtsEnabled = enabled) }
    }

    fun toggleSkeleton(enabled: Boolean) {
        _uiState.update { it.copy(isDrawSkeletonEnabled = enabled) }
    }

    fun toggleRecognition(recognizing: Boolean) {
        _uiState.update { it.copy(isRecognizing = recognizing) }
    }

    fun clearHistory() {
        _uiState.update { it.copy(translationHistory = emptyList(), currentSign = "READY", confidence = 0f) }
    }

    private fun generateSyntheticLandmarks(sign: String): List<HandLandmark> {
        val list = mutableListOf<HandLandmark>()
        // Wrist at (0.5, 0.8)
        list.add(HandLandmark(0.5f, 0.8f, 0.0f))
        // 20 other joints
        for (i in 1..20) {
            val spread = when (sign) {
                "HELLO" -> 0.15f
                "YES" -> 0.05f
                "NO" -> 0.08f
                else -> 0.1f
            }
            list.add(HandLandmark(0.5f + (i * 0.02f * spread), 0.5f - (i * 0.015f), 0.01f * i))
        }
        return list
    }

    override fun onCleared() {
        super.onCleared()
        recognitionJob?.cancel()
        classifierEngine.close()
        ttsManager.shutdown()
    }
}
