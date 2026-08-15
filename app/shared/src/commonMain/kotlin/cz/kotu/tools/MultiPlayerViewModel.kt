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
    val remote: Boolean = false,
    @Assisted private val gameId: String = "initial",
    @Assisted private val debugHttpClientFactory: (String) -> HttpClient,
) : ViewModel() {
    val players = listOf(
        ContactsBoardState.Player("alice"),
        ContactsBoardState.Player("bob"),
    )

    private val localFacade = ContactsGameFacadeImpl(players)
    private val networkScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun gameFacadeForPlayer(username:String): ContactsGameFacade = if (remote) {
        NetworkContactsGameFacade(
            httpClient = debugHttpClientFactory(username),
            endpoint = authBaseUrl().trimEnd('/') + "/api",
            gameId = gameId,
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
