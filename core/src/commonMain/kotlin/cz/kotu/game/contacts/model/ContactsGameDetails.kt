package cz.kotu.game.contacts.model

import kotlinx.serialization.Serializable

@Serializable
data class ContactsGameDetails(
    val gameId: String,
    val players: List<ContactsGamePlayer>,
)

@Serializable
data class ContactsGamePlayer(
    val uid: String? = null,
    val username: String,
    val pictureUrl: String? = null,
)
