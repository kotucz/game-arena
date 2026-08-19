package cz.kotu.game.contacts.model

import kotlinx.serialization.Serializable

@Serializable
data class GameLogEntry (
    val timestamp: Long,
    val text: String,
)
