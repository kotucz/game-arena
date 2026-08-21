package cz.kotu.tools

import androidx.lifecycle.ViewModel
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsGameFacade
import cz.kotu.game.contacts.model.ContactsGameFacadeImpl
import cz.kotu.game.contacts.model.NetworkContactsGameFacade
import cz.kotu.gamearena.authBaseUrl
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias DebugHttpClientFactory = (String) -> HttpClient

@Inject
class MultiPlayerViewModel(
    @Assisted private val remoteGameId: String,
    @Assisted private val debugHttpClientFactory: (String) -> HttpClient,
) : ViewModel() {
    val players = listOf(
        ContactsBoardState.Player("alice"),
        ContactsBoardState.Player("bob"),
    )

    private val localFacade = ContactsGameFacadeImpl(
        players, ContactsBoardState.ContactsGameConfig(
            blueCount = 12,
            yellowCount = 4,
            redCount = 2,
        )
    )
    private val networkScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun gameFacadeForPlayer(username:String): ContactsGameFacade = if (remoteGameId.isNotBlank()) {
        NetworkContactsGameFacade(
            httpClient = debugHttpClientFactory(username),
            endpoint = authBaseUrl().trimEnd('/') + "/api",
            gameId = remoteGameId,
            initialState = localFacade.gameState.value,
            scope = networkScope,
        )
    } else {
        localFacade
    }

    override fun onCleared() {
        networkScope.cancel()
    }
}
