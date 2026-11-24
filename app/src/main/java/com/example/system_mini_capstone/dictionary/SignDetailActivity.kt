package com.example.system_mini_capstone.dictionary

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

class SignDetailActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val signId = intent.getStringExtra("EXTRA_SIGN_ID") ?: "hello"
        val sign = SignRepository.getSignById(signId) ?: SignRepository.signs.first()
        val bookmarkManager = SignBookmarkManager(this)

        setContent {
            System_mini_capstoneTheme {
                var isBookmarked by remember { mutableStateOf(bookmarkManager.isBookmarked(sign.id)) }

                SignDetailScreen(
                    sign = sign,
                    isBookmarked = isBookmarked,
                    onBookmarkToggle = {
                        val added = bookmarkManager.toggleBookmark(sign.id)
                        isBookmarked = added
                        Toast.makeText(this, if (added) "Saved to favorites" else "Removed from favorites", Toast.LENGTH_SHORT).show()
                    },
                    onBack = { finish() },
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
                            3 -> {
                                startActivity(Intent(this, SavedActivity::class.java))
                                finish()
                            }
                        }
                    },
                    onPracticeWithCamera = {
                        startActivity(Intent(this, TranslatorActivity::class.java))
                    }
                )
            }
        }
    }
}

@Composable
fun SignDetailScreen(
    sign: SignItem,
    isBookmarked: Boolean,
    onBookmarkToggle: () -> Unit,
    onBack: () -> Unit,
    onTabSelected: (Int) -> Unit,
    onPracticeWithCamera: () -> Unit
) {
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
            // Header Row: Back button & Bookmark toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SignLinkBackButton(onClick = onBack)
                    IconButton(onClick = onBookmarkToggle) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Breadcrumb, Title & Description
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ASL / ${sign.category.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = accentColor,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = sign.name,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 40.sp,
                        color = SignLinkColors.onBackground
                    )
                    Text(
                        text = sign.description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 16.sp,
                        color = SignLinkColors.secondaryText
                    )
                }
            }

            // HOW TO SIGN Banner
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = accentTint
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "HOW TO SIGN",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = accentColor,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Follow these 3 simple steps.",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = accentColor
                        )
                    }
                }
            }

            // 3 Step Cards
            items(sign.steps) { step ->
                SignLinkCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = accentTint
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = step.number,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = step.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = SignLinkColors.onBackground
                            )
                            Text(
                                text = step.description,
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 13.sp,
                                color = SignLinkColors.secondaryText
                            )
                        }
                    }
                }
            }

            // Practice with camera primary button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SignLinkPrimaryButton(
                    text = "Practice with camera",
                    onClick = onPracticeWithCamera
                )
            }

            // Footnote
            item {
                Text(
                    text = "ASL signs may vary by region. Learn with your local Deaf community.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = SignLinkColors.secondaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )
            }
        }
    }
}
