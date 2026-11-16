package com.example.system_mini_capstone.translator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.system_mini_capstone.admin.config.SystemConfigManager
import com.example.system_mini_capstone.admin.studio.SignSampleNormalizer
import com.example.system_mini_capstone.ml.HandLandmark
import com.example.system_mini_capstone.ml.SignClassifierEngine
import com.example.system_mini_capstone.ml.SignMatcherEngine
import com.example.system_mini_capstone.tts.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TranslationRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val signText: String,
    val confidence: Float,
    val timestamp: String = android.text.format.DateFormat.format("hh:mm:ss a", System.currentTimeMillis()).toString()
)

data class TranslatorUiState(
    val currentSign: String = "No hand detected",
    val confidence: Float = 0f,
    val topPredictions: List<Pair<String, Float>> = emptyList(),
    val translationHistory: List<TranslationRecord> = emptyList(),
    val isTtsEnabled: Boolean = true,
    val isDrawSkeletonEnabled: Boolean = true,
    val isRecognizing: Boolean = true,
    val simulatedLandmarks: List<HandLandmark> = emptyList(),
    val statusMessage: String = "No hand detected",
    val sampleLoadStatus: String = "Loading signs...",
    val debugMatchInfo: String = ""
)

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {

    private val classifierEngine = SignClassifierEngine()
    private val matcherEngine = SignMatcherEngine(application)
    private val ttsManager = TtsManager(application)
    private val configManager = SystemConfigManager(application)
    private val systemConfig = configManager.loadConfig()

    private val _uiState = MutableStateFlow(
        TranslatorUiState(
            isTtsEnabled = systemConfig.featureToggles.autoPronounceTts,
            isDrawSkeletonEnabled = systemConfig.featureToggles.drawHandLandmarkSkeleton
        )
    )
    val uiState: StateFlow<TranslatorUiState> = _uiState.asStateFlow()

    // Sliding window parameters driven by saved System Config debounceFrameCount
    private val requiredStableFrames = systemConfig.gestureConfig.debounceFrameCount.coerceIn(1, 15)
    private val windowSize = requiredStableFrames + 4
    private val recentFrames = mutableListOf<Pair<String, Float>>()
    private var lastAddedSign: String = ""
    private var lastHandSeenTimestamp: Long = System.currentTimeMillis()

    init {
        viewModelScope.launch {
            matcherEngine.loadSamples()
            val count = matcherEngine.loadedCount
            val status = if (count > 0) "Signs loaded: $count" else "No signs recorded yet"
            _uiState.update { it.copy(sampleLoadStatus = status) }
        }
    }

    fun processLandmarks(landmarks: List<HandLandmark>) {
        if (!_uiState.value.isRecognizing) return

        val now = System.currentTimeMillis()

        if (landmarks.isEmpty()) {
            if (now - lastHandSeenTimestamp >= 500) {
                lastAddedSign = ""
            }
            recentFrames.clear()
            _uiState.update {
                it.copy(
                    currentSign = "No hand detected",
                    confidence = 0f,
                    simulatedLandmarks = emptyList(),
                    statusMessage = "No hand detected",
                    debugMatchInfo = ""
                )
            }
            return
        }

        lastHandSeenTimestamp = now

        // Use first hand only if two hands are detected
        val singleHand = if (landmarks.size >= 42) landmarks.take(21) else landmarks

        viewModelScope.launch {
            // 1. Try k-NN Sign Matcher first using recorded sign samples
            val normalized = SignSampleNormalizer.normalize(singleHand)
            val matchResult = matcherEngine.match(normalized)

            val detectedSign: String
            val confidence: Float

            if (matchResult.label != "UNKNOWN") {
                detectedSign = matchResult.label
                confidence = matchResult.confidence
            } else {
                // 2. Fall back to heuristic classifier if no recorded samples match
                val fallbackResult = classifierEngine.classifyGesture(singleHand)
                detectedSign = fallbackResult.gestureName
                confidence = fallbackResult.confidence
            }

            if (detectedSign != "UNKNOWN" && confidence >= 0.70f) {
                recentFrames.add(Pair(detectedSign, confidence))
                if (recentFrames.size > windowSize) {
                    recentFrames.removeAt(0)
                }
            } else {
                recentFrames.add(Pair("UNKNOWN", 0f))
                if (recentFrames.size > windowSize) {
                    recentFrames.removeAt(0)
                }
            }

            val validFrames = recentFrames.filter { it.first != "UNKNOWN" }
            val signCounts = validFrames.groupingBy { it.first }.eachCount()
            val dominantEntry = signCounts.entries.maxByOrNull { it.value }

            if (dominantEntry != null && dominantEntry.value >= requiredStableFrames) {
                val stableSign = dominantEntry.key
                val meanConf = validFrames.filter { it.first == stableSign }.map { it.second }.average().toFloat()

                if (meanConf >= 0.70f) {
                    _uiState.update { state ->
                        val isNewGesture = lastAddedSign != stableSign
                        val updatedHistory = if (isNewGesture) {
                            listOf(TranslationRecord(signText = stableSign, confidence = meanConf)) + state.translationHistory.take(19)
                        } else {
                            state.translationHistory
                        }

                        if (isNewGesture) {
                            lastAddedSign = stableSign
                            if (state.isTtsEnabled) {
                                ttsManager.speak(stableSign)
                            }
                        }

                        state.copy(
                            currentSign = stableSign,
                            confidence = meanConf,
                            translationHistory = updatedHistory,
                            simulatedLandmarks = singleHand,
                            statusMessage = "Hand detected",
                            debugMatchInfo = matchResult.debugInfo
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            currentSign = "Detecting...",
                            confidence = confidence,
                            simulatedLandmarks = singleHand,
                            statusMessage = "Hand detected",
                            debugMatchInfo = matchResult.debugInfo
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        currentSign = "Detecting...",
                        confidence = confidence,
                        simulatedLandmarks = singleHand,
                        statusMessage = "Hand detected",
                        debugMatchInfo = matchResult.debugInfo
                    )
                }
            }
        }
    }

    fun toggleTts(enabled: Boolean) {
        _uiState.update { it.copy(isTtsEnabled = enabled) }
        val currentConfig = configManager.loadConfig()
        configManager.saveConfig(currentConfig.copy(featureToggles = currentConfig.featureToggles.copy(autoPronounceTts = enabled)))
    }

    fun toggleSkeleton(enabled: Boolean) {
        _uiState.update { it.copy(isDrawSkeletonEnabled = enabled) }
        val currentConfig = configManager.loadConfig()
        configManager.saveConfig(currentConfig.copy(featureToggles = currentConfig.featureToggles.copy(drawHandLandmarkSkeleton = enabled)))
    }

    fun toggleRecognition(recognizing: Boolean) {
        _uiState.update {
            it.copy(
                isRecognizing = recognizing,
                statusMessage = if (recognizing) "No hand detected" else "Paused"
            )
        }
    }

    fun clearHistory() {
        _uiState.update {
            it.copy(
                translationHistory = emptyList(),
                currentSign = "No hand detected",
                confidence = 0f,
                simulatedLandmarks = emptyList()
            )
        }
    }

    fun resetState() {
        recentFrames.clear()
        lastAddedSign = ""
        _uiState.update {
            it.copy(
                currentSign = "No hand detected",
                confidence = 0f,
                simulatedLandmarks = emptyList(),
                statusMessage = "No hand detected",
                debugMatchInfo = ""
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        classifierEngine.close()
        ttsManager.shutdown()
    }
}
