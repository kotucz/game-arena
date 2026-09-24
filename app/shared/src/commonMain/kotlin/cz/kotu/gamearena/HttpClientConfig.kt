package cz.kotu.gamearena

import com.mmk.kmpauth.core.KMPAuth
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

fun interface TokenProvider {
    suspend fun getToken(): String?
}

class KmpAuthTokenProvider : TokenProvider {
    override suspend fun getToken(): String? =
        runCatching { KMPAuth.currentUserIdToken().getOrNull() }.getOrNull()
}

class BearerAuthConfig {
    var tokenProvider: TokenProvider? = null
}

val BearerAuthPlugin = createClientPlugin("BearerAuthPlugin", ::BearerAuthConfig) {
    val tokenProvider = pluginConfig.tokenProvider
    onRequest { request, _ ->
        if (tokenProvider != null && !request.headers.contains(HttpHeaders.Authorization)) {
            val token = tokenProvider.getToken()
            if (!token.isNullOrBlank()) {
                request.header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }
}

/**
 * Common Ktor plugin configuration shared across all platforms.
 * Each platform actual calls this inside its engine-specific HttpClient block.
 *
 * @param baseUrl optional base URL; if blank, requests remain origin-relative (useful for web).
 * @param tokenProvider optional dynamic token provider called per request to attach Bearer token.
 * @param onUnauthorized called whenever any response returns HTTP 401.
 */
fun HttpClientConfig<*>.commonHttpClientConfig(
    baseUrl: String = "",
    tokenProvider: TokenProvider? = null,
    onUnauthorized: () -> Unit,
) {
    if (baseUrl.isNotBlank()) {
        install(DefaultRequest) {
            url(baseUrl)
        }
    }

    if (tokenProvider != null) {
        install(BearerAuthPlugin) {
            this.tokenProvider = tokenProvider
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

/** Platform-specific factory; each actual supplies the engine. */
expect fun createPlatformAuthHttpClient(configure: HttpClientConfig<*>.() -> Unit): HttpClient

fun createAuthHttpClient(
    baseUrl: String = "",
    tokenProvider: TokenProvider? = KmpAuthTokenProvider(),
    onUnauthorized: () -> Unit,
): HttpClient =
    createPlatformAuthHttpClient {
        commonHttpClientConfig(baseUrl, tokenProvider, onUnauthorized)
    }
