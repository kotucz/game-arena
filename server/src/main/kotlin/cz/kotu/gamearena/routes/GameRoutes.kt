package cz.kotu.gamearena.routes

import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.gamearena.ContactsGame
import cz.kotu.gamearena.GamesManager
import cz.kotu.gamearena.UserPrincipal
import cz.kotu.gamearena.model.CreateGameRequest
import cz.kotu.gamearena.model.RunningGame
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
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

fun Route.gameRoutes(gamesManager: GamesManager) {
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

    authenticate("auth-session") {
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
                            createdAt = game.metadata.createdAt,
                            status = "created",
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

        route("/api/games/{gameId}") {
            install(contactsGamePlugin)

            sse("/contacts/events") {
                heartbeat {
                    period = 15.seconds
                    event = ServerSentEvent(comments = "heartbeat")
                }
                val principal = call.principal<UserPrincipal>()!!
                call.contactsGame.forUser(principal.username).handleEvents(this)
            }

            post("/contacts/actions") {
                val principal = call.principal<UserPrincipal>()!!
                val game = call.contactsGame
                val result = game.forUser(principal.username).handleAction(call.receiveText())
                if (result.isSuccess) {
                    gamesManager.persist(game)
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
                call.contactsGame.forUser(principal.username).handleLogs(this, lastSentLogIndex)
            }
        }
    }
}
