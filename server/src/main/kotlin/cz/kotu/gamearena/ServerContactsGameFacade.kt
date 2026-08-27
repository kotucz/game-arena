package cz.kotu.gamearena

import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsGameFacade
import cz.kotu.game.contacts.model.ContactsNetworkAction
import cz.kotu.game.contacts.model.GameLogEntry
import io.ktor.server.sse.ServerSSESession
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

class ServerContactsGameFacade(
    private val delegate: ContactsGameFacade,
    private val json: Json = Json { classDiscriminator = "type" },
) {
    val gameState: StateFlow<ContactsBoardState> = delegate.gameState
    val logs: StateFlow<List<GameLogEntry>> = delegate.logs
    suspend fun handleEvents(session: ServerSSESession, username: String) {
        delegate.gameState.collect { state ->
            session.send(ServerSentEvent(data = json.encodeToString(state)))
        }
    }

    suspend fun handleLogs(
        session: ServerSSESession,
        username: String,
        lastSentLogIndex: Int = -1,
    ) {
        session.send(ServerSentEvent(comments = "start"))
        var nextLogIndex = lastSentLogIndex
        delegate.logs.collect { logs ->
            for (i in (nextLogIndex + 1) until logs.size) {
                session.send(ServerSentEvent(data = json.encodeToString(logs[i])))
            }
            nextLogIndex = logs.size - 1
        }
    }

    fun handleAction(payload: String, username: String): String? {
        try {
            when (val action = json.decodeFromString<ContactsNetworkAction>(payload)) {
                is ContactsNetworkAction.Action -> {
                    val state = delegate.gameState.value
                    delegate.action(
                        player = ContactsBoardState.Player(username),
                        actionType = action.actionType,
                        playerContacts = action.playerContacts.map { state.requireContact(it) }.toSet(),
                        otherContacts = action.otherContacts.map { state.requireContact(it) }.toSet(),
                    )
                }
            }
            return null
        } catch (error: Throwable) {
            return error.message ?: "Invalid action"
        }
    }
}
