package fr.stageacademy.quiz.leaderboard_view

internal data class PlayerRanking(val name: String, val points: Long)
internal data class RankedRound(val id: String, val player: String, val points: Int)

internal enum class LeaderboardSource { Local, Shared, Offline }

internal data class LeaderboardStandings(val players: List<PlayerRanking>, val source: LeaderboardSource)

internal interface LeaderboardStore {
    val shared: Boolean get() = false
    fun rankings(): List<PlayerRanking>
    fun record(id: String, player: String, points: Int): List<PlayerRanking>
    fun standings() = LeaderboardStandings(rankings(), LeaderboardSource.Local)
}

internal interface LocalLeaderboardStore : LeaderboardStore {
    fun rounds(): List<RankedRound>
}

internal fun rankPlayers(scores: Sequence<Pair<String, Int>>): List<PlayerRanking> {
    val players = mutableMapOf<String, PlayerRanking>()
    for ((player, points) in scores) {
        val name = player.trim()
        if (name.isEmpty()) continue
        val key = name.lowercase()
        val previous = players[key] ?: PlayerRanking(name, 0)
        players[key] = previous.copy(points = previous.points + points.coerceAtLeast(0))
    }
    return players.values.sortedWith(compareByDescending<PlayerRanking> { it.points }.thenBy { it.name.lowercase() })
}
