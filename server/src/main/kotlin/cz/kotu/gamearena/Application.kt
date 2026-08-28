package cz.kotu.gamearena

import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.gamearena.model.CreateGameRequest
import cz.kotu.gamearena.model.RunningGame
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.session
import io.ktor.server.auth.principal
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.receiveParameters
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.maxAge
import io.ktor.server.sessions.sessions
import io.ktor.server.sse.SSE
import io.ktor.server.sse.heartbeat
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import io.netty.channel.ChannelOption
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import java.io.File
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(
        factory = Netty,
        configure = {
            // Set host and port via connector
            connector {
                host = "0.0.0.0"
                this.port = port
            }

            // Enable HTTP/2 over cleartext (h2c) for local proxy connections
            enableHttp2 = true
            enableH2c = true

            // Disable Nagle's algorithm directly on Netty's Channel bootstrap
            configureBootstrap = {
                option(ChannelOption.TCP_NODELAY, true)
                childOption(ChannelOption.TCP_NODELAY, true)
            }
        },
        module = Application::module
    ).start(wait = true)
}

fun Application.module(serverComponent: ServerBindings = ServerComponent::class.create()) {
    install(CallLogging) {
        level = Level.INFO
        logger = org.slf4j.LoggerFactory.getLogger("Ktor.Server")
    }
    val database = serverComponent.database
    val gamesManager = serverComponent.gamesManager
    kotlinx.coroutines.runBlocking { gamesManager.restorePersistedGames() }
    install(Sessions) {
        cookie<String>(SessionTokens.cookieName) {
            cookie.path = "/"
            cookie.httpOnly = true
            cookie.maxAge = SessionTokens.lifetimeSeconds.seconds
        }
    }

    // Authentication provider that validates session tokens stored in the database.
    install(Authentication) {
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

    install(SSE)
    val webRoot = File(
        // relative url with ./gradlew :server:run
        System.getenv("WEB_ROOT") ?: "../app/webApp/build/dist/wasmJs/productionExecutable",
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

            get("/api/games") {
                val principal = call.principal<UserPrincipal>()!!
                val games = Json.encodeToString(
                    ListSerializer(RunningGame.serializer()),
                    gamesManager.runningGames(),
                )
                call.respondText(games, ContentType.Application.Json)
            }

            sse("/api/games/events") {
                heartbeat {
                    period = 15.seconds
                    event = ServerSentEvent(comments = "heartbeat")
                }

                val principal = call.principal<UserPrincipal>()!!
                gamesManager.runningGames.collect { games ->
                    send(
                        ServerSentEvent(
                            Json.encodeToString(ListSerializer(RunningGame.serializer()), games),
                        ),
                    )
                }
            }

            post("/api/games") {
                val principal = call.principal<UserPrincipal>()!!
                val request = try {
                    Json.decodeFromString(CreateGameRequest.serializer(), call.receiveText())
                } catch (_: SerializationException) {
                    null
                }

                if (request == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid game request")
                } else {
                    val players = request.players.map(String::trim)
                    val config =
                        Json.decodeFromString(ContactsBoardState.ContactsGameConfig.serializer(), request.config)
                    when {
                        request.type != "contacts" ->
                            call.respond(HttpStatusCode.BadRequest, "Unsupported game type")

                        players.isEmpty() || players.any(String::isEmpty) ->
                            call.respond(HttpStatusCode.BadRequest, "At least one player is required")

                        else -> {
                            val game = gamesManager.createContactsGame(players, config)
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

            sse("/api/games/{gameId}/contacts/events") {
                heartbeat {
                    period = 15.seconds
                    event = ServerSentEvent(comments = "heartbeat")
                }
                val principal = call.principal<UserPrincipal>()!!
                val game = gamesManager.contactsGame(call.parameters["gameId"].orEmpty())
                if (game == null) {
                    call.respond(HttpStatusCode.NotFound, "Game not found")
                } else {
                    game.contacts.handleEvents(this, principal.username)
                }
            }

            post("/api/games/{gameId}/contacts/actions") {
                val principal = call.principal<UserPrincipal>()!!
                val game = gamesManager.contactsGame(call.parameters["gameId"].orEmpty())
                if (game == null) {
                    call.respond(HttpStatusCode.NotFound, "Game not found")
                } else {
                    val result = game.contacts.handleAction(call.receiveText(), principal.username)
                    if (result.isSuccess) {
                        gamesManager.persist(game)
                        call.respond(HttpStatusCode.Accepted)
                    } else {
                        call.respond(HttpStatusCode.BadRequest, result.exceptionOrNull()?.message ?: "Invalid action")
                    }
                }
            }

            sse("/api/games/{gameId}/logs") {
                heartbeat {
                    period = 15.seconds
                    event = ServerSentEvent(comments = "heartbeat")
                }
                val principal = call.principal<UserPrincipal>()!!
                val game = gamesManager.contactsGame(call.parameters["gameId"].orEmpty())
                if (game == null) {
                    call.respond(HttpStatusCode.NotFound, "Game not found")
                } else {
                    val lastSentLogIndex = call.request.queryParameters["lastSentLogIndex"]?.toIntOrNull() ?: -1
                    game.contacts.handleLogs(this, principal.username, lastSentLogIndex)
                }
            }
        }

        suspend fun respondAppShell(call: ApplicationCall) {
            val file = File(webRoot, "index.html")
            call.response.headers.append(HttpHeaders.CacheControl, CacheControl.NoCache(CacheControl.Visibility.Private).toString())
            call.respondFile(file)
        }

        get("/") {
            respondAppShell(call)
        }

        // Static assets are versioned and change infrequently, so cache them aggressively.
        // Keep the app shell uncached so clients pick up new bundle hashes after deployment.
        staticFiles("/", webRoot) {
            cacheControl { resource ->
                staticAssetCacheControl(resource)
            }
        }
    }
    monitor.subscribe(ApplicationStopped) {
        database.close()
    }
}

private fun staticAssetCacheControl(resource: File): List<CacheControl> = when {
    resource.name.equals("index.html", ignoreCase = true) -> listOf(CacheControl.NoCache(CacheControl.Visibility.Private))
    resource.extension.lowercase() in setOf("wasm", "js", "css", "svg", "png", "ico", "woff", "woff2", "ttf", "otf") ->
        listOf(CacheControl.MaxAge(365 * 24 * 60 * 60, visibility = CacheControl.Visibility.Public))

    else -> emptyList()
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

