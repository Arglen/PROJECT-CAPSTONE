package com.example.system_mini_capstone.translator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.system_mini_capstone.admin.config.SystemConfigManager
import com.example.system_mini_capstone.admin.studio.SignSampleNormalizer
import com.example.system_mini_capstone.ml.HandResult
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
    val leftSign: String = "-",
    val rightSign: String = "-",
    val topPredictions: List<Pair<String, Float>> = emptyList(),
    val translationHistory: List<TranslationRecord> = emptyList(),
    val isTtsEnabled: Boolean = true,
    val isDrawSkeletonEnabled: Boolean = true,
    val isRecognizing: Boolean = true,
    val simulatedHands: List<HandResult> = emptyList(),
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

    // Separate sliding windows per hand (Left and Right)
    private val requiredStableFrames = systemConfig.gestureConfig.debounceFrameCount.coerceIn(1, 15)
    private val windowSize = requiredStableFrames + 4
    private val leftFrames = mutableListOf<Pair<String, Float>>()
    private val rightFrames = mutableListOf<Pair<String, Float>>()
    private var lastAddedLeftSign: String = ""
    private var lastAddedRightSign: String = ""
    private var lastHandSeenTimestamp: Long = System.currentTimeMillis()

    init {
        viewModelScope.launch {
            matcherEngine.loadSamples()
            val count = matcherEngine.loadedCount
            val status = if (count > 0) "Signs loaded: $count" else "No signs recorded yet"
            _uiState.update { it.copy(sampleLoadStatus = status) }
        }
    }

    fun processHands(hands: List<HandResult>, isFrontCamera: Boolean) {
        if (!_uiState.value.isRecognizing) return

        val now = System.currentTimeMillis()

        if (hands.isEmpty()) {
            if (now - lastHandSeenTimestamp >= 500) {
                lastAddedLeftSign = ""
                lastAddedRightSign = ""
            }
            leftFrames.clear()
            rightFrames.clear()
            _uiState.update {
                it.copy(
                    currentSign = "No hand detected",
                    confidence = 0f,
                    leftSign = "-",
                    rightSign = "-",
                    simulatedHands = emptyList(),
                    statusMessage = "No hand detected",
                    debugMatchInfo = ""
                )
            }
            return
        }

        lastHandSeenTimestamp = now

        viewModelScope.launch {
            var leftDetected = "-"
            var rightDetected = "-"
            var leftConf = 0f
            var rightConf = 0f
            val debugBuilder = StringBuilder()

            for (hand in hands) {
                val isLeftHand = hand.handedness.equals("Left", ignoreCase = true)
                // Canonical form normalization (mirrors left hand x coords to match right hand space)
                val normalized = SignSampleNormalizer.normalize(hand.landmarks, hand.handedness)
                val matchResult = matcherEngine.match(normalized)

                val sign: String
                val conf: Float
                if (matchResult.label != "UNKNOWN") {
                    sign = matchResult.label
                    conf = matchResult.confidence
                } else {
                    val fallback = classifierEngine.classifyGesture(hand.landmarks)
                    sign = fallback.gestureName
                    conf = fallback.confidence
                }

                debugBuilder.append("[${hand.handedness} (score: %.2f): %s (conf: %d%%)] ".format(hand.score, sign, (conf * 100).toInt()))

                val frames = if (isLeftHand) leftFrames else rightFrames
                if (sign != "UNKNOWN" && conf >= 0.70f) {
                    frames.add(Pair(sign, conf))
                    if (frames.size > windowSize) frames.removeAt(0)
                } else {
                    frames.add(Pair("UNKNOWN", 0f))
                    if (frames.size > windowSize) frames.removeAt(0)
                }

                val validFrames = frames.filter { it.first != "UNKNOWN" }
                val signCounts = validFrames.groupingBy { it.first }.eachCount()
                val dominant = signCounts.entries.maxByOrNull { it.value }

                if (dominant != null && dominant.value >= requiredStableFrames) {
                    val stable = dominant.key
                    val meanC = validFrames.filter { it.first == stable }.map { it.second }.average().toFloat()
                    if (meanC >= 0.70f) {
                        if (isLeftHand) {
                            leftDetected = stable
                            leftConf = meanC
                        } else {
                            rightDetected = stable
                            rightConf = meanC
                        }
                    }
                }
            }

            // Determine main label (most confident of the two hands)
            val mainSign: String
            val mainConf: Float
            val historyHandTag: String

            when {
                leftDetected != "-" && rightDetected != "-" -> {
                    if (leftConf >= rightConf) {
                        mainSign = leftDetected
                        mainConf = leftConf
                        historyHandTag = " (Left)"
                    } else {
                        mainSign = rightDetected
                        mainConf = rightConf
                        historyHandTag = " (Right)"
                    }
                }
                leftDetected != "-" -> {
                    mainSign = leftDetected
                    mainConf = leftConf
                    historyHandTag = " (Left)"
                }
                rightDetected != "-" -> {
                    mainSign = rightDetected
                    mainConf = rightConf
                    historyHandTag = " (Right)"
                }
                else -> {
                    mainSign = if (hands.isNotEmpty()) "Detecting..." else "No hand detected"
                    mainConf = 0f
                    historyHandTag = ""
                }
            }

            _uiState.update { state ->
                var updatedHistory = state.translationHistory
                if (mainSign != "No hand detected" && mainSign != "Detecting..." && mainSign != "UNKNOWN") {
                    val recordText = "$mainSign$historyHandTag"
                    val isNewGesture = (historyHandTag.contains("Left") && lastAddedLeftSign != mainSign) ||
                                       (historyHandTag.contains("Right") && lastAddedRightSign != mainSign)

                    if (historyHandTag.contains("Left") && isNewGesture) lastAddedLeftSign = mainSign
                    if (historyHandTag.contains("Right") && isNewGesture) lastAddedRightSign = mainSign

                    if (isNewGesture) {
                        updatedHistory = listOf(TranslationRecord(signText = recordText, confidence = mainConf)) + state.translationHistory.take(19)
                        if (state.isTtsEnabled) {
                            ttsManager.speak(mainSign)
                        }
                    }
                }

                state.copy(
                    currentSign = mainSign,
                    confidence = mainConf,
                    leftSign = leftDetected,
                    rightSign = rightDetected,
                    translationHistory = updatedHistory,
                    simulatedHands = hands,
                    statusMessage = "Hands detected: ${hands.size}",
                    debugMatchInfo = debugBuilder.toString()
                )
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
                leftSign = "-",
                rightSign = "-",
                simulatedHands = emptyList()
            )
        }
    }

    fun resetState() {
        leftFrames.clear()
        rightFrames.clear()
        lastAddedLeftSign = ""
        lastAddedRightSign = ""
        _uiState.update {
            it.copy(
                currentSign = "No hand detected",
                confidence = 0f,
                leftSign = "-",
                rightSign = "-",
                simulatedHands = emptyList(),
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
