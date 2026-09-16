package cz.kotu.gamearena.routes

import cz.kotu.gamearena.AppDatabase
import cz.kotu.gamearena.FirebaseTokenVerifier
import cz.kotu.gamearena.PasswordHasher
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
        val resolvedEmail = existingUser?.email ?: email.ifBlank { claims.email.orEmpty() }

        if (resolvedUsername.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, "username is required")
            return@post
        }

        val user = when {
            existingUser == null -> {
                val created = User(
                    username = resolvedUsername,
                    passwordHash = "",
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

    post("/api/register") {
        val form = call.receiveParameters()
        val username = form["username"]?.trim().orEmpty()
        val email = form["email"]?.trim().orEmpty()
        val password = form["password"].orEmpty()
        val validationError = validateRegistration(username, email, password)
        if (validationError != null) {
            call.respond(HttpStatusCode.BadRequest, validationError)
        } else if (database.userDao().findByUsername(username) != null) {
            call.respond(HttpStatusCode.Conflict, "Username is already registered")
        } else {
            database.userDao().insert(User(username, PasswordHasher.hash(password), email))
            createSession(call, database, username)
            call.respond(HttpStatusCode.Created, "Registration successful")
        }
    }

    post("/api/login") {
        val form = call.receiveParameters()
        val username = form["username"]?.trim().orEmpty()
        val password = form["password"].orEmpty()
        val user = database.userDao().findByUsername(username)
        if (user == null || !PasswordHasher.matches(password, user.passwordHash)) {
            call.respond(HttpStatusCode.Unauthorized, "Invalid username or password")
        } else {
            createSession(call, database, user.username)
            call.respondText("Login successful")
        }
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

private fun validateRegistration(username: String, email: String, password: String): String? = when {
    !username.matches(Regex("^[A-Za-z0-9_]{3,32}$")) -> "Username must be 3-32 letters, numbers, or underscores"
    !email.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) -> "Enter a valid email address"
    password.length < 8 -> "Password must be at least 8 characters"
    else -> null
}
