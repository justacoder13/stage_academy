package fr.stageacademy.quiz.challenge_view

import fr.stageacademy.quiz.R
import fr.stageacademy.quiz.quiz_view.Question

internal enum class ChallengeMode(val title: Int, val description: Int) {
    Timer(R.string.challenge_timer, R.string.challenge_timer_desc),
    Streak(R.string.challenge_streak, R.string.challenge_streak_desc),
    Surprise(R.string.challenge_surprise, R.string.challenge_surprise_desc),
}

internal enum class ChallengeEnd { Complete, TimeUp, WrongAnswer }

internal data class ChallengeState(
    val mode: ChallengeMode,
    val order: List<Int>,
    val index: Int = 0,
    val score: Int = 0,
    val choice: String? = null,
    val deadline: Long? = null,
    val end: ChallengeEnd? = null,
)

internal interface ChallengeContract {
    val state: ChallengeState?
    val question: Question?
    val secondsLeft: Int
    fun start(mode: ChallengeMode)
    fun answer(choice: String)
    fun next()
    fun tick()
    fun close()
}
