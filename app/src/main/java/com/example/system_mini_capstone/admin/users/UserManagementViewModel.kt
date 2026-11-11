package com.example.system_mini_capstone.admin.users

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.system_mini_capstone.data.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserManagementUiState(
    val users: List<AdminUserAccount> = emptyList(),
    val filteredUsers: List<AdminUserAccount> = emptyList(),
    val searchQuery: String = "",
    val selectedRoleFilter: AccountRole? = null,
    val editingUser: AdminUserAccount? = null,
    val submissions: List<ContentSubmission> = emptyList(),
    val filteredSubmissions: List<ContentSubmission> = emptyList(),
    val selectedSubmissionFilter: SubmissionStatus? = SubmissionStatus.PENDING,
    val rejectingSubmission: ContentSubmission? = null,
    val activeTab: Int = 0, // 0 = Accounts, 1 = Submissions
    val statusMessage: String? = null
)

class UserManagementViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: UserManagementRepository = UserManagementRepositoryImpl()
) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow(UserManagementUiState())
    val uiState: StateFlow<UserManagementUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val users = repository.fetchUsers()
            val submissions = repository.fetchSubmissions()

            _uiState.update {
                it.copy(
                    users = users,
                    filteredUsers = applyUserFilters(users, it.searchQuery, it.selectedRoleFilter),
                    submissions = submissions,
                    filteredSubmissions = applySubmissionFilters(submissions, it.selectedSubmissionFilter)
                )
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(activeTab = tabIndex) }
    }

    // --- USER MANAGEMENT ---
    fun onSearchQueryChanged(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                filteredUsers = applyUserFilters(it.users, query, it.selectedRoleFilter)
            )
        }
    }

    fun onRoleFilterChanged(role: AccountRole?) {
        _uiState.update {
            it.copy(
                selectedRoleFilter = role,
                filteredUsers = applyUserFilters(it.users, it.searchQuery, role)
            )
        }
    }

    fun openEditUserDialog(user: AdminUserAccount) {
        _uiState.update { it.copy(editingUser = user) }
    }

    fun dismissEditUserDialog() {
        _uiState.update { it.copy(editingUser = null) }
    }

    fun updateEditingUserRole(newRole: AccountRole) {
        val current = _uiState.value.editingUser ?: return
        // Automatically default permissions based on role tier
        val defaultPerms = when (newRole) {
            AccountRole.ADMIN -> SystemPermission.entries.toSet()
            AccountRole.CONTENT_MODERATOR -> setOf(SystemPermission.APPROVE_SUBMISSIONS, SystemPermission.EXPORT_REPORTS)
            AccountRole.CONTRIBUTOR -> emptySet()
            AccountRole.STANDARD_USER -> emptySet()
        }
        _uiState.update {
            it.copy(editingUser = current.copy(role = newRole, permissions = defaultPerms))
        }
    }

    fun toggleEditingUserPermission(permission: SystemPermission) {
        val current = _uiState.value.editingUser ?: return
        val updatedPerms = current.permissions.toMutableSet()
        if (updatedPerms.contains(permission)) {
            updatedPerms.remove(permission)
        } else {
            updatedPerms.add(permission)
        }
        _uiState.update {
            it.copy(editingUser = current.copy(permissions = updatedPerms))
        }
    }

    fun updateEditingUserStatus(newStatus: AccountStatus) {
        val current = _uiState.value.editingUser ?: return
        _uiState.update {
            it.copy(editingUser = current.copy(status = newStatus))
        }
    }

    fun saveUserChanges() {
        val updated = _uiState.value.editingUser ?: return
        viewModelScope.launch {
            repository.updateUser(updated)
            val allUsers = repository.fetchUsers()
            _uiState.update {
                it.copy(
                    users = allUsers,
                    filteredUsers = applyUserFilters(allUsers, it.searchQuery, it.selectedRoleFilter),
                    editingUser = null,
                    statusMessage = "Updated account permissions for ${updated.fullName} (${updated.role.label})"
                )
            }
        }
    }

    fun revokeSession(userId: String) {
        viewModelScope.launch {
            repository.revokeUserSession(userId)
            val allUsers = repository.fetchUsers()
            _uiState.update {
                it.copy(
                    users = allUsers,
                    filteredUsers = applyUserFilters(allUsers, it.searchQuery, it.selectedRoleFilter),
                    editingUser = null,
                    statusMessage = "Active token session revoked for user."
                )
            }
        }
    }

    // --- SUBMISSION REVIEW QUEUES ---
    fun onSubmissionFilterChanged(status: SubmissionStatus?) {
        _uiState.update {
            it.copy(
                selectedSubmissionFilter = status,
                filteredSubmissions = applySubmissionFilters(it.submissions, status)
            )
        }
    }

    fun approveSubmission(submission: ContentSubmission, optionalFeedback: String? = null) {
        val adminEmail = sessionManager.getUserEmail() ?: "admin@signlink.app"
        viewModelScope.launch {
            val result = repository.approveSubmission(submission.submissionId, adminEmail, optionalFeedback)
            result.onSuccess {
                val updatedList = repository.fetchSubmissions()
                _uiState.update { state ->
                    state.copy(
                        submissions = updatedList,
                        filteredSubmissions = applySubmissionFilters(updatedList, state.selectedSubmissionFilter),
                        statusMessage = "Approved: '${submission.signTitle}' published to lesson library."
                    )
                }
            }
        }
    }

    fun openRejectSubmissionDialog(submission: ContentSubmission) {
        _uiState.update { it.copy(rejectingSubmission = submission) }
    }

    fun dismissRejectSubmissionDialog() {
        _uiState.update { it.copy(rejectingSubmission = null) }
    }

    fun confirmRejectSubmission(feedback: String) {
        val target = _uiState.value.rejectingSubmission ?: return
        val adminEmail = sessionManager.getUserEmail() ?: "admin@signlink.app"

        viewModelScope.launch {
            val result = repository.rejectSubmission(target.submissionId, adminEmail, feedback)
            result.onSuccess {
                val updatedList = repository.fetchSubmissions()
                _uiState.update { state ->
                    state.copy(
                        submissions = updatedList,
                        filteredSubmissions = applySubmissionFilters(updatedList, state.selectedSubmissionFilter),
                        rejectingSubmission = null,
                        statusMessage = "Submission rejected with feedback provided to submitter."
                    )
                }
            }
        }
    }

    fun dismissStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    private fun applyUserFilters(
        list: List<AdminUserAccount>,
        query: String,
        roleFilter: AccountRole?
    ): List<AdminUserAccount> {
        return list.filter { user ->
            val matchesQuery = query.isBlank() ||
                user.fullName.contains(query, ignoreCase = true) ||
                user.email.contains(query, ignoreCase = true) ||
                user.department.contains(query, ignoreCase = true)

            val matchesRole = roleFilter == null || user.role == roleFilter

            matchesQuery && matchesRole
        }
    }

    private fun applySubmissionFilters(
        list: List<ContentSubmission>,
        statusFilter: SubmissionStatus?
    ): List<ContentSubmission> {
        return if (statusFilter == null) {
            list
        } else {
            list.filter { it.status == statusFilter }
        }
    }
}
