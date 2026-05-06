package com.example.grader.firebase

import com.example.grader.models.Exam
import com.example.grader.models.Question
import com.example.grader.models.QuizSubmission
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Helper class for all Firestore operations related to exams and quiz submissions.
 *
 * Uses `kotlinx-coroutines-play-services` to convert Firebase `Task<>` to `suspend` functions,
 * ensuring structured concurrency and proper cancellation support.
 */
/**
 * Modelo auxiliar utilizado para mostrar la revisión de una respuesta
 * realizada por un estudiante en una evaluación.
 *
 * Esta clase no se guarda directamente en Firestore. Se construye en memoria
 * combinando los datos del resultado guardado en la colección "submissions"
 * con las preguntas originales del examen almacenadas en "evaluations".
 *
 * @property questionNumber Número correlativo de la pregunta dentro del examen.
 * @property questionText Enunciado de la pregunta.
 * @property selectedAnswer Respuesta seleccionada por el estudiante.
 * @property correctAnswer Respuesta correcta definida por el administrador.
 * @property isCorrect Indica si la respuesta seleccionada coincide con la respuesta correcta.
 */
data class ReviewedAnswer(
    val questionNumber: Int,
    val questionText: String,
    val selectedAnswer: String,
    val correctAnswer: String,
    val isCorrect: Boolean
)
class FirestoreHelper {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Fetches an exam document from Firestore.
     *
     * @param examId The document ID under `evaluations/`.
     * @return The [Exam] object with its ID populated.
     * @throws Exception if the document doesn't exist or network fails.
     */
    suspend fun getExam(examId: String): Exam {
        val doc = db.collection("evaluations")
            .document(examId)
            .get()
            .await()

        val exam = doc.toObject(Exam::class.java)
            ?: throw IllegalStateException("Exam not found: $examId")
        exam.id = doc.id
        return exam
    }

