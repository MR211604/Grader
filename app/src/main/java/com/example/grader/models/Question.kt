package com.example.grader.models

/**
 * Represents a single question within an exam.
 *
 * Mapped to: `evaluations/{examId}/questions/{questionId}`
 *
 * [options] is a dynamic list of answer choices (typically 4).
 * [correctAnswerIndex] is the 0-based index of the correct option.
 */
data class Question(
    var id: String = "",
    var question: String = "",
    var options: List<String> = emptyList(),
    var correctAnswerIndex: Int = -1,
    var type: String = "multiple_choice"
)