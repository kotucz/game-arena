package cz.kotu.gamearena

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.sse.SSE
import io.ktor.http.HttpStatusCode

/**
 * Common Ktor plugin configuration shared across all platforms.
 * Each platform actual calls this inside its engine-specific HttpClient block.
 *
 * @param baseUrl optional base URL; if blank, requests remain origin-relative (useful for web).
 * @param onUnauthorized called whenever any response returns HTTP 401.
 */
fun HttpClientConfig<*>.commonHttpClientConfig(baseUrl: String = "", onUnauthorized: () -> Unit) {
    if (baseUrl.isNotBlank()) {
        install(DefaultRequest) {
            url(baseUrl)
        }
    }

    install(SSE)
    install(Logging) {
        level = LogLevel.INFO
        logger = object : Logger {
            override fun log(message: String) {
                Napier.i(tag = "KtorClient") { message }
            }
        }
    }
    HttpResponseValidator {
        validateResponse { response ->
            if (response.status == HttpStatusCode.Unauthorized) {
                onUnauthorized()
            }
        }
    }
}

/** Platform-specific factory; each actual supplies the engine and cookie storage. */
expect fun createPlatformAuthHttpClient(configure: HttpClientConfig<*>.() -> Unit): HttpClient

fun createAuthHttpClient(baseUrl: String = "", onUnauthorized: () -> Unit): HttpClient =
    createPlatformAuthHttpClient {
        commonHttpClientConfig(baseUrl, onUnauthorized)
    }
