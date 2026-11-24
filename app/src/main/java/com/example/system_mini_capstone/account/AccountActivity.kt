package com.example.system_mini_capstone.account

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.system_mini_capstone.DashboardActivity
import com.example.system_mini_capstone.LoginActivity
import com.example.system_mini_capstone.RegisterActivity
import com.example.system_mini_capstone.admin.AdminAnalyticsActivity
import com.example.system_mini_capstone.data.SessionManager
import com.example.system_mini_capstone.data.network.UserRole
import com.example.system_mini_capstone.ui.components.SignLinkBackButton
import com.example.system_mini_capstone.ui.components.SignLinkPrimaryButton
import com.example.system_mini_capstone.ui.components.SignLinkSecondaryButton
import com.example.system_mini_capstone.ui.theme.SignLinkColors
import com.example.system_mini_capstone.ui.theme.System_mini_capstoneTheme

class AccountActivity : ComponentActivity() {

    private val sessionManager by lazy { SessionManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val role = UserRole.fromString(sessionManager.getUserRole())
        val email = sessionManager.getUserEmail() ?: "guest@signlink.app"
        val username = sessionManager.getUsername() ?: email.substringBefore("@")
        val isGuest = role == UserRole.GUEST
        val isAdmin = role == UserRole.ADMIN

        setContent {
            System_mini_capstoneTheme {
                AccountScreen(
                    username = if (isGuest) "Guest" else username,
                    email = if (isGuest) "Guest Mode (Local Session)" else email,
                    isGuest = isGuest,
                    isAdmin = isAdmin,
                    onBack = { finish() },
                    onCreateAccount = {
                        startActivity(Intent(this, RegisterActivity::class.java))
                    },
                    onOpenAdmin = {
                        startActivity(Intent(this, AdminAnalyticsActivity::class.java))
                    },
                    onLogout = {
                        sessionManager.clearSession()
                        val intent = Intent(this, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun AccountScreen(
    username: String,
    email: String,
    isGuest: Boolean,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onCreateAccount: () -> Unit,
    onOpenAdmin: () -> Unit,
    onLogout: () -> Unit
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
                            text = "Your learning space",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = SignLinkColors.onBackground
                        )
                        Text(
                            text = "Simple preferences. More time to connect.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SignLinkColors.secondaryText
                        )
                    }
                }
            }

            // Profile Card (White, 24dp radius)
            item {
                Surface(
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
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            color = SignLinkColors.purpleTint
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = SignLinkColors.accentPurple,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = username,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = SignLinkColors.onBackground
                            )
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodySmall,
                                color = SignLinkColors.secondaryText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Your space to learn and connect.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SignLinkColors.accentPurple
                            )
                        }
                    }
                }
            }

            if (isGuest) {
                item {
                    Button(
                        onClick = onCreateAccount,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Create an account", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            }

            // Three White Rows (20dp radius)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Row 1: Sign language
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 1.dp, shape = RoundedCornerShape(20.dp), clip = false),
                        shape = RoundedCornerShape(20.dp),
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
                                modifier = Modifier.size(44.dp),
                                shape = RoundedCornerShape(12.dp),
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sign language", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SignLinkColors.onBackground)
                                Text("American Sign Language (ASL)", style = MaterialTheme.typography.bodySmall, color = SignLinkColors.secondaryText)
                            }
                        }
                    }

                    // Row 2: Camera privacy
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 1.dp, shape = RoundedCornerShape(20.dp), clip = false),
                        shape = RoundedCornerShape(20.dp),
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
                                modifier = Modifier.size(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = SignLinkColors.purpleTint
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = SignLinkColors.accentPurple
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Camera privacy", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SignLinkColors.onBackground)
                                Text("Hand detection runs on your phone", style = MaterialTheme.typography.bodySmall, color = SignLinkColors.secondaryText)
                            }
                        }
                    }

                    // Row 3: Your pace
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 1.dp, shape = RoundedCornerShape(20.dp), clip = false),
                        shape = RoundedCornerShape(20.dp),
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
                                modifier = Modifier.size(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = SignLinkColors.purpleTint
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = SignLinkColors.accentPurple
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Your pace", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SignLinkColors.onBackground)
                                Text("Start with four everyday signs", style = MaterialTheme.typography.bodySmall, color = SignLinkColors.secondaryText)
                            }
                        }
                    }

                    // Admin shortcut (if ADMIN)
                    if (isAdmin) {
                        Surface(
                            onClick = onOpenAdmin,
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(elevation = 1.dp, shape = RoundedCornerShape(20.dp), clip = false),
                            shape = RoundedCornerShape(20.dp),
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
                                    modifier = Modifier.size(44.dp),
                                    shape = RoundedCornerShape(12.dp),
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Admin console", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SignLinkColors.onBackground)
                                    Text("Analytics, users & system configuration", style = MaterialTheme.typography.bodySmall, color = SignLinkColors.secondaryText)
                                }
                            }
                        }
                    }
                }
            }

            // Buttons: Back to learning, Log out
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SignLinkPrimaryButton(
                        text = "Back to learning",
                        onClick = onBack
                    )
                    SignLinkSecondaryButton(
                        text = "Log out",
                        onClick = onLogout
                    )
                }
            }
        }
    }
}
