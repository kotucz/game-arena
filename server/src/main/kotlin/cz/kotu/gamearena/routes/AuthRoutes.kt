package cz.kotu.gamearena.routes

import cz.kotu.gamearena.AppDatabase
import cz.kotu.gamearena.FirebaseTokenVerifier
import cz.kotu.gamearena.ServerConfig
import cz.kotu.gamearena.SessionTokens
import cz.kotu.gamearena.User
import cz.kotu.gamearena.UserPrincipal
import cz.kotu.gamearena.plugins.createSession
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.sessions

fun Route.authRoutes(database: AppDatabase, serverConfig: ServerConfig) {
    val firebaseTokenVerifier = FirebaseTokenVerifier(serverConfig)

    post("/api/auth/firebase") {
        val form = call.receiveParameters()
        val idToken = form["idToken"]?.trim().orEmpty()
        val username = form["username"]?.trim().orEmpty()
        val email = form["email"]?.trim().orEmpty()

        if (idToken.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, "idToken is required")
            return@post
        }

        val claims = firebaseTokenVerifier.verify(idToken)
        if (claims == null) {
            call.respond(HttpStatusCode.Unauthorized, "Invalid Firebase token")
            return@post
        }

        val firebaseUid = claims.uid
        val existingUser = database.userDao().findByFirebaseUid(firebaseUid)
        val resolvedUsername = existingUser?.username
            ?: username.ifBlank { claims.name?.trim().orEmpty().ifBlank { "user_${firebaseUid.take(12)}" } }
        val resolvedEmail = email.ifBlank { claims.email.orEmpty() }.ifBlank { existingUser?.email.orEmpty() }

        if (resolvedUsername.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, "username is required")
            return@post
        }

        val user = when {
            existingUser == null -> {
                val created = User(
                    username = resolvedUsername,
                    email = resolvedEmail,
                    firebaseUid = firebaseUid,
                )
                database.userDao().insert(created)
                created
            }

            existingUser.email != resolvedEmail -> {
                val updated = existingUser.copy(email = resolvedEmail)
                database.userDao().insert(updated)
                updated
            }

            else -> existingUser
        }

        createSession(call, database, user.username, userId = firebaseUid)
        call.respondText("Firebase login successful")
    }

    post("/api/logout") {
        val token = call.request.cookies[SessionTokens.cookieName]
        if (token != null) {
            database.sessionDao().deleteByTokenHash(SessionTokens.hash(token))
        }
        call.sessions.clear(SessionTokens.cookieName)
        call.respondText("Logout successful")
    }

    authenticate("auth-session") {
        get("/api/me") {
            val principal = call.principal<UserPrincipal>()!!
            call.respondText(principal.username)
        }
    }
}
