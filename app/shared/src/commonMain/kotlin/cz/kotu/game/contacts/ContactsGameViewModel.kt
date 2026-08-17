package cz.kotu.game.contacts

import androidx.lifecycle.ViewModel
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsGameFacade
import cz.kotu.game.contacts.model.NetworkContactsGameFacade
import cz.kotu.gamearena.AuthManager
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
    private val httpClient: HttpClient,
    authManager: AuthManager,
) : ViewModel() {
    private val networkScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Username is read once at construction time. If null the server will reject the
    // connection with 401, which the Ktor interceptor will surface as an auth prompt.
    val player = ContactsBoardState.Player(authManager.currentUsername.value ?: "")

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