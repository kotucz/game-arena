package cz.kotu.game.gotfive

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.netty.Netty
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.io.File

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val database = createDatabase()
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
                call.respondText("Login successful")
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
