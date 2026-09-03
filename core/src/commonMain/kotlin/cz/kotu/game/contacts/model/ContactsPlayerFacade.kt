package cz.kotu.game.contacts.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ContactsPlayerFacade {
    val gameState: Flow<PlayerViewState>

    val logs: StateFlow<List<GameLogEntry>>

    suspend fun action(
        actionType: ContactsBoardState.ActionType,
        playerContacts: Set<ContactsBoardState.ContactId>,
        otherContacts: Set<ContactsBoardState.ContactId>,
    ): Result<Unit>
}
