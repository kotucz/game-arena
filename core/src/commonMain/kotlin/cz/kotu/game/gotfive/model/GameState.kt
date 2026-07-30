package cz.kotu.game.gotfive.model

class GameState {
    var notes: Set<Tile> = Tiles.all.toSet()
        private set

    fun toggleNote(tile: Tile) {
        notes = if (tile in notes) {
            notes - tile
        } else {
            notes + tile
        }
    }
}
