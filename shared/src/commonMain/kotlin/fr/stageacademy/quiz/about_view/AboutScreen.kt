package fr.stageacademy.quiz.about_view

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import fr.stageacademy.quiz.resources.Res
import fr.stageacademy.quiz.resources.*
import fr.stageacademy.quiz.shared_view.MenuCard

@Composable
internal fun AboutScreen() {
    MenuCard(title = stringResource(Res.string.about)) {
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(Res.string.about_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
