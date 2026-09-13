package cz.kotu.gamearena

import cz.kotu.gamearena.model.RegisterTokenRequest
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class NotificationClient(private val httpClient: HttpClient) {

    suspend fun registerToken(tokenId: String, service: String, token: String): Result<Unit> =
        registerToken(tokenId, RegisterTokenRequest(service = service, token = token))

    suspend fun registerToken(tokenId: String, request: RegisterTokenRequest): Result<Unit> = runCatching {
        val response = httpClient.put(endpoint("/api/notifications/tokens/$tokenId")) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(Json.encodeToString(RegisterTokenRequest.serializer(), request))
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            error(body.ifBlank { "Failed to register push token" })
        }
    }.onFailure { error ->
        Napier.e("Register token request failed", error)
    }

    suspend fun deleteToken(tokenId: String): Result<Unit> = runCatching {
        val response = httpClient.delete(endpoint("/api/notifications/tokens/$tokenId"))
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            error(body.ifBlank { "Failed to delete push token" })
        }
    }.onFailure { error ->
        Napier.e("Delete token request failed", error)
    }

    private fun endpoint(path: String): String = authBaseUrl().trimEnd('/') + path
}
