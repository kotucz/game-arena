package cz.kotu.game.contacts.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class ContactsPlayerGameAdapter(
    val player: ContactsBoardState.Player,
    val gameFacade: ContactsGameFacade,
) : ContactsPlayerFacade {
    override val gameState: Flow<PlayerViewState>
        get() = gameFacade.gameState.map { it.sanitizedPlayerView(player) }
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