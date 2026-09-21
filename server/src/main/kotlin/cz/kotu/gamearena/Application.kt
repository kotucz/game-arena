package cz.kotu.gamearena

import cz.kotu.gamearena.admin.adminRoutes
import cz.kotu.gamearena.plugins.configureSecurity
import cz.kotu.gamearena.routes.authRoutes
import cz.kotu.gamearena.routes.gameRoutes
import cz.kotu.gamearena.routes.notificationRoutes
import cz.kotu.gamearena.routes.staticRoutes
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.netty.channel.ChannelOption
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.event.Level

fun main() {
    val serverConfig = ServerConfig.load()
    embeddedServer(
        factory = Netty,
        configure = {
            // Set host and port via connector
            connector {
                host = "0.0.0.0"
                this.port = serverConfig.port
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
        module = {
            module(ServerComponent::class.create(serverConfig))
        }
    ).start(wait = true)
}

fun Application.module(serverComponent: ServerBindings) {
    Napier.base(DebugAntilog())
    Napier.i { "Starting GameArena server" }

    install(CallLogging) {
        level = Level.INFO
        logger = org.slf4j.LoggerFactory.getLogger("Ktor.Server")
    }
    install(SSE)

    val database = serverComponent.database
    val gamesManager = serverComponent.gamesManager
    val serverConfig = serverComponent.serverConfig
    val pushNotificationService = serverComponent.notificationService
    runBlocking { gamesManager.restorePersistedGames() }

    configureSecurity(database, serverConfig)

    val webRoot = serverConfig.webRoot

    routing {
        get("/health") {
            call.respondText("OK")
        }

        // Serve public Firebase web SDK config (apiKey, projectId, etc.) plus the FCM VAPID key
        // so browser clients can initialise the Firebase JS SDK and call getToken().
        // Update firebase.webConfig and firebase.webVapidKey in application.conf when the Firebase
        // project settings change — no JS rebuild required.
        get("/api/firebase-config") {
            val json = buildJsonObject {
                serverConfig.firebaseWebConfig.forEach { (k, v) -> put(k, v) }
                serverConfig.firebaseWebVapidKey?.let { put("vapidKey", it) }
            }
            call.respondText(json.toString(), ContentType.Application.Json)
        }

        authRoutes(database, serverConfig)
        notificationRoutes(database)
        gameRoutes(gamesManager)
        adminRoutes(pushNotificationService)
        staticRoutes(webRoot)
    }

    monitor.subscribe(ApplicationStopped) {
        database.close()
    }
}
