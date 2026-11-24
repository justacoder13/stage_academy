package fr.stageacademy.quiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import fr.stageacademy.quiz.about_view.AboutScreen
import fr.stageacademy.quiz.challenge_view.ChallengePresenter
import fr.stageacademy.quiz.challenge_view.ChallengeScreen
import fr.stageacademy.quiz.collection_view.QuizSecret
import fr.stageacademy.quiz.collection_view.SecretCollection
import fr.stageacademy.quiz.collection_view.SecretEntryDialog
import fr.stageacademy.quiz.menu_view.LeaveQuizDialog
import fr.stageacademy.quiz.menu_view.HomeTabs
import fr.stageacademy.quiz.menu_view.PauseMenu
import fr.stageacademy.quiz.menu_view.WelcomePanel
import fr.stageacademy.quiz.leaderboard_view.LeaderboardPresenter
import fr.stageacademy.quiz.leaderboard_view.LeaderboardScreen
import fr.stageacademy.quiz.leaderboard_view.LeaderboardStore
import fr.stageacademy.quiz.leaderboard_view.PlayerDialog
import fr.stageacademy.quiz.profile_view.ProfilePresenter
import fr.stageacademy.quiz.profile_view.ProfileScreen
import fr.stageacademy.quiz.quiz_view.QuestionActions
import fr.stageacademy.quiz.quiz_view.QuestionPanel
import fr.stageacademy.quiz.quiz_view.QuizContract
import fr.stageacademy.quiz.quiz_view.QuizHeader
import fr.stageacademy.quiz.quiz_view.QuizPresenter
import fr.stageacademy.quiz.quiz_view.ResultPanel
import fr.stageacademy.quiz.quiz_view.quizQuestions
import fr.stageacademy.quiz.settings_view.QuizAppearance
import fr.stageacademy.quiz.settings_view.QuizSettings
import fr.stageacademy.quiz.settings_view.SettingsMenu
import fr.stageacademy.quiz.shared_view.MenuButton
import fr.stageacademy.quiz.shared_view.QuizActionBar
import fr.stageacademy.quiz.ui.theme.QuizTheme
import java.util.UUID

