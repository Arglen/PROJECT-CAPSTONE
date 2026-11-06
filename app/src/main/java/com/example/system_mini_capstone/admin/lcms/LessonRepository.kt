package com.example.system_mini_capstone.admin.lcms

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface LessonRepository {
    suspend fun fetchLessons(): List<LessonItem>
    suspend fun getLessonById(lessonId: String): LessonItem?
    suspend fun saveLesson(lesson: LessonItem): LessonItem
    suspend fun deleteLesson(lessonId: String): Boolean
}

class LessonRepositoryImpl : LessonRepository {

    private val lessonsList = mutableListOf(
        LessonItem(
            lessonId = "LSN-101",
            title = "Greeting: Open Palm Hello",
            category = "Greetings & Etiquette",
            difficulty = LessonDifficulty.BEGINNER,
            description = "Learn how to greet someone with a friendly, confident open palm gesture recognized universally in ASL.",
            instructionSteps = listOf(
                "Raise your dominant hand to temple height, palm facing outward.",
                "Extend all 5 fingers comfortably, thumb lightly apart.",
                "Execute a gentle 2-stroke waving arc away from the forehead.",
                "Hold gesture steady for at least 1 second to verify TTS pronounces 'Hello'."
            ),
            targetLandmarksDescription = "Landmarks 4, 8, 12, 16, 20 extended vertically above MCP joints 2-5.",
            mediaType = LessonMediaType.VIDEO_LOOP,
            mediaUrl = "https://assets.signlink.app/lessons/hello_demo.mp4",
            ttsPronunciation = "Hello! Nice to meet you.",
            status = LessonStatus.PUBLISHED,
            lastUpdatedFormatted = "Nov 04, 2026",
            estimatedMinutes = 2
        ),
        LessonItem(
            lessonId = "LSN-102",
            title = "ASL Expression: I Love You",
            category = "Everyday Phrases",
            difficulty = LessonDifficulty.BEGINNER,
            description = "Master the iconic American Sign Language symbol combining letters I, L, and Y into a single handshape.",
            instructionSteps = listOf(
                "Hold your dominant hand with palm facing directly forward toward the camera.",
                "Extend your thumb outward, index finger straight up, and pinky finger straight up.",
                "Curl the middle and ring fingers flat against the palm.",
                "Hold the static shape with wrist upright and stable."
            ),
            targetLandmarksDescription = "Landmark 4 (thumb), 8 (index), and 20 (pinky) extended; landmarks 12 and 16 curled down.",
            mediaType = LessonMediaType.SKELETON_ANIMATION,
            mediaUrl = "https://assets.signlink.app/lessons/ily_skeleton.glb",
            ttsPronunciation = "I love you.",
            status = LessonStatus.PUBLISHED,
            lastUpdatedFormatted = "Nov 03, 2026",
            estimatedMinutes = 3
        ),
        LessonItem(
            lessonId = "LSN-103",
            title = "Emergency: Call Medical Ambulance",
            category = "Healthcare & Emergency",
            difficulty = LessonDifficulty.INTERMEDIATE,
            description = "Critical emergency sign to communicate urgent medical assistance and siren dispatch.",
            instructionSteps = listOf(
                "Raise both hands to chest level, dominant hand slightly in front.",
                "Curve fingers gently while miming the revolving light motion of an ambulance.",
                "Transition seamlessly into a phone gesture near the ear.",
                "Maintain clear separation between palms so camera captures both hand landmark clusters."
            ),
            targetLandmarksDescription = "Dual-hand 42 landmark tracking with rotational velocity check on wrist.",
            mediaType = LessonMediaType.IMAGE_DIAGRAM,
            mediaUrl = "https://assets.signlink.app/lessons/ambulance_diagram.png",
            ttsPronunciation = "Emergency! Please call an ambulance immediately.",
            status = LessonStatus.DRAFT,
            lastUpdatedFormatted = "Nov 05, 2026",
            estimatedMinutes = 5
        ),
        LessonItem(
            lessonId = "LSN-104",
            title = "Alphabet: Letter A to E Basics",
            category = "Alphabet & Fingerspelling",
            difficulty = LessonDifficulty.BEGINNER,
            description = "Foundational fingerspelling vowels and initial letters for spelling names and places.",
            instructionSteps = listOf(
                "Letter A: Form a tight fist with thumb resting alongside the curled index finger.",
                "Letter B: Open 4 fingers straight up, thumb tucked horizontally across lower palm.",
                "Letter C: Form a curved 'C' contour with all fingers and thumb facing outward.",
                "Letter D: Point index finger straight up, form an 'O' loop with thumb and other 3 fingers.",
                "Letter E: Curl all fingertips down resting securely atop the tucked thumb."
            ),
            targetLandmarksDescription = "Fingertip to PIP joint proximity vectors.",
            mediaType = LessonMediaType.VIDEO_LOOP,
            mediaUrl = "https://assets.signlink.app/lessons/alphabet_a_e.mp4",
            ttsPronunciation = "Letters A, B, C, D, E.",
            status = LessonStatus.PUBLISHED,
            lastUpdatedFormatted = "Oct 29, 2026",
            estimatedMinutes = 6
        ),
        LessonItem(
            lessonId = "LSN-105",
            title = "Numbers: Counting 1 through 5",
            category = "Numbers & Quantities",
            difficulty = LessonDifficulty.BEGINNER,
            description = "Standard ASL hand orientations for numbers 1 to 5 (distinguishing palm-in vs palm-out).",
            instructionSteps = listOf(
                "Palm faces inward toward body for cardinal counting numbers.",
                "Number 1: Index finger up, others closed.",
                "Number 2: Index and middle finger up in V shape.",
                "Number 3: Thumb, index, and middle extended (ASL 3).",
                "Number 4: 4 fingers upright, thumb folded in.",
                "Number 5: Open palm with all 5 digits spread."
            ),
            targetLandmarksDescription = "Sequential extension of MCP to tip vectors with orientation angle check.",
            mediaType = LessonMediaType.IMAGE_DIAGRAM,
            mediaUrl = "https://assets.signlink.app/lessons/numbers_diagram.png",
            ttsPronunciation = "Numbers one, two, three, four, five.",
            status = LessonStatus.DRAFT,
            lastUpdatedFormatted = "Nov 05, 2026",
            estimatedMinutes = 4
        )
    )

    override suspend fun fetchLessons(): List<LessonItem> = withContext(Dispatchers.IO) {
        lessonsList.toList()
    }

    override suspend fun getLessonById(lessonId: String): LessonItem? = withContext(Dispatchers.IO) {
        lessonsList.find { it.lessonId == lessonId }
    }

    override suspend fun saveLesson(lesson: LessonItem): LessonItem = withContext(Dispatchers.IO) {
        val now = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())
        val updatedLesson = lesson.copy(lastUpdatedFormatted = now)

        val existingIndex = lessonsList.indexOfFirst { it.lessonId == lesson.lessonId }
        if (existingIndex != -1) {
            lessonsList[existingIndex] = updatedLesson
        } else {
            lessonsList.add(0, updatedLesson)
        }
        updatedLesson
    }

    override suspend fun deleteLesson(lessonId: String): Boolean = withContext(Dispatchers.IO) {
        lessonsList.removeAll { it.lessonId == lessonId }
    }
}
