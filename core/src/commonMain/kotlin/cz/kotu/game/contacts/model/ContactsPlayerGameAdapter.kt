package cz.kotu.game.contacts.model

import kotlinx.coroutines.flow.StateFlow

class ContactsPlayerGameAdapter(
    val player: ContactsBoardState.Player,
    val gameFacade: ContactsGameFacade,
) : ContactsPlayerFacade {
    override val gameState: StateFlow<ContactsGameState>
        get() = gameFacade.gameState
    override val logs: StateFlow<List<GameLogEntry>>
        get() = gameFacade.logs

    override suspend fun action(
        actionType: ContactsBoardState.ActionType,
        playerContacts: Set<ContactsBoardState.ContactId>,
        otherContacts: Set<ContactsBoardState.ContactId>,
    ): Result<Unit> {
        return gameFacade.action(
            player = player,
            actionType = actionType,
            playerContacts = playerContacts,
            otherContacts = otherContacts,
        )
    }

}