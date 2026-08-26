package cz.kotu.gamearena

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header

actual fun createPlatformAuthHttpClient(configure: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(OkHttp) {
    install(HttpCookies) {
        storage = PreferencesCookieStorage()
    }
    // Default REST timeouts (e.g. 15s)
    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        connectTimeoutMillis = 10_000
    }

    engine {
        config {
            // 0 = Infinite socket read timeout for OkHttp.
            // Ktor's HttpTimeout plugin will still enforce timeouts on regular REST calls.
            readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
            connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        }
    }
    configure()
}

/** Creates a desktop test client that authenticates as [username] via a debug header. */
fun createDebugAuthHttpClient(username: String): HttpClient =
    createPlatformAuthHttpClient {
        defaultRequest {
            header(DEBUG_USERNAME_HEADER, username)
        }
        commonHttpClientConfig(onUnauthorized = {})
    }

actual fun authBaseUrl(): String = System.getenv("GAMEARENA_API_URL") ?: "http://localhost:8080"

private const val DEBUG_USERNAME_HEADER = "X-Debug-Username"
