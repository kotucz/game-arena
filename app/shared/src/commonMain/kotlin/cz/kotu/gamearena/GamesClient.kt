package cz.kotu.gamearena

import cz.kotu.gamearena.model.CreateGameRequest
import cz.kotu.gamearena.model.RunningGame
import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class GamesClient(private val httpClient: HttpClient) {
    suspend fun runningGames(): Result<List<RunningGame>> = runCatching {
        val response = httpClient.get(endpoint("/api/games"))
        val body = response.bodyAsText()
        if (response.status.value !in 200..299) {
            error(body.ifBlank { "Could not load running games" })
        }
        Json.decodeFromString(body)
    }

    fun observeGames(): Flow<List<RunningGame>> = channelFlow {
        httpClient.sse(endpoint("/api/games/events")) {
            incoming.collect { event ->
                event.data?.let {
                    send(Json.decodeFromString(ListSerializer(RunningGame.serializer()), it))
                }
            }
        }
    }

    suspend fun createGame(type: String, players: List<String>, config: String): Result<RunningGame> = runCatching {
        val response = httpClient.post(endpoint("/api/games")) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(Json.encodeToString(CreateGameRequest.serializer(), CreateGameRequest(type, players, config)))
        }
        val body = response.bodyAsText()
        if (response.status.value !in 200..299) {
            error(body.ifBlank { "Could not create game" })
        }
        Json.decodeFromString(body)
    }

    private fun endpoint(path: String): String = authBaseUrl().trimEnd('/') + path
}