private enum class QuizPage { Start, Profile, Playing, Paused, Settings, Collection, About, Leaderboard, Challenges }

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
            horizontalAnswers = preferences.getBoolean("horizontalAnswers", false),
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
                    QuizNavigation(
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
                                .putBoolean("horizontalAnswers", updated.horizontalAnswers)
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
private fun QuizNavigation(
    settings: QuizSettings,
    onSettingsChange: (QuizSettings) -> Unit,
    foundSecrets: Set<QuizSecret>,
    onSecretFound: (QuizSecret) -> Unit,
    modifier: Modifier = Modifier,
) {
    val presenter = rememberSaveable(saver = PresenterSaver) { QuizPresenter(quizQuestions) }
    var state by remember(presenter) { mutableStateOf(presenter.getState()) }
    var page by rememberSaveable { mutableStateOf(QuizPage.Start) }
    val challenge = rememberSaveable(saver = ChallengePresenter.saver) { ChallengePresenter() }
    val appContext = LocalContext.current.applicationContext
    val profileScope = rememberCoroutineScope()
    val profilePresenter = remember(appContext, profileScope) { ProfilePresenter(appContext, profileScope) }
    val leaderboard = remember(appContext, profileScope) { LeaderboardPresenter(LeaderboardStore(appContext), profileScope) }
    var choosingPlayer by rememberSaveable { mutableStateOf(false) }
    var playerName by rememberSaveable { mutableStateOf<String?>(null) }
    var roundId by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var leaderboardError by rememberSaveable { mutableStateOf(false) }
    val currentPlayer = playerName ?: stringResource(R.string.leaderboard_guest)
    var quizStarted by rememberSaveable { mutableStateOf(false) }
    var menuOrigin by rememberSaveable { mutableStateOf(QuizPage.Start) }
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    var selectedSecret by rememberSaveable { mutableStateOf<QuizSecret?>(null) }
    var visitedSettingsDuringQuiz by rememberSaveable { mutableStateOf(false) }
    val question = state.question
    val questionNumber = state.questionIndex + 1
    val questionProgress = animateFloatAsState(
        targetValue = if (question == null) 1f else questionNumber.toFloat() / state.questionCount,
        animationSpec = tween(durationMillis = 350),
        label = "Quiz progress",
    )
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
        roundId = UUID.randomUUID().toString()
        quizStarted = true
        visitedSettingsDuringQuiz = false
    }

    fun goBack() {
        if (leaderboard.recording) return
        page = when (page) {
            QuizPage.Start -> QuizPage.Start
            QuizPage.Profile -> QuizPage.Start
            QuizPage.Challenges -> if (challenge.state == null) QuizPage.Start else {
                challenge.close()
                QuizPage.Challenges
            }
            QuizPage.Playing -> if (question == null) QuizPage.Start else QuizPage.Paused
            QuizPage.Paused -> QuizPage.Playing
            QuizPage.Settings, QuizPage.Collection, QuizPage.Leaderboard -> menuOrigin
            QuizPage.About -> QuizPage.Settings
        }
    }

    BackHandler(enabled = page != QuizPage.Start, onBack = ::goBack)
    LaunchedEffect(page) {
        if (page == QuizPage.Leaderboard) leaderboard.load()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.background, colors.primaryContainer))),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.widthIn(max = 520.dp).fillMaxWidth().fillMaxHeight()) {
            if (page == QuizPage.Start || page == QuizPage.Profile) {
                HomeTabs(
                    profileSelected = page == QuizPage.Profile,
                    onHome = { page = QuizPage.Start },
                    onProfile = { page = QuizPage.Profile },
                )
            }
            if (page == QuizPage.Playing) {
                QuizHeader(
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
                    onPause = if (question != null && !leaderboard.recording) ({ page = QuizPage.Paused }) else null,
                )
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = if (page == QuizPage.Start) Alignment.Center else Alignment.TopCenter,
            ) {
                if (page == QuizPage.Profile) {
                    ProfileScreen(editor = profilePresenter, isProfileSelected = { page == QuizPage.Profile })
                } else key(page, state.questionIndex, challenge.state?.let { Triple(it.mode, it.index, it.end) }) {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        when (page) {
                            QuizPage.Start -> WelcomePanel(state.questionCount, ::discoverSecret)
                            QuizPage.Profile -> Unit
                            QuizPage.Paused -> PauseMenu(
                                state = state,
                                keyAvailable = visitedSettingsDuringQuiz && !settings.showCorrections &&
                                    questionNumber == state.questionCount,
                                onSettings = { openMenu(QuizPage.Settings) },
                                onCollection = { openMenu(QuizPage.Collection) },
                                onProfile = { page = QuizPage.Profile },
                                onLeave = { confirmLeave = true },
                                onDiscover = ::discoverSecret,
                            )
                            QuizPage.Settings -> SettingsMenu(
                                settings = settings,
                                onSettingsChange = onSettingsChange,
                                onDiscover = ::discoverSecret,
                                onAbout = { page = QuizPage.About },
                            )
                            QuizPage.About -> AboutScreen()
                            QuizPage.Challenges -> ChallengeScreen(challenge)
                            QuizPage.Leaderboard -> LeaderboardScreen(
                                leaderboard.rankings, leaderboard.loading, leaderboard.failed, leaderboard::load,
                            )
                            QuizPage.Collection -> SecretCollection(
                                foundSecrets = foundSecrets,
                                onOpen = { selectedSecret = it },
                                onDiscover = ::discoverSecret,
                            )
                            QuizPage.Playing -> {
                                if (question == null) {
                                    ResultPanel(state.score, state.questionCount, ::discoverSecret)
                                    Text(stringResource(R.string.leaderboard_result_player, currentPlayer))
                                } else {
                                    QuestionPanel(
                                        question = question,
                                        questionNumber = questionNumber,
                                        questionCount = state.questionCount,
                                        progress = { questionProgress.value },
                                        score = state.score,
                                        selectedChoice = state.selectedChoice,
                                        isCorrect = state.isCorrect,
                                        horizontalAnswers = settings.horizontalAnswers,
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
                    label = stringResource(if (quizStarted && question != null) R.string.resume_quiz else R.string.start_quiz),
                    onClick = {
                        if (quizStarted && question != null) {
                            page = QuizPage.Playing
                        } else {
                            profilePresenter.load()
                            leaderboard.load()
                            choosingPlayer = true
                        }
                    },
                    secondaryAction = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MenuButton(stringResource(R.string.settings), { openMenu(QuizPage.Settings) }, Modifier.weight(1f))
                            MenuButton(stringResource(R.string.secret_collection), { openMenu(QuizPage.Collection) }, Modifier.weight(1f))
                        }
                    },
                    feedback = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MenuButton(stringResource(R.string.leaderboard), { openMenu(QuizPage.Leaderboard) }, Modifier.weight(1f))
                            MenuButton(stringResource(R.string.challenges), { page = QuizPage.Challenges }, Modifier.weight(1f))
                        }
                    },
                )
                QuizPage.Profile -> Unit
                QuizPage.Paused -> QuizActionBar(
                    label = stringResource(R.string.resume_quiz),
                    onClick = { page = QuizPage.Playing },
                )
                QuizPage.Settings, QuizPage.Collection, QuizPage.About, QuizPage.Leaderboard, QuizPage.Challenges -> QuizActionBar(
                    label = stringResource(R.string.back),
                    onClick = ::goBack,
                )
                QuizPage.Playing -> {
                    if (question == null) {
                        QuizActionBar(
                            label = stringResource(R.string.restart_quiz),
                            onClick = ::resetQuiz,
                            secondaryAction = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    MenuButton(stringResource(R.string.main_menu), { page = QuizPage.Start }, Modifier.weight(1f))
                                    MenuButton(stringResource(R.string.leaderboard), { openMenu(QuizPage.Leaderboard) }, Modifier.weight(1f))
                                }
                            },
                        )
                    } else {
                        QuestionActions(
                            selectedChoice = state.selectedChoice,
                            isCorrect = state.isCorrect,
                            correctAnswer = question.correctAnswer.takeIf { settings.showCorrections },
                            isLastQuestion = questionNumber == state.questionCount,
                            recording = leaderboard.recording,
                            onContinue = {
                                if (questionNumber == state.questionCount && state.selectedChoice != null) {
                                    leaderboard.completeGame(
                                        roundId, currentPlayer, state.score,
                                        onRecorded = { state = presenter.goToNextQuestion() },
                                        onFailure = { leaderboardError = true },
                                    )
                                } else {
                                    state = presenter.goToNextQuestion()
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (choosingPlayer) {
        PlayerDialog(profilePresenter.savedName, leaderboard.rankings, { choosingPlayer = false }) { name ->
            playerName = name
            resetQuiz()
            choosingPlayer = false
            page = QuizPage.Playing
        }
    }
    if (leaderboardError) {
        AlertDialog(
            onDismissRequest = { leaderboardError = false },
            title = { Text(stringResource(R.string.leaderboard)) },
            text = { Text(stringResource(R.string.leaderboard_save_error)) },
            confirmButton = {
                TextButton(onClick = { leaderboardError = false }) { Text(stringResource(R.string.close)) }
            },
        )
    }

    if (confirmLeave) {
        LeaveQuizDialog(
            onCancel = { confirmLeave = false },
            onLeave = {
                confirmLeave = false
                resetQuiz()
                quizStarted = false
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
        QuizNavigation(settings = QuizSettings(), onSettingsChange = {}, foundSecrets = emptySet(), onSecretFound = {})
    }
}
