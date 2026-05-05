package com.example.grader.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grader.firebase.FirestoreHelper
import com.example.grader.models.Exam
import com.example.grader.models.Question
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─── Editable data classes (local-only, not persisted) ─────────────────────

/**
 * Represents a single answer option while editing.
 *
 * @param text The option display text.
 * @param isCorrect Whether this option is the correct answer.
 */
data class EditableOption(
    val text: String = "",
    val isCorrect: Boolean = false
)

/**
 * Represents a question while editing, before conversion to the Firestore [Question] model.
 *
 * @param id Local-only identifier used as a stable key in LazyColumn.
 * @param prompt The question text.
 * @param options The list of answer options.
 */
data class EditableQuestion(
    val id: Int,
    val prompt: String = "",
    val options: List<EditableOption> = listOf(
        EditableOption(), EditableOption()
    )
)

// ─── UI State ──────────────────────────────────────────────────────────────

/**
 * Sealed class representing all possible states of the Exam Creator UI.
 */
sealed class ExamCreatorUiState {

    /**
     * The user is actively editing the exam.
     *
     * @param title Exam title.
     * @param category Course / category name.
     * @param durationMins Duration in minutes (as a string for text field binding).
     * @param questions The list of questions being edited.
     * @param isSaving True while the save operation is in-flight.
     * @param validationErrors Map of field key → error message for strict validation.
     */
    data class Editing(
        val title: String = "",
        val category: String = "",
        val durationMins: String = "",
        val questions: List<EditableQuestion> = listOf(
            EditableQuestion(id = 1)
        ),
        val isSaving: Boolean = false,
        val validationErrors: Map<String, String> = emptyMap()
    ) : ExamCreatorUiState()

    /**
     * The exam was saved successfully.
     *
     * @param examId The Firestore-generated document ID.
     */
    data class Saved(val examId: String) : ExamCreatorUiState()

    /**
     * An error occurred while saving.
     *
     * @param message Human-readable error description.
     */
    data class Error(val message: String) : ExamCreatorUiState()
}

// ─── ViewModel ─────────────────────────────────────────────────────────────

/**
 * ViewModel managing the exam creation flow: editing, validation, and Firestore persistence.
 *
 * Exposes a single [StateFlow] of [ExamCreatorUiState] consumed by the Compose UI.
 * All mutations go through dedicated action methods to keep the screen stateless.
 */
