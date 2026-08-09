package cz.kotu.game.gotfive

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters

expect fun createAuthHttpClient(): HttpClient

class AuthClient(private val httpClient: HttpClient = createAuthHttpClient()) {
    suspend fun register(username: String, email: String, password: String): Result<String> = submit(
        "/api/register", Parameters.build {
            append("username", username)
            append("email", email)
            append("password", password)
        }
    )

    suspend fun login(username: String, password: String): Result<String> = submit(
        "/api/login", Parameters.build {
            append("username", username)
            append("password", password)
        }
    )

    suspend fun currentUser(): Result<String> = runCatching {
        val response = httpClient.get("/api/me")
        val message = response.bodyAsText()
        if (response.status.value !in 200..299) error(message.ifBlank { "Not authenticated" })
        message
    }

    private suspend fun submit(path: String, parameters: Parameters): Result<String> = runCatching {
        val response = httpClient.submitForm(path, parameters)
        val message = response.bodyAsText()
        if (response.status.value !in 200..299) error(message.ifBlank { "Request failed" })
        message
    }
}