    /**
     * Fetches all questions for a given exam from its sub-collection.
     *
     * @param examId The parent exam document ID.
     * @return List of [Question] objects with their IDs populated.
     */
    suspend fun getQuestions(examId: String): List<Question> {
        val snapshot = db.collection("evaluations")
            .document(examId)
            .collection("questions")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Question::class.java)?.also { it.id = doc.id }
        }
    }

    /**
     * Submits a quiz result to Firestore for later review by the student.
     *
     * Persists the student's answers, score, and time spent so the student
     * can later see which questions were correct and which were incorrect.
     *
     * @param submission The [QuizSubmission] to persist.
     * @return The generated document ID.
     */
    suspend fun submitQuizResult(submission: QuizSubmission): String {
        val docRef = db.collection("submissions")
            .add(submission)
            .await()

        return docRef.id
    }

    /**
     * Fetches all submissions for a specific student and exam.
     *
     * Useful for checking if a student has already taken an exam
     * or for displaying past results.
     *
     * @param examId The exam ID to filter by.
     * @param studentId The student ID to filter by.
     * @return List of [QuizSubmission] objects sorted by submission time.
     */
    suspend fun getSubmissions(examId: String, studentId: String): List<QuizSubmission> {
        val snapshot = db.collection("submissions")
            .whereEqualTo("examId", examId)
            .whereEqualTo("studentId", studentId)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(QuizSubmission::class.java)?.also { it.id = doc.id }
        }
    }

    /**
     * Creates a new exam with its questions in Firestore atomically.
     *
     * Uses a [WriteBatch] to ensure the exam document and all its questions
     * are written together. If any write fails, the entire batch is rolled back.
     *
     * @param exam The [Exam] to persist (id field is ignored; Firestore generates it).
     * @param questions The list of [Question] objects for this exam.
     * @return The generated Firestore document ID for the new exam.
     */
    suspend fun createExam(exam: Exam, questions: List<Question>): String {
        // Create the exam document reference with an auto-generated ID
        val examRef = db.collection("evaluations").document()
        val examId = examRef.id

        val batch = db.batch()

        // Set exam data (exclude the local 'id' field — Firestore uses the doc ID)
        val examData = hashMapOf(
            "title" to exam.title,
            "course" to exam.course,
            "questionCount" to questions.size,
            "durationMins" to exam.durationMins,
            "type" to exam.type,
            "status" to exam.status,
            "createdAt" to exam.createdAt
        )
        batch.set(examRef, examData)

        // Add each question as a sub-document
        questions.forEach { question ->
            val questionRef = examRef.collection("questions").document()
            val questionData = hashMapOf(
                "question" to question.question,
                "options" to question.options,
                "correctAnswerIndex" to question.correctAnswerIndex,
                "type" to question.type
            )
            batch.set(questionRef, questionData)
        }

        batch.commit().await()
        return examId
    }
    /**
     * Obtiene todos los resultados enviados por un estudiante específico.
     *
     * Esta función se utiliza cuando el administrador selecciona un estudiante
     * y desea revisar todos los exámenes que ese estudiante ha realizado.
     *
     * La búsqueda se realiza en la colección "submissions", filtrando por el campo
     * "studentId", que debe corresponder al UID del usuario autenticado en Firebase.
     *
     * @param studentId UID del estudiante seleccionado.
     * @return Lista de resultados enviados por el estudiante, ordenados del más reciente al más antiguo.
     */
    suspend fun getResultsByStudent(studentId: String): List<QuizSubmission> {
        val snapshot = db.collection("submissions")
            .whereEqualTo("studentId", studentId)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(QuizSubmission::class.java)?.also {
                it.id = doc.id
            }
        }.sortedByDescending { it.submittedAt }
    }

    /**
     * Obtiene todos los resultados asociados a un examen específico.
     *
     * Esta función permite que el administrador seleccione una evaluación
     * y visualice qué estudiantes la han respondido.
     *
     * La búsqueda se realiza en la colección "submissions", filtrando por el campo
     * "examId", que representa el identificador del examen creado por el administrador.
     *
     * @param examId ID del examen seleccionado.
     * @return Lista de resultados enviados para ese examen, ordenados del más reciente al más antiguo.
     */
    suspend fun getResultsByExam(examId: String): List<QuizSubmission> {
        val snapshot = db.collection("submissions")
            .whereEqualTo("examId", examId)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(QuizSubmission::class.java)?.also {
                it.id = doc.id
            }
        }.sortedByDescending { it.submittedAt }
    }

    /**
     * Obtiene los resultados de un estudiante específico en un examen específico.
     *
     * Esta función se utiliza cuando el administrador desea revisar los intentos
     * o resultados de un estudiante determinado en una evaluación concreta.
     *
     * Es útil si el sistema permite que el estudiante realice una evaluación
     * más de una vez, ya que devuelve una lista de resultados y no un único registro.
     *
     * @param studentId UID del estudiante seleccionado.
     * @param examId ID del examen seleccionado.
     * @return Lista de resultados del estudiante en ese examen, ordenados del más reciente al más antiguo.
     */
    suspend fun getResultsByStudentAndExam(
        studentId: String,
        examId: String
    ): List<QuizSubmission> {
        val snapshot = db.collection("submissions")
            .whereEqualTo("studentId", studentId)
            .whereEqualTo("examId", examId)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(QuizSubmission::class.java)?.also {
                it.id = doc.id
            }
        }.sortedByDescending { it.submittedAt }
    }

    /**
     * Obtiene un resultado específico a partir del ID del documento en Firestore.
     *
     * Esta función se utiliza cuando el administrador selecciona un resultado
     * específico para ver su detalle.
     *
     * @param submissionId ID del documento guardado en la colección "submissions".
     * @return Objeto QuizSubmission correspondiente al resultado seleccionado.
     * @throws IllegalStateException Si no se encuentra el resultado solicitado.
     */
    suspend fun getResultById(submissionId: String): QuizSubmission {
        val doc = db.collection("submissions")
            .document(submissionId)
            .get()
            .await()

        val result = doc.toObject(QuizSubmission::class.java)
            ?: throw IllegalStateException("No se encontró el resultado: $submissionId")

        result.id = doc.id
        return result
    }

    /**
     * Obtiene la revisión detallada de las respuestas de un estudiante.
     *
     * Esta función combina:
     * 1. El resultado guardado en la colección "submissions".
     * 2. Las preguntas originales del examen almacenadas en "evaluations/{examId}/questions".
     *
     * Con esta información se construye una lista de respuestas revisadas,
     * indicando para cada pregunta:
     *
     * - El enunciado.
     * - La respuesta seleccionada por el estudiante.
     * - La respuesta correcta.
     * - Si la respuesta fue correcta o incorrecta.
     *
     * Esta función es la base para que el administrador pueda revisar el examen
     * de cada alumno.
     *
     * @param submissionId ID del resultado guardado en la colección "submissions".
     * @return Lista de respuestas revisadas del examen seleccionado.
     */
    suspend fun getReviewedAnswers(submissionId: String): List<ReviewedAnswer> {
        val submission = getResultById(submissionId)

        val questions = getQuestions(submission.examId)

        return questions.mapIndexed { index, question ->
            val selectedIndex = submission.answers[index.toString()] ?: -1
            val correctIndex = question.correctAnswerIndex

            ReviewedAnswer(
                questionNumber = index + 1,
                questionText = question.question,
                selectedAnswer = question.options.getOrNull(selectedIndex) ?: "Sin responder",
                correctAnswer = question.options.getOrNull(correctIndex) ?: "No definida",
                isCorrect = selectedIndex == correctIndex
            )
        }
    }


}