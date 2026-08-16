package cz.kotu.game.contacts.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ContactsGameFacadeImpl(
    private val _gameState: MutableStateFlow<ContactsBoardState>,
) : ContactsGameFacade {
    constructor(players: List<ContactsBoardState.Player>) : this(
        MutableStateFlow(ContactsBoardState.create(players)),
    )

    internal constructor(initialState: ContactsBoardState) : this(MutableStateFlow(initialState))

    override val gameState: StateFlow<ContactsBoardState> = _gameState.asStateFlow()

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

    override fun action(
        player: ContactsBoardState.Player,
        actionType: ContactsBoardState.ActionType,
        playerContacts: Set<ContactsBoardState.Contact>,
        otherContacts: Set<ContactsBoardState.Contact>,
    ) {
        val gameState = this@ContactsGameFacadeImpl.gameState.value

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
            // TODO
            else -> {}
        }
    }
}
