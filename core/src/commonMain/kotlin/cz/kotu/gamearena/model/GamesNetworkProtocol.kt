package cz.kotu.gamearena.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** JSON DTOs shared by clients and the game server. */
@Serializable
data class GamePlayer(val uid: String)

@Serializable
data class CreateGameRequest(
    val type: String,
    val players: List<GamePlayer>,
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

@Serializable
data class RegisterTokenRequest(
    /** Stable service discriminator: "fcm" | "webpush" */
    val service: String,
    val token: String,
)

@Serializable
data class RegisterUserRequest(
    val username: String,
    val email: String? = null,
    val idToken: String? = null,
)

@Serializable
data class AvailablePlayer(
    val uid: String,
    val username: String,
    val pictureUrl: String? = null,
)

@Serializable
data class LoggedUser(
    val uid: String,
    val username: String,
    val pictureUrl: String? = null,
)
