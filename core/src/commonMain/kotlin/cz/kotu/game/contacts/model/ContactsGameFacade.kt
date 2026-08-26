package cz.kotu.game.contacts.model

import kotlinx.coroutines.flow.StateFlow

interface ContactsGameFacade {
    val gameState: StateFlow<ContactsBoardState>

    val logs: StateFlow<List<GameLogEntry>>

    fun action(
        player: ContactsBoardState.Player,
        actionType: ContactsBoardState.ActionType,
        playerContacts: Set<ContactsBoardState.Contact>,
        otherContacts: Set<ContactsBoardState.Contact>,
    )
}
