package cz.kotu.gamearena.model

import kotlinx.serialization.Serializable

/** JSON DTOs shared by clients and the game server. */
@Serializable
data class RunningGame(
    val id: String,
    val type: String,
    val players: List<String>,
    val createdAt: String,
)
