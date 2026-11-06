package com.example.system_mini_capstone.admin.lcms

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class LessonEditorUiState(
    val lesson: LessonItem = LessonItem(
        lessonId = "LSN-" + UUID.randomUUID().toString().take(6).uppercase(),
        title = "",
        category = "Everyday Phrases",
        description = "",
        instructionSteps = listOf("Position hand clearly within camera view."),
        ttsPronunciation = "",
        status = LessonStatus.DRAFT
    ),
    val isNewLesson: Boolean = true,
    val isPreviewMode: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val statusMessage: String? = null,
    val isSavedSuccess: Boolean = false
)

class LessonEditorViewModel(
    application: Application,
    private val repository: LessonRepository = LessonRepositoryImpl()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LessonEditorUiState())
    val uiState: StateFlow<LessonEditorUiState> = _uiState.asStateFlow()

    fun loadLesson(lessonId: String?) {
        if (lessonId == null || lessonId == "new") {
            _uiState.update {
                it.copy(
                    lesson = LessonItem(
                        lessonId = "LSN-" + UUID.randomUUID().toString().take(6).uppercase(),
                        title = "",
                        category = "Everyday Phrases",
                        description = "",
                        instructionSteps = listOf("Position hand clearly within camera view."),
                        ttsPronunciation = "",
                        status = LessonStatus.DRAFT
                    ),
                    isNewLesson = true,
                    isPreviewMode = false,
                    validationErrors = emptyMap()
                )
            }
        } else {
            viewModelScope.launch {
                val existing = repository.getLessonById(lessonId)
                if (existing != null) {
                    _uiState.update {
                        it.copy(
                            lesson = existing,
                            isNewLesson = false,
                            isPreviewMode = false,
                            validationErrors = emptyMap()
                        )
                    }
                }
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update {
            it.copy(
                lesson = it.lesson.copy(title = title),
                validationErrors = it.validationErrors - "title"
            )
        }
    }

    fun updateCategory(category: String) {
        _uiState.update {
            it.copy(
                lesson = it.lesson.copy(category = category),
                validationErrors = it.validationErrors - "category"
            )
        }
    }

    fun updateDifficulty(difficulty: LessonDifficulty) {
        _uiState.update {
            it.copy(lesson = it.lesson.copy(difficulty = difficulty))
        }
    }

    fun updateDescription(description: String) {
        _uiState.update {
            it.copy(
                lesson = it.lesson.copy(description = description),
                validationErrors = it.validationErrors - "description"
            )
        }
    }

    fun updateMediaType(mediaType: LessonMediaType) {
        _uiState.update {
            it.copy(lesson = it.lesson.copy(mediaType = mediaType))
        }
    }

    fun updateMediaUrl(url: String) {
        _uiState.update {
            it.copy(lesson = it.lesson.copy(mediaUrl = url))
        }
    }

    fun updateTtsPronunciation(phrase: String) {
        _uiState.update {
            it.copy(
                lesson = it.lesson.copy(ttsPronunciation = phrase),
                validationErrors = it.validationErrors - "tts"
            )
        }
    }

    fun updateLandmarkDescription(desc: String) {
        _uiState.update {
            it.copy(lesson = it.lesson.copy(targetLandmarksDescription = desc))
        }
    }

    fun addInstructionStep(stepText: String = "") {
        val currentSteps = _uiState.value.lesson.instructionSteps.toMutableList()
        currentSteps.add(stepText.ifBlank { "Hold gesture for 1 second until confirmed." })
        _uiState.update {
            it.copy(
                lesson = it.lesson.copy(instructionSteps = currentSteps),
                validationErrors = it.validationErrors - "steps"
            )
        }
    }

    fun updateInstructionStep(index: Int, text: String) {
        val currentSteps = _uiState.value.lesson.instructionSteps.toMutableList()
        if (index in currentSteps.indices) {
            currentSteps[index] = text
            _uiState.update {
                it.copy(lesson = it.lesson.copy(instructionSteps = currentSteps))
            }
        }
    }

    fun removeInstructionStep(index: Int) {
        val currentSteps = _uiState.value.lesson.instructionSteps.toMutableList()
        if (index in currentSteps.indices && currentSteps.size > 1) {
            currentSteps.removeAt(index)
            _uiState.update {
                it.copy(lesson = it.lesson.copy(instructionSteps = currentSteps))
            }
        }
    }

    fun togglePreviewMode() {
        _uiState.update { it.copy(isPreviewMode = !it.isPreviewMode) }
    }

    /**
     * Securely saves the work-in-progress lesson as a Draft without deploying live.
     */
    fun saveDraft() {
        val currentLesson = _uiState.value.lesson
        if (currentLesson.title.isBlank()) {
            _uiState.update {
                it.copy(validationErrors = mapOf("title" to "Title is required to save a draft."))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val draftLesson = currentLesson.copy(status = LessonStatus.DRAFT)
            repository.saveLesson(draftLesson)
            _uiState.update {
                it.copy(
                    lesson = draftLesson,
                    isSaving = false,
                    isSavedSuccess = true,
                    statusMessage = "Lesson '${draftLesson.title}' securely saved as Draft."
                )
            }
        }
    }

    /**
     * Validates lesson completeness and pushes to production as Published.
     */
    fun publishLesson() {
        val current = _uiState.value.lesson
        val errors = mutableMapOf<String, String>()

        if (current.title.isBlank()) errors["title"] = "Lesson title is required"
        if (current.description.isBlank()) errors["description"] = "Overview description is required"
        if (current.ttsPronunciation.isBlank()) errors["tts"] = "TTS speech phrase is required"
        if (current.instructionSteps.isEmpty()) errors["steps"] = "At least one instruction step is required"

        if (errors.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    validationErrors = errors,
                    statusMessage = "Please complete all required fields before publishing."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val publishedLesson = current.copy(status = LessonStatus.PUBLISHED)
            repository.saveLesson(publishedLesson)
            _uiState.update {
                it.copy(
                    lesson = publishedLesson,
                    isSaving = false,
                    isSavedSuccess = true,
                    statusMessage = "Lesson '${publishedLesson.title}' successfully published live to student curriculum!"
                )
            }
        }
    }

    fun dismissStatus() {
        _uiState.update { it.copy(statusMessage = null, isSavedSuccess = false) }
    }
}
