package cz.kotu.gamearena

import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsGameFacade

class PersistingContactsGameFacade(
    private val delegate: ContactsGameFacade,
    private val persist: suspend () -> Unit,
) : ContactsGameFacade by delegate {
    override suspend fun action(
        player: ContactsBoardState.Player,
        actionType: ContactsBoardState.ActionType,
        playerContacts: Set<ContactsBoardState.ContactId>,
        otherContacts: Set<ContactsBoardState.ContactId>,
    ): Result<Unit> {
        val result = delegate.action(player, actionType, playerContacts, otherContacts)
        if (result.isSuccess) persist()
        return result
    }
}
