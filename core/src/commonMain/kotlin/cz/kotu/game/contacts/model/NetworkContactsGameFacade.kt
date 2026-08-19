package cz.kotu.game.contacts.model

import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class NetworkContactsGameFacade(
    private val httpClient: HttpClient,
    private val endpoint: String,
    private val gameId: String,
    initialState: ContactsBoardState,
    private val scope: CoroutineScope,
    private val json: Json = Json { ignoreUnknownKeys = true; classDiscriminator = "type" },
    private val onError: (Throwable) -> Unit = {},
) : ContactsGameFacade {

    private val gameEndpoint: String = endpoint.trimEnd('/') + "/games/" + gameId + "/contacts"
    private val eventsEndpoint: String = gameEndpoint + "/events"
    private val logsEndpoint: String = gameEndpoint.removeSuffix("/contacts") + "/logs"
    private val actionsEndpoint: String = gameEndpoint + "/actions"

    private val _gameState = MutableStateFlow(initialState)
    override val gameState: StateFlow<ContactsBoardState> = _gameState.asStateFlow()

    private val _logs: MutableStateFlow<List<GameLogEntry>> = MutableStateFlow(listOf())
    override val logs: StateFlow<List<GameLogEntry>> = _logs.asStateFlow()

    init {
        scope.launch { runSession() }
        scope.launch { runLogs() }
    }

    override fun connect(
        player: ContactsBoardState.Player,
        playerContact: ContactsBoardState.Contact,
        otherContact: ContactsBoardState.Contact,
    ) {
        scope.launch {
            runCatching {
                httpClient.post(actionsEndpoint) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        json.encodeToString(
                            ContactsNetworkAction.serializer(),
                            ContactsNetworkAction.Connect(playerContact.id, otherContact.id)
                        )
                    )
                }.also { response ->
                    if (response.status.value !in 200..299) error("Action failed: ${response.status}")
                }
            }.onFailure(onError)
        }
    }

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
            }.onFailure(onError)
        }
    }

    private suspend fun runLogs() {
        try {
            httpClient.sse(logsEndpoint) {
                incoming.collect { event ->
                    event.data?.let { data ->
                        _logs.value += json.decodeFromString<GameLogEntry>(data)
                    }
                }
            }
        } catch (error: Throwable) {
            onError(error)
        }
    }

    private suspend fun runSession() {
        try {
            httpClient.sse(eventsEndpoint) {
                incoming.collect { event ->
                    event.data?.let { data ->
                        _gameState.value = json.decodeFromString<ContactsBoardState>(data)
                    }
                }
            }
        } catch (error: Throwable) {
            onError(error)
        }
    }

}