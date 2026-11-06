package com.example.system_mini_capstone.admin.maintenance

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MaintenanceUiState(
    val telemetry: ServerHealthTelemetry = ServerHealthTelemetry(),
    val backups: List<SystemBackup> = emptyList(),
    val snapshots: List<SystemSnapshot> = emptyList(),
    val scheduleConfig: BackupScheduleConfig = BackupScheduleConfig(),
    val activeSection: Int = 0,
    val isCreatingBackup: Boolean = false,
    val isRestoringSnapshot: Boolean = false,
    val isRefreshingTelemetry: Boolean = false,
    val selectedSnapshotForRestore: SystemSnapshot? = null,
    val showCreateBackupDialog: Boolean = false,
    val statusMessage: String? = null
)

class MaintenanceViewModel(
    application: Application,
    private val repository: MaintenanceRepository = MaintenanceRepositoryImpl()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MaintenanceUiState())
    val uiState: StateFlow<MaintenanceUiState> = _uiState.asStateFlow()

    init {
        loadMaintenanceData()
    }

    fun loadMaintenanceData() {
        viewModelScope.launch {
            val telemetry = repository.fetchServerHealth()
            val backups = repository.fetchBackups()
            val snapshots = repository.fetchSnapshots()
            val schedule = repository.getSchedule()

            _uiState.update {
                it.copy(
                    telemetry = telemetry,
                    backups = backups,
                    snapshots = snapshots,
                    scheduleConfig = schedule
                )
            }
        }
    }

    fun selectSection(index: Int) {
        _uiState.update { it.copy(activeSection = index) }
    }

    fun refreshTelemetry() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingTelemetry = true) }
            val refreshed = repository.fetchServerHealth()
            _uiState.update {
                it.copy(
                    telemetry = refreshed,
                    isRefreshingTelemetry = false,
                    statusMessage = "Telemetry refreshed: CPU ${refreshed.cpuUsagePercent}%, Mem ${refreshed.memoryUsedMb}MB"
                )
            }
        }
    }

    fun openCreateBackupDialog() {
        _uiState.update { it.copy(showCreateBackupDialog = true) }
    }

    fun dismissCreateBackupDialog() {
        _uiState.update { it.copy(showCreateBackupDialog = false) }
    }

    fun triggerManualBackup(label: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCreatingBackup = true,
                    showCreateBackupDialog = false
                )
            }
            try {
                val newBackup = repository.createBackup(label, BackupType.MANUAL)
                val updatedList = repository.fetchBackups()
                _uiState.update {
                    it.copy(
                        isCreatingBackup = false,
                        backups = updatedList,
                        statusMessage = "System backup '${newBackup.label}' completed successfully (${newBackup.sizeFormatted})!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCreatingBackup = false,
                        statusMessage = "Backup creation failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun updateScheduleInterval(interval: BackupScheduleInterval) {
        viewModelScope.launch {
            val updated = _uiState.value.scheduleConfig.copy(interval = interval)
            repository.updateSchedule(updated)
            _uiState.update {
                it.copy(
                    scheduleConfig = updated,
                    statusMessage = "Backup schedule updated to: ${interval.label}"
                )
            }
        }
    }

    fun promptSnapshotRestore(snapshot: SystemSnapshot) {
        _uiState.update { it.copy(selectedSnapshotForRestore = snapshot) }
    }

    fun dismissSnapshotRestore() {
        _uiState.update { it.copy(selectedSnapshotForRestore = null) }
    }

    fun executeSnapshotRecovery() {
        val target = _uiState.value.selectedSnapshotForRestore ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRestoringSnapshot = true,
                    selectedSnapshotForRestore = null
                )
            }

            val result = repository.restoreSnapshot(target.snapshotId)

            result.onSuccess { msg ->
                val refreshedSnapshots = repository.fetchSnapshots()
                _uiState.update {
                    it.copy(
                        isRestoringSnapshot = false,
                        snapshots = refreshedSnapshots,
                        statusMessage = msg
                    )
                }
            }.onFailure { ex ->
                _uiState.update {
                    it.copy(
                        isRestoringSnapshot = false,
                        statusMessage = "Recovery failed: ${ex.localizedMessage}"
                    )
                }
            }
        }
    }

    fun dismissStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
