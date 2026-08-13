package cz.kotu.game.contacts.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ContactsGameFacadeImpl(
    players: List<ContactsBoardState.Player>,
) : ContactsGameFacade {
    private val _gameState = MutableStateFlow(ContactsBoardState.create(players))

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

        _gameState.value = gameState.withSolvedContacts(playerContact, otherContact)
    }
}
