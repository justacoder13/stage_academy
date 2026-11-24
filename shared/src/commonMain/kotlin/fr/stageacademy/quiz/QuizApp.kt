package fr.stageacademy.quiz

import fr.stageacademy.quiz.resources.Res
import fr.stageacademy.quiz.resources.*

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
import androidx.compose.runtime.saveable.listSaver
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import fr.stageacademy.quiz.about_view.AboutScreen
import fr.stageacademy.quiz.challenge_view.ChallengePresenter
import fr.stageacademy.quiz.challenge_view.ChallengeActions
import fr.stageacademy.quiz.challenge_view.ChallengeScreen
import fr.stageacademy.quiz.challenge_view.PlayModeMenu
import fr.stageacademy.quiz.challenge_view.title
import fr.stageacademy.quiz.collection_view.QuizSecret
import fr.stageacademy.quiz.collection_view.SecretCollection
import fr.stageacademy.quiz.collection_view.SecretEntryDialog
import fr.stageacademy.quiz.menu_view.LeaveQuizDialog
import fr.stageacademy.quiz.menu_view.HomeTabs
import fr.stageacademy.quiz.menu_view.PauseMenu
import fr.stageacademy.quiz.menu_view.WelcomePanel
import fr.stageacademy.quiz.leaderboard_view.LeaderboardPresenter
import fr.stageacademy.quiz.leaderboard_view.LeaderboardScreen
import fr.stageacademy.quiz.leaderboard_view.PlayerDialog
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
import fr.stageacademy.quiz.splash_view.SplashScreen
import fr.stageacademy.quiz.ui.theme.QuizTheme

private enum class QuizPage { Start, Profile, Playing, Paused, Settings, Collection, About, Leaderboard, Challenges }

private val PresenterSaver = listSaver<QuizContract, Any>(
    save = { presenter ->
        val state = presenter.getState()
        listOf(state.questionIndex, state.score, state.selectedChoice.orEmpty())
    },
    restore = { saved ->
        QuizPresenter.restore(
            questions = quizQuestions,
            questionIndex = saved[0] as Int,
            score = saved[1] as Int,
            selectedChoice = (saved[2] as String).takeIf { it.isNotEmpty() },
        )
    },
)

