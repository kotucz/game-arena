package cz.kotu.gamearena

import cz.kotu.gamearena.model.RunningGame
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

class GamesClient(private val httpClient: HttpClient = createAuthHttpClient()) {
    suspend fun runningGames(): Result<List<RunningGame>> = runCatching {
        val response = httpClient.get(endpoint("/api/games"))
        val body = response.bodyAsText()
        if (response.status.value !in 200..299) {
            error(body.ifBlank { "Could not load running games" })
        }
        Json.decodeFromString(body)
    }

    private fun endpoint(path: String): String = authBaseUrl().trimEnd('/') + path
}
