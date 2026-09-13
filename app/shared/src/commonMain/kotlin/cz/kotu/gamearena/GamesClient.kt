package cz.kotu.gamearena

import cz.kotu.gamearena.model.CreateGameRequest
import cz.kotu.gamearena.model.RunningGame
import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.SSEClientException
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class GamesClient(private val httpClient: HttpClient) {
    suspend fun runningGames(): Result<List<RunningGame>> = runCatching {
        val response = httpClient.get("/api/games")
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            error(body.ifBlank { "Could not load running games" })
        }
        Json.decodeFromString(body)
    }

    /**
     * Streams the running-games list via Server-Sent Events.
     *
     * - **Normal EOF**: the SSE stream can end silently (TCP connection dropped by the server).
     *   The loop reconnects immediately in that case — no delay, no exception propagated.
     * - **HTTP 401**: Ktor throws [SSEClientException]; this is translated to
     *   [UnauthorizedException] so [GamesViewModel] can suspend via [AuthManager.awaitLogin].
     * - **Other errors**: propagated as-is for the caller's retry/backoff logic.
     */
    fun observeGames(): Flow<List<RunningGame>> = flow {
        while (currentCoroutineContext().isActive) {
            try {
                httpClient.sse("/api/games/events") {
                    incoming.collect { event ->
                        event.data?.let {
                            emit(Json.decodeFromString(ListSerializer(RunningGame.serializer()), it))
                        }
                    }
                }
                // Reached here after a silent EOF — loop to reconnect.
            } catch (e: SSEClientException) {
                if (e.response?.status == HttpStatusCode.Unauthorized) {
                    throw UnauthorizedException()
                }
                throw e
            }
        }
    }

    suspend fun createGame(type: String, players: List<String>, config: String): Result<RunningGame> = runCatching {
        val response = httpClient.post("/api/games") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(Json.encodeToString(CreateGameRequest.serializer(), CreateGameRequest(type, players, config)))
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            error(body.ifBlank { "Could not create game" })
        }
        Json.decodeFromString(body)
    }
}
