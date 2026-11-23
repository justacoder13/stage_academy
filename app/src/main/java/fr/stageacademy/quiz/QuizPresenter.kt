package fr.stageacademy.quiz

class QuizPresenter(private val questions: List<Question>) : QuizContract {
    private var currentQuestionIndex: Int = 0
    private var hasAnsweredCurrentQuestion: Boolean = false
    private var isCurrentAnswerCorrect: Boolean = false
    private var score: Int = 0

    override fun getScore(): Int {
        return score
    }

    override fun getCurrentQuestionIndex(): Int {
        return currentQuestionIndex
    }

    override fun isFinished(): Boolean {
        return currentQuestionIndex >= questions.size
    }

    override fun getCurrentQuestion(): Question {
        return questions[currentQuestionIndex]
    }

    private fun isCorrectAnswer(
        question: Question,
        selectedAnswer: String,
    ): Boolean {
        val result: Boolean = selectedAnswer == question.correctAnswer
        return result
    }

    override fun submitAnswer(selectedAnswer: String): Boolean {
        if (isFinished()) {
            return false
        }
        if (hasAnsweredCurrentQuestion) {
            return isCurrentAnswerCorrect
        }
        val question: Question = getCurrentQuestion()
        isCurrentAnswerCorrect = isCorrectAnswer(question, selectedAnswer)
        hasAnsweredCurrentQuestion = true
        if (isCurrentAnswerCorrect) {
            score = score + 1
        }
        return isCurrentAnswerCorrect
    }

    override fun canGoToNextQuestion(): Boolean {
        return hasAnsweredCurrentQuestion
    }

    override fun goToNextQuestion(): Unit {
        if (canGoToNextQuestion()) {
            currentQuestionIndex = currentQuestionIndex + 1
            hasAnsweredCurrentQuestion = false
            isCurrentAnswerCorrect = false
        }
    }

    override fun restart(): Unit {
        currentQuestionIndex = 0
        hasAnsweredCurrentQuestion = false
        isCurrentAnswerCorrect = false
        score = 0
    }
}