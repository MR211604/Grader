package com.example.grader.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Info
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
import com.example.grader.firebase.FirestoreHelper
import com.example.grader.ui.components.GraderBottomNavigation
import com.example.grader.ui.components.NavRoute
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Assessment(
    val id: String = "mock_id",
    val title: String,
    val department: String,
    val status: String,
    val questions: Int,
    val durationMins: Int,
    val modifiedDate: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    currentRoute: NavRoute = NavRoute.EXAMS,
    onNavigate: (NavRoute) -> Unit = {},
    onNavigateToCreateExam: () -> Unit = {},
    onEditExam: (String) -> Unit = {},
    teacherId: String = "default_teacher_id" // Ideally passed from Auth
) {
    val primaryBlue = Color(0xFF0C5CBF)

    var assessments by remember { mutableStateOf<List<Assessment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val firestoreHelper = object : Any() {
                suspend fun fetchExams() = FirestoreHelper().getExamsByTeacher(teacherId)
            }
            val exams = firestoreHelper.fetchExams()
            val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            
            assessments = exams.map { exam ->
                val dateObj = Date(exam.createdAt)
                Assessment(
                    id = exam.id,
                    title = exam.title,
                    department = exam.course,
                    status = exam.status,
                    questions = exam.questionCount,
                    durationMins = exam.durationMins,
                    modifiedDate = dateFormatter.format(dateObj).uppercase()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = primaryBlue,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.School,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Grader - Panel administrativo",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp
                        )
                    }
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateExam,
                containerColor = primaryBlue,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Assessment")
            }
        },
        bottomBar = {
            GraderBottomNavigation(
                currentRoute = currentRoute,
                onNavigate = onNavigate,
                isAdmin = true
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Evaluaciones activas",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Maneja las evaluaciones de tus estudiantes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(primaryBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = assessments.size.toString(), // You can change this to assessments.size.toString() if needed
                        color = primaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // List of Assessments
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(assessments) { assessment ->
                        AssessmentCard(
                            assessment = assessment,
                            primaryBlue = primaryBlue,
                            onEditExam = onEditExam
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // Extra space for FAB
                    }
                }
            }
        }
    }
}

@Composable
fun AssessmentCard(
    assessment: Assessment,
    primaryBlue: Color,
    onEditExam: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.4f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = assessment.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = assessment.department,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Status Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (assessment.status) {
                        "active" -> Color(0xFF0C8CBF).copy(alpha = 0.1f)
                        "draft" -> Color(0xFF818181).copy(alpha = 0.1f)
                        else -> Color(0xFF0CBF15).copy(alpha = 0.1f)
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = assessment.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when (assessment.status) {
                            "active" -> Color(0xFF0C8CBF)
                            "draft" -> Color(0xFF818181)
                            else -> Color(0xFF0CBF15)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = primaryBlue
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${assessment.questions} Preguntas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = primaryBlue
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${assessment.durationMins} mins",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Footer Row
            Row(
                modifier = Modifier.fillMaxWidth().height(32.dp).clickable(
                    enabled = true,
                    onClick = { onEditExam(assessment.id) },
                ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MODIFICADO: ${assessment.modifiedDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    MaterialTheme {
        DashboardScreen()
    }
}
