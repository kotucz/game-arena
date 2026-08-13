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

    override fun action(player: ContactsBoardState.Player, action: ContactsGameFacade.Action) {
        when (action) {
            is ContactsGameFacade.Action.Connect -> connect(player, action.playerContact, action.otherContact)
        }
    }

    private fun connect(
        player: ContactsBoardState.Player,
        playerContact: ContactsBoardState.Contact,
        otherContact: ContactsBoardState.Contact,
    ) {
        val gameState = this@ContactsGameFacadeImpl.gameState.value

        val playerOwnsContact = gameState.racks.any { rack ->
            rack.owner == player && playerContact in rack.contacts
        }
        val anotherPlayerOwnsContact = gameState.racks.any { rack ->
            rack.owner != player && otherContact in rack.contacts
        }
        val contactsAreUnsolved = playerContact !in gameState.solved && otherContact !in gameState.solved
        val contactsMatch = playerContact.number == otherContact.number

        if (!playerOwnsContact || !anotherPlayerOwnsContact || !contactsAreUnsolved) {
            return
        }

        if (!contactsMatch) {
            _gameState.value = gameState.withFaultFor(otherContact)
            return
        }

        _gameState.value = gameState.withSolvedContacts(playerContact, otherContact)
    }
}
