package com.example.grader.firebase

import android.util.Log
import com.example.grader.models.Course
import com.example.grader.models.Exam
import com.example.grader.models.Question
import com.example.grader.models.QuizSubmission
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Helper class for all Firestore operations related to exams and quiz submissions.
 *
 * Uses `kotlinx-coroutines-play-services` to convert Firebase `Task<>` to `suspend` functions,
 * ensuring structured concurrency and proper cancellation support.
 */
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
     * Creates a new exam with its questions in Firestore atomically.
     *
     * Uses a [Batch] to ensure the exam document and all its questions
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
            "createdAt" to exam.createdAt,
            "creatorId" to exam.creatorId
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
     * Updates the exam in Firestore atomically.
     *
     * Uses a [WriteBatch] to ensure the exam document and all its questions
     * are written together. If any write fails, the entire batch is rolled back.
     *
     * @param examId The ID of the exam to update.
     * @param updatedExam The updated [Exam] data (id field is ignored).
     * @param updatedQuestions The list of [Question] objects for this exam.
     * @return The generated Firestore document ID for the new exam.
     */
    suspend fun updateExam(examId: String, updatedExam: Exam, updatedQuestions: List<Question>) {
        val examRef = db.collection("evaluations").document(examId)

        val batch = db.batch()

        // Update exam data
        val examData = hashMapOf(
            "title" to updatedExam.title,
            "course" to updatedExam.course,
            "questionCount" to updatedQuestions.size,
            "durationMins" to updatedExam.durationMins,
            "type" to updatedExam.type,
            "status" to updatedExam.status,
            "creatorId" to updatedExam.creatorId
        )
        batch.set(examRef, examData)

        // Delete existing questions
        val existingQuestionsSnapshot = examRef.collection("questions").get().await()
        existingQuestionsSnapshot.documents.forEach { doc ->
            batch.delete(doc.reference)
        }

        // Add updated questions
        updatedQuestions.forEach { question ->
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
    }

    /**
     * Fetches all exams created by a specific teacher.
     *
     * Useful for teachers to see a list of their exams and
     * manage them (e.g., edit, delete, view results).
     *
     * @param teacherId The teacher ID to filter by.
     * @return List of [Exam] objects sorted by creation time.
     */
    suspend fun getExamsByTeacher(teacherId: String): List<Exam> {
        val snapshot = db.collection("evaluations")
            .whereEqualTo("creatorId", teacherId)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Exam::class.java)?.also { it.id = doc.id }
        }
    }

    /**
     * Fetches all active/published students exams.
     *
     * Useful for students to see a list of their exams
     *
     * @return List of [Exam] objects sorted by creation time.
     */
    suspend fun getStudentExams(): List<Exam> {
        val snapshot = db.collection("evaluations")
            .whereEqualTo("status", "active")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Exam::class.java)?.also { it.id = doc.id }
        }
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
     * If a submission already exists for the given exam and student, it deletes it first.
     *
     * @param submission The [QuizSubmission] to persist.
     * @return The generated document ID.
     */
    suspend fun submitQuizResult(submission: QuizSubmission): String {
        val existingSubmissions = db.collection("submissions")
            .whereEqualTo("examId", submission.examId)
            .whereEqualTo("studentId", submission.studentId)
            .get()
            .await()

        val batch = db.batch()

        for (doc in existingSubmissions.documents) {
            batch.delete(doc.reference)
        }

        val newDocRef = db.collection("submissions").document()
        batch.set(newDocRef, submission)

        batch.commit().await()

        return newDocRef.id
    }

    /**
     * Comprueba si un estudiante ya ha realizado un examen.
     */
    suspend fun hasStudentSubmittedExam(examId: String, studentId: String): Boolean {
        val snapshot = db.collection("submissions")
            .whereEqualTo("examId", examId)
            .whereEqualTo("studentId", studentId)
            .limit(1)
            .get()
            .await()

        return !snapshot.isEmpty
    }

    /**
     * Fetches all submissions for a specific student.
     *
     * Useful for checking if a student has already taken an exam
     * or for displaying past results.
     *
     * @param studentId The student ID to filter by.
     * @return List of [QuizSubmission] objects sorted by submission time.
     */
    suspend fun getSubmissionsFromStudent(studentId: String): List<QuizSubmission> {
        val submissionsSnapshot = db.collection("submissions")
            .whereEqualTo("studentId", studentId)
            .get()
            .await()

        val submissions = submissionsSnapshot.documents.mapNotNull { doc ->
            doc.toObject(QuizSubmission::class.java)?.also { it.id = doc.id }
        }

        for (submission in submissions) {
            val examDoc = db.collection("evaluations")
                .document(submission.examId)
                .get()
                .await()
            submission.course = examDoc.getString("course") ?: "Curso desconocido"
            submission.examTitle = examDoc.getString("title") ?: "Examen sin título"
        }

        return submissions
    }

    /**
     * Obtiene el promedio de notas de los últimos 6 meses del estudiante.
     * Retorna una lista de pares (Mes, Promedio en porcentaje).
     */
    suspend fun getAverageScoresLast6Months(studentId: String): List<Pair<String, Float>> {
        val submissionsSnapshot = db.collection("submissions")
            .whereEqualTo("studentId", studentId)
            .get()
            .await()

        val submissions = submissionsSnapshot.documents.mapNotNull { doc ->
            doc.toObject(QuizSubmission::class.java)
        }

        val dateFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val monthLabels = mutableListOf<String>()
        val monthYearPairs = mutableListOf<Pair<Int, Int>>()
        
        for (i in 5 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -i)
            monthLabels.add(dateFormat.format(cal.time))
            monthYearPairs.add(Pair(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR)))
        }

        val result = mutableListOf<Pair<String, Float>>()
        for (i in 0..5) {
            val (month, year) = monthYearPairs[i]
            val monthSubmissions = submissions.filter { sub ->
                val cal = Calendar.getInstance()
                cal.timeInMillis = sub.submittedAt
                cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year
            }
            if (monthSubmissions.isEmpty()) {
                result.add(Pair(monthLabels[i], 0f))
            } else {
                val filtered = monthSubmissions.filter { it.total > 0 }
                val avg = if (filtered.isEmpty()) 0f else filtered.map { it.score.toFloat() / it.total.toFloat() * 100f }.average().toFloat()
                result.add(Pair(monthLabels[i], avg))
            }
        }
        return result
    }

    suspend fun getCourses(): List<Course> {
        val snapshot = db.collection("courses")
            .get()
            .await()

        Log.e("FirestoreHelper", "Fetched ${snapshot.size()} courses from Firestore")

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Course::class.java)?.also { it.id = doc.id }
        }
    }
}