package cz.kotu.game.contacts.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock

class ContactsGameFacadeImpl(
    private val _gameState: MutableStateFlow<ContactsBoardState>,
) : ContactsGameFacade {
    constructor(players: List<ContactsBoardState.Player>) : this(
        MutableStateFlow(ContactsBoardState.create(players)),
    )

    internal constructor(initialState: ContactsBoardState) : this(MutableStateFlow(initialState))

    override val gameState: StateFlow<ContactsBoardState> = _gameState.asStateFlow()

    private val _logs: MutableStateFlow<List<GameLogEntry>> = MutableStateFlow(listOf())
    override val logs: StateFlow<List<GameLogEntry>> = _logs.asStateFlow()

    // TODO called via action
    override fun connect(
        player: ContactsBoardState.Player,
        playerContact: ContactsBoardState.Contact,
        otherContact: ContactsBoardState.Contact,
    ) {
        val gameState = this@ContactsGameFacadeImpl.gameState.value

        val playerOwnsContact = gameState.isOwnedBy(player, playerContact)
        val anotherPlayerOwnsContact = gameState.isOwnedByAnotherPlayer(player, otherContact)
        val contactsAreUnsolved = !gameState.isSolved(playerContact) && !gameState.isSolved(otherContact)

        if (!playerOwnsContact || !anotherPlayerOwnsContact || !contactsAreUnsolved) {
            return
        }

        if (!gameState.contactsMatch(playerContact, otherContact)) {
            _gameState.value = gameState.withFaultFor(otherContact)
            return
        }

        _gameState.value = gameState.withSolvedContacts(playerContact, otherContact)
    }

    private fun resolveMultiConnect(
        player: ContactsBoardState.Player,
        targetContact: ContactsBoardState.Contact,
    ) {
        val gameState = this@ContactsGameFacadeImpl.gameState.value
        val resolution = gameState.resolveMultiConnect ?: return

        if (resolution.targetPlayer != player || targetContact.id !in resolution.targetContacts) {
            return
        }

        val originalContact = gameState.requireContact(resolution.originalContact)
        val originalPlayer = gameState.racks
            .firstOrNull { originalContact.id in it.contactIds }
            ?.owner
            ?: return

        // Resolve with exactly the same validation and result as a normal
        // StandardConnect made by the original contact's owner.
        connect(originalPlayer, originalContact, targetContact)

        _gameState.value = this@ContactsGameFacadeImpl.gameState.value.copy(
            resolveMultiConnect = null,
        )
    }

    private fun myDoubleConnect(
        player: ContactsBoardState.Player,
        actionType: ContactsBoardState.ActionType,
        playerContacts: Set<ContactsBoardState.Contact>,
        otherContact: ContactsBoardState.Contact,
    ) {
        if (!actionType.matches(playerContacts.size, 1)) return

        // Select a matching contact before delegating so a non-matching first
        // choice cannot record a fault when the other selected contact matches.
        val playerContact = playerContacts.firstOrNull {
            gameState.value.contactsMatch(it, otherContact)
        } ?: playerContacts.first()

        connect(player, playerContact, otherContact)
    }

    /**
     * Multi connect requires the target player to make a resolution (choose the outcome)
     */
    fun multiConnect(
        player: ContactsBoardState.Player,
        actionType: ContactsBoardState.ActionType,
        playerContact: ContactsBoardState.Contact,
        otherContacts: Set<ContactsBoardState.Contact>,
    ) {
        val gameState = this@ContactsGameFacadeImpl.gameState.value

        if (actionType !in setOf(
                ContactsBoardState.ActionType.DoubleConnect,
                ContactsBoardState.ActionType.TripleConnect,
            ) ||
            !actionType.matches(1, otherContacts.size) ||
            !gameState.isOwnedBy(player, playerContact) ||
            gameState.isSolved(playerContact) ||
            otherContacts.isEmpty() ||
            otherContacts.any { gameState.isSolved(it) } ||
            otherContacts.any { !gameState.isOwnedByAnotherPlayer(player, it) }
        ) {
            return
        }

        // A multi-connect is resolved by the owner of the opposing rack. All
        // selected contacts must therefore belong to the same rack.
        val targetRack = gameState.racks.singleOrNull { rack ->
            otherContacts.all { it.id in rack.contactIds }
        } ?: return

        _gameState.value = gameState.copy(
            resolveMultiConnect = ContactsBoardState.ResolveMultiConnect(
                targetPlayer = targetRack.owner,
                originalContact = playerContact.id,
                targetContacts = otherContacts.map { it.id }.toSet(),
            ),
        )
    }

    override fun action(
        player: ContactsBoardState.Player,
        actionType: ContactsBoardState.ActionType,
        playerContacts: Set<ContactsBoardState.Contact>,
        otherContacts: Set<ContactsBoardState.Contact>,
    ) {
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
            if (!actionType.matches(playerContacts.size, otherContacts.size)) return
            resolveMultiConnect(player, playerContacts.single())
            return
        }

        // TODO should be in single rack?
        val playerOwnsContacts = playerContacts.all { gameState.isOwnedBy(player, it) }
        val anotherPlayerOwnsContacts = otherContacts.all { gameState.isOwnedByAnotherPlayer(player, it) }
        val contactsAreUnsolved =
            playerContacts.all { !gameState.isSolved(it) } &&
                    otherContacts.all { !gameState.isSolved(it) }

        if (!playerOwnsContacts || !anotherPlayerOwnsContacts || !contactsAreUnsolved) {
            return
        }

        when (actionType) {
            ContactsBoardState.ActionType.AddHint -> {
                _gameState.value = gameState.withHintFor(playerContacts.single())
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

            ContactsBoardState.ActionType.ResolveMultiConnect -> error("Handled above")
        }
    }

    private fun addGameLog(text: String) {
        _logs.value += GameLogEntry(
            Clock.System.now().toEpochMilliseconds(),
            text,
        )
    }
}
