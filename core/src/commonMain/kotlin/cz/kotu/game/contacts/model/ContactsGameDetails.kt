package cz.kotu.game.contacts.model

import kotlinx.serialization.Serializable

@Serializable
data class ContactsGameDetails(
    val gameId: String,
    val players: List<ContactsGamePlayer>,
) {
    fun player(uid: String): ContactsGamePlayer? = players.firstOrNull { it.uid == uid }
}

@Serializable
data class ContactsGamePlayer(
    val uid: String,
    val username: String,
    val pictureUrl: String? = null,
)
