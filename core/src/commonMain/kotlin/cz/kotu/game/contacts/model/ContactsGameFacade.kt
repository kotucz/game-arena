package cz.kotu.game.contacts.model

import kotlinx.coroutines.flow.StateFlow

interface ContactsGameFacade {
    val gameState: StateFlow<ContactsBoardState>

    fun action(player: ContactsBoardState.Player, action: Action)

    sealed class Action {
        data class Connect(
            val playerContact: ContactsBoardState.Contact,
            val otherContact: ContactsBoardState.Contact
        ) : Action()
    }
}
