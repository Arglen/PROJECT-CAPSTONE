package com.example.system_mini_capstone.admin.lcms

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.system_mini_capstone.ui.theme.System_mini_capstoneTheme
import kotlinx.coroutines.launch

/**
 * LCMS Content Catalog Activity listing all live lessons and work-in-progress drafts.
 */
class LessonListActivity : ComponentActivity() {

    private val repository: LessonRepository = LessonRepositoryImpl()
    private var reloadTrigger by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            System_mini_capstoneTheme {
                var lessons by remember { mutableStateOf<List<LessonItem>>(emptyList()) }
                var isLoading by remember { mutableStateOf(true) }
                var filterStatus by remember { mutableStateOf<LessonStatus?>(null) }
                val scope = rememberCoroutineScope()

                fun reload() {
                    scope.launch {
                        isLoading = true
                        lessons = repository.fetchLessons()
                        isLoading = false
                    }
                }

                LaunchedEffect(reloadTrigger) {
                    reload()
                }

                val displayedLessons = if (filterStatus == null) {
                    lessons
                } else {
                    lessons.filter { it.status == filterStatus }
                }

                Scaffold(
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = { Text("LCMS Curriculum Hub", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                        )
                    },
                    floatingActionButton = {
                        ExtendedFloatingActionButton(
                            onClick = {
                                val intent = Intent(this@LessonListActivity, LessonEditorActivity::class.java).apply {
                                    putExtra(LessonEditorActivity.EXTRA_LESSON_ID, "new")
                                }
                                startActivity(intent)
                            },
                            icon = { Text("➕", fontSize = 18.sp) },
                            text = { Text("Create New Lesson") },
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    }
                ) { innerPadding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Filter Chips Row
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = filterStatus == null,
                                    onClick = { filterStatus = null },
                                    label = { Text("All Lessons (${lessons.size})") }
                                )
                                FilterChip(
                                    selected = filterStatus == LessonStatus.PUBLISHED,
                                    onClick = { filterStatus = LessonStatus.PUBLISHED },
                                    label = { Text("Live in Curriculum (${lessons.count { it.status == LessonStatus.PUBLISHED }})") }
                                )
                                FilterChip(
                                    selected = filterStatus == LessonStatus.DRAFT,
                                    onClick = { filterStatus = LessonStatus.DRAFT },
                                    label = { Text("Drafts (${lessons.count { it.status == LessonStatus.DRAFT }})") }
                                )
                            }
                        }

                        if (isLoading) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        } else if (displayedLessons.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Box(
                                        modifier = Modifier.padding(32.dp).fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No lessons matching this filter.", color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        } else {
                            items(displayedLessons, key = { it.lessonId }) { item ->
                                LessonCatalogCard(
                                    lesson = item,
                                    onEditClick = {
                                        val intent = Intent(this@LessonListActivity, LessonEditorActivity::class.java).apply {
                                            putExtra(LessonEditorActivity.EXTRA_LESSON_ID, item.lessonId)
                                        }
                                        startActivity(intent)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        reloadTrigger++
    }
}

@Composable
fun LessonCatalogCard(
    lesson: LessonItem,
    onEditClick: () -> Unit
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
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                    Text(
                        text = lesson.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Category: ${lesson.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (lesson.status == LessonStatus.PUBLISHED) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.wrapContentWidth(unbounded = true)
                ) {
                    Text(
                        text = if (lesson.status == LessonStatus.PUBLISHED) "PUBLISHED" else "DRAFT",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (lesson.status == LessonStatus.PUBLISHED) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onTertiaryContainer,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Text(
                text = lesson.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${lesson.instructionSteps.size} steps  •  ${lesson.mediaType.label}  •  ${lesson.lastUpdatedFormatted}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Button(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(8.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        "Open Editor",
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
