package cz.kotu.game.contacts.model

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
                activePlayer = players[0],
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
        val gameState: ContactsGameState = this@ContactsGameFacadeImpl.gameState.value
        return when (val result = gameState.board.applyActionIds(player, actionType, playerContacts, otherContacts)) {
            is ActionExecutionResult.Failure -> Result.failure(IllegalStateException(result.message))
            is ActionExecutionResult.Success -> {
                _gameState.value = gameState.copy(
                    board = result.state,
                    activePlayer = gameState.nextActivePlayer(actionType, result.state),
                )
                addRichGameLog(result.logBuilder)
                Result.success(Unit)
            }
        }
    }

    private fun ContactsGameState.nextActivePlayer(
        actionType: ContactsBoardState.ActionType,
        newBoard: ContactsBoardState,
    ): ContactsBoardState.Player {
        return if (newBoard.resolveMultiConnect == null) {
            players[(players.indexOf(activePlayer) + 1) % players.size]
        } else {
            activePlayer
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
