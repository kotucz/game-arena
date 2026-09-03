package cz.kotu.gamearena

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.cookies.HttpCookies

// Use localhost for simulator; change to device IP or env var if needed.
actual fun authBaseUrl(): String = "http://localhost:8080"

actual fun createPlatformAuthHttpClient(configure: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(Darwin) {
    install(HttpCookies)
    engine {
        // Configure Darwin-specific options here if needed
    }
    configure()
}
