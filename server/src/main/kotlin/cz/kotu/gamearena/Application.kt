package cz.kotu.gamearena

import io.ktor.http.Cookie
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.netty.Netty
import io.ktor.server.request.receiveParameters
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.ktor.server.sse.sse
import cz.kotu.gamearena.model.RunningGame
import cz.kotu.gamearena.model.CreateGameRequest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val database = createDatabase()
    val gamesManager = GamesManager()
    gamesManager.createContactsGame()
    install(SSE)
    val webRoot = File(
        System.getenv("WEB_ROOT") ?: "app/webApp/build/dist/wasmJs/productionExecutable",
    )

    routing {
        get("/health") {
            call.respondText("OK")
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

        get("/api/me") {
            val session = currentSession(call, database)
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized, "Not authenticated")
            } else {
                call.respondText(session.username)
            }
        }

        get("/api/games") {
            val session = currentSession(call, database)
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized, "Not authenticated")
            } else {
                val games = Json.encodeToString(
                    ListSerializer(RunningGame.serializer()),
                    gamesManager.runningGames(),
                )
                call.respondText(games, ContentType.Application.Json)
            }
        }

        post("/api/games") {
            val session = currentSession(call, database)
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized, "Not authenticated")
            } else {
                val request = try {
                    Json.decodeFromString(CreateGameRequest.serializer(), call.receiveText())
                } catch (_: SerializationException) {
                    null
                }

                if (request == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid game request")
                } else {
                    val players = request.players.map(String::trim)
                    when {
                        request.type != "contacts" ->
                            call.respond(HttpStatusCode.BadRequest, "Unsupported game type")
                        players.isEmpty() || players.any(String::isEmpty) ->
                            call.respond(HttpStatusCode.BadRequest, "At least one player is required")
                        else -> {
                            val game = gamesManager.createContactsGame(players)
                            val response = RunningGame(
                                id = game.metadata.id,
                                type = game.metadata.type,
                                players = game.metadata.players,
                                createdAt = game.metadata.createdAt.toString(),
                            )
                            call.respondText(
                                Json.encodeToString(RunningGame.serializer(), response),
                                ContentType.Application.Json,
                                HttpStatusCode.Created,
                            )
                        }
                    }
                }
            }
        }

        sse("/api/games/{gameId}/contacts/events") {
            val session = currentSession(call, database)
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized, "Not authenticated")
            } else {
                val game = gamesManager.contactsGame(call.parameters["gameId"].orEmpty())
                if (game == null) {
                    call.respond(HttpStatusCode.NotFound, "Game not found")
                } else {
                    game.contacts.handleEvents(this, session.username)
                }
            }
        }

        post("/api/games/{gameId}/contacts/actions") {
            val session = currentSession(call, database)
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized, "Not authenticated")
            } else {
                val game = gamesManager.contactsGame(call.parameters["gameId"].orEmpty())
                if (game == null) {
                    call.respond(HttpStatusCode.NotFound, "Game not found")
                } else {
                    val error = game.contacts.handleAction(call.receiveText(), session.username)
                    if (error == null) {
                        call.respond(HttpStatusCode.Accepted)
                    } else {
                        call.respond(HttpStatusCode.BadRequest, error)
                    }
                }
            }
        }

        get("/") {
            call.respondFile(File(webRoot, "index.html"))
        }

        // Do not fall back to index.html for missing assets. In particular, a
        // stale browser requesting an old Wasm hash must receive a 404, not HTML.
        staticFiles("/", webRoot)
    }

    environment.monitor.subscribe(ApplicationStopped) {
        database.close()
    }
}

private fun validateRegistration(username: String, email: String, password: String): String? = when {
    !username.matches(Regex("^[A-Za-z0-9_]{3,32}$")) -> "Username must be 3-32 letters, numbers, or underscores"
    !email.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) -> "Enter a valid email address"
    password.length < 8 -> "Password must be at least 8 characters"
    else -> null
}

private suspend fun createSession(call: ApplicationCall, database: AppDatabase, username: String) {
    val token = SessionTokens.create()
    database.sessionDao().insert(
        Session(
            tokenHash = SessionTokens.hash(token),
            username = username,
            expiresAt = Instant.now().epochSecond + SessionTokens.lifetimeSeconds,
        )
    )
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

internal suspend fun currentSession(call: ApplicationCall, database: AppDatabase): Session? {
    // Development clients can bypass the persistent session cookie by sending
    // a username explicitly. The fake session is only used for the duration
    // of this request; the username is the only value consumed by the game.
    val debugUsername = call.request.headers[DEBUG_USERNAME_HEADER]?.trim()
    if (!debugUsername.isNullOrEmpty()) {
        return Session(
            tokenHash = "debug:$debugUsername",
            username = debugUsername,
            expiresAt = Long.MAX_VALUE,
        )
    }

    val token = call.request.cookies[SessionTokens.cookieName] ?: return null
    val session = database.sessionDao().findByTokenHash(SessionTokens.hash(token)) ?: return null
    if (session.expiresAt <= Instant.now().epochSecond) {
        database.sessionDao().deleteByTokenHash(session.tokenHash)
        return null
    }
    return session
}

private const val DEBUG_USERNAME_HEADER = "X-Debug-Username"
