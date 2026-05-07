package com.example.grader.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.grader.contants.BACKGROUND_COLOR
import com.example.grader.ui.components.GraderBottomNavigation
import com.example.grader.ui.components.NavRoute
import com.example.grader.ui.viewmodels.DateSortOrder
import com.example.grader.ui.viewmodels.StudentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentScreen(
    studentId: String = "",
    currentRoute: NavRoute = NavRoute.EXAMS,
    onNavigate: (NavRoute) -> Unit = {},
    onStartExam: (String) -> Unit = {},
    viewModel: StudentViewModel = viewModel()
) {
    val primaryDark = Color(0xFF1E2772) // Dark blue background for header

    val assessments by viewModel.filteredAssessments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val dateSortOrder by viewModel.dateSortOrder.collectAsState()
    val selectedCourse by viewModel.selectedCourse.collectAsState()
    val courses by viewModel.courses.collectAsState()

    // Track whether the category filter row is visible
    var showCategoryFilter by remember { mutableStateOf(false) }

    LaunchedEffect(studentId) {
        viewModel.loadData(studentId)
    }

    Scaffold(
        bottomBar = {
            GraderBottomNavigation(
                currentRoute = currentRoute,
                onNavigate = onNavigate
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BACKGROUND_COLOR)
                .padding(innerPadding)
        ) {
            // Header Section Background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(primaryDark)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Header Content
                Column(
                    modifier = Modifier.padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 28.dp)
                ) {
                    Text(
                        text = "Evaluaciones",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Maneja y controla tu progreso académico",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }

                // Body Section
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // ─── 1. Search Bar (filters by exam title) ──────────────
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text("Buscar por título...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ─── 2 & 3. Sort by date + Category filter toggle ───────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Date sort toggle button
                        OutlinedButton(
                            onClick = { viewModel.toggleDateSortOrder() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF333333)
                            ),
                            border = borderStroke()
                        ) {
                            Icon(Icons.Outlined.SwapVert, contentDescription = "Sort", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (dateSortOrder) {
                                    DateSortOrder.NEWEST_FIRST -> "Fecha (Reciente)"
                                    DateSortOrder.OLDEST_FIRST -> "Fecha (Antiguo)"
                                },
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }

                        // Category filter toggle button
                        OutlinedButton(
                            onClick = { showCategoryFilter = !showCategoryFilter },
                            modifier = Modifier.size(48.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selectedCourse != null || showCategoryFilter)
                                    Color(0xFFE8EAF6) else Color.White,
                                contentColor = if (selectedCourse != null || showCategoryFilter)
                                    Color(0xFF1E2772) else Color(0xFF333333)
                            ),
                            border = borderStroke()
                        ) {
                            Icon(Icons.Outlined.FilterAlt, contentDescription = "Filter")
                        }
                    }

                    // ─── 3. Category chip row (animated visibility) ─────────
                    AnimatedVisibility(
                        visible = showCategoryFilter,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // "Todas" chip — clears the category filter
                                FilterChip(
                                    selected = selectedCourse == null,
                                    onClick = { viewModel.onCourseSelected(null) },
                                    label = { Text("Todas") },
                                    leadingIcon = if (selectedCourse == null) {
                                        {
                                            Icon(
                                                Icons.Outlined.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    shape = RoundedCornerShape(20.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF1E2772),
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White
                                    )
                                )

                                // One chip per course fetched from Firestore
                                courses.forEach { course ->
                                    FilterChip(
                                        selected = selectedCourse == course.name,
                                        onClick = {
                                            viewModel.onCourseSelected(
                                                if (selectedCourse == course.name) null else course.name
                                            )
                                        },
                                        label = { Text(course.name) },
                                        leadingIcon = if (selectedCourse == course.name) {
                                            {
                                                Icon(
                                                    Icons.Outlined.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null,
                                        shape = RoundedCornerShape(20.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF1E2772),
                                            selectedLabelColor = Color.White,
                                            selectedLeadingIconColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Section Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LISTA DE EVALUACIONES",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF757575),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${assessments.size} Evaluaciones",
                            fontSize = 12.sp,
                            color = Color(0xFFA0A0A0)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (assessments.isEmpty()) {
                        // Empty state when filters yield no results
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = Color(0xFFBDBDBD)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No se encontraron evaluaciones",
                                    fontSize = 14.sp,
                                    color = Color(0xFF9E9E9E)
                                )
                            }
                        }
                    } else {
                        // List
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            items(assessments) { assessment ->
                                AssessmentCard(
                                    item = assessment,
                                    onViewDetails = { onStartExam(assessment.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun borderStroke() = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))

enum class AssessmentStatus {
    Pending, Completed, Missed
}

data class AssessmentItem(
    val id: String,
    val title: String,
    val course: String,
    val date: String,
    val time: String,
    val questionsCount: Int,
    val status: AssessmentStatus,
    /** Raw timestamp for date sorting. */
    val createdAtMillis: Long = 0L
)

@Composable
fun AssessmentCard(item: AssessmentItem, onViewDetails: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = borderStroke(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1E1E1E)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF757575)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.course,
                            fontSize = 13.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }
                
                // Status Chip
                StatusChip(status = item.status)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date & Time Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF9E9E9E)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.date,
                        fontSize = 13.sp,
                        color = Color(0xFF616161),
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                    .weight(1f)
                    .padding(start = 24.dp)) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF9E9E9E)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.time,
                        fontSize = 13.sp,
                        color = Color(0xFF616161),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.questionsCount} Preguntas",
                    fontSize = 13.sp,
                    color = Color(0xFF9E9E9E)
                )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onViewDetails() }
                    ) {
                        Text(
                            text = "Realizar examen",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E2772)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = "View Details",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF1E2772)
                        )
                    }
            }
        }
    }
}

@Composable
fun StatusChip(status: AssessmentStatus) {
    val backgroundColor: Color
    val textColor: Color
    val borderColor: Color?
    val icon: androidx.compose.ui.graphics.vector.ImageVector?
    
    when (status) {
        AssessmentStatus.Pending -> {
            backgroundColor = Color(0xFFF5F5F5)
            textColor = Color(0xFF616161)
            borderColor = Color(0xFFE0E0E0)
            icon = Icons.Outlined.Schedule
        }
        AssessmentStatus.Completed -> {
            backgroundColor = Color(0xFFE8F5E9)
            textColor = Color(0xFF2E7D32)
            borderColor = null
            icon = Icons.Outlined.Check
        }
        AssessmentStatus.Missed -> {
            backgroundColor = Color(0xFFFFEBEE)
            textColor = Color(0xFFC62828)
            borderColor = null
            icon = Icons.Outlined.ErrorOutline
        }
    }

    Row(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(16.dp))
            .run {
                if (borderColor != null) border(
                    1.dp,
                    borderColor,
                    RoundedCornerShape(16.dp)
                ) else this
            }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = status.name,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview(showBackground = true)
@Composable
fun StudentScreenPreview() {
    MaterialTheme {
        StudentScreen()
    }
}
