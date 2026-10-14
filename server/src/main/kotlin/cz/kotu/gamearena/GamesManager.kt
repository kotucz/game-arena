package cz.kotu.gamearena

import cz.kotu.game.contacts.ai.AiPlayerRunner
import cz.kotu.game.contacts.ai.ContactsAiStart
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsGameFacade
import cz.kotu.game.contacts.model.ContactsGameFacadeImpl
import cz.kotu.game.contacts.model.ContactsGameState
import cz.kotu.game.contacts.model.ContactsPlayerFacade
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
    private val contactsAi: ContactsAiStart,
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
        val metadata = newMetadata(players)
        val impl = ContactsGameFacadeImpl(
            players.map(ContactsBoardState::Player),
            config,
        )
        val game = ContactsGame(
            metadata = metadata,
            contactsGameFacade = persistingFacade(metadata, impl),
            messageSender = { player, text -> impl.sendMessage(player, text) },
        )
        persist(metadata, game.contactsGameFacade)
        games[metadata.id] = game
        gamesUpdated.value++
        activate(game)
        game
    }

    private fun activate(game: ContactsGame) {
        observeGameNotifications(game)
        startAiPlayers(game)
    }

    suspend fun register(game: ManagedGame): ManagedGame = gamesMutex.withLock {
        require(game !is ContactsGame) {
            "Contacts games must be created or restored through GamesManager"
        }
        val id = game.metadata.id
        require(id.isNotBlank()) { "Game ID must not be blank" }
        check(games.putIfAbsent(id, game) == null) {
            "Game ID is already registered: $id"
        }
        gamesUpdated.value++
        game
    }

    fun game(id: String): ManagedGame? = games[id]

    fun contactsGame(id: String): ContactsGame? = games[id] as? ContactsGame

    fun runningGames(): List<RunningGame> = games.values.map { game ->
        game.metadata.toRunningGame()
    }

    private suspend fun persist(metadata: GameMetadata, facade: ContactsGameFacade) {
        val dao = gameDao ?: return
        val record = StoredGame(
            id = metadata.id,
            type = metadata.type,
            playersJson = json.encodeToString(metadata.players),
            createdAtMillis = metadata.createdAt.toEpochMilliseconds(),
            stateJson = json.encodeToString(facade.gameState.value),
            logsJson = json.encodeToString(facade.logs.value),
            updatedAtMillis = clock().toEpochMilliseconds(),
        )
        dao.upsert(record)
    }

    private fun persistingFacade(metadata: GameMetadata, facade: ContactsGameFacade): ContactsGameFacade {
        return PersistingContactsGameFacade(facade) {
            persist(metadata, facade)
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
                            val impl = ContactsGameFacadeImpl(
                                initialState = json.decodeFromString<ContactsGameState>(record.stateJson),
                                initialLogs = json.decodeFromString<List<GameLogEntry>>(record.logsJson),
                            )
                            val game = ContactsGame(
                                metadata = metadata,
                                contactsGameFacade = persistingFacade(metadata, impl),
                                messageSender = impl::sendMessage,
                            )
                            games[metadata.id] = game
                            activate(game)
                        }
                    }
                } catch (e: Exception) {
                    Napier.e("Failed to load game: $record", e)
                }
            }
            gamesUpdated.value++
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

    private fun startAiPlayers(game: ContactsGame) {
        Napier.i("startAiPlayers game $game")
        game.metadata.players.forEach { username ->
            if (username == "ai:gemini") {
                Napier.i("startAiPlayers $username")
                val runner = contactsAi.start(
                    coroutineScope = notificationScope,
                    playerFacade = game.aiPlayer(username),
                    sessionId = game.metadata.id,
                    messageSender = { message -> game.messageSender?.invoke(ContactsBoardState.Player(username), message) },
                )
                if (runner != null) {
                    game.aiRunners[username] = runner
                }
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
    val messageSender: ((ContactsBoardState.Player, String) -> Unit)? = null,
    val aiRunners: MutableMap<String, AiPlayerRunner> = mutableMapOf(),
) : ManagedGame {

    override val statusText: Flow<String> = contactsGameFacade.gameState.map { state ->
        when (val gamePhase = state.gamePhase) {
            is ContactsGameState.GamePhase.GameOver -> gamePhase.message
            is ContactsGameState.GamePhase.ResolveMultiConnect -> gamePhase.resolveMultiConnect.targetPlayer.username
            is ContactsGameState.GamePhase.StandardTurn -> gamePhase.activePlayer.username
        }
    }

    suspend fun commandAiPlayer(playerUsername: String, command: String): Boolean {
        val runner = aiRunners[playerUsername] ?: return false
        runner.command(command)
        return true
    }

    fun forUser(username: String): ServerContactsGameFacade {
        return ServerContactsGameFacade(
            ContactsPlayerGameAdapter(
                player = ContactsBoardState.Player(username),
                gameFacade = contactsGameFacade,
            ),
        )
    }

    fun aiPlayer(username: String): ContactsPlayerFacade {
        return ContactsPlayerGameAdapter(
            player = ContactsBoardState.Player(username),
            gameFacade = contactsGameFacade,
        )
    }
}
