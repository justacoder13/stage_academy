package fr.stageacademy.quiz.leaderboard_view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import fr.stageacademy.quiz.StorageException
import fr.stageacademy.quiz.platformIo
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

internal class LeaderboardPresenter(private val store: LeaderboardStore, private val scope: CoroutineScope) {
    private var refreshAfterLoad = false
    val shared: Boolean get() = store.shared
    val googleAvailable: Boolean get() = store.googleAvailable
    private var googleJob: Job? = null
    var googleBusy by mutableStateOf(false)
        private set
    var googleStatus by mutableStateOf<GoogleLoginStatus?>(null)
        private set
    var accountName by mutableStateOf(store.accountName)
        private set
    var source by mutableStateOf(LeaderboardSource.Local)
        private set
    var rankings by mutableStateOf(emptyList<PlayerRanking>())
        private set
    var deviceRankings by mutableStateOf(emptyList<PlayerRanking>())
        private set
    var loading by mutableStateOf(false)
        private set
    var recording by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set

    fun connectGoogle(openBrowser: (String) -> Unit) {
        if (!googleAvailable || googleBusy) return
        googleBusy = true
        googleStatus = null
        googleJob = scope.launch {
            try {
                val login = withContext(platformIo) { store.startGoogleLogin() }
                openBrowser(login.authorizationUrl)
                var result = GoogleLoginStatus.Pending
                while (result == GoogleLoginStatus.Pending) {
                    delay(2_000)
                    result = withContext(platformIo) { store.pollGoogleLogin(login) }
                }
                if (result == GoogleLoginStatus.Connected) {
                    accountName = store.accountName
                    refresh()
                } else {
                    googleStatus = result
                }
            } catch (_: GoogleConfigurationException) {
                googleStatus = GoogleLoginStatus.NotConfigured
            } catch (_: StorageException) {
                googleStatus = GoogleLoginStatus.Failed
            } catch (_: IllegalArgumentException) {
                googleStatus = GoogleLoginStatus.Failed
            } catch (_: UnsupportedOperationException) {
                googleStatus = GoogleLoginStatus.Failed
            } finally {
                if (!currentCoroutineContext().isActive) store.signOut()
                googleBusy = false
            }
        }
    }

    fun cancelGoogle() {
        googleJob?.cancel()
        store.signOut()
        accountName = null
        googleStatus = null
        refresh()
    }

    private fun refresh() {
        if (loading || recording) refreshAfterLoad = true else load()
    }

    fun load() {
        if (loading || recording) return
        loading = true
        failed = false
        scope.launch {
            try {
                val (standings, device) = withContext(platformIo) {
                    val standings = store.standings()
                    val device = if (standings.source == LeaderboardSource.Shared) store.localRankings() else standings.players
                    standings to device
                }
                rankings = standings.players
                deviceRankings = device
                source = standings.source
            } catch (_: StorageException) {
                failed = true
            } finally {
                accountName = store.accountName
                loading = false
                if (refreshAfterLoad) {
                    refreshAfterLoad = false
                    load()
                }
            }
        }
    }

    fun completeGame(id: String, name: String, points: Int, onRecorded: () -> Unit, onFailure: () -> Unit) {
        if (recording) return
        recording = true
        scope.launch {
            var recorded = false
            try {
                val localRankings = withContext(platformIo) { store.record(id, name, points) }
                deviceRankings = localRankings
                if (source != LeaderboardSource.Shared) rankings = localRankings
                recorded = true
                onRecorded()
            } catch (_: StorageException) {
                onFailure()
            } finally {
                recording = false
            }
            if (recorded && store.shared) {
                refresh()
            }
        }
    }
}
