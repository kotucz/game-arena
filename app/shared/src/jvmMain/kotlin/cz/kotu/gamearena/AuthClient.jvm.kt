package cz.kotu.gamearena

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies

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
        preconfigured = okhttp3.OkHttpClient.Builder()
            .protocols(listOf(okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTP_1_1))
            .build()
        config {
            // 0 = Infinite socket read timeout for OkHttp.
            // Ktor's HttpTimeout plugin will still enforce timeouts on regular REST calls.
            readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
            connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            protocols(listOf(okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTP_1_1))
        }
    }
    configure()
}

/**
 * Creates an in-memory HTTP client (using [AcceptAllCookiesStorage]) isolated from the global
 * desktop [PreferencesCookieStorage]. Useful for multi-player testing or isolated sessions.
 */
fun createInMemoryAuthHttpClient(onUnauthorized: () -> Unit = {}): HttpClient = HttpClient(OkHttp) {
    install(HttpCookies) {
        storage = AcceptAllCookiesStorage()
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        connectTimeoutMillis = 10_000
    }
    engine {
        preconfigured = okhttp3.OkHttpClient.Builder()
            .protocols(listOf(okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTP_1_1))
            .build()
        config {
            readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
            connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            protocols(listOf(okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTP_1_1))
        }
    }
    commonHttpClientConfig(onUnauthorized)
}

actual fun authBaseUrl(): String = System.getenv("GAMEARENA_API_URL") ?: "http://localhost:8080"

