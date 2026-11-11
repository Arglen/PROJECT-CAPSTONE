package com.example.system_mini_capstone.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.system_mini_capstone.admin.users.AccountRole
import com.example.system_mini_capstone.admin.users.AccountStatus
import com.example.system_mini_capstone.admin.users.AdminUserAccount
import com.example.system_mini_capstone.admin.users.ContentSubmission
import com.example.system_mini_capstone.admin.users.SubmissionStatus
import com.example.system_mini_capstone.admin.users.SystemPermission
import com.example.system_mini_capstone.admin.users.UserManagementUiState
import com.example.system_mini_capstone.admin.users.UserManagementViewModel
import com.example.system_mini_capstone.ui.theme.System_mini_capstoneTheme

/**
 * Administrative console for managing user profiles, assigning RBAC permissions,
 * and reviewing sign language content submissions with approval/rejection feedback queues.
 */
class UserManagementActivity : ComponentActivity() {

    private val viewModel: UserManagementViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            System_mini_capstoneTheme {
                val uiState by viewModel.uiState.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(uiState.statusMessage) {
                    uiState.statusMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.dismissStatus()
                    }
                }

                UserManagementScreen(
                    uiState = uiState,
                    snackbarHostState = snackbarHostState,
                    onSelectTab = viewModel::selectTab,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onRoleFilterChanged = viewModel::onRoleFilterChanged,
                    onOpenEditUser = viewModel::openEditUserDialog,
                    onDismissEditUser = viewModel::dismissEditUserDialog,
                    onUpdateUserRole = viewModel::updateEditingUserRole,
                    onToggleUserPermission = viewModel::toggleEditingUserPermission,
                    onUpdateUserStatus = viewModel::updateEditingUserStatus,
                    onSaveUserChanges = viewModel::saveUserChanges,
                    onRevokeSession = viewModel::revokeSession,
                    onSubmissionFilterChanged = viewModel::onSubmissionFilterChanged,
                    onApproveSubmission = viewModel::approveSubmission,
                    onOpenRejectDialog = viewModel::openRejectSubmissionDialog,
                    onDismissRejectDialog = viewModel::dismissRejectSubmissionDialog,
                    onConfirmReject = viewModel::confirmRejectSubmission,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    uiState: UserManagementUiState,
    snackbarHostState: SnackbarHostState,
    onSelectTab: (Int) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onRoleFilterChanged: (AccountRole?) -> Unit,
    onOpenEditUser: (AdminUserAccount) -> Unit,
    onDismissEditUser: () -> Unit,
    onUpdateUserRole: (AccountRole) -> Unit,
    onToggleUserPermission: (SystemPermission) -> Unit,
    onUpdateUserStatus: (AccountStatus) -> Unit,
    onSaveUserChanges: () -> Unit,
    onRevokeSession: (String) -> Unit,
    onSubmissionFilterChanged: (SubmissionStatus?) -> Unit,
    onApproveSubmission: (ContentSubmission, String?) -> Unit,
    onOpenRejectDialog: (ContentSubmission) -> Unit,
    onDismissRejectDialog: () -> Unit,
    onConfirmReject: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var rejectionFeedbackInput by remember { mutableStateOf("") }

    // Dialog for editing user profile and assigning RBAC permissions
    if (uiState.editingUser != null) {
        val user = uiState.editingUser
        AlertDialog(
            onDismissRequest = onDismissEditUser,
            title = { Text("Edit Role & Permissions") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "User: ${user.fullName} (${user.email})", fontWeight = FontWeight.Bold)

                    Text("Access Role Assignment", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AccountRole.entries.forEach { role ->
                            FilterChip(
                                selected = user.role == role,
                                onClick = { onUpdateUserRole(role) },
                                label = { Text(role.label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Text("Granular System Permissions", style = MaterialTheme.typography.labelMedium)
                    SystemPermission.entries.forEach { perm ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = user.permissions.contains(perm),
                                onCheckedChange = { onToggleUserPermission(perm) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = perm.label, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Text("Account Status", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AccountStatus.entries.forEach { status ->
                            FilterChip(
                                selected = user.status == status,
                                onClick = { onUpdateUserStatus(status) },
                                label = { Text(status.label, fontSize = 11.sp) }
                            )
                        }
                    }

                    TextButton(
                        onClick = { onRevokeSession(user.userId) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Revoke Active Session Token")
                    }
                }
            },
            confirmButton = {
                Button(onClick = onSaveUserChanges) {
                    Text("Save Permissions")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissEditUser) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog for rejecting a content submission with mandatory structured feedback
    if (uiState.rejectingSubmission != null) {
        val submission = uiState.rejectingSubmission
        AlertDialog(
            onDismissRequest = onDismissRejectDialog,
            title = { Text("Reject Submission with Feedback") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Sign: ${submission.signTitle}",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Provide structured feedback to ${submission.submittedByName} explaining calibration or landmark issues:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = rejectionFeedbackInput,
                        onValueChange = { rejectionFeedbackInput = it },
                        label = { Text("Rejection Feedback") },
                        placeholder = { Text("e.g. Hand partially out of camera frame; please re-record.") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rejectionFeedbackInput.isNotBlank()) {
                            onConfirmReject(rejectionFeedbackInput.trim())
                            rejectionFeedbackInput = ""
                        }
                    },
                    enabled = rejectionFeedbackInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Rejection")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissRejectDialog) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Admin Account & Review", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = uiState.activeTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = uiState.activeTab == 0,
                    onClick = { onSelectTab(0) },
                    text = { Text("👥 Accounts & RBAC") }
                )
                Tab(
                    selected = uiState.activeTab == 1,
                    onClick = { onSelectTab(1) },
                    text = {
                        val pendingCount = uiState.submissions.count { it.status == SubmissionStatus.PENDING }
                        Text(if (pendingCount > 0) "📥 Review Queue ($pendingCount)" else "📥 Review Queue")
                    }
                )
            }

            when (uiState.activeTab) {
                0 -> {
                    UserAccountsTabContent(
                        users = uiState.filteredUsers,
                        searchQuery = uiState.searchQuery,
                        selectedRole = uiState.selectedRoleFilter,
                        onSearchChange = onSearchQueryChanged,
                        onRoleFilterChange = onRoleFilterChanged,
                        onOpenEditUser = onOpenEditUser
                    )
                }
                1 -> {
                    SubmissionsQueueTabContent(
                        submissions = uiState.filteredSubmissions,
                        selectedFilter = uiState.selectedSubmissionFilter,
                        onFilterChange = onSubmissionFilterChanged,
                        onApprove = onApproveSubmission,
                        onOpenReject = onOpenRejectDialog
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// TAB 1: USER ACCOUNTS & RBAC
// -----------------------------------------------------------------------------------------
@Composable
fun UserAccountsTabContent(
    users: List<AdminUserAccount>,
    searchQuery: String,
    selectedRole: AccountRole?,
    onSearchChange: (String) -> Unit,
    onRoleFilterChange: (AccountRole?) -> Unit,
    onOpenEditUser: (AdminUserAccount) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                label = { Text("Search users by name, email, department...") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedRole == null,
                    onClick = { onRoleFilterChange(null) },
                    label = { Text("All Accounts") }
                )
                AccountRole.entries.forEach { role ->
                    FilterChip(
                        selected = selectedRole == role,
                        onClick = { onRoleFilterChange(role) },
                        label = { Text(role.label) }
                    )
                }
            }
        }

        items(users, key = { it.userId }) { user ->
            UserAccountCard(user = user, onEditClick = { onOpenEditUser(user) })
        }
    }
}

@Composable
fun UserAccountCard(
    user: AdminUserAccount,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = CircleShape,
                        color = when (user.role) {
                            AccountRole.ADMIN -> MaterialTheme.colorScheme.errorContainer
                            AccountRole.CONTENT_MODERATOR -> MaterialTheme.colorScheme.tertiaryContainer
                            AccountRole.CONTRIBUTOR -> MaterialTheme.colorScheme.primaryContainer
                            AccountRole.STANDARD_USER -> MaterialTheme.colorScheme.secondaryContainer
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = user.fullName.take(1),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(text = user.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (user.role) {
                        AccountRole.ADMIN -> MaterialTheme.colorScheme.errorContainer
                        AccountRole.CONTENT_MODERATOR -> MaterialTheme.colorScheme.tertiaryContainer
                        AccountRole.CONTRIBUTOR -> MaterialTheme.colorScheme.primaryContainer
                        AccountRole.STANDARD_USER -> MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Text(
                        text = user.role.label,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "Department: ${user.department}  •  Last active: ${user.lastActiveFormatted}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${user.permissions.size} permissions granted",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                OutlinedButton(onClick = onEditClick) {
                    Text("Edit Role & Permissions")
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// TAB 2: SUBMISSIONS REVIEW QUEUE
// -----------------------------------------------------------------------------------------
@Composable
fun SubmissionsQueueTabContent(
    submissions: List<ContentSubmission>,
    selectedFilter: SubmissionStatus?,
    onFilterChange: (SubmissionStatus?) -> Unit,
    onApprove: (ContentSubmission, String?) -> Unit,
    onOpenReject: (ContentSubmission) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { onFilterChange(null) },
                    label = { Text("All Submissions") }
                )
                SubmissionStatus.entries.forEach { status ->
                    FilterChip(
                        selected = selectedFilter == status,
                        onClick = { onFilterChange(status) },
                        label = { Text(status.label) }
                    )
                }
            }
        }

        if (submissions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No submissions matching this filter.", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        items(submissions, key = { it.submissionId }) { submission ->
            SubmissionReviewCard(
                submission = submission,
                onApprove = { onApprove(submission, null) },
                onReject = { onOpenReject(submission) }
            )
        }
    }
}

@Composable
fun SubmissionReviewCard(
    submission: ContentSubmission,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = submission.signTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "Category: ${submission.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (submission.status) {
                        SubmissionStatus.PENDING -> MaterialTheme.colorScheme.tertiaryContainer
                        SubmissionStatus.APPROVED -> Color(0xFFE8F5E9)
                        SubmissionStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Text(
                        text = submission.status.label,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (submission.status) {
                            SubmissionStatus.PENDING -> MaterialTheme.colorScheme.onTertiaryContainer
                            SubmissionStatus.APPROVED -> Color(0xFF2E7D32)
                            SubmissionStatus.REJECTED -> MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }

            Text(
                text = "Submitted by ${submission.submittedByName} (${submission.submittedByEmail}) on ${submission.submittedDateFormatted}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = submission.description,
                style = MaterialTheme.typography.bodyMedium
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Landmark Frames: ${submission.landmarkFramesCount}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "Detection Confidence: ${(submission.detectionConfidenceScore * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Existing Review Feedback
            if (submission.adminFeedback != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (submission.status == SubmissionStatus.REJECTED)
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        else
                            Color(0xFFE8F5E9)
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (submission.status == SubmissionStatus.REJECTED) "Rejection Feedback:" else "Admin Notes:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = submission.adminFeedback, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Action Buttons for PENDING submissions
            if (submission.status == SubmissionStatus.PENDING) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reject with Feedback")
                    }

                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Approve & Publish")
                    }
                }
            }
        }
    }
}
