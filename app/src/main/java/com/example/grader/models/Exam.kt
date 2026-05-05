package com.example.grader.models

/**
 * Represents an exam/evaluation stored in Firestore.
 *
 * Mapped to: `evaluations/{examId}`
 */
data class Exam(
    var id: String = "",
    var title: String = "",
    var course: String = "",
    var questionCount: Int = 0,
    var durationMins: Int = 0,
    var type: String = "multiple_choice",
    var status: String = "draft",
    var createdAt: Long = System.currentTimeMillis()
)