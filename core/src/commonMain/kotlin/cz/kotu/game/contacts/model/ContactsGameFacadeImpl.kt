package cz.kotu.game.contacts.model

import cz.kotu.game.contacts.model.ContactsGameState.GamePhase.GameOver
import cz.kotu.game.contacts.model.ContactsGameState.GamePhase.ResolveMultiConnect
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

    private fun ContactsGameState.applyAction(
        player: ContactsBoardState.Player,
        actionType: ContactsBoardState.ActionType,
        playerContacts: Set<ContactsBoardState.ContactId>,
        otherContacts: Set<ContactsBoardState.ContactId>,
        addRichGameLog: (LogBuilder.() -> Unit) -> Unit,
    ): ContactsGameState {
        val result = board.applyActionIds(player, actionType, playerContacts, otherContacts)
        val nextState = copy(
            board = result.state,
            gamePhase = nextGamePhase(actionType, result.state),
        )
        addRichGameLog(result.logBuilder)
        return nextState
    }

    private fun ContactsGameState.nextGamePhase(
        actionType: ContactsBoardState.ActionType,
        newBoard: ContactsBoardState,
    ): ContactsGameState.GamePhase {
        return when (gamePhase) {
            is StandardTurn -> {
                when (actionType) {
                    ContactsBoardState.ActionType.StandardConnect,
                    ContactsBoardState.ActionType.AddHint,
                    ContactsBoardState.ActionType.MyDoubleConnect,
                    ContactsBoardState.ActionType.SoloConnectRest,
                    ContactsBoardState.ActionType.FinishReds,
                        -> {
                        StandardTurn(
                            nextPlayer(gamePhase.activePlayer),
                        )
                    }

                    ContactsBoardState.ActionType.DoubleConnect,
                    ContactsBoardState.ActionType.TripleConnect,
                        -> {
                        val resolveMultiConnect = newBoard.resolveMultiConnect!!
                        ResolveMultiConnect(
                            restorePlayer = gamePhase.activePlayer,
                            resolveMultiConnect = resolveMultiConnect,
                        )
                    }

                    ContactsBoardState.ActionType.ResolveMultiConnect -> throw InvalidActionException("Not in ResolveMultiConnect phase")
                }
            }

            is ResolveMultiConnect -> {
                when (actionType) {
                    // only legal from ResolveMultiConnect phase
                    ContactsBoardState.ActionType.ResolveMultiConnect -> {
                        StandardTurn(
                            nextPlayer(gamePhase.restorePlayer),
                        )
                    }

                    else -> throw InvalidActionException("Only ResolveMultiConnect")
                }
            }

            is GameOver -> {
                throw InvalidActionException("Game is over - no actions")
            }
        }
    }

    // todo only player with contacts
    private fun ContactsGameState.nextPlayer(afterPlayer: ContactsBoardState.Player): ContactsBoardState.Player =
        players[(players.indexOf(afterPlayer) + 1) % players.size]

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
