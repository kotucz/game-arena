package cz.kotu.game.contacts.model

import kotlinx.coroutines.flow.StateFlow

interface ContactsGameFacade {
    val gameState: StateFlow<ContactsGameState>

    val logs: StateFlow<List<GameLogEntry>>

    suspend fun action(
        player: ContactsBoardState.Player,
        actionType: ContactsBoardState.ActionType,
        playerContacts: Set<ContactsBoardState.ContactId>,
        otherContacts: Set<ContactsBoardState.ContactId>,
    ): Result<Unit>
}
