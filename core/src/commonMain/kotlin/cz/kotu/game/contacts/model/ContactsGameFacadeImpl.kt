package cz.kotu.game.contacts.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class ContactsGameFacadeImpl(
    private val _gameState: MutableStateFlow<ContactsBoardState>,
    private val _logs: MutableStateFlow<List<GameLogEntry>> = MutableStateFlow(listOf()),
) : ContactsGameFacade {
    constructor(
        players: List<ContactsBoardState.Player>,
        config: ContactsBoardState.ContactsGameConfig,
    ) : this(
        MutableStateFlow(
            ContactsBoardState.create(
                players,
                config,
            )
        ),
        MutableStateFlow(listOf()),
    )

    constructor(initialState: ContactsBoardState, initialLogs: List<GameLogEntry> = emptyList()) : this(
        MutableStateFlow(initialState),
        MutableStateFlow(initialLogs),
    )

    override val gameState: StateFlow<ContactsBoardState> = _gameState.asStateFlow()
    override val logs: StateFlow<List<GameLogEntry>> = _logs.asStateFlow()


    override suspend fun action(
        player: ContactsBoardState.Player,
        actionType: ContactsBoardState.ActionType,
        playerContacts: Set<ContactsBoardState.Contact>,
        otherContacts: Set<ContactsBoardState.Contact>,
    ): Result<Unit> {
        val gameState = this@ContactsGameFacadeImpl.gameState.value
        val result = gameState.applyAction(player, actionType, playerContacts, otherContacts)
        return when (result) {
            is ActionExecutionResult.Failure -> Result.failure(IllegalStateException(result.message))
            is ActionExecutionResult.Success -> {
                _gameState.value = result.state
                addRichGameLog(result.logBuilder)
                Result.success(Unit)
            }
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
