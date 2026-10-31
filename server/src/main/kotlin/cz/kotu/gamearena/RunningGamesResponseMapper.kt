package cz.kotu.gamearena

import cz.kotu.game.contacts.model.ContactsGameDetails
import cz.kotu.game.contacts.model.ContactsGamePlayer
import cz.kotu.gamearena.model.RunningGame
import cz.kotu.gamearena.model.RunningGamePlayer

class RunningGamesResponseMapper(
    private val userDao: UserDao,
) {
    suspend fun toResponse(game: RunningGameInfo): RunningGame =
        toResponses(listOf(game)).single()

    suspend fun toResponse(metadata: GameMetadata, status: String): RunningGame =
        toResponse(metadata.toRunningGameInfo(status))

    suspend fun toContactsGameDetails(metadata: GameMetadata): ContactsGameDetails =
        ContactsGameDetails(
            gameId = metadata.id,
            players = resolvePlayers(metadata.players),
        )

    suspend fun toResponses(games: List<RunningGameInfo>): List<RunningGame> {
        val usersByUsername = usersByUsername(games.flatMap(RunningGameInfo::playerUsernames))
        return games.map { game ->
            RunningGame(
                id = game.id,
                type = game.type,
                players = game.playerUsernames.map { username ->
                    val user = usersByUsername[username.lowercase()]
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

    private suspend fun resolvePlayers(usernames: List<String>): List<ContactsGamePlayer> {
        val usersByUsername = usersByUsername(usernames)
        return usernames.map { username ->
            val user = usersByUsername[username.lowercase()]
            ContactsGamePlayer(
                uid = user?.firebaseUid,
                username = username,
                pictureUrl = user?.pictureUrl,
            )
        }
    }

    private suspend fun usersByUsername(usernames: List<String>): Map<String, User> {
        val usernamesLower = usernames
            .distinct()
            .map { it.lowercase() }
        if (usernamesLower.isEmpty()) return emptyMap()
        return userDao.findByUsernamesLower(usernamesLower).associateBy { it.usernameLower }
    }

    private fun GameMetadata.toRunningGameInfo(status: String) = RunningGameInfo(
        id = id,
        type = type,
        playerUsernames = players,
        createdAt = createdAt,
        status = status,
    )
}
