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
    var gameState by mutableStateOf(GameState.create())
        private set

    fun pickOfferTile(tile: Tile) {
        gameState = gameState.pickOfferTile(tile)
    }

    fun pickSortHint(tile: Tile) {
        gameState = gameState.pickSortHint(tile)
    }

    fun toggleNote(tile: Tile) {
        gameState = gameState.toggleNote(tile)
    }

    fun setNote(tile: Tile, selected: Boolean) {
        gameState = gameState.setNote(tile, selected)
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
