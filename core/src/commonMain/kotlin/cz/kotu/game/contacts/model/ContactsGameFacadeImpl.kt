package cz.kotu.game.contacts.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ContactsGameFacadeImpl(
    players: List<ContactsBoardState.Player>,
) : ContactsGameFacade {
    private val _gameState = MutableStateFlow(ContactsBoardState.create(players))

    override val gameState: StateFlow<ContactsBoardState> = _gameState.asStateFlow()
}
