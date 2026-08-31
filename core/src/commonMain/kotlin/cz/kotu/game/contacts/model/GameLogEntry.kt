package cz.kotu.game.contacts.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class GameLogEntry (
    val timestamp: Instant,
    val text: String,
)
