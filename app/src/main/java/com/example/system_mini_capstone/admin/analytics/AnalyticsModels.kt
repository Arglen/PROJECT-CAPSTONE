package com.example.system_mini_capstone.admin.analytics

/**
 * Global application usage metrics and real-time telemetry stats.
 */
data class GlobalMetrics(
    val activeSessions: Int,
    val dailyActiveUsers: Int,
    val totalTranslationsToday: Int,
    val avgAccuracyScore: Float,
    val avgInferenceLatencyMs: Int,
    val totalHoursPracticed: Float
)

/**
 * Performance metrics for individual sign language lessons and content items.
 */
data class LessonPerformance(
    val lessonId: String,
    val signName: String,
    val category: String,
    val totalAttempts: Int,
    val successRate: Float,
    val completionRate: Float,
    val avgConfidence: Float,
    val isLowPerforming: Boolean
)

/**
 * Daily translation volume and session activity for chart visualization.
 */
data class DailyActivityStat(
    val dayOfWeek: String,
    val sessionCount: Int,
    val translationCount: Int
)
