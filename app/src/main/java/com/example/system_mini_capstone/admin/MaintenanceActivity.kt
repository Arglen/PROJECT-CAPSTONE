package com.example.system_mini_capstone.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.system_mini_capstone.admin.maintenance.BackupScheduleInterval
import com.example.system_mini_capstone.admin.maintenance.MaintenanceUiState
import com.example.system_mini_capstone.admin.maintenance.MaintenanceViewModel
import com.example.system_mini_capstone.admin.maintenance.ServerHealthTelemetry
import com.example.system_mini_capstone.admin.maintenance.SystemBackup
import com.example.system_mini_capstone.admin.maintenance.SystemSnapshot
import com.example.system_mini_capstone.ui.theme.System_mini_capstoneTheme

/**
 * Administrative support and maintenance activity for server health monitoring,
 * scheduled/manual data backups, and point-in-time snapshot disaster recovery.
 */
class MaintenanceActivity : ComponentActivity() {

    private val viewModel: MaintenanceViewModel by viewModels()

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

                MaintenanceScreen(
                    uiState = uiState,
                    snackbarHostState = snackbarHostState,
                    onSelectSection = viewModel::selectSection,
                    onRefreshTelemetry = viewModel::refreshTelemetry,
                    onOpenBackupDialog = viewModel::openCreateBackupDialog,
                    onDismissBackupDialog = viewModel::dismissCreateBackupDialog,
                    onCreateBackup = viewModel::triggerManualBackup,
                    onUpdateSchedule = viewModel::updateScheduleInterval,
                    onPromptRestore = viewModel::promptSnapshotRestore,
                    onDismissRestore = viewModel::dismissSnapshotRestore,
                    onConfirmRestore = viewModel::executeSnapshotRecovery,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen(
    uiState: MaintenanceUiState,
    snackbarHostState: SnackbarHostState,
    onSelectSection: (Int) -> Unit,
    onRefreshTelemetry: () -> Unit,
    onOpenBackupDialog: () -> Unit,
    onDismissBackupDialog: () -> Unit,
    onCreateBackup: (String) -> Unit,
    onUpdateSchedule: (BackupScheduleInterval) -> Unit,
    onPromptRestore: (SystemSnapshot) -> Unit,
    onDismissRestore: () -> Unit,
    onConfirmRestore: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var backupLabelInput by remember { mutableStateOf("") }

    // Dialog for creating a new manual backup
    if (uiState.showCreateBackupDialog) {
        AlertDialog(
            onDismissRequest = onDismissBackupDialog,
            title = { Text("Create System Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Capture an encrypted snapshot of user records, lesson repositories, and system vision configs.")
                    OutlinedTextField(
                        value = backupLabelInput,
                        onValueChange = { backupLabelInput = it },
                        label = { Text("Backup Label") },
                        placeholder = { Text("e.g. Pre-Patch Checkpoint") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateBackup(backupLabelInput)
                        backupLabelInput = ""
                    }
                ) {
                    Text("Start Backup")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissBackupDialog) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog for confirming point-in-time snapshot recovery
    if (uiState.selectedSnapshotForRestore != null) {
        val target = uiState.selectedSnapshotForRestore
        AlertDialog(
            onDismissRequest = onDismissRestore,
            title = { Text("Confirm Point-in-Time Rollback") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you sure you want to restore system state to '${target.title}' (${target.configRevision})?",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "This will rollback the lesson dictionary to ${target.lessonsCount} lessons and revert vision thresholds to the snapshot checkpoint.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirmRestore,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Rollback System")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissRestore) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Support & Maintenance", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRefreshTelemetry,
                        enabled = !uiState.isRefreshingTelemetry
                    ) {
                        if (uiState.isRefreshingTelemetry) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("🔄", fontSize = 20.sp)
                        }
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
                selectedTabIndex = uiState.activeSection,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = uiState.activeSection == 0,
                    onClick = { onSelectSection(0) },
                    text = { Text("🩺 Health") }
                )
                Tab(
                    selected = uiState.activeSection == 1,
                    onClick = { onSelectSection(1) },
                    text = { Text("💾 Backups") }
                )
                Tab(
                    selected = uiState.activeSection == 2,
                    onClick = { onSelectSection(2) },
                    text = { Text("⏱️ Recovery") }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (uiState.activeSection) {
                    0 -> {
                        item {
                            ServerHealthSection(
                                telemetry = uiState.telemetry,
                                isRefreshing = uiState.isRefreshingTelemetry,
                                onRefresh = onRefreshTelemetry
                            )
                        }
                    }
                    1 -> {
                        item {
                            BackupTriggersSection(
                                schedule = uiState.scheduleConfig,
                                isCreating = uiState.isCreatingBackup,
                                onOpenCreate = onOpenBackupDialog,
                                onUpdateSchedule = onUpdateSchedule
                            )
                        }
                        item {
                            Text(
                                text = "Secure System Backups Archive (${uiState.backups.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(uiState.backups, key = { it.id }) { backup ->
                            BackupItemCard(backup = backup)
                        }
                    }
                    2 -> {
                        item {
                            SnapshotRecoveryHeader(isRestoring = uiState.isRestoringSnapshot)
                        }
                        items(uiState.snapshots, key = { it.snapshotId }) { snapshot ->
                            SnapshotRecoveryCard(
                                snapshot = snapshot,
                                onRestoreClick = { onPromptRestore(snapshot) }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// SECTION 1: SERVER HEALTH MONITORING
// -----------------------------------------------------------------------------------------
@Composable
fun ServerHealthSection(
    telemetry: ServerHealthTelemetry,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Overall Uptime & Health Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.size(14.dp)
                ) {}

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "System Online & Operational",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Uptime: ${telemetry.uptimeFormatted}  •  ${telemetry.uptimePercent}% Reliability SLA",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(onClick = onRefresh, enabled = !isRefreshing) {
                    Text(if (isRefreshing) "Polling..." else "Poll Live")
                }
            }
        }

        // Metrics Grid: CPU Usage & Memory Load
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("CPU Load", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${telemetry.cpuUsagePercent}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (telemetry.cpuUsagePercent > 70f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { telemetry.cpuUsagePercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = if (telemetry.cpuUsagePercent > 70f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                val memPercent = telemetry.memoryUsedMb.toFloat() / telemetry.memoryTotalMb.toFloat()
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("RAM Allocation", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${telemetry.memoryUsedMb} MB",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { memPercent },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Telemetry Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Backend Infrastructure Telemetry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                TelemetryRow(label = "Database Engine Status", value = telemetry.databaseStatus)
                TelemetryRow(label = "Query Round-trip Latency", value = "${telemetry.databaseLatencyMs} ms")
                TelemetryRow(label = "ML Inference Queue Depth", value = "${telemetry.mlInferenceQueueDepth} tasks pending")
                TelemetryRow(label = "Active WebSocket Sessions", value = "${telemetry.activeWebSocketConnections} live streams")
            }
        }
    }
}

@Composable
fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

// -----------------------------------------------------------------------------------------
// SECTION 2: SYSTEM BACKUP TRIGGERS
// -----------------------------------------------------------------------------------------
@Composable
fun BackupTriggersSection(
    schedule: com.example.system_mini_capstone.admin.maintenance.BackupScheduleConfig,
    isCreating: Boolean,
    onOpenCreate: () -> Unit,
    onUpdateSchedule: (BackupScheduleInterval) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Manual Snapshot Trigger", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Secure current lesson repositories and user records into an encrypted archive.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Button(
                onClick = onOpenCreate,
                enabled = !isCreating,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text("Create Backup Now")
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text("Automated Backup Schedule", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BackupScheduleInterval.entries.forEach { interval ->
                    FilterChip(
                        selected = schedule.interval == interval,
                        onClick = { onUpdateSchedule(interval) },
                        label = { Text(interval.label) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun BackupItemCard(backup: SystemBackup) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = backup.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(text = "${backup.createdAtFormatted}  •  ${backup.sizeFormatted}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = backup.type.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "SHA-256: ${backup.checksumSha256.take(24)}...",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// SECTION 3: POINT-IN-TIME SNAPSHOT RECOVERY
// -----------------------------------------------------------------------------------------
@Composable
fun SnapshotRecoveryHeader(isRestoring: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Disaster Recovery & Point-in-Time Rollback",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Restore system states, lesson dictionaries, and computer vision thresholds back to verified historical checkpoints if a corrupt release or model failure occurs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            if (isRestoring) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Reverting system state and verifying integrity...", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SnapshotRecoveryCard(
    snapshot: SystemSnapshot,
    onRestoreClick: () -> Unit
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
                    Text(text = snapshot.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(text = "Snapshot: ${snapshot.snapshotId}  •  ${snapshot.timestampFormatted}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = snapshot.configRevision,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Text(
                text = snapshot.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Contains: ${snapshot.lessonsCount} lessons  •  Verified Checksum ✓",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF2E7D32)
                )

                OutlinedButton(
                    onClick = onRestoreClick,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Rollback to this point")
                }
            }
        }
    }
}
