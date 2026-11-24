package fr.stageacademy.quiz.leaderboard_view

import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

internal class DesktopLeaderboardStore(private val directory: File) : LeaderboardStore {
    private val file = File(directory, "quiz_leaderboard.json")

    override fun rankings(): List<PlayerRanking> = synchronized(lock) { rankings(readGames()) }

    override fun record(id: String, player: String, points: Int): List<PlayerRanking> = synchronized(lock) {
        val games = readGames()
        if (!games.has(id)) {
            games.put(id, JSONObject().put("player", player).put("points", points))
            Files.createDirectories(directory.toPath())
            val pending = File.createTempFile("leaderboard-", ".tmp", directory)
            try {
                pending.outputStream().use { output ->
                    output.write(games.toString().toByteArray(Charsets.UTF_8))
                    output.fd.sync()
                }
                Files.move(pending.toPath(), file.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
            } finally {
                pending.delete()
            }
        }
        rankings(games)
    }

    private fun readGames(): JSONObject = try {
        if (file.exists()) JSONObject(file.readText()) else JSONObject()
    } catch (error: JSONException) {
        throw IOException("Invalid leaderboard", error)
    }

    private fun rankings(games: JSONObject) = rankPlayers(
        games.keys().asSequence().mapNotNull { id ->
            games.optJSONObject(id)?.let { it.optString("player") to it.optInt("points") }
        },
    )

    private companion object {
        val lock = Any()
    }
}
