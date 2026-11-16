package com.example.system_mini_capstone

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.system_mini_capstone.data.network.UserRole
import com.example.system_mini_capstone.ui.DashboardFeatureItem
import com.example.system_mini_capstone.ui.DashboardNavTab
import com.example.system_mini_capstone.ui.DashboardUiState
import com.example.system_mini_capstone.ui.DashboardViewModel
import com.example.system_mini_capstone.ui.theme.System_mini_capstoneTheme

/**
 * Central Dashboard activity serving as the main navigation hub.
 * Features dynamic Role-Based Access Control (RBAC) to display administrative
 * tools or standard user navigation panels according to verified login credentials.
 */
class DashboardActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            System_mini_capstoneTheme {
                val uiState by viewModel.uiState.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(uiState.infoNotice) {
                    uiState.infoNotice?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.dismissNotice()
                    }
                }

                DashboardScreen(
                    uiState = uiState,
                    snackbarHostState = snackbarHostState,
                    onSelectTab = { tab ->
                        viewModel.selectTab(tab)
                        if (tab == DashboardNavTab.ADMIN_PANEL) {
                            startActivity(Intent(this@DashboardActivity, com.example.system_mini_capstone.admin.AdminAnalyticsActivity::class.java))
                        }
                    },
                    onFeatureClick = { feature ->
                        when (feature.id) {
                            "realtime_translator" -> {
                                startActivity(Intent(this@DashboardActivity, com.example.system_mini_capstone.translator.TranslatorActivity::class.java))
                            }
                            "admin_model_telemetry" -> {
                                startActivity(Intent(this@DashboardActivity, com.example.system_mini_capstone.admin.AdminAnalyticsActivity::class.java))
                            }
                            "admin_maintenance" -> {
                                startActivity(Intent(this@DashboardActivity, com.example.system_mini_capstone.admin.MaintenanceActivity::class.java))
                            }
                            "admin_user_mgmt" -> {
                                startActivity(Intent(this@DashboardActivity, com.example.system_mini_capstone.admin.UserManagementActivity::class.java))
                            }
                            "gesture_dictionary" -> {
                                startActivity(Intent(this@DashboardActivity, com.example.system_mini_capstone.admin.lcms.LessonListActivity::class.java))
                            }
                            "sign_samples" -> {
                                startActivity(Intent(this@DashboardActivity, com.example.system_mini_capstone.admin.studio.SignSamplesActivity::class.java))
                            }
                            "admin_gesture_studio", "tts_settings" -> {
                                startActivity(Intent(this@DashboardActivity, com.example.system_mini_capstone.admin.SystemConfigActivity::class.java))
                            }
                            else -> {
                                if (feature.isRestrictedToAdmin) {
                                    startActivity(Intent(this@DashboardActivity, com.example.system_mini_capstone.admin.SystemConfigActivity::class.java))
                                } else {
                                    viewModel.triggerAction(feature.title)
                                    Toast.makeText(this@DashboardActivity, "Opening ${feature.title}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    onCreateAccountClick = {
                        startActivity(Intent(this@DashboardActivity, RegisterActivity::class.java))
                    },
                    onPromptLogout = viewModel::promptLogout,
                    onDismissLogout = viewModel::dismissLogout,
                    onConfirmLogout = {
                        viewModel.logout()
                        navigateToLogin()
                    }
                )
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    snackbarHostState: SnackbarHostState,
    onSelectTab: (DashboardNavTab) -> Unit,
    onFeatureClick: (DashboardFeatureItem) -> Unit,
    onCreateAccountClick: () -> Unit,
    onPromptLogout: () -> Unit,
    onDismissLogout: () -> Unit,
    onConfirmLogout: () -> Unit
) {
    val isAdmin = uiState.userRole == UserRole.ADMIN
    val isGuest = uiState.userRole == UserRole.GUEST

    if (uiState.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = onDismissLogout,
            title = { Text(if (isGuest) "Exit Guest Mode" else "Sign Out") },
            text = { Text(if (isGuest) "Exit guest mode and return to sign in?" else "Are you sure you want to end your current authenticated session?") },
            confirmButton = {
                Button(
                    onClick = onConfirmLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isGuest) "Exit Guest Mode" else "Sign Out")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissLogout) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("SignLink Hub", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isAdmin) "🛡️" else if (isGuest) "👤" else "🤟",
                                fontSize = 18.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onPromptLogout) {
                        Text("🚪", fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = uiState.activeTab == DashboardNavTab.OVERVIEW,
                    onClick = { onSelectTab(DashboardNavTab.OVERVIEW) },
                    icon = { Text("🏠", fontSize = 20.sp) },
                    label = { Text("Hub") }
                )
                NavigationBarItem(
                    selected = uiState.activeTab == DashboardNavTab.TRANSLATOR,
                    onClick = { onSelectTab(DashboardNavTab.TRANSLATOR) },
                    icon = { Text("🤟", fontSize = 20.sp) },
                    label = { Text("Translator") }
                )
                if (isAdmin) {
                    NavigationBarItem(
                        selected = uiState.activeTab == DashboardNavTab.ADMIN_PANEL,
                        onClick = { onSelectTab(DashboardNavTab.ADMIN_PANEL) },
                        icon = { Text("🛡️", fontSize = 20.sp) },
                        label = { Text("Admin") }
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Header Card
            item {
                UserHeaderCard(uiState = uiState, isAdmin = isAdmin)
            }

            // Role Banner
            item {
                when {
                    isAdmin -> AdminPrivilegesBanner()
                    isGuest -> GuestBanner(onCreateAccountClick = onCreateAccountClick)
                    else -> UserWelcomeBanner()
                }
            }

            // Section Header
            item {
                Text(
                    text = if (isAdmin) "Authorized Modules (RBAC: Full Access)" else "Available Modules (Standard Access)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Feature List (Filtered by RBAC)
            val displayedFeatures = when (uiState.activeTab) {
                DashboardNavTab.OVERVIEW -> uiState.accessibleFeatures
                DashboardNavTab.TRANSLATOR -> uiState.accessibleFeatures.filter { it.id == "realtime_translator" || it.id == "gesture_dictionary" }
                DashboardNavTab.ADMIN_PANEL -> uiState.accessibleFeatures.filter { it.isRestrictedToAdmin }
                DashboardNavTab.SETTINGS -> uiState.accessibleFeatures
            }

            items(displayedFeatures, key = { it.id }) { feature ->
                DashboardFeatureCard(
                    feature = feature,
                    onClick = { onFeatureClick(feature) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onPromptLogout,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isGuest) "Exit Guest Mode" else "End Session & Sign Out")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun UserHeaderCard(
    uiState: DashboardUiState,
    isAdmin: Boolean
) {
    val isGuest = uiState.userRole == UserRole.GUEST

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = CircleShape,
                color = when {
                    isAdmin -> MaterialTheme.colorScheme.errorContainer
                    isGuest -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = if (isAdmin) "🛡️" else if (isGuest) "👤" else "👤", fontSize = 26.sp)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isGuest) "Guest User" else "Hello, ${uiState.username}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isGuest) "Guest Mode (Local Session)" else uiState.userEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Role: ${uiState.userRole.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun GuestBanner(onCreateAccountClick: () -> Unit) {
    var isDismissed by remember { mutableStateOf(false) }
    if (isDismissed) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👤 Guest Mode Active",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                TextButton(onClick = { isDismissed = true }) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "You are using Guest mode. Create an account to save your progress.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onCreateAccountClick,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Create Account", fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun AdminPrivilegesBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🛡️ Administrative Console Active",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Role-based permissions granted: User account administration, custom gesture training studio, and ML model telemetry inspection.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun UserWelcomeBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "👋 Welcome to SignLink",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Point your camera to detect 21 hand landmarks and translate American Sign Language gestures directly into audible speech.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun DashboardFeatureCard(
    feature: DashboardFeatureItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (feature.isRestrictedToAdmin) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = feature.iconEmoji, fontSize = 24.sp)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = feature.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (feature.isRestrictedToAdmin) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "ADMIN",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = feature.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text("→", fontSize = 20.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}
