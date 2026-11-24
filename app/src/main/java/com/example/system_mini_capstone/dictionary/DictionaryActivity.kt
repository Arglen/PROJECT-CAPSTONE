package com.example.system_mini_capstone.dictionary

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.system_mini_capstone.DashboardActivity
import com.example.system_mini_capstone.translator.TranslatorActivity
import com.example.system_mini_capstone.ui.components.SignLinkBackButton
import com.example.system_mini_capstone.ui.components.SignLinkBottomBar
import com.example.system_mini_capstone.ui.components.SignLinkTintedBanner
import com.example.system_mini_capstone.ui.theme.SignLinkColors
import com.example.system_mini_capstone.ui.theme.System_mini_capstoneTheme

class DictionaryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            System_mini_capstoneTheme {
                DictionaryScreen(
                    signs = SignRepository.signs,
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
                            2 -> {} // Dictionary
                            3 -> {
                                startActivity(Intent(this, SavedActivity::class.java))
                                finish()
                            }
                        }
                    },
                    onSignClick = { signId ->
                        val intent = Intent(this, SignDetailActivity::class.java).apply {
                            putExtra("EXTRA_SIGN_ID", signId)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun DictionaryScreen(
    signs: List<SignItem>,
    onBack: () -> Unit,
    onTabSelected: (Int) -> Unit,
    onSignClick: (String) -> Unit
) {
    Scaffold(
        bottomBar = {
            SignLinkBottomBar(
                selectedIndex = 2, // Dictionary
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
                            text = "Your everyday dictionary",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = SignLinkColors.onBackground
                        )
                        Text(
                            text = "Start small. Learn signs you can use every day.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SignLinkColors.secondaryText
                        )
                    }
                }
            }

            // Lavender Banner
            item {
                SignLinkTintedBanner(
                    text = "ASL - Everyday essentials\n4 common signs - scroll to explore",
                    containerColor = SignLinkColors.purpleTint,
                    contentColor = SignLinkColors.accentPurple,
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = SignLinkColors.accentPurple
                        )
                    }
                )
            }

            // Sign Cards List
            items(signs, key = { it.id }) { sign ->
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = accentTint
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = sign.indexNumber,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = sign.category.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = accentColor,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = sign.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    color = SignLinkColors.onBackground
                                )
                            }
                        }

                        Text(
                            text = sign.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SignLinkColors.secondaryText
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "View sign",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = SignLinkColors.primary
                            )
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
}
