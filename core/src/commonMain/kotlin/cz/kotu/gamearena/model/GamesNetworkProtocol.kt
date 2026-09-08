package cz.kotu.gamearena.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** JSON DTOs shared by clients and the game server. */
@Serializable
data class CreateGameRequest(
    val type: String,
    val players: List<String>,
    val config: String,
)

@Serializable
data class RunningGame(
    val id: String,
    val type: String,
    val players: List<String>,
    val createdAt: Instant,
    val status: String,
)
