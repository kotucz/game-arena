package cz.kotu.game.contacts.model

import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
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
import kotlinx.coroutines.launch
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
) : ContactsGameFacade {

    private val gameEndpoint: String = endpoint.trimEnd('/') + "/games/" + gameId + "/contacts"
    private val eventsEndpoint: String = gameEndpoint + "/events"
    private val logsEndpoint: String = gameEndpoint.removeSuffix("/contacts") + "/logs"
    private val actionsEndpoint: String = gameEndpoint + "/actions"

    override val gameState: StateFlow<ContactsBoardState> =
        gameEvents().stateIn(scope, SharingStarted.WhileSubscribed(5.seconds), initialState)

    private val _logs: MutableStateFlow<List<GameLogEntry>> = MutableStateFlow(listOf())

    override val logs: StateFlow<List<GameLogEntry>> = combine(
        _logs,
        gameLogs().stateIn(scope, SharingStarted.WhileSubscribed(5.seconds), Unit)
    ) { logsList, _ -> logsList }
        .stateIn(scope, SharingStarted.WhileSubscribed(5.seconds), _logs.value)

    override fun action(
        player: ContactsBoardState.Player,
        actionType: ContactsBoardState.ActionType,
        playerContacts: Set<ContactsBoardState.Contact>,
        otherContacts: Set<ContactsBoardState.Contact>,
    ) {
        scope.launch {
            runCatching {
                httpClient.post(actionsEndpoint) {
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
                }.also { response ->
                    if (response.status.value !in 200..299) error("Action failed: ${response.status}")
                }
            }.onFailure(::onError)
        }
    }

    private fun gameLogs(): Flow<Unit> = flow {
        while (currentCoroutineContext().isActive) {
            try {
                httpClient.sse(logsEndpoint) {
                    logLocal("Log events connected")
                    incoming.collect { event ->
                        event.data?.let { data ->
                            _logs.value += json.decodeFromString<GameLogEntry>(data)
                        }
                    }
                    onError(IllegalStateException("Incoming logs finished unexpectedly"))
                }
            } catch (error: Throwable) {
                onError(error)
                delay(1000.milliseconds)
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
                    onError(IllegalStateException("Incoming game events finished unexpectedly"))
                }
            } catch (error: Throwable) {
                onError(error)
                delay(1000.milliseconds)
            }
        }
    }

    private fun onError(error: Throwable) {
        logLocal(error.stackTraceToString())
    }

    private fun logLocal(text: String) {
        _logs.value += GameLogEntry(Clock.System.now().toEpochMilliseconds(), text)
    }

}