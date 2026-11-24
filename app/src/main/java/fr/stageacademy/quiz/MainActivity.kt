package fr.stageacademy.quiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import fr.stageacademy.quiz.ui.theme.QuizTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween

private val quizQuestions = listOf(
    Question(
        text = "Quel outil garde l’historique du code ?",
        choices = listOf("Git", "Android", "Compose"),
        correctAnswer = "Git",
    ),
    Question(
        text = "Quel composant Compose affiche du texte ?",
        choices = listOf("Button", "Text", "Column"),
        correctAnswer = "Text",
    ),
    Question(
        text = "Quel mot-clé déclare une fonction en Kotlin ?",
        choices = listOf("fun", "val", "if"),
        correctAnswer = "fun",
    ),
    Question(
        text = "Quel type représente vrai ou faux ?",
        choices = listOf("String", "Boolean", "Int"),
        correctAnswer = "Boolean",
    ),
    Question(
        text = "Quelle commande Git montre les fichiers modifiés ?",
        choices = listOf("git log", "git status", "git init"),
        correctAnswer = "git status",
    ),
    Question(
        text = "Quelle commande Git enregistre les modifications préparées dans l’historique ?",
        choices = listOf("git commit", "git status", "git branch"),
        correctAnswer = "git commit",
    ),
    Question(
        text = "Quel composant Compose place les éléments côte à côte ?",
        choices = listOf("Box", "Column", "Row"),
        correctAnswer = "Row",
    ),
    Question(
        text = "Quel composant Compose sert à créer un bouton sur lequel appuyer ?",
        choices = listOf("Text", "Button", "Column"),
        correctAnswer = "Button",
    ),
    Question(
        text = "En Kotlin, quel mot-clé déclare une variable dont on peut changer la valeur ?",
        choices = listOf("val", "fun", "var"),
        correctAnswer = "var",
    ),
    Question(
        text = "En Kotlin, quel symbole vérifie si deux valeurs sont égales ?",
        choices = listOf("==", "=", "!="),
        correctAnswer = "==",
    ),
)

private enum class QuizPage { Start, Playing, Paused, Settings, Collection }

