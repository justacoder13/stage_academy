package fr.stageacademy.quiz.leaderboard_view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import fr.stageacademy.quiz.StorageException
import fr.stageacademy.quiz.platformIo
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                rankings = withContext(platformIo) { store.rankings() }
            } catch (_: StorageException) {
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
                rankings = withContext(platformIo) { store.record(id, name, points) }
                onRecorded()
            } catch (_: StorageException) {
                onFailure()
            } finally {
                recording = false
            }
        }
    }
}
