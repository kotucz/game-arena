package cz.kotu.game.contacts.model

import cz.kotu.game.contacts.model.ContactsGameState.GamePhase.StandardTurn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class ContactsGameFacadeImpl(
    private val _gameState: MutableStateFlow<ContactsGameState>,
    private val _logs: MutableStateFlow<List<GameLogEntry>> = MutableStateFlow(listOf()),
) : ContactsGameFacade {
    constructor(
        players: List<ContactsBoardState.Player>,
        config: ContactsBoardState.ContactsGameConfig,
    ) : this(
        MutableStateFlow(
            ContactsGameState(
                ContactsBoardState.create(
                    players,
                    config,
                ),
                players = players,
                gamePhase = StandardTurn(players[0]),
            ),
        ),
        MutableStateFlow(listOf()),
    )

    constructor(initialState: ContactsGameState, initialLogs: List<GameLogEntry> = emptyList()) : this(
        MutableStateFlow(initialState),
        MutableStateFlow(initialLogs),
    )

    override val gameState: StateFlow<ContactsGameState> = _gameState.asStateFlow()
    override val logs: StateFlow<List<GameLogEntry>> = _logs.asStateFlow()


    override suspend fun action(
        player: ContactsBoardState.Player,
        actionType: ContactsBoardState.ActionType,
        playerContacts: Set<ContactsBoardState.ContactId>,
        otherContacts: Set<ContactsBoardState.ContactId>,
    ): Result<Unit> {
        val currentState = this@ContactsGameFacadeImpl.gameState.value
        return try {
            val nextState = currentState.applyAction(
                player = player,
                actionType = actionType,
                playerContacts = playerContacts,
                otherContacts = otherContacts,
                addRichGameLog = ::addRichGameLog,
            )
            _gameState.value = nextState
            Result.success(Unit)
        } catch (error: InvalidActionException) {
            Result.failure(error)
        }
    }

    private fun addGameLog(text: String) {
        _logs.value += GameLogEntry(
            Clock.System.now(),
            text,
        )
    }

    private fun addRichGameLog(block: LogBuilder.() -> Unit) {
        val logTokens = LogBuilder().apply(block).build()
        _logs.value += GameLogEntry(
            Clock.System.now(),
            Json.encodeToString(logTokens),
        )
    }
}
