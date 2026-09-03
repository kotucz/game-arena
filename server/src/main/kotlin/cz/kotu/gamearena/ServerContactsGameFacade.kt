package cz.kotu.gamearena

import cz.kotu.game.contacts.model.ContactsNetworkAction
import cz.kotu.game.contacts.model.ContactsPlayerFacade
import io.ktor.server.sse.ServerSSESession
import io.ktor.sse.ServerSentEvent
import kotlinx.serialization.json.Json

class ServerContactsGameFacade(
    private val delegate: ContactsPlayerFacade,
    private val json: Json = Json { classDiscriminator = "type" },
) {

    suspend fun handleEvents(session: ServerSSESession) {
        delegate.gameState.collect { state ->
            session.send(ServerSentEvent(data = json.encodeToString(state)))
        }
    }

    suspend fun handleLogs(
        session: ServerSSESession,
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

    suspend fun handleAction(payload: String): Result<Unit> {
        return try {
            val action = json.decodeFromString<ContactsNetworkAction>(payload)
            when (action) {
                is ContactsNetworkAction.Action -> {
                    val state = delegate.gameState.value
                    delegate.action(
                        actionType = action.actionType,
                        playerContacts = action.playerContacts.map { state.board.requireContact(it) }.toSet(),
                        otherContacts = action.otherContacts.map { state.board.requireContact(it) }.toSet(),
                    )
                }
            }
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }
}
