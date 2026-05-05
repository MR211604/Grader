package com.example.grader.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.grader.contants.BACKGROUND_COLOR
import com.example.grader.contants.DIVIDER_COLOR
import com.example.grader.models.Exam
import com.example.grader.models.Question
import com.example.grader.models.QuizUiState
import com.example.grader.ui.viewmodels.QuizViewModel

// ─── Color Palette ───────────────────────────────────────────────────────────
private val PrimaryBlue = Color(0xFF0C5CBF)
private val LightBlueBackground = Color(0xFFE8F0FE)
private val SelectedBorder = Color(0xFF0C5CBF)
private val UnselectedBorder = Color(0xFFE0E0E0)
private val SurfaceWhite = Color.White
private val CorrectGreen = Color(0xFF2E7D32)
private val CorrectGreenBg = Color(0xFFE8F5E9)
private val IncorrectRed = Color(0xFFC62828)
private val IncorrectRedBg = Color(0xFFFFEBEE)
private val TimerWarning = Color(0xFFE65100)

// ─── Main QuizScreen ─────────────────────────────────────────────────────────

/**
 * Main quiz screen composable.
 *
 * Observes [QuizViewModel.uiState] and renders the appropriate UI
 * based on the current [QuizUiState].
 *
 * @param examId The Firestore document ID of the exam to take.
 * @param onNavigateBack Callback to navigate back to the student screen.
 * @param onQuizFinished Callback when quiz is finished and user wants to go back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    examId: String,
    onNavigateBack: () -> Unit,
    onQuizFinished: (score: Int, total: Int) -> Unit = { _, _ -> },
    quizViewModel: QuizViewModel = viewModel()
) {
    val uiState by quizViewModel.uiState.collectAsStateWithLifecycle()

    // Load exam when the screen first appears
    LaunchedEffect(examId) {
        quizViewModel.loadExam(examId)
    }

    when (val state = uiState) {
        is QuizUiState.Loading -> QuizLoadingScreen()

        is QuizUiState.Active -> QuizActiveScreen(
            state = state,
            onSelectAnswer = quizViewModel::selectAnswer,
            onNextQuestion = quizViewModel::nextQuestion,
            onPreviousQuestion = quizViewModel::previousQuestion,
            onSubmitQuiz = quizViewModel::submitQuiz,
            onNavigateBack = onNavigateBack
        )

        is QuizUiState.Finished -> QuizResultScreen(
            state = state,
            onGoBack = {
                onQuizFinished(state.score, state.total)
                onNavigateBack()
            }
        )

        is QuizUiState.Error -> QuizErrorScreen(
            message = state.message,
            onRetry = { quizViewModel.loadExam(examId) },
            onGoBack = onNavigateBack
        )
    }
}

// ─── Loading Screen ──────────────────────────────────────────────────────────

@Composable
private fun QuizLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = PrimaryBlue)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Cargando examen...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Active Quiz Screen ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuizActiveScreen(
    state: QuizUiState.Active,
    onSelectAnswer: (Int) -> Unit,
    onNextQuestion: () -> Unit,
    onPreviousQuestion: () -> Unit,
    onSubmitQuiz: () -> Unit,
    onNavigateBack: () -> Unit
) {
    // Submitting overlay
    if (state.isSubmitting) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Enviando respuestas...", fontWeight = FontWeight.Medium)
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Pregunta ${state.displayNumber}/${state.totalQuestions}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (state.isFirstQuestion) onNavigateBack() else onPreviousQuestion()
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Atrás"
                            )
                        }
                    },
                    actions = {
                        // Timer display
                        if (state.remainingSeconds >= 0) {
                            val isWarning = state.remainingSeconds < 60
                            val timerColor = if (isWarning) TimerWarning else MaterialTheme.colorScheme.onSurfaceVariant
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Timer,
                                    contentDescription = "Timer",
                                    tint = timerColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = state.formattedTime,
                                    color = timerColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SurfaceWhite,
                    )
                )
                HorizontalDivider(color = DIVIDER_COLOR, thickness = 1.dp)
            }
        },
        containerColor = BACKGROUND_COLOR
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // ─── Quiz Progress Bar ───
            QuizProgressSection(
                progress = state.progress,
                progressPercent = state.progressPercent
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ─── Question Card ───
            QuestionCardComposable(
                question = state.currentQuestion,
                questionType = state.currentQuestion.type
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ─── Answer Options ───
            state.currentQuestion.options.forEachIndexed { index, optionText ->
                val letter = ('A' + index).toString()
                val isSelected = state.currentSelectedOption == index

                AnswerOptionCardComposable(
                    letter = letter,
                    text = optionText,
                    isSelected = isSelected,
                    onClick = { onSelectAnswer(index) }
                )

                if (index < state.currentQuestion.options.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ─── Submit / Next Button ───
            Button(
                onClick = {
                    if (state.isLastQuestion) {
                        onSubmitQuiz()
                    } else {
                        onNextQuestion()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                enabled = state.currentSelectedOption != null
            ) {
                Text(
                    text = if (state.isLastQuestion) "Subir respuesta" else "Subir respuesta",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─── Progress Section ────────────────────────────────────────────────────────

@Composable
private fun QuizProgressSection(
    progress: Float,
    progressPercent: Int
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PROGRESO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF757575),
                letterSpacing = 1.sp
            )
            Text(
                text = "$progressPercent%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = PrimaryBlue,
            trackColor = Color(0xFFE0E0E0),
        )
    }
}

// ─── Question Card ───────────────────────────────────────────────────────────

@Composable
private fun QuestionCardComposable(
    question: Question,
    questionType: String
) {
    val displayType = when (questionType) {
        "multiple_choice" -> "OPCIÓN MÚLTIPLE"
        "true_false" -> "TRUE / FALSE"
        else -> questionType.uppercase()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, UnselectedBorder)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Blue left border accent
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(PrimaryBlue)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Type label with icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.HelpOutline,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = displayType,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Question text
                Text(
                    text = question.question,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1E1E),
                    lineHeight = 26.sp
                )
            }
        }
    }
}

// ─── Answer Option Card ──────────────────────────────────────────────────────

@Composable
private fun AnswerOptionCardComposable(
    letter: String,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) SelectedBorder else UnselectedBorder,
        animationSpec = tween(200),
        label = "borderColor"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) LightBlueBackground else SurfaceWhite,
        animationSpec = tween(200),
        label = "backgroundColor"
    )
    val letterBgColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryBlue else Color(0xFFF5F5F5),
        animationSpec = tween(200),
        label = "letterBgColor"
    )
    val letterTextColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFF616161),
        animationSpec = tween(200),
        label = "letterTextColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Letter circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(letterBgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = letterTextColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Option text
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = Color(0xFF333333),
                lineHeight = 22.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─── Result Screen ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuizResultScreen(
    state: QuizUiState.Finished,
    onGoBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.drawBehind {
                    val borderSize = 1.dp.toPx()
                    drawLine(
                        color = Color(0xFFE0E0E0),
                        start = androidx.compose.ui.geometry.Offset(0f, size.height),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                        strokeWidth = borderSize
                    )
                },
                title = {
                    Text(
                        "Quiz Results",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BACKGROUND_COLOR)
            )
            HorizontalDivider(color = DIVIDER_COLOR, thickness = 1.dp)
        },
        containerColor = BACKGROUND_COLOR
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Score circle
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(
                        if (state.scorePercent >= 60) CorrectGreenBg else IncorrectRedBg
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${state.score}/${state.total}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state.scorePercent >= 60) CorrectGreen else IncorrectRed
                    )
                    Text(
                        text = "${state.scorePercent}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (state.scorePercent >= 60) CorrectGreen else IncorrectRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = state.exam.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1E1E)
            )
            Text(
                text = state.exam.course,
                fontSize = 14.sp,
                color = Color(0xFF757575)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Review section header
            Text(
                text = "REVISIÓN DE RESPUESTAS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF757575),
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Question review list
            state.questions.forEachIndexed { index, question ->
                val selectedAnswer = state.answers[index]
                val isCorrect = selectedAnswer == question.correctAnswerIndex

                ReviewQuestionCard(
                    questionNumber = index + 1,
                    questionText = question.question,
                    options = question.options,
                    selectedIndex = selectedAnswer,
                    correctIndex = question.correctAnswerIndex,
                    isCorrect = isCorrect
                )

                if (index < state.questions.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Back button
            Button(
                onClick = onGoBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(
                    text = "Volver a Exámenes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─── Review Question Card ────────────────────────────────────────────────────

@Composable
private fun ReviewQuestionCard(
    questionNumber: Int,
    questionText: String,
    options: List<String>,
    selectedIndex: Int?,
    correctIndex: Int,
    isCorrect: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect) CorrectGreenBg.copy(alpha = 0.3f) else IncorrectRedBg.copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            1.dp,
            if (isCorrect) CorrectGreen.copy(alpha = 0.3f) else IncorrectRed.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Pregunta $questionNumber",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF333333)
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = if (isCorrect) CorrectGreen else IncorrectRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isCorrect) "Correcta" else "Incorrecta",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCorrect) CorrectGreen else IncorrectRed
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = questionText,
                fontSize = 14.sp,
                color = Color(0xFF333333),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            options.forEachIndexed { index, option ->
                val letter = ('A' + index).toString()
                val isThisSelected = selectedIndex == index
                val isThisCorrect = correctIndex == index

                val bgColor = when {
                    isThisCorrect -> CorrectGreenBg
                    isThisSelected && !isThisCorrect -> IncorrectRedBg
                    else -> Color(0xFFF5F5F5)
                }
                val textColor = when {
                    isThisCorrect -> CorrectGreen
                    isThisSelected && !isThisCorrect -> IncorrectRed
                    else -> Color(0xFF616161)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .background(bgColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$letter.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = option,
                        fontSize = 13.sp,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )
                    if (isThisCorrect) {
                        Text("✓", color = CorrectGreen, fontWeight = FontWeight.Bold)
                    } else if (isThisSelected) {
                        Text("✗", color = IncorrectRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─── Error Screen ────────────────────────────────────────────────────────────

@Composable
private fun QuizErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onGoBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Error",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = IncorrectRed
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = Color(0xFF757575)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Reintentar", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onGoBack) {
                Text("Volver", color = PrimaryBlue, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Previews ────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun QuizActivePreview() {
    val mockState = QuizUiState.Active(
        exam = Exam(
            id = "1",
            title = "Monthly Assessment",
            course = "UI/UX Design",
            questionCount = 10,
            durationMins = 35,
            type = "multiple_choice"
        ),
        questions = listOf(
            Question(
                id = "q1",
                question = "Which of the following describes the 'Material Design 3' principle of color harmony?",
                options = listOf(
                    "Static color palettes defined by fixed hex values.",
                    "Dynamic color schemes generated from a user's wallpaper.",
                    "Grayscale only for maximum accessibility across all devices.",
                    "Randomly generated palettes to ensure visual uniqueness."
                ),
                correctAnswerIndex = 1,
                type = "multiple_choice"
            )
        ),
        currentIndex = 0,
        selectedAnswers = emptyMap(),
        remainingSeconds = 1260
    )

    MaterialTheme {
        QuizActiveScreen(
            state = mockState,
            onSelectAnswer = {},
            onNextQuestion = {},
            onPreviousQuestion = {},
            onSubmitQuiz = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun QuizResultPreview() {
    val mockState = QuizUiState.Finished(
        score = 7,
        total = 10,
        exam = Exam(id = "1", title = "Monthly Assessment", course = "UI/UX Design"),
        answers = mapOf(0 to 1, 1 to 0, 2 to 2),
        questions = listOf(
            Question(
                id = "q1",
                question = "Which describes Material Design 3 color harmony?",
                options = listOf("Static palettes", "Dynamic from wallpaper", "Grayscale only", "Random palettes"),
                correctAnswerIndex = 1
            ),
            Question(
                id = "q2",
                question = "What is Jetpack Compose?",
                options = listOf("A database", "UI toolkit", "Network library", "Build tool"),
                correctAnswerIndex = 1
            ),
            Question(
                id = "q3",
                question = "What is StateFlow?",
                options = listOf("A widget", "A layout", "A reactive stream", "A database"),
                correctAnswerIndex = 2
            )
        )
    )

    MaterialTheme {
        QuizResultScreen(state = mockState, onGoBack = {})
    }
}
