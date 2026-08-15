package cz.kotu.gamearena

import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsGameFacadeImpl
import cz.kotu.gamearena.model.RunningGame
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class GamesManager(
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Instant = Instant::now,
) {
    private val games = ConcurrentHashMap<String, ManagedGame>()

    @Synchronized
    fun createContactsGame(players: List<String> = listOf("alice", "bob")): ContactsGame {
        val game = ContactsGame(
            metadata = newMetadata(type = "contacts", players = players),
            contacts = ServerContactsGameFacade(
                ContactsGameFacadeImpl(
                    players.map(ContactsBoardState::Player),
                ),
            ),
        )
        games[game.metadata.id] = game
        return game
    }

    fun register(game: ManagedGame): ManagedGame {
        check(games.putIfAbsent(game.metadata.id, game) == null) {
            "Game ID is already registered: ${game.metadata.id}"
        }
        return game
    }

    fun game(id: String): ManagedGame? = games[id]

    fun contactsGame(id: String): ContactsGame? = games[id] as? ContactsGame

    fun runningGames(): List<RunningGame> = games.values.map { game ->
        game.metadata.toRunningGame()
    }

    private fun GameMetadata.toRunningGame() = RunningGame(
        id = id,
        type = type,
        players = players,
        createdAt = createdAt.toString(),
    )

    private fun newMetadata(type: String, players: List<String>): GameMetadata {
        var id: String
        do {
            id = idGenerator()
            require(id.isNotBlank()) { "Game ID must not be blank" }
        } while (games.containsKey(id))
        return GameMetadata(id = id, type = type, players = players, createdAt = clock())
    }
}

data class GameMetadata(
    val id: String,
    val type: String,
    val players: List<String>,
    val createdAt: Instant,
)

interface ManagedGame {
    val metadata: GameMetadata
}

data class ContactsGame(
    override val metadata: GameMetadata,
    val contacts: ServerContactsGameFacade,
) : ManagedGame
