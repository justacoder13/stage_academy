package fr.stageacademy.quiz.leaderboard_view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

internal class LeaderboardPresenter(private val store: LeaderboardStore, private val scope: CoroutineScope) {
    var rankings by mutableStateOf(emptyList<PlayerRanking>())
        private set
    var loading by mutableStateOf(false)
        private set
    var recording by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set

    fun load() {
        if (loading || recording) return
        loading = true
        failed = false
        scope.launch {
            try {
                rankings = withContext(Dispatchers.IO) { store.rankings() }
            } catch (_: IOException) {
                failed = true
            } finally {
                loading = false
            }
        }
    }

    fun completeGame(id: String, name: String, points: Int, onRecorded: () -> Unit, onFailure: () -> Unit) {
        if (recording) return
        recording = true
        scope.launch {
            try {
                rankings = withContext(Dispatchers.IO) { store.record(id, name, points) }
                onRecorded()
            } catch (_: IOException) {
                onFailure()
            } finally {
                recording = false
            }
        }
    }
}
