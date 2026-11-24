package com.example.system_mini_capstone.dictionary

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.system_mini_capstone.DashboardActivity
import com.example.system_mini_capstone.translator.TranslatorActivity
import com.example.system_mini_capstone.ui.components.SignLinkBackButton
import com.example.system_mini_capstone.ui.components.SignLinkBottomBar
import com.example.system_mini_capstone.ui.components.SignLinkCard
import com.example.system_mini_capstone.ui.components.SignLinkPrimaryButton
import com.example.system_mini_capstone.ui.theme.SignLinkColors
import com.example.system_mini_capstone.ui.theme.System_mini_capstoneTheme

class SavedActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bookmarkManager = SignBookmarkManager(this)
        val userId = com.example.system_mini_capstone.data.SessionManager(this).getUserEmail() ?: "guest"

        setContent {
            System_mini_capstoneTheme {
                var savedIds by remember { mutableStateOf(bookmarkManager.getSavedSignIds(userId)) }

                SavedScreen(
                    savedSigns = SignRepository.signs.filter { it.id in savedIds },
                    onBack = {
                        startActivity(Intent(this, DashboardActivity::class.java))
                        finish()
                    },
                    onTabSelected = { index ->
                        when (index) {
                            0 -> {
                                startActivity(Intent(this, DashboardActivity::class.java))
                                finish()
                            }
                            1 -> {
                                startActivity(Intent(this, TranslatorActivity::class.java))
                                finish()
                            }
                            2 -> {
                                startActivity(Intent(this, DictionaryActivity::class.java))
                                finish()
                            }
                            3 -> {} // Saved
                        }
                    },
                    onSignClick = { signId ->
                        val intent = Intent(this, SignDetailActivity::class.java).apply {
                            putExtra("EXTRA_SIGN_ID", signId)
                        }
                        startActivity(intent)
                    },
                    onExploreDictionary = {
                        startActivity(Intent(this, DictionaryActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun SavedScreen(
    savedSigns: List<SignItem>,
    onBack: () -> Unit,
    onTabSelected: (Int) -> Unit,
    onSignClick: (String) -> Unit,
    onExploreDictionary: () -> Unit
) {
    Scaffold(
        bottomBar = {
            SignLinkBottomBar(
                selectedIndex = 3, // Saved
                onTabSelected = onTabSelected
            )
        }
    ) { innerPadding ->
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
                            text = "Keep your favorites close",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = SignLinkColors.onBackground
                        )
                        Text(
                            text = "Signs you save appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SignLinkColors.secondaryText
                        )
                    }
                }
            }

            // Tiny purple uppercase label
            item {
                Text(
                    text = "YOUR SAVED SIGNS - ${savedSigns.size} SIGNS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = SignLinkColors.accentPurple,
                    letterSpacing = 1.sp
                )
            }

            if (savedSigns.isEmpty()) {
                item {
                    SignLinkCard {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "A little practice goes a long way.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SignLinkColors.onBackground
                            )
                            Text(
                                text = "Save signs you want to come back to.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SignLinkColors.secondaryText,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(savedSigns, key = { it.id }) { sign ->
                    val accentColor = when (sign.accent) {
                        SignAccent.PURPLE -> SignLinkColors.accentPurple
                        SignAccent.BLUE -> SignLinkColors.primary
                        SignAccent.GREEN -> SignLinkColors.successGreen
                        SignAccent.AMBER -> SignLinkColors.warningAmber
                    }
                    val accentTint = when (sign.accent) {
                        SignAccent.PURPLE -> SignLinkColors.purpleTint
                        SignAccent.BLUE -> SignLinkColors.blueTint
                        SignAccent.GREEN -> SignLinkColors.successTint
                        SignAccent.AMBER -> SignLinkColors.warningTint
                    }

                    Surface(
                        onClick = { onSignClick(sign.id) },
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
                                color = accentTint
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = null,
                                        tint = accentColor
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = sign.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = SignLinkColors.onBackground
                                )
                                Text(
                                    text = sign.category,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SignLinkColors.secondaryText
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = accentColor
                            )
                        }
                    }
                }
            }

            // Always show explore dictionary button under the list
            item {
                Spacer(modifier = Modifier.height(12.dp))
                SignLinkPrimaryButton(
                    text = "Explore the dictionary",
                    onClick = onExploreDictionary
                )
            }
        }
    }
}
