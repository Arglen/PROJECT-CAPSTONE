package com.example.system_mini_capstone.admin.users

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface UserManagementRepository {
    suspend fun fetchUsers(): List<AdminUserAccount>
    suspend fun updateUser(updated: AdminUserAccount)
    suspend fun revokeUserSession(userId: String)

    suspend fun fetchSubmissions(): List<ContentSubmission>
    suspend fun approveSubmission(submissionId: String, adminEmail: String, feedback: String?): Result<ContentSubmission>
    suspend fun rejectSubmission(submissionId: String, adminEmail: String, feedback: String): Result<ContentSubmission>
}

class UserManagementRepositoryImpl : UserManagementRepository {

    private val usersList = mutableListOf(
        AdminUserAccount(
            userId = "USR-001",
            fullName = "Alex Mercer",
            email = "admin@signlink.app",
            department = "System Architecture & Security",
            role = AccountRole.ADMIN,
            status = AccountStatus.ACTIVE,
            permissions = SystemPermission.entries.toSet(),
            lastActiveFormatted = "Active now"
        ),
        AdminUserAccount(
            userId = "USR-002",
            fullName = "Elena Rostova",
            email = "elena.moderator@signlink.app",
            department = "Curriculum & Linguistics",
            role = AccountRole.CONTENT_MODERATOR,
            status = AccountStatus.ACTIVE,
            permissions = setOf(
                SystemPermission.APPROVE_SUBMISSIONS,
                SystemPermission.EXPORT_REPORTS
            ),
            lastActiveFormatted = "18 mins ago"
        ),
        AdminUserAccount(
            userId = "USR-003",
            fullName = "Marcus Vance",
            email = "marcus.contributor@signlink.app",
            department = "ASL Community Contributor",
            role = AccountRole.CONTRIBUTOR,
            status = AccountStatus.ACTIVE,
            permissions = emptySet(),
            lastActiveFormatted = "2 hours ago"
        ),
        AdminUserAccount(
            userId = "USR-004",
            fullName = "Jordan Reed",
            email = "jordan.user@signlink.app",
            department = "General Public",
            role = AccountRole.STANDARD_USER,
            status = AccountStatus.ACTIVE,
            permissions = emptySet(),
            lastActiveFormatted = "Yesterday"
        ),
        AdminUserAccount(
            userId = "USR-005",
            fullName = "Suspended Test Account",
            email = "flagged.user@signlink.app",
            department = "Audit Review",
            role = AccountRole.STANDARD_USER,
            status = AccountStatus.SUSPENDED,
            permissions = emptySet(),
            lastActiveFormatted = "3 days ago"
        )
    )

