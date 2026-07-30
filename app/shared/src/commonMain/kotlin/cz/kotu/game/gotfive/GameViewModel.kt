package cz.kotu.game.gotfive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cz.kotu.game.gotfive.model.GameState
import cz.kotu.game.gotfive.model.Tile
import kotlin.reflect.KClass

class GameViewModel : ViewModel() {
    val gameState = GameState()

    var notes by mutableStateOf(gameState.notes)
        private set

    fun toggleNote(tile: Tile) {
        gameState.toggleNote(tile)
        notes = gameState.notes
    }
}

object GameViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: KClass<T>,
        extras: CreationExtras,
    ): T {
        if (modelClass == GameViewModel::class) {
            return GameViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.simpleName}")
    }
}
