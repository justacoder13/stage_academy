package fr.stageacademy.quiz.challenge_view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.stageacademy.quiz.R
import fr.stageacademy.quiz.shared_view.MenuButton
import fr.stageacademy.quiz.shared_view.MenuCard
import kotlinx.coroutines.delay

@Composable
internal fun ChallengeScreen(presenter: ChallengeContract) {
    val game = presenter.state
    if (game == null) {
        ChallengeMode.entries.forEach { mode ->
            MenuCard(title = stringResource(mode.title)) {
                Text(stringResource(mode.description))
                MenuButton(stringResource(R.string.challenge_play_mode, stringResource(mode.title)), { presenter.start(mode) })
            }
        }
        return
    }

    val question = presenter.question ?: return
    LaunchedEffect(game.deadline, game.end) {
        while (presenter.state?.deadline != null && presenter.state?.end == null) {
            presenter.tick()
            delay(100)
        }
    }
    val feedback = when {
        game.choice == null -> null
        game.choice == question.correctAnswer -> stringResource(R.string.correct_feedback)
        else -> stringResource(R.string.incorrect_feedback, question.correctAnswer)
    }
    val end = game.end
    if (end != null) {
        val title = when (end) {
            ChallengeEnd.Complete -> R.string.challenge_complete
            ChallengeEnd.TimeUp -> R.string.challenge_time_up
            ChallengeEnd.WrongAnswer -> R.string.challenge_streak_end
        }
        MenuCard(title = stringResource(title)) {
            Text(stringResource(R.string.final_score, game.score, game.order.size), style = MaterialTheme.typography.displaySmall)
            if (end == ChallengeEnd.WrongAnswer && feedback != null) Text(feedback)
            MenuButton(stringResource(R.string.restart_quiz), { presenter.start(game.mode) })
            MenuButton(stringResource(R.string.challenge_choose_another), presenter::close)
        }
    } else {
        MenuCard(title = stringResource(game.mode.title)) {
            Text(stringResource(R.string.question_progress, game.index + 1, game.order.size))
            Text(stringResource(R.string.quiz_score, game.score, game.order.size))
            if (game.deadline != null) {
                Text(
                    stringResource(R.string.challenge_seconds, presenter.secondsLeft),
                    color = if (presenter.secondsLeft <= 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(question.text, style = MaterialTheme.typography.titleLarge)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                question.choices.forEach { choice ->
                    MenuButton(choice, { presenter.answer(choice) }, enabled = game.choice == null)
                }
            }
            if (feedback != null) {
                Text(feedback, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                Button(onClick = presenter::next, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (game.index == game.order.lastIndex) R.string.show_results else R.string.next_question))
                }
            }
        }
    }
}
