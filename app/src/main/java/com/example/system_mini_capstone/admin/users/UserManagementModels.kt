package com.example.system_mini_capstone.admin.users

enum class AccountRole(val label: String) {
    ADMIN("Administrator"),
    CONTENT_MODERATOR("Content Moderator"),
    CONTRIBUTOR("Verified Contributor"),
    STANDARD_USER("Standard User")
}

enum class AccountStatus(val label: String) {
    ACTIVE("Active"),
    PENDING_VERIFICATION("Pending"),
    SUSPENDED("Suspended")
}

enum class SystemPermission(val permissionKey: String, val label: String, val description: String) {
    MANAGE_USERS("perm_manage_users", "Manage Accounts & Roles", "Assign RBAC roles, edit profiles, and revoke sessions."),
    APPROVE_SUBMISSIONS("perm_approve_submissions", "Content Review & Publishing", "Approve or reject submitted signs with structured feedback."),
    MODIFY_SYSTEM_CONFIG("perm_system_config", "System Configuration", "Modify camera vision sensitivity and API endpoints."),
    EXPORT_REPORTS("perm_export_reports", "Analytical Audits & Reports", "Generate and export administrative usage PDFs."),
    TRIGGER_BACKUPS("perm_trigger_backups", "Backup & Disaster Recovery", "Initiate system backups and point-in-time rollbacks.")
}

/**
 * Administrative user profile with RBAC roles and granular system permissions.
 */
data class AdminUserAccount(
    val userId: String,
    val fullName: String,
    val email: String,
    val department: String,
    val role: AccountRole,
    val status: AccountStatus = AccountStatus.ACTIVE,
    val permissions: Set<SystemPermission> = emptySet(),
    val lastActiveFormatted: String = "Just now"
)

enum class SubmissionStatus(val label: String) {
    PENDING("Pending Review"),
    APPROVED("Approved & Published"),
    REJECTED("Rejected")
}

/**
 * Community or contributor sign language content submission awaiting administrative moderation.
 */
data class ContentSubmission(
    val submissionId: String,
    val signTitle: String,
    val category: String,
    val submittedByEmail: String,
    val submittedByName: String,
    val submittedDateFormatted: String,
    val landmarkFramesCount: Int,
    val detectionConfidenceScore: Float,
    val description: String,
    val status: SubmissionStatus = SubmissionStatus.PENDING,
    val adminFeedback: String? = null,
    val reviewedByEmail: String? = null,
    val reviewedDateFormatted: String? = null
)
