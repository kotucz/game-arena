package cz.kotu.gamearena

import cz.kotu.gamearena.model.RunningGame
import cz.kotu.gamearena.model.RunningGamePlayer
import java.util.Locale

class RunningGamesResponseMapper(
    private val userDao: UserDao,
) {
    suspend fun toResponse(game: RunningGameInfo): RunningGame =
        toResponses(listOf(game)).single()

    suspend fun toResponse(metadata: GameMetadata, status: String): RunningGame =
        toResponse(metadata.toRunningGameInfo(status))

    suspend fun toResponses(games: List<RunningGameInfo>): List<RunningGame> {
        val usernamesLower = games
            .flatMap(RunningGameInfo::playerUsernames)
            .distinct()
            .map { it.lowercase(Locale.ROOT) }
        val usersByUsername = if (usernamesLower.isEmpty()) {
            emptyMap()
        } else {
            userDao.findByUsernamesLower(usernamesLower).associateBy { it.usernameLower }
        }
        return games.map { game ->
            RunningGame(
                id = game.id,
                type = game.type,
                players = game.playerUsernames.map { username ->
                    val user = usersByUsername[username.lowercase(Locale.ROOT)]
                    RunningGamePlayer(
                        uid = user?.firebaseUid,
                        username = username,
                        pictureUrl = user?.pictureUrl,
                    )
                },
                createdAt = game.createdAt,
                status = game.status,
            )
        }
    }

    private fun GameMetadata.toRunningGameInfo(status: String) = RunningGameInfo(
        id = id,
        type = type,
        playerUsernames = players,
        createdAt = createdAt,
        status = status,
    )
}
