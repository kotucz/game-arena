package cz.kotu.gamearena

/** Session principal-like holder exposed to handlers after authentication.
 *
 * `userId` is the canonical stable identity (Firebase UID in the future). `username`
 * remains a display handle for games and UI.
 */
data class UserPrincipal(
    val userId: String,
    val username: String,
    val pictureUrl: String?,
)
