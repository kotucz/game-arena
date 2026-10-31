package cz.kotu.gamearena

import cz.kotu.game.contacts.model.ContactsGameDetails
import cz.kotu.game.contacts.model.ContactsGamePlayer
import cz.kotu.game.contacts.model.ContactsPlayer
import cz.kotu.gamearena.model.RunningGame
import cz.kotu.gamearena.model.RunningGamePlayer

class RunningGamesResponseMapper(
    private val userDao: UserDao,
) {
    suspend fun toResponse(game: RunningGameInfo): RunningGame =
        toResponses(listOf(game)).single()

    suspend fun toResponse(metadata: GameMetadata, status: String): RunningGame =
        toResponse(metadata.toRunningGameInfo(status))

    suspend fun toContactsGameDetails(game: ContactsGame): ContactsGameDetails {
        val players = game.contactsGameFacade.gameState.value.players
        val playerUids = players.map(ContactsPlayer::uid)
        val usersByUid = usersByUid(playerUids)
        return ContactsGameDetails(
            gameId = game.metadata.id,
            players = players.map { player ->
                val user = usersByUid[player.uid]
                ContactsGamePlayer(
                    uid = player.uid,
                    username = user?.username ?: player.username,
                    pictureUrl = user?.pictureUrl,
                )
            },
        )
    }

    suspend fun toResponses(games: List<RunningGameInfo>): List<RunningGame> {
        val usersByUid = usersByUid(games.flatMap(RunningGameInfo::playerUids))
        return games.map { game ->
            RunningGame(
                id = game.id,
                type = game.type,
                players = game.playerUids.map { uid ->
                    val user = usersByUid[uid]
                    RunningGamePlayer(
                        uid = uid,
                        username = user?.username ?: uid,
                        pictureUrl = user?.pictureUrl,
                    )
                },
                createdAt = game.createdAt,
                status = game.status,
            )
        }
    }

    private suspend fun usersByUid(playerUids: List<String>): Map<String, User> {
        val distinctUids = playerUids.distinct()
        if (distinctUids.isEmpty()) return emptyMap()
        return userDao.findByFirebaseUids(distinctUids).associateBy { it.firebaseUid }
    }

    private fun GameMetadata.toRunningGameInfo(status: String) = RunningGameInfo(
        id = id,
        type = type,
        playerUids = playerUids,
        createdAt = createdAt,
        status = status,
    )
}
