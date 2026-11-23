package fr.stageacademy.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import fr.stageacademy.quiz.ui.theme.QuizForest
import fr.stageacademy.quiz.ui.theme.QuizLime

private val PanelShape = RoundedCornerShape(28.dp)
private val ControlShape = RoundedCornerShape(16.dp)

@Composable
internal fun WelcomePanel(questionCount: Int, onStart: () -> Unit) {
    Surface(shape = PanelShape, color = QuizForest, contentColor = Color.White) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CodeIllustration(Modifier.fillMaxWidth().height(152.dp))
            QuizTag(
                text = stringResource(R.string.question_count, questionCount),
                color = QuizLime,
                contentColor = QuizForest,
            )
            Text(
                text = stringResource(R.string.welcome_title),
                modifier = Modifier.widthIn(max = 260.dp).semantics { heading() },
                style = MaterialTheme.typography.displaySmall,
            )
        }
    }

    Surface(shape = PanelShape) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.welcome_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            QuizButton(label = stringResource(R.string.start_quiz), onClick = onStart)
        }
    }
}

@Composable
internal fun QuizHeader(score: Int?, questionCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.welcome_title),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (score != null) {
            QuizTag(
                text = stringResource(R.string.quiz_score, score, questionCount),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
internal fun QuestionPanel(
    question: Question,
    questionNumber: Int,
    questionCount: Int,
    selectedChoice: String?,
    isCorrect: Boolean,
    canContinue: Boolean,
    onChoice: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = PanelShape) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(
                    text = stringResource(R.string.question_progress, questionNumber, questionCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { questionNumber.toFloat() / questionCount },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    trackColor = colors.primaryContainer,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
            }
            Text(
                text = question.text,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
            )
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                question.choices.forEachIndexed { index, choice ->
                    val selectedAnswer = choice == selectedChoice
                    val containerColor = when {
                        !selectedAnswer -> colors.surface
                        isCorrect -> colors.primaryContainer
                        else -> colors.errorContainer
                    }
                    val contentColor = when {
                        !selectedAnswer -> colors.onSurface
                        isCorrect -> colors.onPrimaryContainer
                        else -> colors.onErrorContainer
                    }
                    Surface(
                        onClick = { onChoice(choice) },
                        modifier = Modifier.fillMaxWidth().semantics { selected = selectedAnswer },
                        enabled = !canContinue,
                        shape = ControlShape,
                        color = containerColor,
                        contentColor = contentColor,
                        border = BorderStroke(1.dp, if (selectedAnswer) contentColor else colors.outlineVariant),
                    ) {
                        Row(
                            modifier = Modifier.heightIn(min = 54.dp).padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Surface(shape = CircleShape, color = contentColor.copy(alpha = 0.08f)) {
                                Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = ('A' + index).toString(),
                                        modifier = Modifier.clearAndSetSemantics {},
                                        style = MaterialTheme.typography.labelLarge,
                                        color = contentColor,
                                    )
                                }
                            }
                            Text(
                                text = choice,
                                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                            )
                        }
                    }
                }
            }
            val feedback = when {
                selectedChoice == null -> R.string.choose_answer
                isCorrect -> R.string.correct_feedback
                else -> R.string.incorrect_feedback
            }
            Text(
                text = stringResource(feedback),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.labelLarge,
                color = when {
                    selectedChoice == null -> colors.onSurfaceVariant
                    isCorrect -> colors.onPrimaryContainer
                    else -> colors.onErrorContainer
                },
            )
        }
    }
    QuizButton(
        label = stringResource(if (questionNumber == questionCount) R.string.show_results else R.string.next_question),
        onClick = onContinue,
        enabled = canContinue,
    )
}

@Composable
internal fun ResultPanel(score: Int, questionCount: Int, onRestart: () -> Unit) {
    Surface(shape = PanelShape, color = QuizForest, contentColor = Color.White) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.quiz_finished),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.final_score, score, questionCount),
                style = MaterialTheme.typography.displayLarge.copy(fontFamily = FontFamily.Serif),
                color = QuizLime,
            )
            Text(text = stringResource(R.string.final_score_label), style = MaterialTheme.typography.bodyLarge)
        }
    }
    QuizButton(label = stringResource(R.string.restart_quiz), onClick = onRestart)
}

@Composable
private fun QuizButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
        enabled = enabled,
        shape = ControlShape,
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun QuizTag(text: String, color: Color, contentColor: Color) {
    Surface(shape = CircleShape, color = color, contentColor = contentColor) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun CodeIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val spacing = 16.dp.toPx()
        for (column in 0..(size.width / spacing).toInt()) {
            for (row in 0..(size.height / spacing).toInt()) {
                drawCircle(QuizLime.copy(alpha = 0.20f), 1.dp.toPx(), Offset(column * spacing, row * spacing))
            }
        }

        val tileWidth = 126.dp.toPx().coerceAtMost(size.width * 0.8f)
        val tileHeight = tileWidth * 0.72f
        val tileSize = Size(tileWidth, tileHeight)
        val tilePosition = Offset(center.x - tileWidth / 2, center.y - tileHeight / 2)
        val corner = CornerRadius(18.dp.toPx())
        rotate(-16f) {
            drawRoundRect(Color(0xFF285648), tilePosition, tileSize, corner)
            drawRoundRect(QuizLime.copy(alpha = 0.5f), tilePosition, tileSize, corner, style = Stroke(1.dp.toPx()))
        }
        rotate(9f) {
            drawRoundRect(QuizLime, tilePosition, tileSize, corner)
            val unit = tileWidth / 12f
            val code = Path().apply {
                moveTo(center.x - 2.1f * unit, center.y - unit)
                lineTo(center.x - 3.4f * unit, center.y)
                lineTo(center.x - 2.1f * unit, center.y + unit)
                moveTo(center.x + 0.7f * unit, center.y - 1.55f * unit)
                lineTo(center.x - 0.7f * unit, center.y + 1.55f * unit)
                moveTo(center.x + 2.1f * unit, center.y - unit)
                lineTo(center.x + 3.4f * unit, center.y)
                lineTo(center.x + 2.1f * unit, center.y + unit)
            }
            drawPath(code, QuizForest, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
