package cz.kotu.gamearena

/** Session principal-like holder exposed to handlers after Firebase Authentication. */
data class UserPrincipal(
    val firebaseUid: String,
    val username: String,
    val pictureUrl: String?,
)
