package cz.kotu.game.contacts

import androidx.lifecycle.ViewModel
import cz.kotu.game.contacts.model.ContactsPlayerFacade
import cz.kotu.game.contacts.model.NetworkContactsGameFacade
import cz.kotu.gamearena.ApiBaseUrl
import cz.kotu.gamearena.AuthManager
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@Inject
class ContactsGameViewModel(
    @Assisted private val gameId: String,
    private val apiBaseUrl: ApiBaseUrl,
    private val httpClient: HttpClient,
    authManager: AuthManager,
) : ViewModel() {
    private val networkScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val username = authManager.currentUsername

    private val _gameNotFound = MutableStateFlow(false)
    val gameNotFound: StateFlow<Boolean> = _gameNotFound.asStateFlow()

    val gameFacade: ContactsPlayerFacade = NetworkContactsGameFacade(
        httpClient = httpClient,
        endpoint = apiBaseUrl.endpoint("/api"),
        gameId = gameId,
        scope = networkScope,
        onGameNotFound = { _gameNotFound.value = true },
        awaitLogin = { authManager.awaitLogin() },
    )

    override fun onCleared() {
        networkScope.cancel()
    }
}
