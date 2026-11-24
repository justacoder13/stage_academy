package fr.stageacademy.quiz

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import fr.stageacademy.quiz.challenge_view.ChallengePresenter
import fr.stageacademy.quiz.leaderboard_view.LeaderboardPresenter
import fr.stageacademy.quiz.navigation_view.QuizNavigation
import fr.stageacademy.quiz.navigation_view.QuizNavigationPresenter
import fr.stageacademy.quiz.quiz_view.QuizPresenter
import fr.stageacademy.quiz.quiz_view.QuizPresenterSaver
import fr.stageacademy.quiz.quiz_view.quizQuestions
import fr.stageacademy.quiz.settings_view.QuizAppearance
import fr.stageacademy.quiz.splash_view.SplashScreen
import fr.stageacademy.quiz.ui.theme.QuizTheme

@Composable
fun QuizApp(onDarkThemeChanged: (Boolean) -> Unit = {}) {
    val preferences = rememberQuizPreferences()
    val quiz = rememberSaveable(saver = QuizPresenterSaver) { QuizPresenter(quizQuestions) }
    val challenge = rememberSaveable(saver = ChallengePresenter.saver) { ChallengePresenter() }
    val scope = rememberCoroutineScope()
    val profile = rememberProfile(scope)
    val leaderboardStore = rememberLeaderboardStore()
    val leaderboard = remember(leaderboardStore, scope) { LeaderboardPresenter(leaderboardStore, scope) }
    val presenter = rememberSaveable(
        quiz, challenge, leaderboard, profile, preferences,
        saver = QuizNavigationPresenter.saver(quiz, challenge, leaderboard, profile, preferences),
    ) {
        QuizNavigationPresenter(quiz, challenge, leaderboard, profile, preferences)
    }

    var splashFinished by rememberSaveable { mutableStateOf(false) }
    val darkTheme = when (presenter.settings.appearance) {
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
            QuizNavigation(presenter, profile, modifier = Modifier.padding(innerPadding))
        }
    }
}
