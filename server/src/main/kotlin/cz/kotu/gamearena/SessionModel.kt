package cz.kotu.gamearena

/** Session principal-like holder exposed to handlers after authentication.
 *
 * `userId` is the canonical stable identity (Firebase UID in the future). `username`
 * remains a display handle for games and UI. The secondary constructor preserves the
 * older username-only call sites while allowing new code to use a richer principal.
 */
data class UserPrincipal(val userId: String, val username: String) {
    constructor(username: String) : this(username, username)
}
