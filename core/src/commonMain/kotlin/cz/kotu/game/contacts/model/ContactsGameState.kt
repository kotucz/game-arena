package cz.kotu.game.contacts.model

import kotlinx.serialization.Serializable

@Serializable
data class ContactsGameState(
    val board: ContactsBoardState,
)
