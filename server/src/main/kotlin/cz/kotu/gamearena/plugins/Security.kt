package cz.kotu.gamearena.plugins

import cz.kotu.gamearena.AppDatabase
import cz.kotu.gamearena.ServerConfig
import cz.kotu.gamearena.Session
import cz.kotu.gamearena.SessionTokens
import cz.kotu.gamearena.TokenVerifier
import cz.kotu.gamearena.UserPrincipal
import cz.kotu.gamearena.admin.adminBasicAuthentication
import io.ktor.http.Cookie
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.bearer
import io.ktor.server.sessions.sessions
import java.time.Instant

fun Application.configureSecurity(
    database: AppDatabase,
    serverConfig: ServerConfig,
    tokenVerifier: TokenVerifier,
) {
    install(Authentication) {
        adminBasicAuthentication(serverConfig)

        bearer("auth-firebase") {
            authenticate { credential ->
                val claims = tokenVerifier.verify(credential.token) ?: return@authenticate null
                val user = database.userDao().findByFirebaseUid(claims.uid) ?: return@authenticate null
                UserPrincipal(
                    userId = claims.uid,
                    username = user.username,
                )
            }
        }
    }
}

suspend fun createSession(
    call: ApplicationCall,
    database: AppDatabase,
    username: String,
    userId: String = username,
) {
    val token = SessionTokens.create()
    database.sessionDao().insert(
        Session(
            tokenHash = SessionTokens.hash(token),
            username = username,
            expiresAt = Instant.now().epochSecond + SessionTokens.lifetimeSeconds,
            userId = userId.ifBlank { username },
        )
    )
    // Use Ktor Sessions API to set the cookie-backed session value so Authentication/session can read it.
    call.sessions.set(SessionTokens.cookieName, token)
    // Also append a Set-Cookie header for clients that rely on raw cookies (tests and non-ktor clients)
    call.response.cookies.append(
        Cookie(
            name = SessionTokens.cookieName,
            value = token,
            maxAge = SessionTokens.lifetimeSeconds.toInt(),
            httpOnly = true,
            path = "/",
        ),
    )
}
