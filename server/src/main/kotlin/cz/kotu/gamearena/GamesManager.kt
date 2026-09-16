package cz.kotu.gamearena

import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsGameFacade
import cz.kotu.game.contacts.model.ContactsGameFacadeImpl
import cz.kotu.game.contacts.model.ContactsGameState
import cz.kotu.game.contacts.model.ContactsPlayerGameAdapter
import cz.kotu.game.contacts.model.GameLogEntry
import cz.kotu.gamearena.model.RunningGame
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
    private val notificationService: PushNotificationService = NoopPushNotificationService(),
) {
    private val notificationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val games = ConcurrentHashMap<String, ManagedGame>()
    private val gamesUpdated = MutableStateFlow(0)
    private val json = Json { ignoreUnknownKeys = true }
    private val gamesMutex = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    val runningGames: Flow<List<RunningGame>> = gamesUpdated.flatMapLatest {
        val gameFlows = games.values.map { game ->
            game.statusText.map { status ->
                game.metadata.toRunningGame(status = status)
            }
        }
        if (gameFlows.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(gameFlows) { it.toList() }
        }
    }

    suspend fun createContactsGame(
        players: List<String>,
        config: ContactsBoardState.ContactsGameConfig,
    ): ContactsGame = gamesMutex.withLock {
        val game = ContactsGame(
            metadata = newMetadata(players),
            contactsGameFacade = ContactsGameFacadeImpl(
                players.map(ContactsBoardState::Player),
                config,
            ),
        )
        games[game.metadata.id] = game
        gamesUpdated.value++
        persist(game)
        observeGameNotifications(game)
        game
    }

    suspend fun register(game: ManagedGame): ManagedGame = gamesMutex.withLock {
        check(games.putIfAbsent(game.metadata.id, game) == null) {
            "Game ID is already registered: ${game.metadata.id}"
        }
        gamesUpdated.value++
        persist(game)
        if (game is ContactsGame) observeGameNotifications(game)
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
                val state = game.contactsGameFacade.gameState.value
                val logs = game.contactsGameFacade.logs.value
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
                try {
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
                                contactsGameFacade = ContactsGameFacadeImpl(
                                    initialState = json.decodeFromString<ContactsGameState>(record.stateJson),
                                    initialLogs = json.decodeFromString<List<GameLogEntry>>(record.logsJson),
                                ),
                            )
                            games[metadata.id] = game
                            observeGameNotifications(game)
                        }
                    }
                } catch (e: Exception) {
                    Napier.e("Failed to load game: $record", e)
                }
            }
            if (games.isNotEmpty()) {
                gamesUpdated.value++
            }
        }
    }

    private fun observeGameNotifications(game: ContactsGame) {
        notificationScope.launch {
            var lastState: ContactsGameState.GamePhase? = null
            game.contactsGameFacade.gameState.collect { state ->
                val phase = state.gamePhase
                if (lastState == null) {
                    lastState = phase
                    return@collect
                }
                if (phase == lastState) return@collect

                val recipients = notificationRecipients(game.metadata, phase)
                if (recipients.isEmpty()) {
                    lastState = phase
                    return@collect
                }

                val (title, body) = notificationText(game.metadata, phase)
                runCatching {
                    notificationService.sendToUsers(
                        usernames = recipients,
                        title = title,
                        body = body,
                        data = mapOf(
                            "gameId" to game.metadata.id,
                            "gameType" to game.metadata.type,
                            "gamePhase" to phase.javaClass.simpleName,
                        ),
                    )
                }.onFailure { error ->
                    Napier.e(error) { "Failed to send game notification for ${game.metadata.id}" }
                }
                lastState = phase
            }
        }
    }

    private fun notificationRecipients(
        metadata: GameMetadata,
        phase: ContactsGameState.GamePhase,
    ): List<String> = when (phase) {
        is ContactsGameState.GamePhase.GameOver -> metadata.players
        is ContactsGameState.GamePhase.ResolveMultiConnect -> listOf(phase.resolveMultiConnect.targetPlayer.username)
        is ContactsGameState.GamePhase.StandardTurn -> listOf(phase.activePlayer.username)
    }

    private fun notificationText(
        metadata: GameMetadata,
        phase: ContactsGameState.GamePhase,
    ): Pair<String, String> = when (phase) {
        is ContactsGameState.GamePhase.GameOver -> "Game over" to phase.message
        is ContactsGameState.GamePhase.ResolveMultiConnect -> "Resolve multi-connect" to "${phase.resolveMultiConnect.targetPlayer.username}, resolve the multi-connect in ${metadata.type}"
        is ContactsGameState.GamePhase.StandardTurn -> "Your turn" to "It is your turn in ${metadata.type} game"
    }

    private fun GameMetadata.toRunningGame(status: String = "") = RunningGame(
        id = id,
        type = type,
        players = players,
        createdAt = createdAt,
        status = status,
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
    val statusText: Flow<String>
}

data class ContactsGame(
    override val metadata: GameMetadata,
    val contactsGameFacade: ContactsGameFacade,
) : ManagedGame {

    override val statusText: Flow<String> =
        contactsGameFacade.gameState.map { state ->
            when (val gamePhase = state.gamePhase) {
                is ContactsGameState.GamePhase.GameOver -> gamePhase.message
                is ContactsGameState.GamePhase.ResolveMultiConnect -> gamePhase.resolveMultiConnect.targetPlayer.username
                is ContactsGameState.GamePhase.StandardTurn -> gamePhase.activePlayer.username
            }
        }

    fun forUser(username: String): ServerContactsGameFacade {
        return ServerContactsGameFacade(
            ContactsPlayerGameAdapter(
                player = ContactsBoardState.Player(username),
                gameFacade = contactsGameFacade,
            ),
        )
    }
}
