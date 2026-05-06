package com.example.grader.firebase

import android.util.Log
import com.example.grader.models.Course
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
     * Fetches all submissions for a specific student and exam.
     *
     * Useful for checking if a student has already taken an exam
     * or for displaying past results.
     *
     * @param examId The exam ID to filter by.
     * @param studentId The student ID to filter by.
     * @return List of [QuizSubmission] objects sorted by submission time.
     */
    suspend fun getSubmissions(studentId: String): List<QuizSubmission> {
        val snapshot = db.collection("submissions")
            .whereEqualTo("studentId", studentId)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(QuizSubmission::class.java)?.also { it.id = doc.id }
        }
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