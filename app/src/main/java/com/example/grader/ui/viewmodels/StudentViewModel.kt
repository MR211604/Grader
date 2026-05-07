package com.example.grader.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grader.firebase.FirestoreHelper
import com.example.grader.models.Course
import com.example.grader.ui.AssessmentItem
import com.example.grader.ui.AssessmentStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Enum representing the date sort order for assessments.
 */
enum class DateSortOrder {
    /** Most recent first. */
    NEWEST_FIRST,

    /** Oldest first. */
    OLDEST_FIRST
}

/**
 * ViewModel that manages student exam listing with search, date sorting,
 * and category (course) filtering capabilities.
 *
 * Uses [combine] to reactively merge the master list with all active filters,
 * producing a single [filteredAssessments] StateFlow that the UI observes.
 */
class StudentViewModel(
    private val firestoreHelper: FirestoreHelper = FirestoreHelper()
) : ViewModel() {

    // ── Raw data ────────────────────────────────────────────────────────
    private val _allAssessments = MutableStateFlow<List<AssessmentItem>>(emptyList())

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    /** Available course categories for the filter chip list. */
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    /** True while the initial data fetch is in progress. */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ── Filter inputs ───────────────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    /** The current text in the search bar. */
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _dateSortOrder = MutableStateFlow(DateSortOrder.NEWEST_FIRST)
    /** The current date sort direction. */
    val dateSortOrder: StateFlow<DateSortOrder> = _dateSortOrder.asStateFlow()

    private val _selectedCourse = MutableStateFlow<String?>(null)
    /** The currently selected course name for filtering, or null for "all". */
    val selectedCourse: StateFlow<String?> = _selectedCourse.asStateFlow()

    // ── Derived filtered list ───────────────────────────────────────────
    /**
     * Reactive combination of every filter applied to the master list.
     *
     * Recomputes automatically whenever [_allAssessments], [_searchQuery],
     * [_dateSortOrder], or [_selectedCourse] emits a new value.
     */
    val filteredAssessments: StateFlow<List<AssessmentItem>> = combine(
        _allAssessments,
        _searchQuery,
        _dateSortOrder,
        _selectedCourse
    ) { assessments, query, sortOrder, course ->
        var result = assessments

        // 1. Filter by search query (title match, case-insensitive)
        if (query.isNotBlank()) {
            result = result.filter { it.title.contains(query, ignoreCase = true) }
        }

        // 2. Filter by category (course)
        if (course != null) {
            result = result.filter { it.course == course }
        }

        // 3. Sort by creation date
        result = when (sortOrder) {
            DateSortOrder.NEWEST_FIRST -> result.sortedByDescending { it.createdAtMillis }
            DateSortOrder.OLDEST_FIRST -> result.sortedBy { it.createdAtMillis }
        }

        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    // ── Public actions ──────────────────────────────────────────────────

    /**
     * Updates the search query used to filter assessments by title.
     *
     * @param query The text typed into the search bar.
     */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    /**
     * Toggles the date sort order between newest-first and oldest-first.
     */
    fun toggleDateSortOrder() {
        _dateSortOrder.value = when (_dateSortOrder.value) {
            DateSortOrder.NEWEST_FIRST -> DateSortOrder.OLDEST_FIRST
            DateSortOrder.OLDEST_FIRST -> DateSortOrder.NEWEST_FIRST
        }
    }

    /**
     * Selects a course to filter by, or clears the filter if [courseName] is null.
     *
     * @param courseName The course name to filter by, or null to show all.
     */
    fun onCourseSelected(courseName: String?) {
        _selectedCourse.value = courseName
    }

    /**
     * Loads all student exams and available courses from Firestore.
     *
     * @param studentId The current student's ID, used to check submission status.
     */
    fun loadData(studentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())
                val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

                // Fetch exams and courses concurrently
                val exams = firestoreHelper.getStudentExams()
                val coursesList = firestoreHelper.getCourses()

                _courses.value = coursesList

                _allAssessments.value = exams.map { exam ->
                    val dateObj = Date(exam.createdAt)
                    val isCompleted = if (studentId.isNotEmpty()) {
                        firestoreHelper.hasStudentSubmittedExam(exam.id, studentId)
                    } else false

                    AssessmentItem(
                        id = exam.id,
                        title = exam.title,
                        course = exam.course,
                        date = dateFormatter.format(dateObj),
                        time = timeFormatter.format(dateObj),
                        questionsCount = exam.questionCount,
                        status = if (isCompleted) AssessmentStatus.Completed else AssessmentStatus.Pending,
                        createdAtMillis = exam.createdAt
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
