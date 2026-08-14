package cz.kotu.game.contacts.model

import kotlinx.coroutines.flow.StateFlow

interface ContactsGameFacade {
    val gameState: StateFlow<ContactsBoardState>

    fun connect(
        player: ContactsBoardState.Player,
        playerContact: ContactsBoardState.Contact,
        otherContact: ContactsBoardState.Contact,
    )

    // Transport DTOs. A network adapter maps these to the typed facade methods.
    sealed class Action {
        data class Connect(
            val playerContact: ContactsBoardState.ContactId,
            val otherContact: ContactsBoardState.ContactId,
        ) : Action()
    }
}