    private val submissionsList = mutableListOf(
        ContentSubmission(
            submissionId = "SUB-202611-001",
            signTitle = "Medical Sign: Need Insulin Injection",
            category = "Healthcare & Emergency",
            submittedByEmail = "marcus.contributor@signlink.app",
            submittedByName = "Marcus Vance",
            submittedDateFormatted = "Nov 05, 2026 14:20",
            landmarkFramesCount = 64,
            detectionConfidenceScore = 0.94f,
            description = "Specialized medical gesture indicating insulin requirement for diabetic emergencies with dominant hand miming injection on forearm.",
            status = SubmissionStatus.PENDING
        ),
        ContentSubmission(
            submissionId = "SUB-202611-002",
            signTitle = "Greeting: Good Morning (Regional Dialect)",
            category = "Phrases & Greetings",
            submittedByEmail = "sarah.lingo@signlink.app",
            submittedByName = "Sarah Jenkins",
            submittedDateFormatted = "Nov 05, 2026 10:15",
            landmarkFramesCount = 52,
            detectionConfidenceScore = 0.91f,
            description = "Mid-Atlantic regional ASL variation combining chin-touch 'good' smoothly into sun-rise motion.",
            status = SubmissionStatus.PENDING
        ),
        ContentSubmission(
            submissionId = "SUB-202611-003",
            signTitle = "Alphabet: Two-Handed Letter P",
            category = "Alphabet",
            submittedByEmail = "david.asl@signlink.app",
            submittedByName = "David Chen",
            submittedDateFormatted = "Nov 04, 2026 19:45",
            landmarkFramesCount = 38,
            detectionConfidenceScore = 0.88f,
            description = "Two-handed British Sign Language (BSL) letter P representation for cross-lingual comparison module.",
            status = SubmissionStatus.PENDING
        ),
        ContentSubmission(
            submissionId = "SUB-202611-004",
            signTitle = "Emergency: Call Fire Department",
            category = "Healthcare & Emergency",
            submittedByEmail = "marcus.contributor@signlink.app",
            submittedByName = "Marcus Vance",
            submittedDateFormatted = "Nov 03, 2026 11:00",
            landmarkFramesCount = 58,
            detectionConfidenceScore = 0.96f,
            description = "Universal emergency symbol miming fire helmet and phone dispatch call.",
            status = SubmissionStatus.APPROVED,
            adminFeedback = "Excellent landmark tracking stability. Approved for core lesson library.",
            reviewedByEmail = "admin@signlink.app",
            reviewedDateFormatted = "Nov 03, 2026 16:30"
        ),
        ContentSubmission(
            submissionId = "SUB-202611-005",
            signTitle = "Informal: High Five Wave",
            category = "Informal Gestures",
            submittedByEmail = "jordan.user@signlink.app",
            submittedByName = "Jordan Reed",
            submittedDateFormatted = "Nov 02, 2026 08:30",
            landmarkFramesCount = 30,
            detectionConfidenceScore = 0.62f,
            description = "Casual wave gesture.",
            status = SubmissionStatus.REJECTED,
            adminFeedback = "Motion is too ambiguous and overlaps with standard 'Hello'. Please calibrate trajectory.",
            reviewedByEmail = "elena.moderator@signlink.app",
            reviewedDateFormatted = "Nov 02, 2026 14:10"
        )
    )

    override suspend fun fetchUsers(): List<AdminUserAccount> = withContext(Dispatchers.IO) {
        usersList.toList()
    }

    override suspend fun updateUser(updated: AdminUserAccount) = withContext(Dispatchers.IO) {
        val index = usersList.indexOfFirst { it.userId == updated.userId }
        if (index != -1) {
            usersList[index] = updated
        }
    }

    override suspend fun revokeUserSession(userId: String) = withContext(Dispatchers.IO) {
        val index = usersList.indexOfFirst { it.userId == userId }
        if (index != -1) {
            usersList[index] = usersList[index].copy(lastActiveFormatted = "Session Revoked")
        }
    }

    override suspend fun fetchSubmissions(): List<ContentSubmission> = withContext(Dispatchers.IO) {
        submissionsList.toList()
    }

    override suspend fun approveSubmission(
        submissionId: String,
        adminEmail: String,
        feedback: String?
    ): Result<ContentSubmission> = withContext(Dispatchers.IO) {
        val index = submissionsList.indexOfFirst { it.submissionId == submissionId }
        if (index != -1) {
            val date = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date())
            val updated = submissionsList[index].copy(
                status = SubmissionStatus.APPROVED,
                adminFeedback = feedback ?: "Approved by administrator for production lesson repository.",
                reviewedByEmail = adminEmail,
                reviewedDateFormatted = date
            )
            submissionsList[index] = updated
            Result.success(updated)
        } else {
            Result.failure(Exception("Submission $submissionId not found"))
        }
    }

    override suspend fun rejectSubmission(
        submissionId: String,
        adminEmail: String,
        feedback: String
    ): Result<ContentSubmission> = withContext(Dispatchers.IO) {
        val index = submissionsList.indexOfFirst { it.submissionId == submissionId }
        if (index != -1) {
            val date = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date())
            val updated = submissionsList[index].copy(
                status = SubmissionStatus.REJECTED,
                adminFeedback = feedback,
                reviewedByEmail = adminEmail,
                reviewedDateFormatted = date
            )
            submissionsList[index] = updated
            Result.success(updated)
        } else {
            Result.failure(Exception("Submission $submissionId not found"))
        }
    }
}
