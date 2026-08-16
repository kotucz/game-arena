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

    override fun multiConnect(
        player: ContactsBoardState.Player,
        playerContacts: Set<ContactsBoardState.Contact>,
        otherContacts: Set<ContactsBoardState.Contact>,
    ) {
        // TODO: Implement multiConnect
    }
}
