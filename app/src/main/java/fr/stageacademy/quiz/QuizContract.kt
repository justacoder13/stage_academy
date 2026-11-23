package fr.stageacademy.quiz

interface QuizContract {
    fun getScore(): Int
    fun getCurrentQuestionIndex(): Int
    fun isFinished(): Boolean
    fun getCurrentQuestion(): Question
    fun submitAnswer(selectedAnswer: String): Boolean
    fun canGoToNextQuestion(): Boolean
    fun goToNextQuestion(): Unit
    fun restart(): Unit
}