package fr.stageacademy.quiz.leaderboard_view

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

internal fun decodeRounds(games: JSONObject): Sequence<RankedRound> =
    games.keys().asSequence().mapNotNull { id ->
        games.optJSONObject(id)?.let { RankedRound(id, it.optString("player"), it.optInt("points")) }
    }

internal fun leaderboardStore(local: LocalLeaderboardStore): LeaderboardStore =
    if (LeaderboardUrl.isBlank()) local else SharedLeaderboardStore(local, LeaderboardUrl)

internal class SharedLeaderboardStore(
    private val local: LocalLeaderboardStore,
    baseUrl: String,
) : LeaderboardStore {
    private val endpoint = URI("${baseUrl.trimEnd('/')}/api/leaderboard/sync").toURL()
    override val shared = true

    override fun rankings() = standings().players

    override fun record(id: String, player: String, points: Int) = local.record(id, player, points)

    override fun standings(): LeaderboardStandings {
        val rounds = local.rounds()
        return try {
            var standings = emptyList<PlayerRanking>()
            if (rounds.isEmpty()) standings = sync(emptyList())
            else for (batch in rounds.chunked(100)) standings = sync(batch)
            LeaderboardStandings(standings, LeaderboardSource.Shared)
        } catch (_: IOException) {
            LeaderboardStandings(rankPlayers(rounds.asSequence().map { it.player to it.points }), LeaderboardSource.Offline)
        }
    }

    private fun sync(rounds: List<RankedRound>): List<PlayerRanking> {
        val entries = JSONArray()
        rounds.forEach { round ->
            entries.put(JSONObject().put("id", round.id).put("player", round.player.take(1024)).put("points", round.points))
        }
        val body = JSONObject().put("rounds", entries).toString().toByteArray(Charsets.UTF_8)
        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 2_000
            readTimeout = 3_000
            instanceFollowRedirects = false
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setFixedLengthStreamingMode(body.size)
        }
        try {
            connection.outputStream.use { it.write(body) }
            if (connection.responseCode != 200) throw IOException("Leaderboard HTTP ${connection.responseCode}")
            val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val players = JSONObject(response).getJSONArray("players")
            return List(players.length()) { index ->
                val player = players.getJSONObject(index)
                PlayerRanking(player.getString("name"), player.getLong("points"))
            }
        } catch (error: JSONException) {
            throw IOException("Invalid leaderboard response", error)
        } finally {
            connection.disconnect()
        }
    }
}
