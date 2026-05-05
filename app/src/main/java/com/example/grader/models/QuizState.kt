package com.example.grader.models

/**
 * Sealed class representing all possible states of the Quiz UI.
 *
 * Uses exhaustive `when` to enforce handling of every state in Compose.
 */
sealed class QuizUiState {

    /** Initial loading state while fetching exam data from Firestore. */
    data object Loading : QuizUiState()

    /**
     * Quiz is actively in progress.
     *
     * @param exam The exam metadata.
     * @param questions The full list of questions.
     * @param currentIndex Index of the currently displayed question (0-based).
     * @param selectedAnswers Map of questionIndex to the selectedOptionIndex.
     * @param remainingSeconds Seconds left on the countdown timer (-1 if no timer).
     * @param isSubmitting True while the submission is being saved to Firestore.
     */
    data class Active(
        val exam: Exam,
        val questions: List<Question>,
        val currentIndex: Int = 0,
        val selectedAnswers: Map<Int, Int> = emptyMap(),
        val remainingSeconds: Int = -1,
        val isSubmitting: Boolean = false
    ) : QuizUiState() {

        /** Total number of questions. */
        val totalQuestions: Int get() = questions.size

        /** Current question (1-based display number). */
        val displayNumber: Int get() = currentIndex + 1

        /** Progress fraction (0f.1f). */
        val progress: Float
            get() = if (totalQuestions > 0) (currentIndex + 1).toFloat() / totalQuestions else 0f

        /** Progress percentage (0..100). */
        val progressPercent: Int get() = (progress * 100).toInt()

        /** The current question object. */
        val currentQuestion: Question get() = questions[currentIndex]

        /** The selected option index for the current question, or null if unanswered. */
        val currentSelectedOption: Int? get() = selectedAnswers[currentIndex]

        /** Whether the user is on the last question. */
        val isLastQuestion: Boolean get() = currentIndex >= totalQuestions - 1

        /** Whether the user is on the first question. */
        val isFirstQuestion: Boolean get() = currentIndex == 0

        /** Formatted remaining time as "MM:SS". */
        val formattedTime: String
            get() {
                if (remainingSeconds < 0) return ""
                val mins = remainingSeconds / 60
                val secs = remainingSeconds % 60
                return "%02d:%02d".format(mins, secs)
            }
    }

    /**
     * Quiz has been completed and results are available.
     *
     * @param score Number of correct answers.
     * @param total Total number of questions.
     * @param exam The exam metadata.
     * @param answers Map of questionIndex to selectedOptionIndex.
     * @param questions The full list of questions for review.
     */
    data class Finished(
        val score: Int,
        val total: Int,
        val exam: Exam,
        val answers: Map<Int, Int> = emptyMap(),
        val questions: List<Question> = emptyList()
    ) : QuizUiState() {

        /** Score as a percentage (0..100). */
        val scorePercent: Int
            get() = if (total > 0) (score * 100) / total else 0
    }

    /** Error state when something goes wrong loading or submitting. */
    data class Error(val message: String) : QuizUiState()
}
