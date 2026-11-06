package com.example.system_mini_capstone.admin.analytics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AnalyticsRepository {
    suspend fun queryGlobalMetrics(): GlobalMetrics
    suspend fun queryTopPerformingLessons(limit: Int = 5): List<LessonPerformance>
    suspend fun queryLowPerformingLessons(limit: Int = 5): List<LessonPerformance>
    suspend fun queryWeeklyActivity(): List<DailyActivityStat>
}

class AnalyticsRepositoryImpl : AnalyticsRepository {

    // Seeded dataset simulating backend database tables for content analytics
    private val contentDatabase = listOf(
        LessonPerformance(
            lessonId = "LSN-001",
            signName = "Hello / Open Palm",
            category = "Greetings",
            totalAttempts = 2480,
            successRate = 96.4f,
            completionRate = 94.2f,
            avgConfidence = 0.94f,
            isLowPerforming = false
        ),
        LessonPerformance(
            lessonId = "LSN-002",
            signName = "I Love You (ASL)",
            category = "Expressions",
            totalAttempts = 2190,
            successRate = 94.8f,
            completionRate = 92.0f,
            avgConfidence = 0.93f,
            isLowPerforming = false
        ),
        LessonPerformance(
            lessonId = "LSN-003",
            signName = "Thumbs Up / Good",
            category = "Feedback",
            totalAttempts = 1950,
            successRate = 93.5f,
            completionRate = 89.6f,
            avgConfidence = 0.91f,
            isLowPerforming = false
        ),
        LessonPerformance(
            lessonId = "LSN-004",
            signName = "Peace / Victory (V)",
            category = "Expressions",
            totalAttempts = 1820,
            successRate = 91.2f,
            completionRate = 88.4f,
            avgConfidence = 0.89f,
            isLowPerforming = false
        ),
        LessonPerformance(
            lessonId = "LSN-005",
            signName = "Alphabet: Letter A (Fist)",
            category = "Alphabet",
            totalAttempts = 1640,
            successRate = 88.7f,
            completionRate = 85.1f,
            avgConfidence = 0.87f,
            isLowPerforming = false
        ),
        LessonPerformance(
            lessonId = "LSN-006",
            signName = "Letter J (Dynamic Arc)",
            category = "Alphabet",
            totalAttempts = 890,
            successRate = 54.2f,
            completionRate = 51.0f,
            avgConfidence = 0.61f,
            isLowPerforming = true
        ),
        LessonPerformance(
            lessonId = "LSN-007",
            signName = "Alphabet: Letter X (Hook)",
            category = "Alphabet",
            totalAttempts = 760,
            successRate = 58.6f,
            completionRate = 55.4f,
            avgConfidence = 0.63f,
            isLowPerforming = true
        ),
        LessonPerformance(
            lessonId = "LSN-008",
            signName = "Phrase: Need Hospital",
            category = "Emergency",
            totalAttempts = 640,
            successRate = 61.3f,
            completionRate = 58.0f,
            avgConfidence = 0.66f,
            isLowPerforming = true
        ),
        LessonPerformance(
            lessonId = "LSN-009",
            signName = "Number 7 (Ring Finger)",
            category = "Numbers",
            totalAttempts = 580,
            successRate = 63.8f,
            completionRate = 60.2f,
            avgConfidence = 0.67f,
            isLowPerforming = true
        )
    )

    override suspend fun queryGlobalMetrics(): GlobalMetrics = withContext(Dispatchers.IO) {
        GlobalMetrics(
            activeSessions = 142,
            dailyActiveUsers = 1280,
            totalTranslationsToday = 8490,
            avgAccuracyScore = 91.8f,
            avgInferenceLatencyMs = 26,
            totalHoursPracticed = 345.5f
        )
    }

    override suspend fun queryTopPerformingLessons(limit: Int): List<LessonPerformance> =
        withContext(Dispatchers.IO) {
            contentDatabase
                .filter { !it.isLowPerforming }
                .sortedByDescending { it.successRate }
                .take(limit)
        }

    override suspend fun queryLowPerformingLessons(limit: Int): List<LessonPerformance> =
        withContext(Dispatchers.IO) {
            contentDatabase
                .filter { it.isLowPerforming }
                .sortedBy { it.successRate }
                .take(limit)
        }

    override suspend fun queryWeeklyActivity(): List<DailyActivityStat> =
        withContext(Dispatchers.IO) {
            listOf(
                DailyActivityStat("Mon", sessionCount = 890, translationCount = 4200),
                DailyActivityStat("Tue", sessionCount = 1040, translationCount = 5100),
                DailyActivityStat("Wed", sessionCount = 1120, translationCount = 5800),
                DailyActivityStat("Thu", sessionCount = 980, translationCount = 4900),
                DailyActivityStat("Fri", sessionCount = 1340, translationCount = 7200),
                DailyActivityStat("Sat", sessionCount = 1580, translationCount = 8600),
                DailyActivityStat("Sun", sessionCount = 1280, translationCount = 6800)
            )
        }
}
