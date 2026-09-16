package cz.kotu.gamearena.routes

import cz.kotu.gamearena.AppDatabase
import cz.kotu.gamearena.PasswordHasher
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

fun Route.authRoutes(database: AppDatabase) {
    post("/api/auth/firebase") {
        val form = call.receiveParameters()
        val firebaseUid = form["firebaseUid"]?.trim().orEmpty()
        val username = form["username"]?.trim().orEmpty()
        val email = form["email"]?.trim().orEmpty()

        if (firebaseUid.isBlank() || username.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, "firebaseUid and username are required")
            return@post
        }

        val existingUser = database.userDao().findByFirebaseUid(firebaseUid)
        if (existingUser == null) {
            database.userDao().insert(
                User(
                    username = username,
                    passwordHash = "",
                    email = email,
                    firebaseUid = firebaseUid,
                ),
            )
        }
        createSession(call, database, username, userId = firebaseUid)
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
