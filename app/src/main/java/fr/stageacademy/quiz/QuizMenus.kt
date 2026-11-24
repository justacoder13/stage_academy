package fr.stageacademy.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

internal enum class QuizAppearance(val label: Int) {
    System(R.string.appearance_system),
    Light(R.string.appearance_light),
    Dark(R.string.appearance_dark),
}

internal data class QuizSettings(
    val appearance: QuizAppearance = QuizAppearance.System,
    val showCorrections: Boolean = true,
)

@Composable
internal fun PauseMenu(
    state: QuizState,
    keyAvailable: Boolean,
    onSettings: () -> Unit,
    onCollection: () -> Unit,
    onLeave: () -> Unit,
    onDiscover: (QuizSecret) -> Unit,
) {
    val questionNumber = state.questionIndex + 1
    MenuCard(
        title = stringResource(R.string.quiz_paused),
        titleModifier = Modifier
            .secretGesture(QuizSecret.Hourglass, onDiscover, enabled = questionNumber * 2 == state.questionCount)
            .secretGesture(QuizSecret.Key, onDiscover, taps = 2, enabled = keyAvailable),
    ) {
        Text(stringResource(R.string.question_progress, questionNumber, state.questionCount))
        Text(
            text = stringResource(R.string.quiz_score, state.score, state.questionCount),
            modifier = Modifier.secretGesture(QuizSecret.Music, onDiscover, taps = 2),
        )
        MenuButton(label = stringResource(R.string.settings), onClick = onSettings)
        MenuButton(label = stringResource(R.string.secret_collection), onClick = onCollection)
        TextButton(onClick = onLeave, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.leave_quiz))
        }
    }
}

@Composable
internal fun SettingsMenu(
    settings: QuizSettings,
    onSettingsChange: (QuizSettings) -> Unit,
    onDiscover: (QuizSecret) -> Unit,
) {
    MenuCard(
        title = stringResource(R.string.settings),
        titleModifier = Modifier.secretGesture(QuizSecret.Semicolon, onDiscover, taps = 5),
    ) {
        Column(Modifier.selectableGroup()) {
            Text(
                text = stringResource(R.string.appearance),
                style = MaterialTheme.typography.titleMedium,
            )
            QuizAppearance.entries.forEach { appearance ->
                val selected = settings.appearance == appearance
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = { onSettingsChange(settings.copy(appearance = appearance)) },
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    RadioButton(selected = selected, onClick = null)
                    Text(stringResource(appearance.label))
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
                value = settings.showCorrections,
                role = Role.Switch,
                onValueChange = { onSettingsChange(settings.copy(showCorrections = it)) },
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.show_corrections),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.show_corrections_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = settings.showCorrections, onCheckedChange = null)
        }
    }

}

@Composable
internal fun MenuButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
internal fun LeaveQuizDialog(onCancel: () -> Unit, onLeave: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.confirm_leave_quiz)) },
        text = { Text(stringResource(R.string.leave_quiz_message)) },
        confirmButton = { TextButton(onClick = onLeave) { Text(stringResource(R.string.leave_quiz)) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun MenuCard(
    title: String,
    titleModifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(shape = RoundedCornerShape(28.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = title,
                modifier = titleModifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
            )
            content()
        }
    }
}
