package fr.stageacademy.quiz.leaderboard_view

import fr.stageacademy.quiz.collection_view.CollectionProgress
import fr.stageacademy.quiz.collection_view.CollectionUpdate
import fr.stageacademy.quiz.collection_view.CrateReward
import fr.stageacademy.quiz.collection_view.PendingCrate
import fr.stageacademy.quiz.collection_view.QuizSecret
import fr.stageacademy.quiz.collection_view.SecretFloat
import fr.stageacademy.quiz.collection_view.SecretFloatScale
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
    private class HttpError(val status: Int) : IOException("Server HTTP $status")
    @Volatile private var session: Account? = null
    private val account get() = session?.takeIf { System.nanoTime() < it.expiresAt }
    private val onRender = URI(baseUrl).host.endsWith(".onrender.com", ignoreCase = true)
    override val shared = true
    override val accountAvailable = URI(baseUrl).let { it.scheme == "https" || it.host in setOf("127.0.0.1", "localhost") }
    override val accountName get() = account?.name
    override val collectionAccountId get() = account?.id

    override fun signOut() { session = null }

    override fun authenticate(username: String, password: String, email: String?): AccountResult {
        if (!accountAvailable) return AccountResult.Unavailable
        val body = JSONObject().put("username", username).put("password", password)
        return try {
            if (email != null) {
                request("/auth/register", body.put("email", email))
                AccountResult.Created
            } else {
                parse {
                    val result = JSONObject(request("/auth/login", body))
                    session = Account(
                        result.getString("user_id"), result.getString("username"), result.getString("access_token"),
                        System.nanoTime() + result.getLong("expires_in") * 1_000_000_000L,
                    )
                }
                AccountResult.Connected
            }
        } catch (error: HttpError) {
            when (error.status) {
                401 -> AccountResult.InvalidCredentials
                409 -> AccountResult.Conflict
                422 -> AccountResult.InvalidFields
                429 -> AccountResult.RateLimited
                else -> AccountResult.Unavailable
            }
        }
    }

    override fun rankings() = standings().players
    override fun localRankings() = local.rankings()

    override fun fetchCollection(accountId: String): CollectionProgress = parse {
        readCollection(JSONObject(request("/collection", token = collectionToken(accountId))))
    }

    override fun pushCollection(accountId: String, update: CollectionUpdate): CollectionProgress = parse {
        val floats = JSONObject()
        update.delta.floats.forEach { (secret, value) -> floats.put(secret.name, value.units) }
        val body = JSONObject().put("id", update.id).put("earned", update.delta.points).put("floats", floats)
        update.initialPoints?.let { body.put("initial_points", it) }
        readCollection(JSONObject(request("/collection/sync", body, collectionToken(accountId))))
    }

    override fun purchaseCrate(accountId: String, purchase: PendingCrate): CrateReward? = parse {
        val body = JSONObject().put("id", purchase.id).put("crate", purchase.crate.name)
        try {
            val result = JSONObject(request("/collection/crates", body, collectionToken(accountId)))
            val secret = QuizSecret.entries.firstOrNull { it.name == result.optString("reward") }
                ?: throw IOException("Unknown crate reward")
            CrateReward(secret, readCollection(result))
        } catch (error: HttpError) {
            if (error.status != 409) throw error
            null
        }
    }

    private fun collectionToken(accountId: String): String =
        account?.takeIf { it.id == accountId }?.token ?: throw IOException("Account session unavailable")

    private fun readCollection(result: JSONObject): CollectionProgress {
        val floats = result.getJSONObject("floats")
        val known = buildMap {
            for (secret in QuizSecret.entries) {
                val units = floats.optLong(secret.name, -1)
                if (units in 0 until SecretFloatScale) put(secret, SecretFloat(units))
            }
        }
        return CollectionProgress(result.getInt("points"), known)
    }

    override fun record(id: String, player: String, points: Int): List<PlayerRanking> {
        val current = account
        return local.recordOwned(id, current?.name ?: player, points, current?.id)
    }

    override fun standings(): LeaderboardStandings {
        val current = account ?: return LeaderboardStandings(local.rankings(), LeaderboardSource.Local)
        return try {
            val rounds = local.rounds().filter { it.accountId == current.id }
            var players = emptyList<PlayerRanking>()
            if (rounds.isEmpty()) {
                players = readPlayers(request("/leaderboard", token = current.token))
            } else {
                for (batch in rounds.chunked(100)) {
                    val body = JSONArray()
                    batch.forEach { round -> body.put(JSONObject().put("id", round.id).put("points", round.points)) }
                    players = readPlayers(request("/leaderboard/rounds", JSONObject().put("rounds", body), current.token))
                }
            }
            LeaderboardStandings(players, LeaderboardSource.Shared)
        } catch (_: IOException) {
            val source = if (account == null) LeaderboardSource.Local else LeaderboardSource.Offline
            LeaderboardStandings(local.rankings(), source)
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
    }

    private fun request(path: String, body: JSONObject? = null, token: String? = null): String {
        val connection = (URI("$baseUrl$path").toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = if (body == null) "GET" else "POST"
            connectTimeout = 4_000
            readTimeout = if (onRender) 90_000 else 5_000
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
            if (status == 401 && token != null && session?.token == token) signOut()
            if (status !in 200..299) throw HttpError(status)
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
