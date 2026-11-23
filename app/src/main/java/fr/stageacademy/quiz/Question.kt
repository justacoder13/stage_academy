package fr.stageacademy.quiz

data class Question(
    val text: String,
    val choices: List<String>,
    val correctAnswer: String,
)