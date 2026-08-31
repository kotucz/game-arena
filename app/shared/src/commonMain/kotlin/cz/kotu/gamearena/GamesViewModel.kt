package cz.kotu.gamearena

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.gamearena.model.RunningGame
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds

@Inject
class GamesViewModel(
    private val gamesClient: GamesClient,
    private val authManager: AuthManager,
) : ViewModel() {
    val username: StateFlow<String?> = authManager.currentUsername

    private val _games = MutableStateFlow<List<RunningGame>?>(null)
    val games: StateFlow<List<RunningGame>?> = _games.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _creatingGame = MutableStateFlow(false)
    val creatingGame: StateFlow<Boolean> = _creatingGame.asStateFlow()

    private val _playersText = MutableStateFlow("")
    val playersText: StateFlow<String> = _playersText.asStateFlow()

    private val _configText = MutableStateFlow(
        Json.encodeToString(
            ContactsBoardState.ContactsGameConfig.serializer(),
            ContactsBoardState.ContactsGameConfig(
                blueCount = 8,
                yellowCount = 4,
                redCount = 2,
            )
        )
    )
    val configText: StateFlow<String> = _configText.asStateFlow()

    val createGameDialogVisible = mutableStateOf(false)

    val gameFilterMyAll = mutableStateOf(GameFilterTab.My)

    init {
        // Trigger the lazy /me check so the username appears as soon as the screen loads.
        viewModelScope.launch { authManager.ensureLoaded() }

        authManager.currentUsername
//        authManager.authState
//            .map { (it as? AuthState.Authorized)?.username }
            .onEach { _playersText.value = it ?: "" }
            .launchIn(viewModelScope)
    }

    suspend fun observeLobby() {
        gamesClient.observeGames()
            .retryWhen { cause, attempt ->
                when {
                    cause is UnauthorizedException -> {
                        // Session expired: surface the error and wait for the user to log in.
                        // The Ktor interceptor has already emitted to unauthorizedEvents which
                        // shows the login modal; we just suspend here until it completes.
                        _error.value = "Session expired — please log in again"
                        authManager.awaitLogin()
                        _error.value = null
                        true
                    }

                    else -> {
                        // Network / server error: exponential back-off capped at 30 s.
                        _error.value = cause.message ?: "Could not observe games"
                        val backoff = minOf(30.seconds, 1.seconds * 2.0.pow(attempt.toInt()))
                        delay(backoff)
                        true
                    }
                }
            }
            .collect { games ->
                _error.value = null
                _games.value = games
            }
    }

    fun updatePlayersText(text: String) {
        _playersText.value = text
    }

    fun updateConfigText(text: String) {
        _configText.value = text
    }

    fun logout() {
        viewModelScope.launch {
            authManager.logout()
        }
    }

    /** Called by the "Try again" button to clear a displayed error and let the collector retry. */
    fun loadGames() {
        _error.value = null
    }

    fun createGame(onSuccess: (RunningGame) -> Unit) {
        viewModelScope.launch {
            _creatingGame.value = true
            _error.value = null
            val username = authManager.currentUsername.value
            val players = _playersText.value.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .ifEmpty { listOfNotNull(username) }
            gamesClient.createGame(type = "contacts", players = players, config = _configText.value).fold(
                onSuccess = { game ->
                    _creatingGame.value = false
                    onSuccess(game)
                },
                onFailure = {
                    _error.value = it.message ?: "Could not create game"
                    _creatingGame.value = false
                },
            )
        }
    }
}
