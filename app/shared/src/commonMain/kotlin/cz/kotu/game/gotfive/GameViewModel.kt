package cz.kotu.game.gotfive

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cz.kotu.game.gotfive.model.GameState
import cz.kotu.game.gotfive.model.Tile
import me.tatarka.inject.annotations.Inject

@Inject
class GameViewModel : ViewModel() {
    var gameState by mutableStateOf(GameState.create())
        private set

    var result by mutableStateOf(GameResult.NotChecked)
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
            result = GameResult.FiveTilesRequired
        } else if (notes.map { it.color }.toSet().size != 5) {
            result = GameResult.OneOfEachColorRequired
        } else if (notes == gameState.secretTiles) {
            result = GameResult.Correct
            secretVisible = true
        } else {
            result = GameResult.Incorrect
            secretVisible = true
        }
    }

}

enum class GameResult {
    NotChecked,
    FiveTilesRequired,
    OneOfEachColorRequired,
    Correct,
    Incorrect,
}
