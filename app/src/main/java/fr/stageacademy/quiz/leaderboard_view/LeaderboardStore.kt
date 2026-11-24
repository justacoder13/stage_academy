package fr.stageacademy.quiz.leaderboard_view

import android.content.Context
import android.util.AtomicFile
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.Locale

internal data class PlayerRanking(val name: String, val points: Long)

internal class LeaderboardStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "quiz_leaderboard.json"))

    fun rankings(): List<PlayerRanking> = synchronized(lock) { rankings(readGames()) }

    fun record(id: String, player: String, points: Int): List<PlayerRanking> = synchronized(lock) {
        val games = readGames()
        if (!games.has(id)) {
            games.put(id, JSONObject().put("player", player).put("points", points))
            val output = file.startWrite()
            try {
                output.write(games.toString().toByteArray(Charsets.UTF_8))
                output.fd.sync()
            } catch (error: IOException) {
                file.failWrite(output)
                throw error
            }
            file.finishWrite(output)
            if (!readGames().has(id)) throw IOException("The game was not saved")
        }
        rankings(games)
    }

    private fun readGames(): JSONObject {
        if (!file.baseFile.exists()) return JSONObject()
        return try {
            JSONObject(file.readFully().toString(Charsets.UTF_8))
        } catch (error: JSONException) {
            throw IOException("Invalid leaderboard", error)
        }
    }

    private fun rankings(games: JSONObject): List<PlayerRanking> {
        val players = mutableMapOf<String, PlayerRanking>()
        for (id in games.keys()) {
            val game = games.optJSONObject(id) ?: continue
            val name = game.optString("player").trim()
            if (name.isEmpty()) continue
            val key = name.lowercase(Locale.ROOT)
            val previous = players[key] ?: PlayerRanking(name, 0)
            players[key] = previous.copy(points = previous.points + game.optInt("points").coerceAtLeast(0))
        }
        return players.values.sortedWith(compareByDescending<PlayerRanking> { it.points }.thenBy { it.name.lowercase(Locale.ROOT) })
    }

    private companion object {
        val lock = Any()
    }
}
