package fr.stageacademy.quiz.leaderboard_view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import fr.stageacademy.quiz.resources.Res
import fr.stageacademy.quiz.resources.*
import fr.stageacademy.quiz.shared_view.MenuCard

@Composable
internal fun LeaderboardScreen(presenter: LeaderboardPresenter) {
    val players = presenter.rankings
    val loading = presenter.loading
    val failed = presenter.failed
    val shared = presenter.shared
    val uriHandler = LocalUriHandler.current
    MenuCard(title = stringResource(Res.string.leaderboard)) {
        Text(
            stringResource(if (shared) Res.string.leaderboard_shared_description else Res.string.leaderboard_description),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (shared && presenter.googleAvailable) {
            val accountName = presenter.accountName
            when {
                presenter.googleBusy -> {
                    Text(stringResource(Res.string.google_waiting))
                    TextButton(onClick = presenter::cancelGoogle) { Text(stringResource(Res.string.google_cancel)) }
                }
                accountName != null -> {
                    Text(stringResource(Res.string.google_connected, accountName))
                    TextButton(onClick = presenter::cancelGoogle) { Text(stringResource(Res.string.google_logout)) }
                }
                else -> Button(onClick = { presenter.connectGoogle(uriHandler::openUri) }) {
                    Text(stringResource(Res.string.google_login))
                }
            }
            val error = when (presenter.googleStatus) {
                GoogleLoginStatus.Conflict -> Res.string.google_conflict
                GoogleLoginStatus.Expired -> Res.string.google_expired
                GoogleLoginStatus.Failed -> Res.string.google_failed
                else -> null
            }
            if (error != null) Text(stringResource(error))
        }
        when {
            loading -> Text(stringResource(Res.string.leaderboard_loading))
            failed -> {
                Text(stringResource(Res.string.leaderboard_load_error))
                TextButton(onClick = presenter::load) { Text(stringResource(Res.string.leaderboard_retry)) }
            }
            presenter.source == LeaderboardSource.Offline -> Text(stringResource(Res.string.leaderboard_offline))
            players.isEmpty() -> Text(stringResource(Res.string.leaderboard_empty))
        }
        if (shared && !loading && !failed) {
            TextButton(onClick = presenter::load) { Text(stringResource(Res.string.leaderboard_refresh)) }
        }
    }
    if (!loading && !failed) {
        val ranks = players.mapIndexed { index, player -> player.points to index + 1 }
            .asReversed().toMap()
        players.forEach { player ->
            MenuCard(title = stringResource(Res.string.leaderboard_rank, ranks.getValue(player.points), player.name)) {
                Text(
                    pluralStringResource(Res.plurals.leaderboard_points, if (player.points == 1L) 1 else 2, player.points),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
internal fun PlayerDialog(suggestedName: String, players: List<PlayerRanking>, onDismiss: () -> Unit, onStart: (String) -> Unit) {
    var editedName by rememberSaveable { mutableStateOf<String?>(null) }
    val name = editedName ?: suggestedName
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.leaderboard_choose_player)) },
        text = {
            Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { editedName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.profile_name)) },
                    singleLine = true,
                )
                if (players.isNotEmpty()) {
                    Text(stringResource(Res.string.leaderboard_existing_players), style = MaterialTheme.typography.labelLarge)
                    players.forEach { player ->
                        TextButton(onClick = { editedName = player.name }, modifier = Modifier.align(Alignment.Start)) {
                            Text(player.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = { onStart(name.trim()) }) { Text(stringResource(Res.string.start_quiz)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}
