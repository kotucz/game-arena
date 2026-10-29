package cz.kotu.gamearena.routes

import cz.kotu.gamearena.AppDatabase
import cz.kotu.gamearena.model.AvailablePlayer
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.playerRoutes(database: AppDatabase) {
    authenticate("auth-firebase") {
        get("/api/players") {
            val players = database.userDao().listUsers().map {
                AvailablePlayer(
                    uid = it.firebaseUid,
                    username = it.username,
                    pictureUrl = it.pictureUrl,
                )
            }
            call.respond(players)
        }
    }
}
