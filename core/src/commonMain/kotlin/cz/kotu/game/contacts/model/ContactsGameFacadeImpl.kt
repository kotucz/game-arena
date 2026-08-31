package cz.kotu.game.contacts.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    internal fun connect(
        player: ContactsBoardState.Player,
        playerContact: ContactsBoardState.Contact,
        otherContact: ContactsBoardState.Contact,
    ): Result<Unit> {
        val gameState = this@ContactsGameFacadeImpl.gameState.value

        val error = gameState.isActionLegal(
            player,
            ContactsBoardState.ActionType.StandardConnect,
            setOf(playerContact),
            setOf(otherContact),
        )
        if (error != null) {
            return Result.failure(IllegalStateException(error))
        }

        if (!gameState.contactsMatch(playerContact, otherContact)) {
            if (otherContact.type == ContactsBoardState.ContactType.Red) {
                addGameLog("${player.username}: Red Connected! [Game Over]")
            }
            _gameState.value = gameState.withFaultFor(otherContact)
            return Result.success(Unit)
        }

        if (otherContact.type == ContactsBoardState.ContactType.Red) {
            addGameLog("${player.username}: Connected successfully!")
        }
        _gameState.value = gameState.withSolvedContacts(playerContact, otherContact)
        return Result.success(Unit)
    }

    private fun resolveMultiConnect(
        player: ContactsBoardState.Player,
        targetContact: ContactsBoardState.Contact,
    ): Result<Unit> {
        val gameState = this@ContactsGameFacadeImpl.gameState.value
        val resolution = gameState.resolveMultiConnect ?: return Result.failure(IllegalStateException("No multi connect to resolve"))

        if (resolution.targetPlayer != player || targetContact.id !in resolution.targetContacts) {
            return Result.failure(IllegalStateException("Invalid multi connect target"))
        }

        val originalContact = gameState.requireContact(resolution.originalContact)
        val originalPlayer = gameState.racks
            .firstOrNull { originalContact.id in it.contactIds }
            ?.owner
            ?: return Result.failure(IllegalStateException("Original player not found"))

        // Resolve with exactly the same validation and result as a normal
        // StandardConnect made by the original contact's owner.
        val result = connect(originalPlayer, originalContact, targetContact)
        if (result.isFailure) return result

        _gameState.value = this@ContactsGameFacadeImpl.gameState.value.copy(
            resolveMultiConnect = null,
        )
        return Result.success(Unit)
    }

    private fun myDoubleConnect(
        player: ContactsBoardState.Player,
        actionType: ContactsBoardState.ActionType,
        playerContacts: Set<ContactsBoardState.Contact>,
        otherContact: ContactsBoardState.Contact,
    ): Result<Unit> {
        if (!actionType.matches(playerContacts.size, 1)) {
            return Result.failure(IllegalStateException("Invalid number of selected contacts"))
        }

        // Select a matching contact before delegating so a non-matching first
        // choice cannot record a fault when the other selected contact matches.
        val playerContact = playerContacts.firstOrNull {
            gameState.value.contactsMatch(it, otherContact)
        } ?: playerContacts.first()

        return connect(player, playerContact, otherContact)
    }

    /**
     * Multi connect requires the target player to make a resolution (choose the outcome)
     */
    internal fun multiConnect(
        player: ContactsBoardState.Player,
        actionType: ContactsBoardState.ActionType,
        playerContact: ContactsBoardState.Contact,
        otherContacts: Set<ContactsBoardState.Contact>,
    ): Result<Unit> {
        val gameState = this@ContactsGameFacadeImpl.gameState.value

        val error = gameState.isActionLegal(player, actionType, setOf(playerContact), otherContacts)
        if (error != null) {
            return Result.failure(IllegalStateException(error))
        }

        val targetRack = gameState.racks.single { rack ->
            otherContacts.all { it.id in rack.contactIds }
        }

        addGameLog("${targetRack.owner.username} has to resolve multi connect")
        _gameState.value = gameState.copy(
            resolveMultiConnect = ContactsBoardState.ResolveMultiConnect(
                targetPlayer = targetRack.owner,
                originalContact = playerContact.id,
                targetContacts = otherContacts.map { it.id }.toSet(),
            ),
        )
        return Result.success(Unit)
    }

    override suspend fun action(
        player: ContactsBoardState.Player,
        actionType: ContactsBoardState.ActionType,
        playerContacts: Set<ContactsBoardState.Contact>,
        otherContacts: Set<ContactsBoardState.Contact>,
    ): Result<Unit> {
        addGameLog(
            "${player.username}: $actionType ${
                playerContacts.joinToString { "[${it.number}]" }
            } other: ${
//                otherContacts.joinToString { "[${ it.number }]" } // TODO only visible to owner. position may be
                otherContacts.joinToString { "[?]" }
            }"
        )

        val gameState = this@ContactsGameFacadeImpl.gameState.value

        if (actionType == ContactsBoardState.ActionType.ResolveMultiConnect) {
            val error = gameState.isActionLegal(player, actionType, playerContacts, otherContacts)
            if (error != null) {
                return Result.failure(IllegalStateException(error))
            }
            return resolveMultiConnect(player, playerContacts.single())
        }

        val error = gameState.isActionLegal(player, actionType, playerContacts, otherContacts)
        if (error != null) {
            return Result.failure(IllegalStateException(error))
        }

        return when (actionType) {
            ContactsBoardState.ActionType.AddHint -> {
                _gameState.value = gameState.withHintFor(playerContacts.single())
                Result.success(Unit)
            }

            ContactsBoardState.ActionType.StandardConnect -> connect(
                player,
                playerContact = playerContacts.single(),
                otherContact = otherContacts.single(),
            )

            ContactsBoardState.ActionType.DoubleConnect,
            ContactsBoardState.ActionType.TripleConnect -> multiConnect(
                player,
                actionType = actionType,
                playerContact = playerContacts.single(),
                otherContacts = otherContacts,
            )

            ContactsBoardState.ActionType.MyDoubleConnect -> myDoubleConnect(
                player,
                actionType = actionType,
                playerContacts = playerContacts,
                otherContact = otherContacts.single(),
            )

            ContactsBoardState.ActionType.SoloConnectRest -> {
                _gameState.value = gameState.withSolvedContacts(*playerContacts.toTypedArray())
                Result.success(Unit)
            }

            ContactsBoardState.ActionType.FinishReds -> {
                _gameState.value = gameState.withSolvedContacts(*playerContacts.toTypedArray())
                Result.success(Unit)
            }

            ContactsBoardState.ActionType.ResolveMultiConnect -> error("Handled above")
        }
    }

    private fun addGameLog(text: String) {
        _logs.value += GameLogEntry(
            Clock.System.now(),
            text,
        )
    }
}
