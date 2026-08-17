package cz.kotu.gamearena

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.sse.SSE
import io.ktor.http.HttpStatusCode

/**
 * Common Ktor plugin configuration shared across all platforms.
 * Each platform actual calls this inside its engine-specific HttpClient block.
 *
 * @param onUnauthorized called whenever any response returns HTTP 401.
 */
fun HttpClientConfig<*>.commonHttpClientConfig(onUnauthorized: () -> Unit) {
    install(SSE)
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

fun createAuthHttpClient(onUnauthorized: () -> Unit): HttpClient =
    createPlatformAuthHttpClient {
        commonHttpClientConfig(onUnauthorized)
    }
