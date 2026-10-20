package cz.kotu.gamearena

import cz.kotu.gamearena.model.AvailablePlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayerPickerViewModel(
    private val gamesClient: GamesClient,
    private val scope: CoroutineScope,
) {
    private val _availablePlayers = MutableStateFlow<List<AvailablePlayer>?>(null)
    val availablePlayers: StateFlow<List<AvailablePlayer>?> = _availablePlayers.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _filter = MutableStateFlow("")
    val filter: StateFlow<String> = _filter.asStateFlow()

    val filteredPlayers: StateFlow<List<AvailablePlayer>> = combine(_availablePlayers, _filter) { players, filter ->
        players.orEmpty().filter { it.username.lowercase().contains(filter.trim().lowercase()) }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun prepareForOpen() {
        _filter.value = ""
        loadPlayers()
    }

    fun updateFilter(filter: String) {
        _filter.value = filter
    }

    fun loadPlayers() {
        if (_availablePlayers.value != null || _loading.value) return

        scope.launch {
            _loading.value = true
            _error.value = null
            gamesClient.availablePlayers().fold(
                onSuccess = { _availablePlayers.value = it },
                onFailure = { _error.value = it.message ?: "Could not load players" },
            )
            _loading.value = false
        }
    }
}
