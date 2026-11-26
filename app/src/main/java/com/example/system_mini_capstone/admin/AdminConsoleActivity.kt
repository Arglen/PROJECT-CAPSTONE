package com.example.system_mini_capstone.admin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.system_mini_capstone.DashboardActivity
import com.example.system_mini_capstone.SignLinkBaseActivity
import com.example.system_mini_capstone.admin.lcms.LessonListActivity
import com.example.system_mini_capstone.admin.studio.SignSamplesActivity
import com.example.system_mini_capstone.data.SessionManager
import com.example.system_mini_capstone.ui.components.SignLinkBackButton
import com.example.system_mini_capstone.ui.theme.SignLinkColors
import com.example.system_mini_capstone.ui.theme.System_mini_capstoneTheme

class AdminConsoleActivity : SignLinkBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Security Guard: Admin only
        if (SessionManager(this).getUserRole() != "ADMIN") {
            Log.w("AdminGuard", "Unauthorized access attempt to AdminConsoleActivity. Redirecting.")
            val intent = Intent(this, DashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }

        setContent {
            System_mini_capstoneTheme {
                AdminConsoleScreen(
                    onBack = { finish() },
                    onOpenSignSamples = {
                        startActivity(Intent(this, SignSamplesActivity::class.java))
                    },
                    onOpenLessons = {
                        startActivity(Intent(this, LessonListActivity::class.java))
                    },
                    onOpenUsers = {
                        startActivity(Intent(this, UserManagementActivity::class.java))
                    },
                    onOpenAnalytics = {
                        startActivity(Intent(this, AdminAnalyticsActivity::class.java))
                    },
                    onOpenSystemConfig = {
                        startActivity(Intent(this, SystemConfigActivity::class.java))
                    },
                    onOpenMaintenance = {
                        startActivity(Intent(this, MaintenanceActivity::class.java))
                    }
                )
            }
        }
    }
}

@Composable
fun AdminConsoleScreen(
    onBack: () -> Unit,
    onOpenSignSamples: () -> Unit,
    onOpenLessons: () -> Unit,
    onOpenUsers: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenSystemConfig: () -> Unit,
    onOpenMaintenance: () -> Unit
) {
    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SignLinkColors.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header: Back button, Title & Subtitle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SignLinkBackButton(onClick = onBack)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Admin Console",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = SignLinkColors.onBackground
                        )
                        Text(
                            text = "Manage system tools, content and sign samples.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SignLinkColors.secondaryText
                        )
                    }
                }
            }

            // Admin Tools List
            val tools = listOf(
                Triple("Sign Samples Studio", "Record letters, numbers and signs", Icons.Default.CameraAlt) to onOpenSignSamples,
                Triple("Lessons", "Manage learning content", Icons.AutoMirrored.Filled.MenuBook) to onOpenLessons,
                Triple("Users", "Manage accounts and roles", Icons.Default.Group) to onOpenUsers,
                Triple("Analytics", "Usage and recognition insights", Icons.Default.BarChart) to onOpenAnalytics,
                Triple("System settings", "Detection and speech options", Icons.Default.Settings) to onOpenSystemConfig,
                Triple("Maintenance", "Cache and database tools", Icons.Default.Build) to onOpenMaintenance
            )

            items(tools.size) { index ->
                val (tool, onClick) = tools[index]
                val (title, subtitle, icon) = tool

                Surface(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp), clip = false),
                    shape = RoundedCornerShape(24.dp),
                    color = SignLinkColors.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = SignLinkColors.purpleTint
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = SignLinkColors.accentPurple
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = SignLinkColors.onBackground
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = SignLinkColors.secondaryText
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = SignLinkColors.secondaryText
                        )
                    }
                }
            }
        }
    }
}
