package com.example.grader.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grader.contants.BACKGROUND_COLOR
import com.example.grader.contants.DIVIDER_COLOR
import com.example.grader.firebase.FirestoreHelper
import com.example.grader.models.QuizSubmission
import com.example.grader.ui.components.GraderBottomNavigation
import com.example.grader.ui.components.NavRoute
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsView(
    currentRoute: NavRoute = NavRoute.STATS,
    onNavigate: (NavRoute) -> Unit = {},
    studentId: String = "mockStudentId"
) {
    val primaryBlue = Color(0xFF0C5CBF)
    var results by remember { mutableStateOf<List<QuizSubmission>>(emptyList()) }
    var chartData by remember { mutableStateOf<List<Pair<String, Float>>>(emptyList()) }
    var trend by remember { mutableStateOf("+0.0%") }
    var avgScoreLabel by remember { mutableStateOf("0%") }
    var successRateLabel by remember { mutableStateOf("0%") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val firestoreHelper = object : Any() {
                suspend fun fetchSubmissions() = FirestoreHelper().getSubmissionsFromStudent(studentId)
                suspend fun fetchAverages() = FirestoreHelper().getAverageScoresLast6Months(studentId)
            }
            val submissions = firestoreHelper.fetchSubmissions()
            val averages = firestoreHelper.fetchAverages()
            val dateFormatter = java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

            chartData = averages

            val allAvg = if (submissions.isNotEmpty() && submissions.any { it.total > 0 }) {
                submissions.filter { it.total > 0 }.map { it.score.toFloat() / it.total * 100 }.average().toFloat()
            } else 0f
            avgScoreLabel = String.format(Locale.US, "%.1f%%", allAvg)

            val passes = submissions.count { it.total > 0 && it.score.toFloat() / it.total >= 0.6f }
            val successRate = if (submissions.isNotEmpty()) (passes.toFloat() / submissions.size) * 100 else 0f
            successRateLabel = String.format(Locale.US, "%.0f%%", successRate)

            val nonZero = averages.filter { it.second > 0 }
            if (nonZero.size >= 2) {
                val current = nonZero.last().second
                val prev = nonZero[nonZero.size - 2].second
                val diff = current - prev
                val sign = if (diff >= 0) "+" else ""
                trend = String.format(Locale.US, "$sign%.1f%%", diff)
            }

            results = submissions.map { submission ->
                val dateObj = Date(submission.submittedAt)
                QuizSubmission(
                    id = submission.id,
                    examId =  submission.examId,
                    studentId = submission.studentId,
                    score = submission.score,
                    total = submission.total,
                    answers = submission.answers,
                    timeSpentSeconds = submission.timeSpentSeconds,
                    submittedAt = submission.submittedAt,
                    modifiedDate = dateFormatter.format(dateObj).uppercase(),
                    examTitle = submission.examTitle,
                    course = submission.course
                )
            }.sortedByDescending { it.submittedAt }
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
                            text = "Grader - Estadísticas",
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
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = borderSize
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            GraderBottomNavigation(
                currentRoute = currentRoute,
                onNavigate = onNavigate
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BACKGROUND_COLOR)
        ) {
            HorizontalDivider(color = DIVIDER_COLOR, thickness = 1.dp)

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Header
                    item {
                        Column {
                            Text(
                                text = "Visualización de rendimiento",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Lleva el control de tu progreso académico y registro de evaluaciones.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Stats Cards
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            StatCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Outlined.TrendingUp,
                                value = avgScoreLabel,
                                label = "NOTA PROM.",
                                iconColor = primaryBlue
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Outlined.MenuBook,
                                value = results.size.toString(),
                                label = "EXAMENES",
                                iconColor = Color(0xFF00ACC1)
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Outlined.CheckCircle,
                                value = successRateLabel,
                                label = "APROBACIÓN",
                                iconColor = Color(0xFF333333)
                            )
                        }
                    }

                    // Chart Section
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column {
                                        Text(
                                            text = "Tendencias de notas",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = "Rendimiento promedio (Últimos 6 meses)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.TrendingUp,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = trend,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                val barData = if (chartData.isNotEmpty()) chartData.map { it.second } else listOf(0f, 0f, 0f, 0f, 0f, 0f)
                                val months = if (chartData.isNotEmpty()) chartData.map { it.first } else listOf("-", "-", "-", "-", "-", "-")
                                val maxVal = 100f

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxHeight().padding(bottom = 24.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "100",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "75",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "50",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "25",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "0",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Draw chart area
                                    Box(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                        // Background grid lines
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val dashEffect =
                                                PathEffect.dashPathEffect(
                                                    floatArrayOf(10f, 10f),
                                                    0f
                                                )
                                            val lineColor = Color.LightGray.copy(alpha = 0.5f)
                                            for (i in 0..4) {
                                                val y = size.height * (i / 4f)
                                                drawLine(
                                                    color = lineColor,
                                                    start = Offset(0f, y),
                                                    end = Offset(size.width, y),
                                                    strokeWidth = 1f,
                                                    pathEffect = dashEffect
                                                )
                                            }
                                        }

                                        // Bars
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.SpaceAround,
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            barData.forEachIndexed { index, value ->
                                                val fraction = value / maxVal

                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Bottom,
                                                    modifier = Modifier.fillMaxHeight()
                                                ) {
                                                    if (1f - fraction > 0f) {
                                                        Spacer(modifier = Modifier.weight(1f - fraction))
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .width(28.dp)
                                                            .weight(fraction.coerceAtLeast(0.01f))
                                                            .clip(
                                                                RoundedCornerShape(
                                                                    topStart = 4.dp,
                                                                    topEnd = 4.dp
                                                                )
                                                            )
                                                            .background(primaryBlue)
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = months[index],
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Recent Results Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Resultados recientes",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    // Results List
                    items(results.takeLast(4)) { result ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Blue vertical indicator
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(primaryBlue.copy(alpha = 0.6f))
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = result.examTitle,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.CalendarToday,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = result.modifiedDate,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                        ) {
                                            Text(
                                                text = result.course,
                                                modifier = Modifier.padding(
                                                    horizontal = 6.dp,
                                                    vertical = 2.dp
                                                ),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.sp
                                                ),
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "${result.score}/${result.total}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = primaryBlue
                                )

                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    iconColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StatsViewPreview() {
    MaterialTheme {
        StatsView()
    }
}