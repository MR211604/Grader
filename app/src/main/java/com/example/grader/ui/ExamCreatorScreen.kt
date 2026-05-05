package com.example.grader.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.grader.ui.viewmodels.ExamCreatorUiState
import com.example.grader.ui.viewmodels.ExamCreatorViewModel
import com.example.grader.ui.viewmodels.EditableQuestion
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamCreatorScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExamCreatorViewModel = viewModel()
) {
    var currentRoute by remember { mutableStateOf(NavRoute.EXAMS) }
    val primaryBlue = Color(0xFF0C5CBF)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Handle saved / error states
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is ExamCreatorUiState.Saved -> {
                snackbarHostState.showSnackbar("Examen guardado exitosamente")
                onNavigateBack()
            }
            is ExamCreatorUiState.Error -> {
                snackbarHostState.showSnackbar("Error: ${state.message}")
                viewModel.dismissError()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Crear Nueva Evaluación",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = primaryBlue
                    )
                },
                modifier = Modifier.drawBehind {
                    val borderSize = 1.dp.toPx()
                    drawLine(
                        color = Color(0xFFE0E0E0),
                        start = androidx.compose.ui.geometry.Offset(0f, size.height),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                        strokeWidth = borderSize
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            GraderBottomNavigation(
                currentRoute = currentRoute,
                onNavigate = { currentRoute = it }
            )
        }
    ) { innerPadding ->

        // Only render when in Editing state
        val editingState = uiState as? ExamCreatorUiState.Editing ?: return@Scaffold

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Show validation errors summary if present
            if (editingState.validationErrors.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Corrige los siguientes errores:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        editingState.validationErrors.values.forEach { error ->
                            Text(
                                text = "• $error",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            val dragScope = rememberCoroutineScope()
            var overscrollJob by remember { mutableStateOf<Job?>(null) }
            val dragDropListState = rememberDragDropListState(onMove = { fromIndex, toIndex ->
                val fromQ = fromIndex - 2
                val toQ = toIndex - 2
                if (fromQ in 0 until editingState.questions.size && toQ in 0 until editingState.questions.size) {
                    viewModel.moveQuestion(fromQ, toQ)
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
                                        overscrollJob = dragScope.launch {
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
                            text = "Detalles de Examen",
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
                        text = "Titulo de examen",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editingState.title,
                        onValueChange = { viewModel.updateTitle(it) },
                        placeholder = { Text("p.ej. Examen de ciencias II periodo") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = editingState.validationErrors.containsKey("title"),
                        supportingText = editingState.validationErrors["title"]?.let { err ->
                            { Text(err, color = MaterialTheme.colorScheme.error) }
                        },
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
                                text = "Categoria",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editingState.category,
                                onValueChange = { viewModel.updateCategory(it) },
                                placeholder = { Text("p.ej. Ciencias") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                isError = editingState.validationErrors.containsKey("category"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tiempo limite (min)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editingState.durationMins,
                                onValueChange = { viewModel.updateDuration(it) },
                                placeholder = { Text("p.ej. 45") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                isError = editingState.validationErrors.containsKey("duration"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }
                    }
                }
                
                // Questions Section Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Preguntas",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(${editingState.questions.size})",
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
                                text = "Auto-guardado habilitado",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Show questions-level validation error
                    editingState.validationErrors["questions"]?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                items(editingState.questions.size, key = { editingState.questions[it].id }) { i ->
                    val question = editingState.questions[i]
                    val itemIndex = i + 2
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
                            questionNumber = i + 1,
                            question = question,
                            primaryBlue = primaryBlue,
                            validationErrors = editingState.validationErrors,
                            onPromptChange = { viewModel.updateQuestionPrompt(question.id, it) },
                            onOptionTextChange = { optIndex, text ->
                                viewModel.updateOptionText(question.id, optIndex, text)
                            },
                            onToggleCorrect = { optIndex ->
                                viewModel.toggleOptionCorrect(question.id, optIndex)
                            },
                            onAddOption = { viewModel.addOption(question.id) },
                            onRemoveOption = { optIndex ->
                                viewModel.removeOption(question.id, optIndex)
                            },
                            onDeleteQuestion = { viewModel.removeQuestion(question.id) }
                        )
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { viewModel.addQuestion() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = primaryBlue
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Agregar nueva pregunta", fontWeight = FontWeight.Bold)
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
                            modifier = Modifier.weight(1f).height(48.dp).background(Color.White).clip(RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, primaryBlue),
                            enabled = !editingState.isSaving
                        ) {
                            Text("Cancelar", color = primaryBlue, fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = { viewModel.saveExam() },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                            enabled = !editingState.isSaving
                        ) {
                            if (editingState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Guardando...", fontWeight = FontWeight.Bold)
                            } else {
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
}

@Composable
fun QuestionCard(
    questionNumber: Int,
    question: EditableQuestion,
    primaryBlue: Color,
    validationErrors: Map<String, String> = emptyMap(),
    onPromptChange: (String) -> Unit = {},
    onOptionTextChange: (Int, String) -> Unit = { _, _ -> },
    onToggleCorrect: (Int) -> Unit = {},
    onAddOption: () -> Unit = {},
    onRemoveOption: (Int) -> Unit = {},
    onDeleteQuestion: () -> Unit = {}
) {
    val promptErrorKey = "question_${question.id}_prompt"
    val optionsErrorKey = "question_${question.id}_options"
    val correctErrorKey = "question_${question.id}_correct"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                            text = "Pregunta $questionNumber",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = primaryBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                IconButton(onClick = onDeleteQuestion) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Borrar pregunta",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "ENUNCIADO DE PREGUNTA",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = question.prompt,
                onValueChange = onPromptChange,
                placeholder = { Text("Ingresa el enunciado de la pregunta...") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = RoundedCornerShape(12.dp),
                isError = validationErrors.containsKey(promptErrorKey),
                supportingText = validationErrors[promptErrorKey]?.let { err ->
                    { Text(err, color = MaterialTheme.colorScheme.error) }
                },
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
                    text = "RESPUESTAS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onAddOption,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Option", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Agregar opción", fontSize = 12.sp)
                }
            }

            // Show options-level or correct-answer validation errors
            validationErrors[optionsErrorKey]?.let { error ->
                Text(text = error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            validationErrors[correctErrorKey]?.let { error ->
                Text(text = error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                question.options.forEachIndexed { index, option ->
                    val letter = ('A' + index).toString()
                    val optionErrorKey = "question_${question.id}_option_$index"
                    AnswerOptionRow(
                        letter = letter,
                        text = option.text,
                        isCorrect = option.isCorrect,
                        primaryBlue = primaryBlue,
                        isError = validationErrors.containsKey(optionErrorKey),
                        onTextChange = { onOptionTextChange(index, it) },
                        onToggleCorrect = { onToggleCorrect(index) },
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
    isError: Boolean = false,
    onTextChange: (String) -> Unit = {},
    onToggleCorrect: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            1.dp,
            if (isError) MaterialTheme.colorScheme.error
            else if (isCorrect) primaryBlue
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isCorrect) primaryBlue.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCorrect) primaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))

            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Ingresar respuesta...", style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = primaryBlue.copy(alpha = 0.5f)
                )
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isCorrect,
                    onCheckedChange = { onToggleCorrect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = primaryBlue
                    )
                )
                Text(
                    text = "Correcta",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCorrect) primaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Borrar opción",
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
