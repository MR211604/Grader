package com.example.grader.models

data class Question(
    var id: String = "",
    var question: String = "",
    var option1: String = "",
    var option2: String = "",
    var option3: String = "",
    var correctAnswer: String = "",
    var selectedAnswer: String = ""
)