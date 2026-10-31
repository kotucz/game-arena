package cz.kotu.game.contacts.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class ContactsPlayerGameAdapter(
    val player: ContactsPlayer,
    val gameFacade: ContactsGameFacade,
) : ContactsPlayerFacade {
    private val playerId = player.playerId

    override val gameState: Flow<PlayerViewState>
        get() = gameFacade.gameState.map { gameState ->
            gameState.sanitizedPlayerView(
                player = playerId,
                actions = gameState.getAvailablePlayerActions(playerId),
            )
        }
    override val logs: StateFlow<List<GameLogEntry>>
        get() = gameFacade.logs

    override suspend fun action(
        actionType: ContactsBoardState.ActionType,
        playerContacts: Set<ContactsBoardState.ContactId>,
        otherContacts: Set<ContactsBoardState.ContactId>,
    ): Result<Unit> {
        return gameFacade.action(
            player = playerId,
            actionType = actionType,
            playerContacts = playerContacts,
            otherContacts = otherContacts,
        )
    }

}