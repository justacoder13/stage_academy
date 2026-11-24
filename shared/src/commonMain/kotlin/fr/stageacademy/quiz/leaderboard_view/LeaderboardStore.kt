package fr.stageacademy.quiz.leaderboard_view

internal data class PlayerRanking(val name: String, val points: Long)

internal interface LeaderboardStore {
    fun rankings(): List<PlayerRanking>
    fun record(id: String, player: String, points: Int): List<PlayerRanking>
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
