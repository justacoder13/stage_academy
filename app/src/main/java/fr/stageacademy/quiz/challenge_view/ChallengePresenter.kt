package fr.stageacademy.quiz.challenge_view

import android.os.Bundle
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import fr.stageacademy.quiz.quiz_view.quizQuestions

internal class ChallengePresenter(initial: ChallengeState? = null) : ChallengeContract {
    override var state by mutableStateOf(initial)
        private set
    override var secondsLeft by mutableIntStateOf(0)
        private set
    override val question get() = state?.let { quizQuestions[it.order[it.index]] }

    init { tick() }

    override fun start(mode: ChallengeMode) {
        val order = quizQuestions.indices.shuffled()
        state = ChallengeState(
            mode = mode,
            order = if (mode == ChallengeMode.Streak) order else order.take(5),
            deadline = if (mode == ChallengeMode.Timer) SystemClock.elapsedRealtime() + 45_000L else null,
        )
        tick()
    }

    override fun tick() {
        val current = state ?: return
        val deadline = current.deadline ?: return
        if (current.end != null) return
        val remaining = (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        secondsLeft = ((remaining + 999L) / 1000L).toInt()
        if (remaining == 0L) state = current.copy(end = ChallengeEnd.TimeUp)
    }

    override fun answer(choice: String) {
        tick()
        val current = state ?: return
        val question = question ?: return
        if (current.end != null || current.choice != null || choice !in question.choices) return
        val correct = choice == question.correctAnswer
        state = current.copy(
            choice = choice,
            score = current.score + if (correct) 1 else 0,
            deadline = current.deadline.takeUnless { current.index == current.order.lastIndex },
            end = ChallengeEnd.WrongAnswer.takeIf { !correct && current.mode == ChallengeMode.Streak },
        )
    }

    override fun next() {
        tick()
        val current = state ?: return
        if (current.end != null || current.choice == null) return
        state = if (current.index == current.order.lastIndex) current.copy(end = ChallengeEnd.Complete)
        else current.copy(index = current.index + 1, choice = null)
    }

    override fun close() {
        state = null
    }

    companion object {
        val saver = Saver<ChallengePresenter, Bundle>(
            save = { presenter ->
                Bundle().apply {
                    presenter.state?.let { state ->
                        putString("mode", state.mode.name)
                        putIntArray("order", state.order.toIntArray())
                        putInt("index", state.index)
                        putInt("score", state.score)
                        putString("choice", state.choice)
                        state.deadline?.let { putLong("deadline", it) }
                        putString("end", state.end?.name)
                    }
                }
            },
            restore = { bundle ->
                val mode = ChallengeMode.entries.find { it.name == bundle.getString("mode") }
                val order = bundle.getIntArray("order")?.toList().orEmpty()
                val index = bundle.getInt("index")
                val state = if (mode != null && index in order.indices && order.all { it in quizQuestions.indices }) {
                    ChallengeState(
                        mode, order, index, bundle.getInt("score"), bundle.getString("choice"),
                        bundle.getLong("deadline").takeIf { bundle.containsKey("deadline") },
                        ChallengeEnd.entries.find { it.name == bundle.getString("end") },
                    )
                } else null
                ChallengePresenter(state)
            },
        )
    }
}
