package cz.kotu.game.contacts

import androidx.lifecycle.ViewModel
import cz.kotu.game.contacts.model.ContactsGameDetails
import cz.kotu.game.contacts.model.ContactsPlayerFacade
import cz.kotu.game.contacts.model.NetworkContactsGameFacade
import cz.kotu.gamearena.AuthManager
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

data class ContactsGameDetailsUiState(
    val details: ContactsGameDetails? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@Inject
class ContactsGameViewModel(
    @Assisted private val gameId: String,
    private val httpClient: HttpClient,
    private val authManager: AuthManager,
) : ViewModel() {
    private val networkScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val username = authManager.currentUsername

    private val _gameNotFound = MutableStateFlow(false)
    val gameNotFound: StateFlow<Boolean> = _gameNotFound.asStateFlow()

    private val _gameDetails = MutableStateFlow(ContactsGameDetailsUiState())
    val gameDetails: StateFlow<ContactsGameDetailsUiState> = _gameDetails.asStateFlow()

    val gameFacade: ContactsPlayerFacade = NetworkContactsGameFacade(
        httpClient = httpClient,
        gameId = gameId,
        scope = networkScope,
        onGameNotFound = { _gameNotFound.value = true },
        awaitLogin = { authManager.awaitLogin() },
    )

    private val json = Json { ignoreUnknownKeys = true }

    init {
        loadGameDetails()
    }

    fun loadGameDetails() {
        networkScope.launch {
            _gameDetails.value = _gameDetails.value.copy(isLoading = true, error = null)
            try {
                var response = httpClient.get("/api/games/$gameId/contacts")
                if (response.status == HttpStatusCode.Unauthorized) {
                    authManager.awaitLogin()
                    response = httpClient.get("/api/games/$gameId/contacts")
                }
                if (response.status == HttpStatusCode.NotFound) {
                    _gameNotFound.value = true
                    _gameDetails.value = ContactsGameDetailsUiState(isLoading = false)
                    return@launch
                }
                if (!response.status.isSuccess()) {
                    val detail = response.bodyAsText().ifBlank {
                        "Could not load game details: ${response.status}"
                    }
                    error(detail)
                }
                _gameDetails.value = ContactsGameDetailsUiState(
                    details = json.decodeFromString<ContactsGameDetails>(response.bodyAsText()),
                    isLoading = false,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _gameDetails.value = ContactsGameDetailsUiState(
                    isLoading = false,
                    error = error.message ?: "Could not load player details",
                )
            }
        }
    }

    override fun onCleared() {
        networkScope.cancel()
    }
}
