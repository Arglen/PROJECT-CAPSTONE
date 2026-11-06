package com.example.system_mini_capstone.admin.lcms

enum class LessonDifficulty(val label: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced")
}

enum class LessonStatus(val label: String) {
    DRAFT("Draft (Unpublished)"),
    PUBLISHED("Live in Curriculum"),
    ARCHIVED("Archived")
}

enum class LessonMediaType(val label: String, val iconEmoji: String) {
    IMAGE_DIAGRAM("Hand Anatomy Diagram", "🖼️"),
    VIDEO_LOOP("Video Demonstration", "🎬"),
    SKELETON_ANIMATION("21-Node 3D Skeleton", "🦴")
}

/**
 * Sign language lesson curriculum item managed within the LCMS.
 */
data class LessonItem(
    val lessonId: String,
    val title: String,
    val category: String,
    val difficulty: LessonDifficulty = LessonDifficulty.BEGINNER,
    val description: String,
    val instructionSteps: List<String> = emptyList(),
    val targetLandmarksDescription: String = "",
    val mediaType: LessonMediaType = LessonMediaType.VIDEO_LOOP,
    val mediaUrl: String = "",
    val ttsPronunciation: String = "",
    val status: LessonStatus = LessonStatus.DRAFT,
    val lastUpdatedFormatted: String = "Just now",
    val estimatedMinutes: Int = 3
)
