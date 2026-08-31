package cz.kotu.game.contacts.model

import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.SSEClientException
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class NetworkContactsGameFacade(
    private val httpClient: HttpClient,
    private val endpoint: String,
    private val gameId: String,
    initialState: ContactsBoardState,
    private val scope: CoroutineScope,
    private val json: Json = Json { ignoreUnknownKeys = true; classDiscriminator = "type" },
    private val onGameNotFound: (() -> Unit)? = null,
    private val awaitLogin: (suspend () -> Unit)? = null,
) : ContactsGameFacade {

    private val gameEndpoint: String = endpoint.trimEnd('/') + "/games/" + gameId + "/contacts"
    private val eventsEndpoint: String = gameEndpoint + "/events"
    private val logsEndpoint: String = gameEndpoint.removeSuffix("/contacts") + "/logs"
    private val actionsEndpoint: String = gameEndpoint + "/actions"

    override val gameState: StateFlow<ContactsBoardState> =
        gameEvents().stateIn(scope, SharingStarted.WhileSubscribed(5.seconds), initialState)

    private val _logs: MutableStateFlow<List<GameLogEntry>> = MutableStateFlow(listOf())
    private var lastSentLogIndex: Int = -1

    override val logs: StateFlow<List<GameLogEntry>> = combine(
        _logs,
        gameLogs().stateIn(scope, SharingStarted.WhileSubscribed(5.seconds), Unit)
    ) { logsList, _ -> logsList }
        .stateIn(scope, SharingStarted.WhileSubscribed(5.seconds), _logs.value)

    override suspend fun action(
        player: ContactsBoardState.Player,
        actionType: ContactsBoardState.ActionType,
        playerContacts: Set<ContactsBoardState.Contact>,
        otherContacts: Set<ContactsBoardState.Contact>,
    ): Result<Unit> {
        return runCatching {
            val response = httpClient.post(actionsEndpoint) {
                contentType(ContentType.Application.Json)
                setBody(
                    json.encodeToString(
                        ContactsNetworkAction.serializer(),
                        ContactsNetworkAction.Action(
                            actionType = actionType,
                            playerContacts = playerContacts.map { it.id }.toSet(),
                            otherContacts = otherContacts.map { it.id }.toSet(),
                        )
                    )
                )
            }
            if (response.status == HttpStatusCode.NotFound) {
                onGameNotFound?.invoke()
                error("Game not found")
            }
            if (!response.status.isSuccess()) {
                val detail = response.bodyAsText().ifBlank { "Action failed: ${response.status}" }
                error(detail)
            }
        }.onFailure(::onError)
    }

    private suspend fun handleSseError(error: Throwable): Boolean {
        if (error is SSEClientException) {
            when (error.response?.status) {
                HttpStatusCode.NotFound -> {
                    logLocal("Game not found (404)")
                    onGameNotFound?.invoke()
                    return false
                }
                HttpStatusCode.Unauthorized -> {
                    logLocal("Session unauthorized (401)")
                    if (awaitLogin != null) {
                        awaitLogin.invoke()
                    } else {
                        delay(1000.milliseconds)
                    }
                    return true
                }
                else -> {
                    onError(error)
                    delay(1000.milliseconds)
                    return true
                }
            }
        }
        onError(error)
        delay(1000.milliseconds)
        return true
    }

    private fun gameLogs(): Flow<Unit> = flow {
        while (currentCoroutineContext().isActive) {
            try {
                val connectAfterIndex = maxOf(lastSentLogIndex, _logs.value.lastIndex)
                val subscriptionUrl = if (connectAfterIndex >= 0) {
                    "$logsEndpoint?lastSentLogIndex=$connectAfterIndex"
                } else {
                    logsEndpoint
                }
                httpClient.sse(subscriptionUrl) {
                    logLocal("Log events connected")
                    incoming.collect { event ->
                        event.data?.let { data ->
                            val parsed = json.decodeFromString<GameLogEntry>(data)
                            _logs.value += parsed
                            lastSentLogIndex = _logs.value.lastIndex
                        }
                    }
                }
                logLocal("Incoming logs finished unexpectedly")
            } catch (error: Throwable) {
                val shouldRetry = handleSseError(error)
                if (!shouldRetry) return@flow
            }
        }
    }

    private fun gameEvents(): Flow<ContactsBoardState> = flow {
        while (currentCoroutineContext().isActive) {
            try {
                httpClient.sse(eventsEndpoint) {
                    logLocal("Game events connected")
                    incoming.collect { event ->
                        event.data?.let { data ->
                            emit(json.decodeFromString<ContactsBoardState>(data))
                        }
                    }
                }
                logLocal("Incoming game events finished unexpectedly")
            } catch (error: Throwable) {
                val shouldRetry = handleSseError(error)
                if (!shouldRetry) return@flow
            }
        }
    }

    private fun onError(error: Throwable) {
        logLocal("Error: " + error.message)
        logLocal(error.stackTraceToString())
    }

    private fun logLocal(text: String) {
        _logs.value += GameLogEntry(Clock.System.now(), text)
        lastSentLogIndex = _logs.value.lastIndex
    }

}
