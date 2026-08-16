package cz.kotu.game.contacts

import androidx.lifecycle.ViewModel
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsGameFacade
import cz.kotu.game.contacts.model.NetworkContactsGameFacade
import cz.kotu.gamearena.authBaseUrl
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@Inject
class ContactsGameViewModel(
    @Assisted private val gameId: String,
    @Assisted private val username: String,
    private val httpClient: HttpClient,
) : ViewModel() {
    private val networkScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val player = ContactsBoardState.Player(username)
    
    val gameFacade: ContactsGameFacade = NetworkContactsGameFacade(
        httpClient = httpClient,
        endpoint = authBaseUrl().trimEnd('/') + "/api",
        gameId = gameId,
        initialState = ContactsBoardState.empty(),
        scope = networkScope,
    )

    override fun onCleared() {
        networkScope.cancel()
    }
}