private val PresenterSaver = Saver<QuizContract, Bundle>(
    save = { presenter ->
        val state = presenter.getState()
        Bundle().apply {
            putInt("questionIndex", state.questionIndex)
            putInt("score", state.score)
            putString("selectedChoice", state.selectedChoice)
        }
    },
    restore = { saved ->
        QuizPresenter.restore(
            questions = quizQuestions,
            questionIndex = saved.getInt("questionIndex"),
            score = saved.getInt("score"),
            selectedChoice = saved.getString("selectedChoice"),
        )
    },
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferences = getSharedPreferences("quiz_settings", MODE_PRIVATE)
        val initialSettings = QuizSettings(
            appearance = QuizAppearance.entries.find { it.name == preferences.getString("appearance", null) }
                ?: QuizAppearance.System,
            showCorrections = preferences.getBoolean("showCorrections", true),
        )
        val savedSecretNames = preferences.getStringSet("foundSecrets", emptySet()).orEmpty()
        val initialSecrets = QuizSecret.entries.filter { it.name in savedSecretNames }.toSet()

        setContent {
            var settings by remember { mutableStateOf(initialSettings) }
            var foundSecrets by remember { mutableStateOf(initialSecrets) }
            val darkTheme = when (settings.appearance) {
                QuizAppearance.System -> isSystemInDarkTheme()
                QuizAppearance.Light -> false
                QuizAppearance.Dark -> true
            }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            QuizTheme(darkTheme = darkTheme) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing,
                ) { innerPadding ->
                    QuizScreen(
                        settings = settings,
                        foundSecrets = foundSecrets,
                        onSecretFound = { secret ->
                            if (secret !in foundSecrets) {
                                val updated = foundSecrets + secret
                                foundSecrets = updated
                                preferences.edit().putStringSet("foundSecrets", updated.map { it.name }.toSet()).apply()
                            }
                        },
                        onSettingsChange = { updated ->
                            settings = updated
                            preferences.edit()
                                .putString("appearance", updated.appearance.name)
                                .putBoolean("showCorrections", updated.showCorrections)
                                .apply()
                        },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizScreen(
    settings: QuizSettings,
    onSettingsChange: (QuizSettings) -> Unit,
    foundSecrets: Set<QuizSecret>,
    onSecretFound: (QuizSecret) -> Unit,
    modifier: Modifier = Modifier,
) {
    val presenter = rememberSaveable(saver = PresenterSaver) { QuizPresenter(quizQuestions) }
    var state by remember(presenter) { mutableStateOf(presenter.getState()) }
    var page by rememberSaveable { mutableStateOf(QuizPage.Start) }
    var menuOrigin by rememberSaveable { mutableStateOf(QuizPage.Start) }
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    var selectedSecret by rememberSaveable { mutableStateOf<QuizSecret?>(null) }
    var visitedSettingsDuringQuiz by rememberSaveable { mutableStateOf(false) }
    val question = state.question
    val questionNumber = state.questionIndex + 1
    val colors = MaterialTheme.colorScheme

    fun openMenu(target: QuizPage) {
        if (page == QuizPage.Paused && target == QuizPage.Settings) visitedSettingsDuringQuiz = true
        menuOrigin = page
        page = target
    }

    fun discoverSecret(secret: QuizSecret) {
        onSecretFound(secret)
        selectedSecret = secret
    }

    fun resetQuiz() {
        state = presenter.restart()
        visitedSettingsDuringQuiz = false
    }

    BackHandler(enabled = page != QuizPage.Start) {
        page = when (page) {
            QuizPage.Start -> QuizPage.Start
            QuizPage.Playing -> if (question == null) QuizPage.Start else QuizPage.Paused
            QuizPage.Paused -> QuizPage.Playing
            QuizPage.Settings, QuizPage.Collection -> menuOrigin
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.background, colors.primaryContainer))),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.widthIn(max = 520.dp).fillMaxWidth().fillMaxHeight()) {
            if (page == QuizPage.Playing) {
                QuizHeader(
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
                    onPause = if (question != null) ({ page = QuizPage.Paused }) else null,
                )
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = if (page == QuizPage.Start) Alignment.Center else Alignment.TopCenter,
            ) {
                key(page, state.questionIndex) {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        when (page) {
                            QuizPage.Start -> WelcomePanel(state.questionCount, ::discoverSecret)
                            QuizPage.Paused -> PauseMenu(
                                state = state,
                                keyAvailable = visitedSettingsDuringQuiz && !settings.showCorrections &&
                                    questionNumber == state.questionCount,
                                onSettings = { openMenu(QuizPage.Settings) },
                                onCollection = { openMenu(QuizPage.Collection) },
                                onLeave = { confirmLeave = true },
                                onDiscover = ::discoverSecret,
                            )
                            QuizPage.Settings -> SettingsMenu(settings, onSettingsChange, ::discoverSecret)
                            QuizPage.Collection -> SecretCollection(
                                foundSecrets = foundSecrets,
                                onOpen = { selectedSecret = it },
                                onDiscover = ::discoverSecret,
                            )
                            QuizPage.Playing -> {
                                if (question == null) {
                                    ResultPanel(state.score, state.questionCount, ::discoverSecret)
                                } else {
                                    QuestionPanel(
                                        question = question,
                                        questionNumber = questionNumber,
                                        questionCount = state.questionCount,
                                        score = state.score,
                                        selectedChoice = state.selectedChoice,
                                        isCorrect = state.isCorrect,
                                        onChoice = { state = presenter.submitAnswer(it) },
                                        onDiscover = ::discoverSecret,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            when (page) {
                QuizPage.Start -> QuizActionBar(
                    label = stringResource(R.string.start_quiz),
                    onClick = {
                        resetQuiz()
                        page = QuizPage.Playing
                    },
                    secondaryAction = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MenuButton(stringResource(R.string.settings), { openMenu(QuizPage.Settings) }, Modifier.weight(1f))
                            MenuButton(stringResource(R.string.secret_collection), { openMenu(QuizPage.Collection) }, Modifier.weight(1f))
                        }
                    },
                )
                QuizPage.Paused -> QuizActionBar(
                    label = stringResource(R.string.resume_quiz),
                    onClick = { page = QuizPage.Playing },
                )
                QuizPage.Settings, QuizPage.Collection -> QuizActionBar(
                    label = stringResource(R.string.back),
                    onClick = { page = menuOrigin },
                )
                QuizPage.Playing -> {
                    if (question == null) {
                        QuizActionBar(
                            label = stringResource(R.string.restart_quiz),
                            onClick = ::resetQuiz,
                            secondaryAction = {
                                MenuButton(label = stringResource(R.string.main_menu), onClick = { page = QuizPage.Start })
                            },
                        )
                    } else {
                        QuestionActions(
                            selectedChoice = state.selectedChoice,
                            isCorrect = state.isCorrect,
                            correctAnswer = question.correctAnswer.takeIf { settings.showCorrections },
                            isLastQuestion = questionNumber == state.questionCount,
                            onContinue = { state = presenter.goToNextQuestion() },
                        )
                    }
                }
            }
        }
    }

    if (confirmLeave) {
        LeaveQuizDialog(
            onCancel = { confirmLeave = false },
            onLeave = {
                confirmLeave = false
                resetQuiz()
                page = QuizPage.Start
            },
        )
    }

    selectedSecret?.let { secret ->
        SecretEntryDialog(secret, foundSecrets, { selectedSecret = null }, ::discoverSecret)
    }
}

@Preview(showBackground = true, locale = "fr")
@Composable
private fun WelcomeScreenPreview() {
    QuizTheme {
        QuizScreen(settings = QuizSettings(), onSettingsChange = {}, foundSecrets = emptySet(), onSecretFound = {})
    }
}
