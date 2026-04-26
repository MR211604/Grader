package com.example.grader.activities

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.grader.R

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val txtResult: TextView = findViewById(R.id.txtResult)

        val score = intent.getIntExtra("score", 0)
        val total = intent.getIntExtra("total", 0)

        txtResult.text = "Obtuviste $score de $total"
    }
}