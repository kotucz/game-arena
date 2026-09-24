package cz.kotu.gamearena

import cz.kotu.gamearena.model.RegisterUserRequest
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class AuthClient(private val httpClient: HttpClient) {
    suspend fun registerUser(
        idToken: String,
        username: String,
        email: String? = null,
    ): Result<String> = runCatching {
        val response = httpClient.post("/api/auth/register") {
            header(HttpHeaders.Authorization, "Bearer $idToken")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterUserRequest.serializer(), RegisterUserRequest(username = username, email = email)))
        }
        val message = response.bodyAsText()
        if (!response.status.isSuccess()) error(message.ifBlank { "Registration failed" })
        message
    }.onFailure { error ->
        Napier.e("User registration failed", error)
    }

    suspend fun logout(): Result<String> = runCatching {
        val response = httpClient.post("/api/logout")
        val message = response.bodyAsText()
        if (!response.status.isSuccess()) error(message.ifBlank { "Logout failed" })
        message
    }

    suspend fun currentUser(): Result<String> = runCatching {
        val response = httpClient.get("/api/me")
        val message = response.bodyAsText()
        if (!response.status.isSuccess()) error(message.ifBlank { "Not authenticated" })
        message
    }
}
