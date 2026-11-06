package com.example.system_mini_capstone.admin.maintenance

/**
 * Real-time server and host telemetry health metrics.
 */
data class ServerHealthTelemetry(
    val cpuUsagePercent: Float = 24.5f,
    val memoryUsedMb: Int = 1840,
    val memoryTotalMb: Int = 4096,
    val uptimeFormatted: String = "14d 6h 28m",
    val uptimePercent: Float = 99.98f,
    val databaseStatus: String = "HEALTHY",
    val databaseLatencyMs: Int = 12,
    val mlInferenceQueueDepth: Int = 3,
    val activeWebSocketConnections: Int = 142
)

enum class BackupType {
    MANUAL,
    SCHEDULED
}

enum class BackupStatus {
    COMPLETED,
    IN_PROGRESS,
    FAILED
}

/**
 * Metadata record for a system backup file securing app data and lesson repositories.
 */
data class SystemBackup(
    val id: String,
    val label: String,
    val createdAtFormatted: String,
    val timestampMs: Long,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val checksumSha256: String,
    val type: BackupType,
    val status: BackupStatus = BackupStatus.COMPLETED,
    val lessonsCount: Int = 9,
    val userRecordsCount: Int = 128
)

/**
 * Point-in-time system state snapshot for rollback and disaster recovery.
 */
data class SystemSnapshot(
    val snapshotId: String,
    val title: String,
    val timestampFormatted: String,
    val timestampMs: Long,
    val configRevision: String,
    val lessonsCount: Int,
    val isVerified: Boolean = true,
    val description: String
)

enum class BackupScheduleInterval(val label: String) {
    DAILY("Daily at 02:00 AM UTC"),
    WEEKLY("Weekly on Sundays"),
    MONTHLY("Monthly on 1st"),
    DISABLED("Disabled (Manual Only)")
}

/**
 * Automated backup scheduler configuration.
 */
data class BackupScheduleConfig(
    val interval: BackupScheduleInterval = BackupScheduleInterval.DAILY,
    val backupLessons: Boolean = true,
    val backupUserProfiles: Boolean = true,
    val backupTelemetry: Boolean = false,
    val cloudMirroringEnabled: Boolean = true
)
