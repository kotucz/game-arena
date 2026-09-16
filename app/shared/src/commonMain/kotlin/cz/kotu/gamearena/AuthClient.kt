package cz.kotu.gamearena

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class AuthClient(private val httpClient: HttpClient) {
    suspend fun register(username: String, email: String, password: String): Result<String> = submit(
        "/api/register",
        Parameters.build {
            append("username", username)
            append("email", email)
            append("password", password)
        },
    )

    suspend fun login(username: String, password: String): Result<String> = submit(
        "/api/login",
        Parameters.build {
            append("username", username)
            append("password", password)
        },
    )

    suspend fun loginWithFirebase(
        idToken: String,
        username: String? = null,
        email: String? = null,
    ): Result<String> = submit(
        "/api/auth/firebase",
        Parameters.build {
            append("idToken", idToken)
            if (!username.isNullOrBlank()) append("username", username)
            if (!email.isNullOrBlank()) append("email", email)
        },
    )

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

    private suspend fun submit(path: String, parameters: Parameters): Result<String> = runCatching {
        val response = httpClient.submitForm(path, parameters)
        val message = response.bodyAsText()
        if (!response.status.isSuccess()) error(message.ifBlank { "Request failed" })
        message
    }.onFailure { error ->
        Napier.e("Authentication request failed", error)
    }
}
