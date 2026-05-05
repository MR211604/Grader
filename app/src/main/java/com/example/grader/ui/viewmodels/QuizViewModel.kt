package com.example.grader.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grader.firebase.FirestoreHelper
import com.example.grader.models.QuizSubmission
import com.example.grader.models.QuizUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing the quiz lifecycle: loading, answering, timing, and submitting.
 *
 * Uses [StateFlow] for reactive UI state and structured concurrency
 * via [viewModelScope] for proper cancellation on teardown.
 */
class QuizViewModel(
    private val firestoreHelper: FirestoreHelper = FirestoreHelper()
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var totalDurationSeconds: Int = 0

    /**
     * Loads an exam and its questions from Firestore, then starts the timer.
     *
     * @param examId The Firestore document ID of the exam to load.
     */
    fun loadExam(examId: String) {
        viewModelScope.launch {
            _uiState.value = QuizUiState.Loading
            try {
                val exam = firestoreHelper.getExam(examId)
                val questions = firestoreHelper.getQuestions(examId)

                if (questions.isEmpty()) {
                    _uiState.value = QuizUiState.Error("No se encontraron preguntas para este examen.")
                    return@launch
                }

                totalDurationSeconds = exam.durationMins * 60

                _uiState.value = QuizUiState.Active(
                    exam = exam,
                    questions = questions,
                    currentIndex = 0,
                    selectedAnswers = emptyMap(),
                    remainingSeconds = if (totalDurationSeconds > 0) totalDurationSeconds else -1
                )

                // Start countdown timer if duration is configured
                if (totalDurationSeconds > 0) {
                    startTimer()
                }
            } catch (e: Exception) {
                _uiState.value = QuizUiState.Error(
                    e.message ?: "Error al cargar el examen."
                )
            }
        }
    }

    /**
     * Selects an answer option for the currently displayed question.
     *
     * @param optionIndex The 0-based index of the selected option.
     */
    fun selectAnswer(optionIndex: Int) {
        val current = _uiState.value as? QuizUiState.Active ?: return

        _uiState.update {
            current.copy(
                selectedAnswers = current.selectedAnswers + (current.currentIndex to optionIndex)
            )
        }
    }

    /**
     * Advances to the next question if not on the last one.
     */
    fun nextQuestion() {
        val current = _uiState.value as? QuizUiState.Active ?: return
        if (!current.isLastQuestion) {
            _uiState.update {
                current.copy(currentIndex = current.currentIndex + 1)
            }
        }
    }

    /**
     * Goes back to the previous question if not on the first one.
     */
    fun previousQuestion() {
        val current = _uiState.value as? QuizUiState.Active ?: return
        if (!current.isFirstQuestion) {
            _uiState.update {
                current.copy(currentIndex = current.currentIndex - 1)
            }
        }
    }

    /**
     * Submits the quiz: calculates score, persists to Firestore, and transitions to Finished state.
     */
    fun submitQuiz() {
        val current = _uiState.value as? QuizUiState.Active ?: return

        viewModelScope.launch {
            _uiState.value = current.copy(isSubmitting = true)

            // Stop the timer
            timerJob?.cancel()

            // Calculate score
            var score = 0
            current.questions.forEachIndexed { index, question ->
                val selectedOption = current.selectedAnswers[index]
                if (selectedOption != null && selectedOption == question.correctAnswerIndex) {
                    score++
                }
            }

            // Calculate time spent
            val timeSpent = if (totalDurationSeconds > 0) {
                totalDurationSeconds - current.remainingSeconds.coerceAtLeast(0)
            } else {
                0
            }

            // Persist to Firestore
            try {
                val submission = QuizSubmission(
                    examId = current.exam.id,
                    studentId = "current_student", // TODO: Replace with actual auth user ID
                    score = score,
                    total = current.totalQuestions,
                    answers = current.selectedAnswers.mapKeys { it.key.toString() },
                    timeSpentSeconds = timeSpent,
                    submittedAt = System.currentTimeMillis()
                )
                firestoreHelper.submitQuizResult(submission)
            } catch (e: Exception) {
                // Log error but still show results to the user
                e.printStackTrace()
            }

            // Transition to Finished state
            _uiState.value = QuizUiState.Finished(
                score = score,
                total = current.totalQuestions,
                exam = current.exam,
                answers = current.selectedAnswers,
                questions = current.questions
            )
        }
    }

    /**
     * Starts a countdown timer that ticks every second.
     * Auto-submits when time runs out.
     */
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _uiState.value as? QuizUiState.Active ?: break

                val newRemaining = current.remainingSeconds - 1
                if (newRemaining <= 0) {
                    // Time's up — auto-submit
                    _uiState.value = current.copy(remainingSeconds = 0)
                    submitQuiz()
                    break
                } else {
                    _uiState.value = current.copy(remainingSeconds = newRemaining)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
