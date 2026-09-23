package cz.kotu.gamearena

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin

// Use localhost for simulator; change to device IP or env var if needed.
actual fun defaultApiBaseUrl(): String = "http://localhost:8080"

actual fun createPlatformAuthHttpClient(configure: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(Darwin) {
    engine {
        // Configure Darwin-specific options here if needed
    }
    configure()
}
