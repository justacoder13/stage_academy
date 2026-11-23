package fr.stageacademy.quiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
    var hasStarted by rememberSaveable { mutableStateOf(false) }
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

    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.background, colors.primaryContainer))),
        contentAlignment = if (hasStarted) Alignment.TopCenter else Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (!hasStarted) {
                WelcomePanel(questionCount = questions.size, onStart = { hasStarted = true })
                return@Column
            }

            QuizHeader(score = displayedScore.takeUnless { isQuizFinished }, questionCount = questions.size)
            if (isQuizFinished) {
                ResultPanel(
                    score = displayedScore,
                    questionCount = questions.size,
                    onRestart = {
                        presenter.restart()
                        displayedQuestionIndex = presenter.getCurrentQuestionIndex()
                        isQuizFinished = presenter.isFinished()
                        displayedScore = presenter.getScore()
                    },
                )
            } else {
                QuestionView(
                    question = presenter.getCurrentQuestion(),
                    questionIndex = displayedQuestionIndex,
                    questionCount = questions.size,
                    presenter = presenter,
                    onAnswerSubmitted = { displayedScore = presenter.getScore() },
                    onNextQuestion = {
                        presenter.goToNextQuestion()
                        displayedQuestionIndex = presenter.getCurrentQuestionIndex()
                        isQuizFinished = presenter.isFinished()
                    },
                )
            }
        }
    }
}

@Composable
private fun QuestionView(
    question: Question,
    questionIndex: Int,
    questionCount: Int,
    presenter: QuizContract,
    onAnswerSubmitted: () -> Unit,
    onNextQuestion: () -> Unit,
) {
    var selectedChoice by remember(presenter, questionIndex) { mutableStateOf<String?>(null) }
    var isCorrect by remember(presenter, questionIndex) { mutableStateOf(false) }

    QuestionPanel(
        question = question,
        questionNumber = questionIndex + 1,
        questionCount = questionCount,
        selectedChoice = selectedChoice,
        isCorrect = isCorrect,
        canContinue = presenter.canGoToNextQuestion(),
        onChoice = { choice ->
            isCorrect = presenter.submitAnswer(choice)
            selectedChoice = choice
            onAnswerSubmitted()
        },
        onContinue = onNextQuestion,
    )
}

@Preview(showBackground = true, locale = "fr")
@Composable
fun WelcomeScreenPreview() {
    QuizTheme {
        WelcomeScreen()
    }
}
