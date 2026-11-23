package fr.stageacademy.quiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

fun isCorrectAnswer(selectedAnswer: String, correctAnswer: String): Boolean {
    return selectedAnswer == correctAnswer
}
// Android ouvre cette activité au lancement de l’application.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuizTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WelcomeScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Preview(showBackground = true, locale = "fr")
@Composable
fun WelcomeScreen(modifier: Modifier = Modifier) {
    val questionText: String = "Quel outil garde l’historique du code ?"
    val firstQuestionChoices: List<String> = listOf(
        "Git",
        "Android",
        "Compose",
    )
    val correctAnswer: String = "Git"
    val secondQuestionText: String = "Quel composant Compose affiche du texte ?"
    val secondQuestionChoices: List<String> = listOf(
        "Button",
        "Text",
        "Column",
    )
    val secondQuestionCorrectAnswer: String = "Text"


    var firstFeedbackText: String by remember {
        mutableStateOf("Choisis une réponse pour la question 1.")
    }
    var secondFeedbackText: String by remember {
        mutableStateOf("Choisis une réponse pour la question 2.")
    }

fun checkFirstAnswer(selectedAnswer: String): Unit {
    val isAnswerCorrect: Boolean = isCorrectAnswer(
        selectedAnswer,
        correctAnswer
    )

    if (isAnswerCorrect) {
        firstFeedbackText = "Bonne réponse !"
    } else {
        firstFeedbackText = "Essaie encore."
    }
}

    fun checkSecondAnswer(selectedAnswer: String): Unit {
        val isAnswerCorrect: Boolean = isCorrectAnswer(
            selectedAnswer,
            secondQuestionCorrectAnswer,
        )
        if (isAnswerCorrect) {
            secondFeedbackText = "Bonne réponse !"
        } else {
            secondFeedbackText = "Essaie encore."
        }
    }

Column(
modifier = modifier
.fillMaxSize()
.verticalScroll(rememberScrollState())
.padding(24.dp),

verticalArrangement = Arrangement.spacedBy(16.dp),
horizontalAlignment = Alignment.CenterHorizontally,
) {
    Text(
        text = stringResource(R.string.welcome_title),
        style = MaterialTheme.typography.headlineMedium,
    )

    // Question 1
    Text(text = "Question 1")
    Text(text = questionText)

    Button(onClick = { checkFirstAnswer(firstQuestionChoices[0]) }) {
        Text(text = firstQuestionChoices[0])
    }

    Button(onClick = { checkFirstAnswer(firstQuestionChoices[1]) }) {
        Text(text = firstQuestionChoices[1])
    }

    Button(onClick = { checkFirstAnswer(firstQuestionChoices[2]) }) {
        Text(text = firstQuestionChoices[2])
    }

    Text(text = firstFeedbackText)

    // Question 2
    Text(text = "Question 2")
    Text(text = secondQuestionText)

    Button(onClick = { checkSecondAnswer(secondQuestionChoices[0]) }) {
        Text(text = secondQuestionChoices[0])
    }

    Button(onClick = { checkSecondAnswer(secondQuestionChoices[1]) }) {
        Text(text = secondQuestionChoices[1])
    }

    Button(onClick = { checkSecondAnswer(secondQuestionChoices[2]) }) {
        Text(text = secondQuestionChoices[2])
    }

    Text(text = secondFeedbackText)
    }
}