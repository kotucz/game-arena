package cz.kotu.gamearena

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.header

actual fun createAuthHttpClient(): HttpClient = HttpClient(CIO) {
    install(HttpCookies) {
        storage = PreferencesCookieStorage()
    }
    install(SSE)
}

/** Creates a desktop test client that authenticates as [username]. */
fun createDebugAuthHttpClient(username: String): HttpClient = HttpClient(CIO) {
    defaultRequest {
        header(DEBUG_USERNAME_HEADER, username)
    }
    install(SSE)
}

actual fun authBaseUrl(): String = System.getenv("GAMEARENA_API_URL") ?: "http://localhost:8080"

private const val DEBUG_USERNAME_HEADER = "X-Debug-Username"
