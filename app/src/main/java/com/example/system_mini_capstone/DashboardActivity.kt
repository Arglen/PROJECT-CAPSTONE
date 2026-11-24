package com.example.system_mini_capstone

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.system_mini_capstone.account.AccountActivity
import com.example.system_mini_capstone.admin.AdminAnalyticsActivity
import com.example.system_mini_capstone.admin.lcms.LessonListActivity
import com.example.system_mini_capstone.data.network.UserRole
import com.example.system_mini_capstone.dictionary.SavedActivity
import com.example.system_mini_capstone.translator.TranslatorActivity
import com.example.system_mini_capstone.ui.DashboardUiState
import com.example.system_mini_capstone.ui.DashboardViewModel
import com.example.system_mini_capstone.ui.components.SignLinkBottomBar
import com.example.system_mini_capstone.ui.components.SignLinkTintedBanner
import com.example.system_mini_capstone.ui.theme.SignLinkColors
import com.example.system_mini_capstone.ui.theme.System_mini_capstoneTheme

class DashboardActivity : SignLinkBaseActivity() {

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
                    onTabSelected = { index ->
                        when (index) {
                            0 -> {} // Home
                            1 -> startActivity(Intent(this@DashboardActivity, TranslatorActivity::class.java))
                            2 -> startActivity(Intent(this@DashboardActivity, LessonListActivity::class.java))
                            3 -> startActivity(Intent(this@DashboardActivity, SavedActivity::class.java))
                        }
                    },
                    onOpenTranslator = {
                        startActivity(Intent(this@DashboardActivity, TranslatorActivity::class.java))
                    },
                    onOpenDictionary = {
                        startActivity(Intent(this@DashboardActivity, LessonListActivity::class.java))
                    },
                    onOpenAdminConsole = {
                        startActivity(Intent(this@DashboardActivity, AdminAnalyticsActivity::class.java))
                    },
                    onOpenAccount = {
                        startActivity(Intent(this@DashboardActivity, AccountActivity::class.java))
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
    onTabSelected: (Int) -> Unit,
    onOpenTranslator: () -> Unit,
    onOpenDictionary: () -> Unit,
    onOpenAdminConsole: () -> Unit,
    onOpenAccount: () -> Unit,
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
        bottomBar = {
            SignLinkBottomBar(
                selectedIndex = 0, // Home
                onTabSelected = onTabSelected
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SignLinkColors.background),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 24.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "YOUR DAILY CONNECTION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SignLinkColors.accentPurple,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Hello, ready to sign?",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = SignLinkColors.onBackground
                        )
                        Text(
                            text = "A few minutes today. More confidence tomorrow.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SignLinkColors.secondaryText
                        )
                    }

                    // Profile / Account Button (44dp white rounded surface)
                    Surface(
                        onClick = onOpenAccount,
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = SignLinkColors.surface,
                        border = BorderStroke(1.dp, SignLinkColors.outline),
                        shadowElevation = 1.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Account",
                                tint = SignLinkColors.onBackground
                            )
                        }
                    }
                }
            }

            // Guest Mode Banner (if guest)
            if (isGuest) {
                item {
                    SignLinkTintedBanner(
                        text = "You are using Guest mode. Create an account to save your progress",
                        containerColor = SignLinkColors.warningTint,
                        contentColor = SignLinkColors.warningAmber,
                        icon = {
                            Button(
                                onClick = onCreateAccountClick,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SignLinkColors.warningAmber)
                            ) {
                                Text("Create account", fontSize = 12.sp)
                            }
                        }
                    )
                }
            }

            // Hero Card (#7C3AED, 24dp radius, ~208dp tall, 20dp padding)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(208.dp)
                        .shadow(elevation = 4.dp, shape = RoundedCornerShape(24.dp), clip = false),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SignLinkColors.accentPurple)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "START WITH SOMETHING SIMPLE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "A hello can open a conversation.",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    color = Color.White
                                )
                            }

                            Button(
                                onClick = onOpenTranslator,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = SignLinkColors.onBackground
                                )
                            ) {
                                Text(
                                    text = "Learn \"Hello\"",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Illustration Circle on right
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "👋", fontSize = 42.sp)
                        }
                    }
                }
            }

            // Section Title
            item {
                Text(
                    text = "Make it part of your day",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = SignLinkColors.onBackground
                )
            }

            // Two White Cards (~92dp tall)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Card 1: Camera practice
                    Surface(
                        onClick = onOpenTranslator,
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp), clip = false),
                        shape = RoundedCornerShape(24.dp),
                        color = SignLinkColors.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = SignLinkColors.blueTint
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = SignLinkColors.primary
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Camera practice",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = SignLinkColors.onBackground
                                )
                                Text(
                                    text = "Practice with your camera",
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

                    // Card 2: Everyday dictionary
                    Surface(
                        onClick = onOpenDictionary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp), clip = false),
                        shape = RoundedCornerShape(24.dp),
                        color = SignLinkColors.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
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
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = SignLinkColors.accentPurple
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Everyday dictionary",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = SignLinkColors.onBackground
                                )
                                Text(
                                    text = "Explore 4 common signs",
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

                    // ADMIN Card (Only for ADMIN role)
                    if (isAdmin) {
                        Surface(
                            onClick = onOpenAdminConsole,
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp), clip = false),
                            shape = RoundedCornerShape(24.dp),
                            color = SignLinkColors.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = SignLinkColors.errorTint
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = SignLinkColors.error
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Admin console",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = SignLinkColors.onBackground
                                    )
                                    Text(
                                        text = "Analytics, user management & maintenance",
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

            // Green Banner (tint #E7F5F1, 16dp radius)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = SignLinkColors.successTint
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = SignLinkColors.successGreen.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = SignLinkColors.successGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Small steps, real connections",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = SignLinkColors.successGreen
                            )
                            Text(
                                text = "One sign at a time is a great start.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                                color = SignLinkColors.secondaryText
                            )
                        }
                    }
                }
            }
        }
    }
}
