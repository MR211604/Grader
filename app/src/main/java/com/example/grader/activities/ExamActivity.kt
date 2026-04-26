package com.example.grader.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.grader.R
import com.example.grader.models.Question
import com.google.firebase.firestore.FirebaseFirestore

class ExamActivity : AppCompatActivity() {

    private lateinit var txtQuestion: TextView
    private lateinit var btnOption1: Button
    private lateinit var btnOption2: Button
    private lateinit var btnOption3: Button
    private lateinit var btnNext: Button
    private lateinit var btnFinish: Button

    private val db = FirebaseFirestore.getInstance()
    private val questionList = mutableListOf<Question>()
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exam)

        txtQuestion = findViewById(R.id.txtQuestion)
        btnOption1 = findViewById(R.id.btnOption1)
        btnOption2 = findViewById(R.id.btnOption2)
        btnOption3 = findViewById(R.id.btnOption3)
        btnNext = findViewById(R.id.btnNext)
        btnFinish = findViewById(R.id.btnFinish)

        val examId = intent.getStringExtra("examId") ?: ""

        getQuestions(examId)

        btnNext.setOnClickListener {
            if (currentIndex < questionList.size - 1) {
                currentIndex++
                showQuestion()
            }
        }

        btnFinish.setOnClickListener {
            calculateResult()
        }
    }

    //OBTENER PREGUNTAS DE FIREBASE
    private fun getQuestions(examId: String) {
        db.collection("evaluations")
            .document(examId)
            .collection("questions")
            .get()
            .addOnSuccessListener { result ->
                questionList.clear()

                for (doc in result) {
                    val question = doc.toObject(Question::class.java)
                    question.id = doc.id
                    questionList.add(question)
                }

                showQuestion()
            }
    }

    //MOSTRAR PREGUNTA
    private fun showQuestion() {
        val q = questionList[currentIndex]

        txtQuestion.text = q.question
        btnOption1.text = q.option1
        btnOption2.text = q.option2
        btnOption3.text = q.option3

        // Guardar respuesta
        btnOption1.setOnClickListener {
            q.selectedAnswer = q.option1
        }

        btnOption2.setOnClickListener {
            q.selectedAnswer = q.option2
        }

        btnOption3.setOnClickListener {
            q.selectedAnswer = q.option3
        }
    }

    //CALCULAR RESULTADO
    private fun calculateResult() {
        var score = 0

        for (q in questionList) {
            if (q.selectedAnswer == q.correctAnswer) {
                score++
            }
        }

        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra("score", score)
        intent.putExtra("total", questionList.size)
        startActivity(intent)
    }
}