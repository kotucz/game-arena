package cz.kotu.gamearena.model

import cz.kotu.game.contacts.model.ContactsGameOverReason
import cz.kotu.game.contacts.model.ContactsBoardState
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
    val players: List<RunningGamePlayer>,
    val createdAt: Instant,
    val status: RunningGameStatus,
)

@Serializable
sealed class RunningGameStatus {
    @Serializable
    data class ContactsPlayerTurn(val player: ContactsBoardState.Player) : RunningGameStatus()

    @Serializable
    data class ContactsMultiConnectResolution(val player: ContactsBoardState.Player) : RunningGameStatus()

    @Serializable
    data class ContactsGameOver(val reason: ContactsGameOverReason) : RunningGameStatus()

    @Serializable
    data object Unknown : RunningGameStatus()
}

@Serializable
data class RunningGamePlayer(
    val uid: String? = null,
    val username: String,
    val pictureUrl: String? = null,
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
