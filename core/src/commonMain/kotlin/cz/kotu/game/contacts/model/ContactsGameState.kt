package cz.kotu.game.contacts.model

import kotlinx.serialization.Serializable

@Serializable
data class ContactsGameState(
    val board: ContactsBoardState,

    val players: List<ContactsBoardState.Player>,
    val activePlayer: ContactsBoardState.Player,

    ) {

    companion object {
        fun empty(): ContactsGameState = ContactsGameState(
            board = ContactsBoardState.empty(),
            players = listOf(),
            activePlayer = ContactsBoardState.Player(""),
        )
    }
}