@Composable
fun QuizApp(onDarkThemeChanged: (Boolean) -> Unit = {}) {
    val preferences = rememberQuizPreferences()
    var settings by remember(preferences) { mutableStateOf(preferences.settings()) }
    var foundSecrets by remember(preferences) { mutableStateOf(preferences.secrets()) }
    var splashFinished by rememberSaveable { mutableStateOf(false) }
    val darkTheme = when (settings.appearance) {
        QuizAppearance.System -> isSystemInDarkTheme()
        QuizAppearance.Light -> false
        QuizAppearance.Dark -> true
    }
    SideEffect { onDarkThemeChanged(darkTheme) }
    if (!splashFinished) {
        SplashScreen(darkTheme) { splashFinished = true }
        return
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
                        foundSecrets = foundSecrets + secret
                        preferences.saveSecrets(foundSecrets)
                    }
                },
                onSettingsChange = { updated ->
                    settings = updated
                    preferences.saveSettings(updated)
                },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
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
    val challengeGame = challenge.state
    val profileScope = rememberCoroutineScope()
    val profile = rememberProfile(profileScope)
    val leaderboard = rememberLeaderboardStore().let { store ->
        remember(store, profileScope) { LeaderboardPresenter(store, profileScope) }
    }
    var choosingPlayer by rememberSaveable { mutableStateOf(false) }
    var playerName by rememberSaveable { mutableStateOf<String?>(null) }
    var roundId by rememberSaveable { mutableStateOf(Uuid.random().toString()) }
    var leaderboardError by rememberSaveable { mutableStateOf(false) }
    val currentPlayer = playerName ?: stringResource(Res.string.leaderboard_guest)
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
    val challengeProgress = animateFloatAsState(
        targetValue = challengeGame?.let { (it.index + 1f) / it.order.size } ?: 0f,
        animationSpec = tween(durationMillis = 350),
        label = "Challenge progress",
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
        roundId = Uuid.random().toString()
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
            QuizPage.Settings, QuizPage.Collection -> menuOrigin
            QuizPage.About, QuizPage.Leaderboard -> QuizPage.Settings
        }
    }

    PlatformBackHandler(enabled = page != QuizPage.Start, onBack = ::goBack)
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
            } else if (page == QuizPage.Challenges && challengeGame != null) {
                QuizHeader(
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
                    title = stringResource(challengeGame.mode.title),
                    onPause = ::goBack,
                    actionLabel = stringResource(Res.string.back),
                )
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = if (page == QuizPage.Start) Alignment.Center else Alignment.TopCenter,
            ) {
                if (page == QuizPage.Profile) {
                    profile.Content(isProfileSelected = { page == QuizPage.Profile })
                } else key(page, state.questionIndex, challengeGame?.let { Triple(it.mode, it.index, it.end) }) {
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
                                onLeaderboard = { page = QuizPage.Leaderboard },
                            )
                            QuizPage.About -> AboutScreen()
                            QuizPage.Challenges -> {
                                if (challengeGame == null) {
                                    val canResumeNormal = quizStarted && question != null
                                    PlayModeMenu(
                                        canResumeNormal = canResumeNormal,
                                        onNormal = {
                                            if (canResumeNormal) {
                                                page = QuizPage.Playing
                                            } else {
                                                profile.load()
                                                leaderboard.load()
                                                choosingPlayer = true
                                            }
                                        },
                                        onChallenge = challenge::start,
                                    )
                                } else {
                                    ChallengeScreen(challenge, settings.horizontalAnswers, progress = { challengeProgress.value })
                                }
                            }
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
                                    Text(stringResource(Res.string.leaderboard_result_player, currentPlayer))
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
                    label = stringResource(Res.string.play),
                    onClick = { page = QuizPage.Challenges },
                    secondaryAction = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MenuButton(stringResource(Res.string.secret_collection), { openMenu(QuizPage.Collection) }, Modifier.weight(1f))
                            MenuButton(stringResource(Res.string.settings), { openMenu(QuizPage.Settings) }, Modifier.weight(1f))
                        }
                    },
                )
                QuizPage.Profile -> Unit
                QuizPage.Paused -> QuizActionBar(
                    label = stringResource(Res.string.resume_quiz),
                    onClick = { page = QuizPage.Playing },
                )
                QuizPage.Settings, QuizPage.Collection, QuizPage.About, QuizPage.Leaderboard -> QuizActionBar(
                    label = stringResource(Res.string.back),
                    onClick = ::goBack,
                )
                QuizPage.Challenges -> {
                    if (challengeGame == null) {
                        QuizActionBar(label = stringResource(Res.string.back), onClick = ::goBack)
                    } else {
                        ChallengeActions(challenge)
                    }
                }
                QuizPage.Playing -> {
                    if (question == null) {
                        QuizActionBar(
                            label = stringResource(Res.string.restart_quiz),
                            onClick = ::resetQuiz,
                            secondaryAction = {
                                MenuButton(stringResource(Res.string.main_menu), { page = QuizPage.Start })
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
        PlayerDialog(profile.savedName, leaderboard.rankings, { choosingPlayer = false }) { name ->
            playerName = name
            resetQuiz()
            choosingPlayer = false
            page = QuizPage.Playing
        }
    }
    if (leaderboardError) {
        AlertDialog(
            onDismissRequest = { leaderboardError = false },
            title = { Text(stringResource(Res.string.leaderboard)) },
            text = { Text(stringResource(Res.string.leaderboard_save_error)) },
            confirmButton = {
                TextButton(onClick = { leaderboardError = false }) { Text(stringResource(Res.string.close)) }
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
