package cz.kotu.tools

import androidx.lifecycle.ViewModel
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsGameFacadeImpl
import cz.kotu.game.contacts.model.ContactsPlayerFacade
import cz.kotu.game.contacts.model.ContactsPlayerGameAdapter
import cz.kotu.game.contacts.model.NetworkContactsGameFacade
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias DebugHttpClientFactory = (String) -> HttpClient

data class MultiPlayerUser(
    val username: String,
)

@Inject
class MultiPlayerViewModel(
    @Assisted private val remoteGameId: String,
    @Assisted private val debugHttpClientFactory: (String) -> HttpClient,
) : ViewModel() {
    val configuredPlayers = listOf(
        MultiPlayerUser("alice"),
        MultiPlayerUser("bob"),
    )

    val players = configuredPlayers.map { ContactsBoardState.Player(it.username) }

    private val playerClients = mutableMapOf<String, HttpClient>()

    private val localFacade = ContactsGameFacadeImpl(
        players, ContactsBoardState.ContactsGameConfig(
            blueCount = 12,
            yellowCount = 4,
            redCount = 2,
        )
    )
    private val networkScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun getOrCreateClient(username: String): HttpClient {
        return playerClients.getOrPut(username) {
            debugHttpClientFactory(username)
        }
    }

    fun gameFacadeForPlayer(username: String): ContactsPlayerFacade = if (remoteGameId.isNotBlank()) {
        NetworkContactsGameFacade(
            httpClient = getOrCreateClient(username),
            gameId = remoteGameId,
            scope = networkScope,
        )
    } else {
        ContactsPlayerGameAdapter(
            players.first { it.username == username },
            localFacade,
        )

    }

    override fun onCleared() {
        networkScope.cancel()
    }
}
