package com.example.grader.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grader.firebase.FirestoreHelper
import com.example.grader.models.Course
import com.example.grader.ui.Assessment
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

class DashboardViewModel(
    private val firestoreHelper: FirestoreHelper = FirestoreHelper()
) : ViewModel() {

    private val _allAssessments = MutableStateFlow<List<Assessment>>(emptyList())

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _dateSortOrder = MutableStateFlow(DateSortOrder.NEWEST_FIRST)
    val dateSortOrder: StateFlow<DateSortOrder> = _dateSortOrder.asStateFlow()

    private val _selectedCourse = MutableStateFlow<String?>(null)
    val selectedCourse: StateFlow<String?> = _selectedCourse.asStateFlow()

    val filteredAssessments: StateFlow<List<Assessment>> = combine(
        _allAssessments,
        _searchQuery,
        _dateSortOrder,
        _selectedCourse
    ) { assessments, query, sortOrder, course ->
        var result = assessments

        if (query.isNotBlank()) {
            result = result.filter { it.title.contains(query, ignoreCase = true) }
        }

        if (course != null) {
            result = result.filter { it.department == course }
        }

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

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleDateSortOrder() {
        _dateSortOrder.value = when (_dateSortOrder.value) {
            DateSortOrder.NEWEST_FIRST -> DateSortOrder.OLDEST_FIRST
            DateSortOrder.OLDEST_FIRST -> DateSortOrder.NEWEST_FIRST
        }
    }

    fun onCourseSelected(courseName: String?) {
        _selectedCourse.value = courseName
    }

    fun deleteExam(examId: String, teacherId: String) {
        viewModelScope.launch {
            try {
                firestoreHelper.deleteExam(examId)
                loadData(teacherId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadData(teacherId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

                val exams = firestoreHelper.getExamsByTeacher(teacherId)
                val coursesList = firestoreHelper.getCourses()

                _courses.value = coursesList

                _allAssessments.value = exams.map { exam ->
                    val dateObj = Date(exam.createdAt)
                    Assessment(
                        id = exam.id,
                        title = exam.title,
                        department = exam.course,
                        status = exam.status,
                        questions = exam.questionCount,
                        durationMins = exam.durationMins,
                        modifiedDate = dateFormatter.format(dateObj).uppercase(),
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
