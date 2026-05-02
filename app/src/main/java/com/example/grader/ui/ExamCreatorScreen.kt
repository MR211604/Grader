package com.example.grader.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.grader.ui.components.GraderBottomNavigation
import com.example.grader.ui.components.NavRoute
import com.example.grader.ui.utils.rememberDragDropListState
import com.example.grader.ui.utils.move
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamCreatorScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentRoute by remember { mutableStateOf(NavRoute.EXAMS) }
    val primaryBlue = Color(0xFF0C5CBF)

    data class QuestionData(
        val id: Int,
        var prompt: String,
        val options: MutableList<Pair<String, Boolean>>
    )

    val questions = remember {
        mutableStateListOf(
            QuestionData(
                1,
                "Which planet is known as the Red Planet?",
                mutableStateListOf("Venus" to false, "Mars" to true, "Jupiter" to false, "Saturn" to false)
            ),
            QuestionData(
                2,
                "What is the chemical symbol for Gold?",
                mutableStateListOf("Gd" to false, "Ag" to false, "Au" to true, "Pb" to false)
            ),
            QuestionData(
                3,
                "This is a third test?",
                mutableStateListOf("Yes" to false, "Maybe" to true, "No" to false, "Definitely not" to false)
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create Exam",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            GraderBottomNavigation(
                currentRoute = currentRoute,
                onNavigate = { currentRoute = it }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            val scope = rememberCoroutineScope()
            var overscrollJob by remember { mutableStateOf<Job?>(null) }
            val dragDropListState = rememberDragDropListState(onMove = { fromIndex, toIndex ->
                val fromQ = fromIndex - 2
                val toQ = toIndex - 2
                if (fromQ in 0 until questions.size && toQ in 0 until questions.size) {
                    questions.move(fromQ, toQ)
                }
            })

            LazyColumn(
                state = dragDropListState.lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDrag = { change, offset ->
                                change.consume()
                                dragDropListState.onDrag(offset)
                                if (overscrollJob?.isActive == true)
                                    return@detectDragGesturesAfterLongPress
                                dragDropListState
                                    .checkForOverScroll()
                                    .takeIf { it != 0f }
                                    ?.let {
                                        overscrollJob = scope.launch {
                                            var scrollAmount = it
                                            while (scrollAmount != 0f && isActive) {
                                                dragDropListState.lazyListState.scrollBy(scrollAmount)
                                                kotlinx.coroutines.delay(10)
                                                scrollAmount = dragDropListState.checkForOverScroll()
                                            }
                                        }
                                    } ?: kotlin.run { overscrollJob?.cancel() }
                            },
                            onDragStart = { offset ->
                                dragDropListState.onDragStart(offset)
                            },
                            onDragEnd = {
                                dragDropListState.onDragInterrupted()
                            },
                            onDragCancel = {
                                dragDropListState.onDragInterrupted()
                            }
                        )
                    },
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Exam Details Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Exam Details",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "DRAFT",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Exam Title",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        placeholder = { Text("e.g. Mid-term Science Assessment") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Category",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = "Science",
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Time Limit (Min)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = "45",
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }
                    }
                }
                
                // Questions Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Questions",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(${questions.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Auto-saving enabled",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                items(questions.size, key = { questions[it].id }) { i ->
                    val itemId = questions[i].id
                    val currentIndex = questions.indexOfFirst { it.id == itemId }
                    if (currentIndex == -1) return@items
                    
                    val question = questions[currentIndex]
                    val itemIndex = currentIndex + 2
                    val offsetOrNull = dragDropListState.elementDisplacement.takeIf {
                        itemIndex == dragDropListState.currentIndexOfDraggedItem
                    }

                    Box(
                        modifier = Modifier
                            .zIndex(if (offsetOrNull != null) 1f else 0f)
                            .graphicsLayer {
                                translationY = offsetOrNull ?: 0f
                            }
                    ) {
                        QuestionCard(
                            questionNumber = currentIndex + 1,
                            prompt = question.prompt,
                            options = question.options,
                            primaryBlue = primaryBlue,
                            onAddOption = {
                                question.options.add("New Option" to false)
                            },
                            onRemoveOption = { optIndex ->
                                question.options.removeAt(optIndex)
                            }
                        )
                    }
                }

                // Add New Question
                item {
                    OutlinedButton(
                        onClick = {
                            questions.add(
                                QuestionData(
                                    id = (questions.maxOfOrNull { it.id } ?: 0) + 1,
                                    prompt = "This is a new question",
                                    options = mutableStateListOf("Option 1" to false, "Option 2" to false),
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = primaryBlue
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add New Question", fontWeight = FontWeight.Bold)
                    }
                }

                // Footer Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, primaryBlue)
                        ) {
                            Text("Cancel", color = primaryBlue, fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = { },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                        ) {
                            Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Guardar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionCard(
    questionNumber: Int,
    prompt: String,
    options: List<Pair<String, Boolean>>,
    primaryBlue: Color,
    onAddOption: () -> Unit = {},
    onRemoveOption: (Int) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(2.dp, primaryBlue)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = "Drag",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = primaryBlue.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "Question $questionNumber",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = primaryBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "QUESTION PROMPT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = prompt,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ANSWER OPTIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onAddOption,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(24.dp) // Make it compact
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Option", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Option", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEachIndexed { index, option ->
                    val letter = ('A' + index).toString()
                    AnswerOptionRow(
                        letter = letter,
                        text = option.first,
                        isCorrect = option.second,
                        primaryBlue = primaryBlue,
                        onDelete = { onRemoveOption(index) }
                    )
                }
            }
        }
    }
}

@Composable
fun AnswerOptionRow(
    letter: String,
    text: String,
    isCorrect: Boolean,
    primaryBlue: Color,
    onDelete: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isCorrect,
                    onCheckedChange = {},
                    colors = CheckboxDefaults.colors(
                        checkedColor = primaryBlue
                    )
                )
                Text(
                    text = "Correct",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete Option",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExamCreatorPreview() {
    MaterialTheme {
        ExamCreatorScreen(onNavigateBack = {})
    }
}
