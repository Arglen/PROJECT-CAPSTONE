package com.example.system_mini_capstone.admin.lcms

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.system_mini_capstone.tts.TtsManager
import com.example.system_mini_capstone.ui.theme.System_mini_capstoneTheme

/**
 * LCMS Lesson Editor and Live Preview Activity.
 * Supports creating new lessons, editing existing ones, saving as drafts,
 * and previewing text/media layouts before deploying live.
 */
class LessonEditorActivity : ComponentActivity() {

    private val viewModel: LessonEditorViewModel by viewModels()
    private lateinit var ttsManager: TtsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ttsManager = TtsManager(this)

        val targetLessonId = intent.getStringExtra(EXTRA_LESSON_ID)
        viewModel.loadLesson(targetLessonId)

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

                LessonEditorScreen(
                    uiState = uiState,
                    snackbarHostState = snackbarHostState,
                    onUpdateTitle = viewModel::updateTitle,
                    onUpdateCategory = viewModel::updateCategory,
                    onUpdateDifficulty = viewModel::updateDifficulty,
                    onUpdateDescription = viewModel::updateDescription,
                    onUpdateMediaType = viewModel::updateMediaType,
                    onUpdateMediaUrl = viewModel::updateMediaUrl,
                    onUpdateTts = viewModel::updateTtsPronunciation,
                    onUpdateLandmarkDesc = viewModel::updateLandmarkDescription,
                    onAddStep = viewModel::addInstructionStep,
                    onUpdateStep = viewModel::updateInstructionStep,
                    onRemoveStep = viewModel::removeInstructionStep,
                    onTogglePreview = viewModel::togglePreviewMode,
                    onSaveDraft = viewModel::saveDraft,
                    onPublish = viewModel::publishLesson,
                    onTestTtsSpeech = { phrase ->
                        ttsManager.speak(phrase)
                        Toast.makeText(this@LessonEditorActivity, "Speaking: $phrase", Toast.LENGTH_SHORT).show()
                    },
                    onNavigateBack = { finish() }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
    }

