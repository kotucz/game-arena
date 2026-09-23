package cz.kotu.gamearena.routes

import cz.kotu.gamearena.AppDatabase
import cz.kotu.gamearena.TokenVerifier
import cz.kotu.gamearena.User
import cz.kotu.gamearena.UserPrincipal
import cz.kotu.gamearena.model.RegisterUserRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveParameters
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.json.Json
import java.util.Locale

fun Route.authRoutes(database: AppDatabase, tokenVerifier: TokenVerifier) {
    suspend fun handleUserRegistration(call: ApplicationCall) {
        val idToken: String?
        val username: String?
        val email: String?

        val contentType = call.request.contentType()
        if (contentType.match(ContentType.Application.Json)) {
            val body = runCatching {
                Json.decodeFromString<RegisterUserRequest>(call.receiveText())
            }.getOrNull()
            if (body == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid JSON body")
                return
            }
            username = body.username.trim()
            email = body.email?.trim()
            idToken = body.idToken?.trim()
        } else {
            val form = call.receiveParameters()
            username = form["username"]?.trim()
            email = form["email"]?.trim()
            idToken = form["idToken"]?.trim()
        }

        val authHeader = call.request.headers[HttpHeaders.Authorization]
        val bearerToken = authHeader?.removePrefix("Bearer ")?.trim()
        val token = idToken?.takeIf { it.isNotBlank() } ?: bearerToken?.takeIf { it.isNotBlank() }

        if (token.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "idToken is required")
            return
        }

        if (username.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "username is required")
            return
        }

        val claims = tokenVerifier.verify(token)
        if (claims == null) {
            call.respond(HttpStatusCode.Unauthorized, "Invalid Firebase token")
            return
        }

        val firebaseUid = claims.uid
        val existingByUid = database.userDao().findByFirebaseUid(firebaseUid)
        if (existingByUid != null) {
            call.respond(HttpStatusCode.Conflict, "User is already registered")
            return
        }

        val usernameLower = username.lowercase(Locale.ROOT)
        val existingByUsername = database.userDao().findByUsernameLower(usernameLower)
        if (existingByUsername != null) {
            call.respond(HttpStatusCode.Conflict, "Username is already taken")
            return
        }

        val resolvedEmail = email?.takeIf { it.isNotBlank() } ?: claims.email.orEmpty()
        val created = User(
            firebaseUid = firebaseUid,
            username = username,
            usernameLower = usernameLower,
            email = resolvedEmail,
        )
        database.userDao().insert(created)
        call.respond(HttpStatusCode.Created, "User registered successfully")
    }

    post("/api/auth/register") {
        handleUserRegistration(call)
    }

    post("/api/auth/user") {
        handleUserRegistration(call)
    }

    post("/api/logout") {
        call.respondText("Logout successful")
    }

    authenticate("auth-firebase") {
        get("/api/me") {
            val principal = call.principal<UserPrincipal>()!!
            call.respondText(principal.username)
        }
    }
}
