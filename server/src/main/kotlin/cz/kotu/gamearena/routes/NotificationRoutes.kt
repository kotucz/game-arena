package cz.kotu.gamearena.routes

import cz.kotu.gamearena.AppDatabase
import cz.kotu.gamearena.PushToken
import cz.kotu.gamearena.UserPrincipal
import cz.kotu.gamearena.model.RegisterTokenRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.put
import io.ktor.server.routing.route

private val knownServices = setOf("fcm")

fun Route.notificationRoutes(database: AppDatabase) {
    authenticate("auth-firebase") {
        route("/api/notifications/tokens/{tokenId}") {
            put {
                val principal = call.principal<UserPrincipal>()!!
                val tokenId = call.parameters["tokenId"]
                if (tokenId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "tokenId is required")
                    return@put
                }
                val body = call.receive<RegisterTokenRequest>()
                if (body.service !in knownServices) {
                    call.respond(HttpStatusCode.BadRequest, "Unknown service '${body.service}'. Expected one of: $knownServices")
                    return@put
                }
                database.pushTokenDao().upsert(
                    PushToken(
                        tokenId = tokenId,
                        firebaseUid = principal.firebaseUid,
                        service = body.service,
                        token = body.token,
                        updatedAtMillis = System.currentTimeMillis(),
                    )
                )
                call.respond(HttpStatusCode.NoContent)
            }

            delete {
                val principal = call.principal<UserPrincipal>()!!
                val tokenId = call.parameters["tokenId"]
                if (tokenId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "tokenId is required")
                    return@delete
                }
                // Scoped to the authenticated user — silently no-ops if token belongs to another user.
                database.pushTokenDao().delete(tokenId, principal.firebaseUid)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