    companion object {
        const val EXTRA_LESSON_ID = "extra_lesson_id"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonEditorScreen(
    uiState: LessonEditorUiState,
    snackbarHostState: SnackbarHostState,
    onUpdateTitle: (String) -> Unit,
    onUpdateCategory: (String) -> Unit,
    onUpdateDifficulty: (LessonDifficulty) -> Unit,
    onUpdateDescription: (String) -> Unit,
    onUpdateMediaType: (LessonMediaType) -> Unit,
    onUpdateMediaUrl: (String) -> Unit,
    onUpdateTts: (String) -> Unit,
    onUpdateLandmarkDesc: (String) -> Unit,
    onAddStep: (String) -> Unit,
    onUpdateStep: (Int, String) -> Unit,
    onRemoveStep: (Int) -> Unit,
    onTogglePreview: () -> Unit,
    onSaveDraft: () -> Unit,
    onPublish: () -> Unit,
    onTestTtsSpeech: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (uiState.isPreviewMode) "Previewing Lesson" else if (uiState.isNewLesson) "Create New Lesson" else "Edit Lesson",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (uiState.lesson.status == LessonStatus.PUBLISHED) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = if (uiState.lesson.status == LessonStatus.PUBLISHED) "PUBLISHED" else "DRAFT",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.lesson.status == LessonStatus.PUBLISHED) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    // Preview Toggle Action
                    OutlinedButton(
                        onClick = onTogglePreview,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(if (uiState.isPreviewMode) "✏️ Edit Mode" else "👁️ Preview")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // "Save as Draft" Button
                    OutlinedButton(
                        onClick = onSaveDraft,
                        enabled = !uiState.isSaving,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Save as Draft")
                    }

                    // "Publish" Button
                    Button(
                        onClick = onPublish,
                        enabled = !uiState.isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("Publish Live")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = uiState.isPreviewMode,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "EditorOrPreview"
        ) { isPreview ->
            if (isPreview) {
                LessonPreviewLayout(
                    lesson = uiState.lesson,
                    onTestTtsSpeech = onTestTtsSpeech,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            } else {
                LessonEditorForm(
                    lesson = uiState.lesson,
                    validationErrors = uiState.validationErrors,
                    onUpdateTitle = onUpdateTitle,
                    onUpdateCategory = onUpdateCategory,
                    onUpdateDifficulty = onUpdateDifficulty,
                    onUpdateDescription = onUpdateDescription,
                    onUpdateMediaType = onUpdateMediaType,
                    onUpdateMediaUrl = onUpdateMediaUrl,
                    onUpdateTts = onUpdateTts,
                    onUpdateLandmarkDesc = onUpdateLandmarkDesc,
                    onAddStep = onAddStep,
                    onUpdateStep = onUpdateStep,
                    onRemoveStep = onRemoveStep,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// COMPONENT 1: LESSON FORM EDITOR
// -----------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonEditorForm(
    lesson: LessonItem,
    validationErrors: Map<String, String>,
    onUpdateTitle: (String) -> Unit,
    onUpdateCategory: (String) -> Unit,
    onUpdateDifficulty: (LessonDifficulty) -> Unit,
    onUpdateDescription: (String) -> Unit,
    onUpdateMediaType: (LessonMediaType) -> Unit,
    onUpdateMediaUrl: (String) -> Unit,
    onUpdateTts: (String) -> Unit,
    onUpdateLandmarkDesc: (String) -> Unit,
    onAddStep: (String) -> Unit,
    onUpdateStep: (Int, String) -> Unit,
    onRemoveStep: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        "Greetings & Etiquette",
        "Everyday Phrases",
        "Healthcare & Emergency",
        "Alphabet & Fingerspelling",
        "Numbers & Quantities"
    )
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Lesson ID & Status Badge
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lesson Identifier: ${lesson.lessonId}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "Last Edited: ${lesson.lastUpdatedFormatted}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Title Input
        item {
            OutlinedTextField(
                value = lesson.title,
                onValueChange = onUpdateTitle,
                label = { Text("Lesson Title *") },
                placeholder = { Text("e.g. Greeting: Open Palm Hello") },
                singleLine = true,
                isError = validationErrors.containsKey("title"),
                supportingText = {
                    validationErrors["title"]?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Category Dropdown
        item {
            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = lesson.category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Curriculum Category *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                onUpdateCategory(cat)
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Difficulty Chips
        item {
            Column {
                Text("Difficulty Tier", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LessonDifficulty.entries.forEach { diff ->
                        FilterChip(
                            selected = lesson.difficulty == diff,
                            onClick = { onUpdateDifficulty(diff) },
                            label = { Text(diff.label) }
                        )
                    }
                }
            }
        }

        // Description Overview
        item {
            OutlinedTextField(
                value = lesson.description,
                onValueChange = onUpdateDescription,
                label = { Text("Curriculum Overview & Educational Goals *") },
                placeholder = { Text("Briefly explain what physical sign or concept this lesson teaches...") },
                minLines = 3,
                isError = validationErrors.containsKey("description"),
                supportingText = {
                    validationErrors["description"]?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Media Demonstration Type & Asset URL
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Media Demonstration Asset", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LessonMediaType.entries.forEach { mType ->
                            FilterChip(
                                selected = lesson.mediaType == mType,
                                onClick = { onUpdateMediaType(mType) },
                                label = { Text("${mType.iconEmoji} ${mType.label}", fontSize = 12.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = lesson.mediaUrl,
                        onValueChange = onUpdateMediaUrl,
                        label = { Text("Media Asset URL / Path") },
                        placeholder = { Text("https://assets.signlink.app/lessons/gesture.mp4") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // TextToSpeech Pronunciation Phrase
        item {
            OutlinedTextField(
                value = lesson.ttsPronunciation,
                onValueChange = onUpdateTts,
                label = { Text("TTS Spoken Audio Output Phrase *") },
                placeholder = { Text("e.g. Hello! Nice to meet you.") },
                singleLine = true,
                isError = validationErrors.containsKey("tts"),
                supportingText = {
                    validationErrors["tts"]?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Target Landmark Coordinates Criteria
        item {
            OutlinedTextField(
                value = lesson.targetLandmarksDescription,
                onValueChange = onUpdateLandmarkDesc,
                label = { Text("Landmark & Vision Geometry Notes") },
                placeholder = { Text("e.g. Index and Pinky extended, Thumb folded across curled ring finger...") },
                minLines = 2,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Step-by-Step Instructions Dynamic List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Step-by-Step Signing Instructions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = { onAddStep("") }) {
                    Text("+ Add Step")
                }
            }
        }

        itemsIndexed(lesson.instructionSteps) { index, stepText ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("${index + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                OutlinedTextField(
                    value = stepText,
                    onValueChange = { onUpdateStep(index, it) },
                    singleLine = false,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                )

                if (lesson.instructionSteps.size > 1) {
                    IconButton(onClick = { onRemoveStep(index) }) {
                        Text("✕", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

// -----------------------------------------------------------------------------------------
// COMPONENT 2: LIVE LESSON PREVIEW LAYOUT (STUDENT VIEW SIMULATION)
// -----------------------------------------------------------------------------------------
@Composable
fun LessonPreviewLayout(
    lesson: LessonItem,
    onTestTtsSpeech: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Preview Header Notice
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("👁️ Preview Mode: Viewing layout as students will experience it.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Title & Category Header
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = lesson.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(text = lesson.difficulty.label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = lesson.title.ifBlank { "Untitled Lesson" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Estimated time: ${lesson.estimatedMinutes} mins",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Visual Media Demonstration Box
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(lesson.mediaType.iconEmoji, fontSize = 28.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "[Media Demonstration: ${lesson.mediaType.label}]",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = lesson.mediaUrl.ifBlank { "Default bundled asset demonstration" },
                            color = Color.Gray,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        // Overview Description
        item {
            Text("About this Sign", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = lesson.description.ifBlank { "No overview description provided." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // TTS Pronunciation Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Vocalized Translation (TTS)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "\"${lesson.ttsPronunciation.ifBlank { "No speech phrase set" }}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { onTestTtsSpeech(lesson.ttsPronunciation) },
                        enabled = lesson.ttsPronunciation.isNotBlank(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🔊 Play Audio")
                    }
                }
            }
        }

        // Step by Step Instructions
        item {
            Text("Signing Instructions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        itemsIndexed(lesson.instructionSteps) { index, step ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("${index + 1}", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(text = step, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Landmark Geometry Requirements
        if (lesson.targetLandmarksDescription.isNotBlank()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Target Hand Landmarks Criteria", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(lesson.targetLandmarksDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}
