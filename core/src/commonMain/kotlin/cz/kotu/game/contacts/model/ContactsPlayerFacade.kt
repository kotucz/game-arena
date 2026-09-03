package cz.kotu.game.contacts.model

import kotlinx.coroutines.flow.StateFlow

interface ContactsPlayerFacade {
    val gameState: StateFlow<ContactsGameState>

    val logs: StateFlow<List<GameLogEntry>>

    suspend fun action(
        actionType: ContactsBoardState.ActionType,
        playerContacts: Set<ContactsBoardState.Contact>,
        otherContacts: Set<ContactsBoardState.Contact>,
    ): Result<Unit>
}
