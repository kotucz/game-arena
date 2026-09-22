package cz.kotu.gamearena.plugins

import cz.kotu.gamearena.AppDatabase
import cz.kotu.gamearena.ServerConfig
import cz.kotu.gamearena.TokenVerifier
import cz.kotu.gamearena.UserPrincipal
import cz.kotu.gamearena.admin.adminBasicAuthentication
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.bearer

fun Application.configureSecurity(
    database: AppDatabase,
    serverConfig: ServerConfig,
    tokenVerifier: TokenVerifier,
) {
    install(Authentication) {
        adminBasicAuthentication(serverConfig)

        bearer("auth-firebase") {
            authenticate { credential ->
                val claims = tokenVerifier.verify(credential.token) ?: return@authenticate null
                val user = database.userDao().findByFirebaseUid(claims.uid) ?: return@authenticate null
                UserPrincipal(
                    userId = claims.uid,
                    username = user.username,
                )
            }
        }
    }
}
