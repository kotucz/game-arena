package cz.kotu.game.gotfive

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import cz.kotu.game.gotfive.model.GameState
import cz.kotu.game.gotfive.model.Tile
import kotlin.reflect.KClass

class GameViewModel : ViewModel() {
    var gameState by mutableStateOf(GameState.create())
        private set

    var result by mutableStateOf("Confirm solution in notes when ready")
        private set
    var secretVisible by mutableStateOf(false)
        private set

    fun pickOfferTile(tile: Tile) {
        gameState = gameState.pickOfferTile(tile)
    }

    fun pickHintTile(tile: Tile) {
        gameState = gameState.pickHintTile(tile)
    }

    fun pickSortHint(tile: Tile) {
        gameState = gameState.pickSortHint(tile)
    }

    fun pickDotsHintSecretTile(secretTile: Tile) {
        gameState = gameState.pickDotsHintSecretTile(secretTile)
    }

    fun toggleNote(tile: Tile) {
        gameState = gameState.toggleNote(tile)
    }

    fun setNote(tile: Tile, selected: Boolean) {
        gameState = gameState.setNote(tile, selected)
    }

    fun checkSolution() {
        val notes = gameState.notes
        if (notes.size != 5) {
            result = "Solution must contain 5 tiles:"
        } else if (notes.map { it.color }.toSet().size != 5) {
            result = "Solution must contain one tile of each color"
        } else if (notes == gameState.secretTiles) {
            result = "Correct solution. Good job"
            secretVisible = true
        } else {
            result = "Sorry. Incorrect solution"
            secretVisible = true
        }
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
