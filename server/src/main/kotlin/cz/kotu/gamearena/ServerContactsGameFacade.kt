package cz.kotu.gamearena

import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsGameFacade
import cz.kotu.game.contacts.model.ContactsNetworkAction
import io.ktor.server.sse.ServerSSESession
import io.ktor.sse.ServerSentEvent
import kotlinx.serialization.json.Json

class ServerContactsGameFacade(
    private val delegate: ContactsGameFacade,
    private val json: Json = Json { classDiscriminator = "type" },
) {
    suspend fun handleEvents(session: ServerSSESession, username: String) {
        delegate.gameState.collect { state ->
            session.send(ServerSentEvent(data = json.encodeToString(state)))
        }
    }

    fun handleAction(payload: String, username: String): String? {
        try {
            when (val action = json.decodeFromString<ContactsNetworkAction>(payload)) {
                is ContactsNetworkAction.Connect -> {
                    val state = delegate.gameState.value
                    delegate.connect(
                        ContactsBoardState.Player(username),
                        state.requireContact(action.playerContact),
                        state.requireContact(action.otherContact),
                    )
                }
            }
            return null
        } catch (error: Throwable) {
            return error.message ?: "Invalid action"
        }
    }

}
