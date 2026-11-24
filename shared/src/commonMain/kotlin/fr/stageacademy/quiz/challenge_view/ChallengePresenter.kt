package fr.stageacademy.quiz.challenge_view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import fr.stageacademy.quiz.elapsedMillis
import androidx.compose.runtime.setValue
import fr.stageacademy.quiz.quiz_view.quizQuestions

internal class ChallengePresenter(initial: ChallengeState? = null) : ChallengeContract {
    private val game = ChallengeGame(::elapsedMillis, initial)
    override var state by mutableStateOf(game.state)
        private set
    override var secondsLeft by mutableIntStateOf(game.secondsLeft)
        private set
    override val question get() = state?.let { quizQuestions[it.order[it.index]] }

    override fun start(mode: ChallengeMode) = update { game.start(mode) }
    override fun tick() = update { game.tick() }
    override fun answer(choice: String) = update { game.answer(choice) }
    override fun next() = update { game.next() }
    override fun close() = update { game.close() }

    private inline fun update(action: () -> Unit) {
        action()
        state = game.state
        secondsLeft = game.secondsLeft
    }

    companion object {
        val saver = listSaver<ChallengePresenter, Any>(
            save = { presenter ->
                presenter.state?.let { state ->
                    listOf(
                        state.mode.name, state.order.toIntArray(), state.index, state.score,
                        state.choice.orEmpty(), state.deadline?.toString().orEmpty(), state.end?.name.orEmpty(),
                    )
                }.orEmpty()
            },
            restore = { saved ->
                val mode = ChallengeMode.entries.find { it.name == saved[0] }
                val order = (saved[1] as IntArray).toList()
                val index = saved[2] as Int
                val state = if (mode != null && index in order.indices && order.all { it in quizQuestions.indices }) {
                    ChallengeState(
                        mode, order, index, saved[3] as Int,
                        (saved[4] as String).takeIf { it.isNotEmpty() },
                        (saved[5] as String).toLongOrNull(),
                        ChallengeEnd.entries.find { it.name == saved[6] },
                    )
                } else null
                ChallengePresenter(state)
            },
        )
    }
}
