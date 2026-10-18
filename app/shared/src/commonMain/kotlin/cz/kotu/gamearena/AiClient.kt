package cz.kotu.gamearena

import cz.kotu.game.contacts.ai.AiCommandRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class AiClient(private val httpClient: HttpClient) {
    suspend fun command(
        gameId: String,
        playerUsername: String,
        prompt: String,
    ): Result<Unit> = runCatching {
        require(prompt.isNotBlank()) { "Prompt must not be empty" }
        val response = httpClient.post("/api/games/$gameId/ai/$playerUsername/command") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(AiCommandRequest.serializer(), AiCommandRequest(prompt)))
        }
        if (!response.status.isSuccess()) {
            error(response.bodyAsText().ifBlank { "AI command failed: ${response.status}" })
        }
    }
}

class AiPlayerAdapter(
    private val aiClient: AiClient,
    private val gameId: String,
    private val aiPlayerId: String,
) {
    suspend fun command(prompt: String): Result<Unit> = aiClient.command(gameId, aiPlayerId, prompt)

    companion object {
        const val GEMINI_AI_PLAYER_USERNAME = "ai:gemini"
    }
}
