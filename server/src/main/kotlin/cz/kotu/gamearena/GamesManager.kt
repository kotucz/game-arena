package cz.kotu.gamearena

import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsGameFacadeImpl
import cz.kotu.game.contacts.model.GameLogEntry
import cz.kotu.gamearena.model.RunningGame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.Instant

class GamesManager(
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Instant = Clock.System::now,
    private val gameDao: GameDao? = null,
) {
    private val games = ConcurrentHashMap<String, ManagedGame>()
    private val gamesUpdated = MutableStateFlow(0)
    private val json = Json { ignoreUnknownKeys = true }
    private val gamesMutex = Mutex()
    val runningGames = gamesUpdated.map { runningGames() }

    suspend fun createContactsGame(
        players: List<String>,
        config: ContactsBoardState.ContactsGameConfig,
    ): ContactsGame = gamesMutex.withLock {
        val game = ContactsGame(
            metadata = newMetadata(players),
            contacts = ServerContactsGameFacade(
                ContactsGameFacadeImpl(
                    players.map(ContactsBoardState::Player),
                    config,
                ),
            ),
        )
        games[game.metadata.id] = game
        gamesUpdated.value++
        persist(game)
        game
    }

    suspend fun register(game: ManagedGame): ManagedGame = gamesMutex.withLock {
        check(games.putIfAbsent(game.metadata.id, game) == null) {
            "Game ID is already registered: ${game.metadata.id}"
        }
        gamesUpdated.value++
        persist(game)
        game
    }

    fun game(id: String): ManagedGame? = games[id]

    fun contactsGame(id: String): ContactsGame? = games[id] as? ContactsGame

    fun runningGames(): List<RunningGame> = games.values.map { game ->
        game.metadata.toRunningGame()
    }

    suspend fun persist(game: ManagedGame) {
        val dao = gameDao ?: return
        when (game) {
            is ContactsGame -> {
                val state = game.contacts.gameState.value
                val logs = game.contacts.logs.value
                val record = StoredGame(
                    id = game.metadata.id,
                    type = game.metadata.type,
                    playersJson = json.encodeToString(game.metadata.players),
                    createdAtMillis = game.metadata.createdAt.toEpochMilliseconds(),
                    stateJson = json.encodeToString(state),
                    logsJson = json.encodeToString(logs),
                    updatedAtMillis = clock().toEpochMilliseconds(),
                )
                dao.upsert(record)
            }
        }
    }

    suspend fun restorePersistedGames() {
        val dao = gameDao ?: return
        val records = dao.list()
        gamesMutex.withLock {
            records.forEach { record ->
                when (record.type) {
                    "contacts" -> {
                        val metadata = GameMetadata(
                            id = record.id,
                            type = record.type,
                            players = json.decodeFromString<List<String>>(record.playersJson),
                            createdAt = Instant.fromEpochMilliseconds(record.createdAtMillis),
                        )
                        val game = ContactsGame(
                            metadata = metadata,
                            contacts = ServerContactsGameFacade(
                                ContactsGameFacadeImpl(
                                    initialState = json.decodeFromString<ContactsBoardState>(record.stateJson),
                                    initialLogs = json.decodeFromString<List<GameLogEntry>>(record.logsJson),
                                ),
                            ),
                        )
                        games[metadata.id] = game
                    }
                }
            }
            if (games.isNotEmpty()) {
                gamesUpdated.value++
            }
        }
    }

    private fun GameMetadata.toRunningGame() = RunningGame(
        id = id,
        type = type,
        players = players,
        createdAt = createdAt,
    )

    private fun newMetadata(players: List<String>): GameMetadata {
        var id: String
        do {
            id = idGenerator()
            require(id.isNotBlank()) { "Game ID must not be blank" }
        } while (games.containsKey(id))
        return GameMetadata(id = id, type = "contacts", players = players, createdAt = clock())
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
