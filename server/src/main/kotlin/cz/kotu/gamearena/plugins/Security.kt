package cz.kotu.gamearena.plugins

import cz.kotu.gamearena.AppDatabase
import cz.kotu.gamearena.ServerConfig
import cz.kotu.gamearena.Session
import cz.kotu.gamearena.SessionTokens
import cz.kotu.gamearena.UserPrincipal
import cz.kotu.gamearena.admin.adminBasicAuthentication
import io.ktor.http.Cookie
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.session
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.maxAge
import io.ktor.server.sessions.sessions
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

fun Application.configureSecurity(database: AppDatabase, serverConfig: ServerConfig) {
    install(Sessions) {
        cookie<String>(SessionTokens.cookieName) {
            cookie.path = "/"
            cookie.httpOnly = true
            cookie.maxAge = SessionTokens.lifetimeSeconds.seconds
        }
    }

    // Authentication provider that validates session tokens stored in the database.
    install(Authentication) {
        adminBasicAuthentication(serverConfig)

        session<String>("auth-session") {
            validate { token ->
                val session = database.sessionDao().findByTokenHash(SessionTokens.hash(token))
                if (session == null) return@validate null
                if (session.expiresAt <= Instant.now().epochSecond) {
                    database.sessionDao().deleteByTokenHash(session.tokenHash)
                    return@validate null
                }
                UserPrincipal(session.username)
            }
        }
    }
}

suspend fun createSession(call: ApplicationCall, database: AppDatabase, username: String) {
    val token = SessionTokens.create()
    database.sessionDao().insert(
        Session(
            tokenHash = SessionTokens.hash(token),
            username = username,
            expiresAt = Instant.now().epochSecond + SessionTokens.lifetimeSeconds,
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
