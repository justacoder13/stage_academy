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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.stageacademy.quiz.R
import fr.stageacademy.quiz.shared_view.MenuCard

@Composable
internal fun LeaderboardScreen(players: List<PlayerRanking>, loading: Boolean, failed: Boolean, onRetry: () -> Unit) {
    MenuCard(title = stringResource(R.string.leaderboard)) {
        Text(stringResource(R.string.leaderboard_description), style = MaterialTheme.typography.bodyMedium)
        when {
            loading -> Text(stringResource(R.string.leaderboard_loading))
            failed -> {
                Text(stringResource(R.string.leaderboard_load_error))
                TextButton(onClick = onRetry) { Text(stringResource(R.string.leaderboard_retry)) }
            }
            players.isEmpty() -> Text(stringResource(R.string.leaderboard_empty))
        }
    }
    if (!loading && !failed) {
        val ranks = players.mapIndexed { index, player -> player.points to index + 1 }
            .asReversed().toMap()
        players.forEach { player ->
            MenuCard(title = stringResource(R.string.leaderboard_rank, ranks.getValue(player.points), player.name)) {
                Text(
                    pluralStringResource(R.plurals.leaderboard_points, if (player.points == 1L) 1 else 2, player.points),
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
        title = { Text(stringResource(R.string.leaderboard_choose_player)) },
        text = {
            Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { editedName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.profile_name)) },
                    singleLine = true,
                )
                if (players.isNotEmpty()) {
                    Text(stringResource(R.string.leaderboard_existing_players), style = MaterialTheme.typography.labelLarge)
                    players.forEach { player ->
                        TextButton(onClick = { editedName = player.name }, modifier = Modifier.align(Alignment.Start)) {
                            Text(player.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = { onStart(name.trim()) }) { Text(stringResource(R.string.start_quiz)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
