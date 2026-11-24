package fr.stageacademy.quiz

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import fr.stageacademy.quiz.collection_view.QuizSecret
import fr.stageacademy.quiz.leaderboard_view.DesktopLeaderboardStore
import fr.stageacademy.quiz.leaderboard_view.LeaderboardStore
import fr.stageacademy.quiz.profile_view.DesktopPhotoFiles
import fr.stageacademy.quiz.profile_view.DesktopProfileScreen
import fr.stageacademy.quiz.profile_view.ProfileIdentity
import fr.stageacademy.quiz.profile_view.ProfilePanel
import fr.stageacademy.quiz.profile_view.ProfilePresenter
import fr.stageacademy.quiz.profile_view.ProfileStore
import fr.stageacademy.quiz.resources.*
import fr.stageacademy.quiz.settings_view.QuizAppearance
import fr.stageacademy.quiz.settings_view.QuizSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.painterResource
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.util.prefs.Preferences

internal actual fun elapsedMillis(): Long = System.nanoTime() / 1_000_000L
internal actual val platformIo: CoroutineDispatcher get() = Dispatchers.IO
internal actual typealias StorageException = java.io.IOException

internal val LocalQuizWindow = staticCompositionLocalOf<ComposeWindow> { error("Missing desktop window") }
internal val LocalQuizWindowFocused = staticCompositionLocalOf { false }
private val LocalBackActions = staticCompositionLocalOf<BackActions> { error("Missing desktop navigation") }

fun launchDesktopQuiz() = application {
    val backActions = remember { BackActions() }
    val availableScreen = remember { GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds }
    Window(
        title = stringResource(Res.string.app_name),
        icon = painterResource(Res.drawable.quiz_icon),
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            width = minOf(526, availableScreen.width - 24).dp,
            height = minOf(820, availableScreen.height - 24).dp,
        ),
        onPreviewKeyEvent = {
            it.key == Key.Escape && it.type == KeyEventType.KeyDown && backActions.goBack()
        },
    ) {
        var focused by remember(window) { mutableStateOf(window.isFocused) }
        DisposableEffect(window) {
            window.minimumSize = Dimension(360, 460)
            val focusListener = object : WindowAdapter() {
                override fun windowGainedFocus(event: WindowEvent) { focused = true }
                override fun windowLostFocus(event: WindowEvent) { focused = false }
            }
            window.addWindowFocusListener(focusListener)
            onDispose { window.removeWindowFocusListener(focusListener) }
        }
        CompositionLocalProvider(
            LocalQuizWindow provides window,
            LocalQuizWindowFocused provides focused,
            LocalBackActions provides backActions,
        ) {
            QuizApp()
        }
    }
}

private val dataDirectory: File
    get() = System.getenv("APPDATA")?.takeIf { it.isNotBlank() }?.let { File(it, "StageAcademy") }
        ?: File(System.getProperty("user.home"), ".stage-academy")

@Composable
internal actual fun rememberQuizPreferences(): QuizPreferences =
    remember { DesktopQuizPreferences(Preferences.userRoot().node("fr/stageacademy/quiz")) }

@Composable
internal actual fun rememberLeaderboardStore(): LeaderboardStore =
    remember { DesktopLeaderboardStore(dataDirectory) }

@Composable
internal actual fun rememberProfile(scope: CoroutineScope): ProfilePanel = remember(scope) {
    val presenter = ProfilePresenter(DesktopPhotoFiles(dataDirectory), ProfileStore(dataDirectory), scope)
    object : ProfilePanel, ProfileIdentity by presenter {
        @Composable
        override fun Content(isProfileSelected: () -> Boolean) = DesktopProfileScreen(presenter, isProfileSelected)
    }
}

@Composable
internal actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val actions = LocalBackActions.current
    val currentEnabled by rememberUpdatedState(enabled)
    val currentAction by rememberUpdatedState(onBack)
    DisposableEffect(actions) {
        val handler = {
            if (currentEnabled) currentAction()
            currentEnabled
        }
        actions.handlers.add(handler)
        onDispose { actions.handlers.remove(handler) }
    }
}

private class BackActions {
    val handlers = mutableListOf<() -> Boolean>()

    fun goBack(): Boolean = handlers.toList().asReversed().any { it() }
}

private class DesktopQuizPreferences(private val preferences: Preferences) : QuizPreferences {
    override fun settings() = QuizSettings(
        appearance = QuizAppearance.entries.find { it.name == preferences.get("appearance", "") }
            ?: QuizAppearance.System,
        showCorrections = preferences.getBoolean("showCorrections", true),
        horizontalAnswers = preferences.getBoolean("horizontalAnswers", false),
    )

    override fun secrets(): Set<QuizSecret> {
        val names = preferences.get("foundSecrets", "").split(',').toSet()
        return QuizSecret.entries.filter { it.name in names }.toSet()
    }

    override fun saveSettings(settings: QuizSettings) {
        preferences.put("appearance", settings.appearance.name)
        preferences.putBoolean("showCorrections", settings.showCorrections)
        preferences.putBoolean("horizontalAnswers", settings.horizontalAnswers)
    }

    override fun saveSecrets(secrets: Set<QuizSecret>) {
        preferences.put("foundSecrets", secrets.joinToString(",") { it.name })
    }
}