class ExamCreatorViewModel(
    private val firestoreHelper: FirestoreHelper = FirestoreHelper()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExamCreatorUiState>(
        ExamCreatorUiState.Editing()
    )
    val uiState: StateFlow<ExamCreatorUiState> = _uiState.asStateFlow()

    // ── Field-level update actions ──────────────────────────────────────

    fun updateTitle(title: String) = mutateEditing { copy(title = title) }

    fun updateCategory(category: String) = mutateEditing { copy(category = category) }

    /**
     * Updates the duration field. Strips non-digit characters so the user
     * cannot accidentally type letters into a numeric field.
     */
    fun updateDuration(duration: String) {
        val sanitised = duration.filter { it.isDigit() }
        mutateEditing { copy(durationMins = sanitised) }
    }

    // ── Question-level actions ──────────────────────────────────────────

    fun updateQuestionPrompt(questionId: Int, prompt: String) = mutateQuestions { q ->
        if (q.id == questionId) q.copy(prompt = prompt) else q
    }

    fun addQuestion() = mutateEditing {
        val nextId = (questions.maxOfOrNull { it.id } ?: 0) + 1
        copy(questions = questions + EditableQuestion(id = nextId))
    }

    fun removeQuestion(questionId: Int) = mutateEditing {
        copy(questions = questions.filter { it.id != questionId })
    }

    fun moveQuestion(fromIndex: Int, toIndex: Int) = mutateEditing {
        val mutable = questions.toMutableList()
        if (fromIndex in mutable.indices && toIndex in mutable.indices) {
            val item = mutable.removeAt(fromIndex)
            mutable.add(toIndex, item)
        }
        copy(questions = mutable)
    }

    // ── Option-level actions ────────────────────────────────────────────

    fun updateOptionText(questionId: Int, optionIndex: Int, text: String) = mutateQuestions { q ->
        if (q.id == questionId) {
            val updatedOptions = q.options.toMutableList().also {
                if (optionIndex in it.indices) it[optionIndex] = it[optionIndex].copy(text = text)
            }
            q.copy(options = updatedOptions)
        } else q
    }

    /**
     * Toggles the correct answer for a question.
     * Only one option can be correct at a time (radio-button behavior).
     */
    fun toggleOptionCorrect(questionId: Int, optionIndex: Int) = mutateQuestions { q ->
        if (q.id == questionId) {
            val updatedOptions = q.options.mapIndexed { i, opt ->
                opt.copy(isCorrect = i == optionIndex)
            }
            q.copy(options = updatedOptions)
        } else q
    }

    fun addOption(questionId: Int) = mutateQuestions { q ->
        if (q.id == questionId) q.copy(options = q.options + EditableOption()) else q
    }

    fun removeOption(questionId: Int, optionIndex: Int) = mutateQuestions { q ->
        if (q.id == questionId) {
            val updatedOptions = q.options.toMutableList().also {
                if (optionIndex in it.indices) it.removeAt(optionIndex)
            }
            q.copy(options = updatedOptions)
        } else q
    }

    // ── Save ────────────────────────────────────────────────────────────

    /**
     * Validates all fields and, if valid, persists the exam + questions to Firestore.
     *
     * Validation rules (strict):
     * - Title must not be blank
     * - Category must not be blank
     * - Duration must be a positive integer
     * - At least 1 question
     * - Each question must have a non-blank prompt
     * - Each question must have at least 2 options
     * - Each option must have non-blank text
     * - Each question must have exactly 1 correct answer selected
     */
    fun saveExam() {
        val current = _uiState.value as? ExamCreatorUiState.Editing ?: return

        // ── Validation ──────────────────────────────────────────────────
        val errors = mutableMapOf<String, String>()

        if (current.title.isBlank()) {
            errors["title"] = "El título es obligatorio"
        }
        if (current.category.isBlank()) {
            errors["category"] = "La categoría es obligatoria"
        }

        val duration = current.durationMins.toIntOrNull()
        if (duration == null || duration <= 0) {
            errors["duration"] = "La duración debe ser un número mayor a 0"
        }

        if (current.questions.isEmpty()) {
            errors["questions"] = "Debe haber al menos 1 pregunta"
        }

        current.questions.forEachIndexed { qi, question ->
            if (question.prompt.isBlank()) {
                errors["question_${question.id}_prompt"] = "Pregunta ${qi + 1}: el enunciado es obligatorio"
            }
            if (question.options.size < 2) {
                errors["question_${question.id}_options"] = "Pregunta ${qi + 1}: debe tener al menos 2 opciones"
            }
            question.options.forEachIndexed { oi, option ->
                if (option.text.isBlank()) {
                    errors["question_${question.id}_option_$oi"] =
                        "Pregunta ${qi + 1}, opción ${('A' + oi)}: el texto es obligatorio"
                }
            }
            if (question.options.none { it.isCorrect }) {
                errors["question_${question.id}_correct"] =
                    "Pregunta ${qi + 1}: debe seleccionar una respuesta correcta"
            }
        }

        if (errors.isNotEmpty()) {
            _uiState.value = current.copy(validationErrors = errors)
            return
        }

        // ── Convert to Firestore models & persist ───────────────────────
        _uiState.value = current.copy(isSaving = true, validationErrors = emptyMap())

        viewModelScope.launch {
            try {
                val exam = Exam(
                    title = current.title.trim(),
                    course = current.category.trim(),
                    questionCount = current.questions.size,
                    durationMins = duration ?: 0,
                    type = "multiple_choice",
                    status = "draft",
                    createdAt = System.currentTimeMillis()
                )

                val questions = current.questions.map { eq ->
                    val correctIndex = eq.options.indexOfFirst { it.isCorrect }
                    Question(
                        question = eq.prompt.trim(),
                        options = eq.options.map { it.text.trim() },
                        correctAnswerIndex = correctIndex,
                        type = "multiple_choice"
                    )
                }

                val examId = firestoreHelper.createExam(exam, questions)
                _uiState.value = ExamCreatorUiState.Saved(examId)
            } catch (e: Exception) {
                _uiState.value = ExamCreatorUiState.Error(
                    e.message ?: "Error desconocido al guardar el examen"
                )
            }
        }
    }

    /**
     * Resets back to editing state after an error so the user can retry.
     */
    fun dismissError() {
        _uiState.value = ExamCreatorUiState.Editing()
    }

    // ── Private helpers ─────────────────────────────────────────────────

    /** Safely mutates only when in Editing state. */
    private inline fun mutateEditing(block: ExamCreatorUiState.Editing.() -> ExamCreatorUiState.Editing) {
        _uiState.update { state ->
            if (state is ExamCreatorUiState.Editing) state.block() else state
        }
    }

    /** Maps over the questions list within the Editing state. */
    private inline fun mutateQuestions(crossinline transform: (EditableQuestion) -> EditableQuestion) {
        mutateEditing { copy(questions = questions.map(transform)) }
    }
}
