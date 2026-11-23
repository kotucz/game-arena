package cz.kotu.game.tricks

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.kotu.game.tricks.model.Card
import cz.kotu.game.tricks.model.TricksPlayer
import cz.kotu.game.tricks.model.TricksPlayerFacade
import cz.kotu.game.tricks.model.TricksPlayerViewState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TricksPlayerViewModel(
    val gameFacade: TricksPlayerFacade,
) : ViewModel() {

    val player: TricksPlayer = gameFacade.player

    val viewState: StateFlow<TricksPlayerViewState?> = gameFacade.viewState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    var actionInProgress by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun playCard(card: Card) {
        if (actionInProgress) return
        errorMessage = null
        viewModelScope.launch {
            actionInProgress = true
            val result = gameFacade.playCard(card)
            result.onFailure { error ->
                errorMessage = error.message ?: "Failed to play card"
            }
            actionInProgress = false
        }
    }

    fun clearError() {
        errorMessage = null
    }
}
