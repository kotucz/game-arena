package cz.kotu.gamearena

import cz.kotu.gamearena.plugins.configureSecurity
import cz.kotu.gamearena.routes.authRoutes
import cz.kotu.gamearena.routes.gameRoutes
import cz.kotu.gamearena.routes.notificationRoutes
import cz.kotu.gamearena.routes.staticRoutes
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
import org.slf4j.event.Level
import java.io.File

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
    install(SSE)

    val database = serverComponent.database
    val gamesManager = serverComponent.gamesManager
    runBlocking { gamesManager.restorePersistedGames() }

    configureSecurity(database)

    val webRoot = File(
        // relative url with ./gradlew :server:run
        System.getenv("WEB_ROOT") ?: "../app/webApp/build/dist/wasmJs/productionExecutable",
    )

    routing {
        get("/health") {
            call.respondText("OK")
        }
        authRoutes(database)
        notificationRoutes(database)
        gameRoutes(gamesManager)
        staticRoutes(webRoot)
    }

    monitor.subscribe(ApplicationStopped) {
        database.close()
    }
}
