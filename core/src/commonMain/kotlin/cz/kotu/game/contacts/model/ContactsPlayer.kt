package cz.kotu.game.contacts.model

import kotlinx.serialization.Serializable

@Serializable
data class ContactsPlayer(
    val username: String,
    val uid: String,
) {
    val playerId: ContactsBoardState.Player
        get() = ContactsBoardState.Player(username)
}
