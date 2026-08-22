package cz.kotu.gamearena

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
import kotlinx.coroutines.flow.update
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
    val username = authManager.currentUsername
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

    init {
        authManager.currentUsername
            .onEach { _playersText.value = it ?: "" }
            .launchIn(viewModelScope)
    }

    suspend fun observeLobby() {
        gamesClient.observeGames()
            .retryWhen { cause, attempt ->
                // Log exception if needed
                _error.value = cause.message ?: "Could not observe games"
                // exponential delay (1s, 2s, 4s, capped at 10s)
                val delay = (1.seconds * 2.0.pow(attempt.toDouble())).coerceAtMost(10.seconds)
                delay(delay)

                // Returning true tells Kotlin Flow to retry executing the channelFlow block
                true
            }
            .collect {
                _error.value = null
                _games.value = it
            }
    }

    fun updatePlayersText(text: String) {
        _playersText.update { text }
    }

    fun updateConfigText(text: String) {
        _configText.update { text }
    }

    fun logout() {
        viewModelScope.launch {
            authManager.logout()
        }
    }

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
