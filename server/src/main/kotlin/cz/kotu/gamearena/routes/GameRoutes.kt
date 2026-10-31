package cz.kotu.gamearena.routes

import cz.kotu.game.contacts.ai.AiCommandRequest
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsNetworkAction
import cz.kotu.gamearena.ContactsGame
import cz.kotu.gamearena.GamesManager
import cz.kotu.gamearena.RunningGamesResponseMapper
import cz.kotu.gamearena.UnknownPlayerException
import cz.kotu.gamearena.UnknownGamePlayerException
import cz.kotu.gamearena.UserPrincipal
import cz.kotu.gamearena.model.CreateGameRequest
import cz.kotu.gamearena.model.RunningGame
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sse.heartbeat
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import io.ktor.util.AttributeKey
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

private val ContactsGameAttributeKey = AttributeKey<ContactsGame>("ContactsGame")

val ApplicationCall.contactsGame: ContactsGame
    get() = attributes[ContactsGameAttributeKey]

fun Route.gameRoutes(
    gamesManager: GamesManager,
    runningGamesResponseMapper: RunningGamesResponseMapper,
) {
    val contactsGamePlugin = createRouteScopedPlugin("ContactsGamePlugin") {
        onCall { call ->
            val gameId = call.parameters["gameId"].orEmpty()
            val game = gamesManager.contactsGame(gameId)
            if (game == null) {
                call.respond(HttpStatusCode.NotFound, "Game not found")
            } else {
                call.attributes.put(ContactsGameAttributeKey, game)
            }
        }
    }

    authenticate("auth-firebase") {
        get("/api/games") {
            call.respond(runningGamesResponseMapper.toResponses(gamesManager.runningGames()))
        }

        sse("/api/games/events") {
            heartbeat {
                period = 15.seconds
                event = ServerSentEvent(comments = "heartbeat")
            }

            gamesManager.runningGames.collect { games ->
                send(
                    ServerSentEvent(
                        Json.encodeToString(
                            ListSerializer(RunningGame.serializer()),
                            runningGamesResponseMapper.toResponses(games),
                        ),
                    ),
                )
            }
        }

        post("/api/games") {
            val request = call.receive<CreateGameRequest>()
            if (request.type != "contacts") {
                call.respond(HttpStatusCode.BadRequest, "Unsupported game type")
                return@post
            }
            if (request.players.isEmpty()) {
                call.respond(HttpStatusCode.BadRequest, "At least one player is required")
                return@post
            }
            val config = try {
                Json.decodeFromString(ContactsBoardState.ContactsGameConfig.serializer(), request.config)
            } catch (_: SerializationException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid game configuration")
                return@post
            }
            val game = try {
                gamesManager.createContactsGame(request.players, config)
            } catch (e: UnknownPlayerException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Unknown player")
                return@post
            }
            call.respond(
                HttpStatusCode.Created,
                runningGamesResponseMapper.toResponse(game.metadata, status = "created"),
            )
        }

        route("/api/games/{gameId}") {
            install(contactsGamePlugin)

            get("/contacts") {
                call.respond(
                    runningGamesResponseMapper.toContactsGameDetails(call.contactsGame),
                )
            }

            sse("/contacts/events") {
                heartbeat {
                    period = 15.seconds
                    event = ServerSentEvent(comments = "heartbeat")
                }
                val principal = call.principal<UserPrincipal>()!!
                val facade = try {
                    call.contactsGame.forUser(principal.firebaseUid)
                } catch (_: UnknownGamePlayerException) {
                    call.respond(HttpStatusCode.Forbidden, "User is not a player in this game")
                    return@sse
                }
                facade.handleEvents(this)
            }

            post("/contacts/actions") {
                val principal = call.principal<UserPrincipal>()!!
                val game = call.contactsGame
                val action = call.receive<ContactsNetworkAction>()
                val facade = try {
                    game.forUser(principal.firebaseUid)
                } catch (_: UnknownGamePlayerException) {
                    call.respond(HttpStatusCode.Forbidden, "User is not a player in this game")
                    return@post
                }
                val result = facade.handleAction(action)
                if (result.isSuccess) {
                    call.respond(HttpStatusCode.Accepted)
                } else {
                    call.respond(HttpStatusCode.BadRequest, result.exceptionOrNull()?.message ?: "Invalid action")
                }
            }

            sse("/logs") {
                heartbeat {
                    period = 15.seconds
                    event = ServerSentEvent(comments = "heartbeat")
                }
                val principal = call.principal<UserPrincipal>()!!
                val lastSentLogIndex = call.request.queryParameters["lastSentLogIndex"]?.toIntOrNull() ?: -1
                val facade = try {
                    call.contactsGame.forUser(principal.firebaseUid)
                } catch (_: UnknownGamePlayerException) {
                    call.respond(HttpStatusCode.Forbidden, "User is not a player in this game")
                    return@sse
                }
                facade.handleLogs(this, lastSentLogIndex)
            }

            post("/ai/{id}/command") {
                val aiId = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Missing AI player ID")
                    return@post
                }
                val request = call.receive<AiCommandRequest>()
                if (request.prompt.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Prompt must not be empty")
                } else {
                    val game = call.contactsGame
                    val found = game.commandAiPlayer(aiId, request.prompt)
                    if (found) {
                        call.respond(HttpStatusCode.Accepted)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "AI player not found: $aiId")
                    }
                }
            }
        }
    }
}
