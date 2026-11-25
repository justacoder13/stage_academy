package fr.stageacademy.quiz.leaderboard_view

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

internal fun decodeRounds(games: JSONObject): Sequence<RankedRound> =
    games.keys().asSequence().mapNotNull { id ->
        val round = games.optJSONObject(id) ?: return@mapNotNull null
        RankedRound(id, round.optString("player"), round.optInt("points"), round.optString("accountId").ifBlank { null })
    }

internal fun leaderboardStore(local: LocalLeaderboardStore, fallbackUrl: String = ""): LeaderboardStore {
    val url = LeaderboardUrl.ifBlank { fallbackUrl }
    return if (url.isBlank()) local else SharedLeaderboardStore(local, url)
}

internal class SharedLeaderboardStore(
    private val local: LocalLeaderboardStore,
    private val baseUrl: String,
) : LeaderboardStore {
    private data class Account(val id: String, val name: String, val token: String, val expiresAt: Long)
    @Volatile private var session: Account? = null
    private val account get() = session?.takeIf { System.nanoTime() < it.expiresAt }
    override val shared = true
    override val googleAvailable = URI(baseUrl).let { it.scheme == "https" || it.host in setOf("127.0.0.1", "localhost") }
    override val accountName get() = account?.name

    override fun signOut() { session = null }

    override fun startGoogleLogin(): GoogleLogin = parse {
        val result = JSONObject(request("/auth/google/start", JSONObject(), googleStart = true))
        val url = result.getString("authorization_url")
        val uri = URI(url)
        if (uri.scheme != "https" || uri.host != "accounts.google.com") throw IOException("Invalid Google URL")
        GoogleLogin(result.getString("id"), result.getString("secret"), url)
    }

    override fun pollGoogleLogin(login: GoogleLogin): GoogleLoginStatus = parse {
        val body = JSONObject().put("id", login.id).put("secret", login.secret)
        val result = JSONObject(request("/auth/google/poll", body))
        when (result.getString("status")) {
            "pending" -> GoogleLoginStatus.Pending
            "expired" -> GoogleLoginStatus.Expired
            "conflict" -> GoogleLoginStatus.Conflict
            "complete" -> {
                session = Account(
                    result.getString("id"), result.getString("username"), result.getString("access_token"),
                    System.nanoTime() + result.getLong("expires_in") * 1_000_000_000L,
                )
                GoogleLoginStatus.Connected
            }
            else -> GoogleLoginStatus.Failed
        }
    }

    override fun rankings() = standings().players
    override fun localRankings() = local.rankings()

    override fun record(id: String, player: String, points: Int): List<PlayerRanking> {
        val current = account
        return local.recordOwned(id, current?.name ?: player, points, current?.id)
    }

    override fun standings(): LeaderboardStandings {
        val current = account
        return try {
            val rounds = if (current == null) emptyList() else local.rounds().filter { it.accountId == current.id }
            var players = emptyList<PlayerRanking>()
            if (rounds.isEmpty()) {
                players = readPlayers(request("/leaderboard"))
            } else if (current != null) {
                for (batch in rounds.chunked(100)) {
                    val body = JSONArray()
                    batch.forEach { round -> body.put(JSONObject().put("id", round.id).put("points", round.points)) }
                    players = readPlayers(request("/leaderboard/rounds", JSONObject().put("rounds", body), current.token))
                }
            }
            LeaderboardStandings(players, LeaderboardSource.Shared)
        } catch (_: IOException) {
            LeaderboardStandings(local.rankings(), LeaderboardSource.Offline)
        }
    }

    private fun readPlayers(response: String): List<PlayerRanking> = parse {
        val players = JSONArray(response)
        List(players.length()) { index ->
            val player = players.getJSONObject(index)
            PlayerRanking(player.getString("name"), player.getLong("points"))
        }
    }

    private inline fun <T> parse(read: () -> T): T = try {
        read()
    } catch (error: JSONException) {
        throw IOException("Invalid server response", error)
    } catch (error: java.net.URISyntaxException) {
        throw IOException("Invalid server URL", error)
    }

    private fun request(
        path: String, body: JSONObject? = null, token: String? = null, googleStart: Boolean = false,
    ): String {
        val connection = (URI("$baseUrl$path").toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = if (body == null) "GET" else "POST"
            connectTimeout = 4_000
            readTimeout = 5_000
            instanceFollowRedirects = false
            token?.let { setRequestProperty("Authorization", "Bearer $it") }
        }
        try {
            if (body != null) {
                val bytes = body.toString().toByteArray(Charsets.UTF_8)
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use { it.write(bytes) }
            }
            val status = connection.responseCode
            if (status == 503 && googleStart) throw GoogleConfigurationException()
            if (status == 401 && token != null && session?.token == token) signOut()
            if (status != 200) throw IOException("Server HTTP $status")
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
