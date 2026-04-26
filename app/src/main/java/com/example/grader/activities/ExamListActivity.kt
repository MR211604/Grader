package com.example.grader.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.grader.R
import com.example.grader.adapters.ExamAdapter
import com.example.grader.models.Exam
import com.google.firebase.firestore.FirebaseFirestore

class ExamListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ExamAdapter
    private val examList = mutableListOf<Exam>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exam_list)

        recyclerView = findViewById(R.id.recyclerExams)

        adapter = ExamAdapter(examList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        getExams()

        // CLICK en examen
        adapter.onItemClick = { exam ->
            val intent = Intent(this, ExamActivity::class.java)
            intent.putExtra("examId", exam.id)
            startActivity(intent)
        }
    }

    private fun getExams() {
        db.collection("evaluations")
            .get()
            .addOnSuccessListener { result ->
                examList.clear()

                for (doc in result) {
                    val exam = doc.toObject(Exam::class.java)
                    exam.id = doc.id
                    examList.add(exam)
                }

                adapter.setData(examList)
            }
    }
}