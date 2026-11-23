package fr.stageacademy.quiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.stageacademy.quiz.ui.theme.QuizTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            QuizTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    WelcomeScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(modifier: Modifier = Modifier) {
    val firstQuestion: Question = Question(
        text = "Quel outil garde l’historique du code ?",
        choices = listOf("Git", "Android", "Compose"),
        correctAnswer = "Git",
    )

    val secondQuestion: Question = Question(
        text = "Quel composant Compose affiche du texte ?",
        choices = listOf("Button", "Text", "Column"),
        correctAnswer = "Text",
    )

    val thirdQuestion: Question = Question(
        text = "Quel mot-clé déclare une fonction en Kotlin ?",
        choices = listOf("fun", "val", "if"),
        correctAnswer = "fun",
    )

    val fourthQuestion: Question = Question(
        text = "Quel type représente vrai ou faux ?",
        choices = listOf("String", "Boolean", "Int"),
        correctAnswer = "Boolean",
    )

    val questions: List<Question> = listOf(
        firstQuestion,
        secondQuestion,
        thirdQuestion,
        fourthQuestion,
    )
    val presenter: QuizContract = remember(questions) { QuizPresenter(questions) }
    var displayedQuestionIndex: Int by remember(presenter) {
        mutableStateOf(presenter.getCurrentQuestionIndex())
    }
    var isQuizFinished: Boolean by remember(presenter) {
        mutableStateOf(presenter.isFinished())
    }
    var displayedScore: Int by remember(presenter) {
        mutableStateOf(presenter.getScore())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(text = "Score : $displayedScore / ${questions.size}")
        if (isQuizFinished) {
            Text(text = "Quiz terminé")
            Button(onClick = {
                presenter.restart()
                displayedQuestionIndex = presenter.getCurrentQuestionIndex()
                isQuizFinished = presenter.isFinished()
                displayedScore = presenter.getScore()
            }) {
                Text(text = "Recommencer")
            }
        } else {
            Text(text = "Question ${displayedQuestionIndex + 1}")
            val question: Question = presenter.getCurrentQuestion()
            QuestionView(
                question = question,
                questionIndex = displayedQuestionIndex,
                presenter = presenter,
                onAnswerSubmitted = {
                    displayedScore = presenter.getScore()
                },
                onNextQuestion = {
                    presenter.goToNextQuestion()
                    displayedQuestionIndex = presenter.getCurrentQuestionIndex()
                    isQuizFinished = presenter.isFinished()
                },
            )
        }
    }
}

@Composable
private fun QuestionView(
    question: Question,
    questionIndex: Int,
    presenter: QuizContract,
    onAnswerSubmitted: () -> Unit,
    onNextQuestion: () -> Unit,
) {
    var feedbackText: String by remember(presenter, questionIndex) {
        mutableStateOf("Choisis une réponse.")
    }
    var canGoToNextQuestion: Boolean by remember(presenter, questionIndex) {
        mutableStateOf(presenter.canGoToNextQuestion())
    }

    fun checkAnswer(selectedAnswer: String): Unit {
        val isAnswerCorrect: Boolean = presenter.submitAnswer(selectedAnswer)
        if (isAnswerCorrect) {
            feedbackText = "Bonne réponse !"
        } else {
            feedbackText = "Réponse incorrecte."
        }
        canGoToNextQuestion = presenter.canGoToNextQuestion()
        onAnswerSubmitted()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = question.text)
        for (choice: String in question.choices) {
            Button(
                onClick = { checkAnswer(choice) },
                enabled = !canGoToNextQuestion,
            ) {
                Text(text = choice)
            }
        }
        Text(text = feedbackText)
        Button(
            onClick = onNextQuestion,
            enabled = canGoToNextQuestion,
        ) {
            Text(text = "Question suivante")
        }
    }
}

@Preview(showBackground = true, locale = "fr")
@Composable
fun WelcomeScreenPreview() {
    QuizTheme {
        WelcomeScreen()
    }
}
