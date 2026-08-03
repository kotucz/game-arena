package cz.kotu.game.gotfive

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import java.io.File

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val webRoot = File(
        System.getenv("WEB_ROOT") ?: "app/webApp/build/dist/wasmJs/productionExecutable",
    )

    routing {
        get("/health") {
            call.respondText("OK")
        }

        staticFiles("/", webRoot) {
            default("index.html")
        }
    }
}
