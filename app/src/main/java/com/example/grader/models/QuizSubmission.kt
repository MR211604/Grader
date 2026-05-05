package com.example.grader.models

/**
 * Represents a student's submission for an exam.
 *
 * Stored in Firestore at: `submissions/{submissionId}`
 *
 * [answers] maps question index (0-based) to selected option index (0-based).
 * This allows the student to later review which answers were correct/incorrect.
 */
data class QuizSubmission(
    var id: String = "",
    var examId: String = "",
    var studentId: String = "",
    var score: Int = 0,
    var total: Int = 0,
    var answers: Map<String, Int> = emptyMap(),
    var timeSpentSeconds: Int = 0,
    var submittedAt: Long = System.currentTimeMillis()
)

