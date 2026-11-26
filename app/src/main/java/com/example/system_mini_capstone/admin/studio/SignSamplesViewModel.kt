package com.example.system_mini_capstone.admin.studio

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.system_mini_capstone.BuildConfig
import com.example.system_mini_capstone.ml.HandLandmark
import com.example.system_mini_capstone.ml.HandResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

data class SignSampleDto(
    val sampleId: String = java.util.UUID.randomUUID().toString(),
    val label: String = "",
    val landmarks: List<Float> = emptyList(),
    val handedness: String = "Right",
    val lens: String = "FRONT",
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
)

data class LabelSummary(
    val label: String,
    val count: Int,
    val isSynced: Boolean
)

data class SignSamplesUiState(
    val labelInput: String = "",
    val labelError: String? = null,
    val isCountingDown: Boolean = false,
    val countdownSeconds: Int = 3,
    val isRecording: Boolean = false,
    val recordedCount: Int = 0,
    val targetCount: Int = 40,
    val statusMessage: String = "Ready to record",
    val isSaving: Boolean = false,
    val saveError: Boolean = false,
    val labelSummaries: List<LabelSummary> = emptyList(),
    val isLoadingSummaries: Boolean = false,
    val simulatedLandmarks: List<HandLandmark> = emptyList(),
    val simulatedHands: List<HandResult> = emptyList()
)

class SignSamplesViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val _uiState = MutableStateFlow(SignSamplesUiState())
    val uiState: StateFlow<SignSamplesUiState> = _uiState.asStateFlow()

    private val capturedBuffer = mutableListOf<List<Float>>()
    private val capturedHandednessList = mutableListOf<String>()
    private var currentLandmarks: List<HandLandmark> = emptyList()
    private var detectedHandResult: HandResult? = null
    private var isHandPresent = false
    private var isFrontLens = true
    private var recordingJob: kotlinx.coroutines.Job? = null
    private var lastRecordedDtos: List<SignSampleDto> = emptyList()

    init {
        loadSummaries()
    }

    fun updateLabel(label: String) {
        val uppercase = label.trim().uppercase()
        _uiState.update {
            it.copy(
                labelInput = uppercase,
                labelError = if (SignSampleValidator.isValidLabel(uppercase)) null else "Invalid label (1-20 letters/numbers/underscore)"
            )
        }
    }

    fun setLensFacing(isFront: Boolean) {
        isFrontLens = isFront
    }

    fun onHandsReceived(hands: List<HandResult>) {
        if (hands.size == 1) {
            val hand = hands.first()
            detectedHandResult = hand
            currentLandmarks = hand.landmarks
            isHandPresent = true
            _uiState.update {
                it.copy(
                    simulatedHands = hands,
                    simulatedLandmarks = hand.landmarks,
                    statusMessage = "${hand.handedness} hand detected"
                )
            }
        } else if (hands.size > 1) {
            detectedHandResult = null
            currentLandmarks = emptyList()
            isHandPresent = false
            _uiState.update {
                it.copy(
                    simulatedHands = hands,
                    simulatedLandmarks = emptyList(),
                    statusMessage = "Show one hand to record"
                )
            }
        } else {
            detectedHandResult = null
            currentLandmarks = emptyList()
            isHandPresent = false
            _uiState.update {
                it.copy(
                    simulatedHands = emptyList(),
                    simulatedLandmarks = emptyList(),
                    statusMessage = "No hand detected"
                )
            }
        }
    }

    fun startRecording() {
        val label = _uiState.value.labelInput.trim().uppercase()
        if (!SignSampleValidator.isValidLabel(label)) {
            _uiState.update { it.copy(labelError = "Please enter a valid label (1-20 chars, A-Z, 0-9, _)") }
            return
        }

        if (_uiState.value.isRecording || _uiState.value.isCountingDown) return

        capturedBuffer.clear()
        capturedHandednessList.clear()
        _uiState.update {
            it.copy(
                isCountingDown = true,
                countdownSeconds = 3,
                recordedCount = 0,
                statusMessage = "Get ready...",
                saveError = false
            )
        }

        recordingJob = viewModelScope.launch {
            // 3-2-1 Countdown
            for (i in 3 downTo 1) {
                _uiState.update { it.copy(countdownSeconds = i, statusMessage = "Starting in $i...") }
                delay(1000)
            }

            _uiState.update {
                it.copy(
                    isCountingDown = false,
                    isRecording = true,
                    statusMessage = "Recording..."
                )
            }

            while (capturedBuffer.size < 40) {
                val hand = detectedHandResult
                if (isHandPresent && hand != null && hand.landmarks.size >= 21) {
                    val normalized = SignSampleNormalizer.normalize(hand.landmarks, hand.handedness)
                    if (normalized.size == 63) {
                        capturedBuffer.add(normalized)
                        capturedHandednessList.add(hand.handedness)
                        _uiState.update { it.copy(recordedCount = capturedBuffer.size, statusMessage = "Recording ${hand.handedness} hand...") }
                    }
                } else {
                    _uiState.update { it.copy(statusMessage = "Show one hand to record") }
                }
                delay(100)
            }

            // Finished capturing 40 samples
            _uiState.update { it.copy(isRecording = false, statusMessage = "Saving locally...") }
            saveCapturedSamples(label)
        }
    }

    fun cancelRecording() {
        recordingJob?.cancel()
        recordingJob = null
        capturedBuffer.clear()
        capturedHandednessList.clear()
        _uiState.update {
            it.copy(
                isCountingDown = false,
                isRecording = false,
                recordedCount = 0,
                statusMessage = "Recording cancelled",
                isSaving = false,
                saveError = false
            )
        }
    }

    private fun saveCapturedSamples(label: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = false) }
            val currentUser = auth.currentUser
            val uid = currentUser?.uid ?: "admin_local"
            val lensStr = if (isFrontLens) "FRONT" else "BACK"

            val dtos = capturedBuffer.mapIndexed { idx, floats ->
                SignSampleDto(
                    label = label,
                    landmarks = floats,
                    handedness = capturedHandednessList.getOrElse(idx) { "Right" },
                    lens = lensStr,
                    createdBy = uid
                )
            }
            lastRecordedDtos = dtos

            // 1. Save locally FIRST
            SignSampleLocalStore.saveLocal(getApplication(), dtos)
            _uiState.update {
                it.copy(
                    statusMessage = "Saved on this phone. Syncing..."
                )
            }

            // 2. Try Cloud Upload with strict 8 second timeout
            try {
                val batch = firestore.batch()
                val samplesCollection = firestore.collection("signSamples")
                for (dto in dtos) {
                    val docRef = samplesCollection.document(dto.sampleId)
                    batch.set(docRef, dto)
                }

                withTimeout(8000) {
                    batch.commit().await()
                }

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        statusMessage = "Saved & Synced successfully!"
                    )
                }
                loadSummaries()
            } catch (e: Exception) {
                val userStateLog = if (currentUser == null) "FirebaseAuth.currentUser is NULL (Not signed in)" else "FirebaseAuth.currentUser UID: ${currentUser.uid}"
                Log.e("SignLinkSamples", "Save failed. $userStateLog", e)

                val msg = if (currentUser == null) {
                    "Not signed in to Firebase"
                } else if (BuildConfig.DEBUG) {
                    "Saved on this phone. Cloud sync failed (${e.javaClass.simpleName}: ${e.message})."
                } else {
                    "Saved on this phone. Cloud sync failed."
                }

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveError = true,
                        statusMessage = msg
                    )
                }
                loadSummaries()
            }
        }
    }

    fun retrySave() {
        if (lastRecordedDtos.isNotEmpty()) {
            val label = _uiState.value.labelInput.trim().uppercase()
            if (SignSampleValidator.isValidLabel(label)) {
                saveCapturedSamples(label)
            }
        }
    }

    fun loadSummaries() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSummaries = true) }
            try {
                val localDtos = SignSampleLocalStore.loadLocal(getApplication())

                val cloudSnapshot = withTimeout(6000) {
                    firestore.collection("signSamples").get().await()
                }

                val cloudLabels = mutableSetOf<String>()
                val labelCounts = mutableMapOf<String, Int>()

                for (doc in cloudSnapshot.documents) {
                    val lbl = doc.getString("label") ?: continue
                    cloudLabels.add(lbl)
                    labelCounts[lbl] = (labelCounts[lbl] ?: 0) + 1
                }

                // Merge local counts
                for (dto in localDtos) {
                    if (dto.label !in labelCounts) {
                        labelCounts[dto.label] = localDtos.count { it.label == dto.label }
                    }
                }

                val summaries = labelCounts.map { entry ->
                    val isSynced = entry.key in cloudLabels
                    LabelSummary(entry.key, entry.value, isSynced)
                }.sortedBy { it.label }

                _uiState.update {
                    it.copy(
                        labelSummaries = summaries,
                        isLoadingSummaries = false
                    )
                }
            } catch (e: Exception) {
                Log.e("SignLinkSamples", "Error loading sample summaries, using local cache", e)
                val localDtos = SignSampleLocalStore.loadLocal(getApplication())
                val labelCounts = localDtos.groupBy { it.label }.mapValues { it.value.size }
                val summaries = labelCounts.map { LabelSummary(it.key, it.value, false) }.sortedBy { it.label }
                _uiState.update {
                    it.copy(
                        labelSummaries = summaries,
                        isLoadingSummaries = false
                    )
                }
            }
        }
    }

    fun deleteSamplesForLabel(label: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSummaries = true) }
            try {
                // Delete locally
                SignSampleLocalStore.deleteLocalForLabel(getApplication(), label)

                // Delete from cloud
                val snapshot = firestore.collection("signSamples")
                    .whereEqualTo("label", label)
                    .get()
                    .await()

                val batch = firestore.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
                loadSummaries()
            } catch (e: Exception) {
                Log.e("SignLinkSamples", "Error deleting samples for label $label", e)
                loadSummaries()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        recordingJob?.cancel()
    }
}
