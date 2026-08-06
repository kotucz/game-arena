package cz.kotu.game.gotfive.model

data class GameState private constructor(
    val phase: Phase,
    val shuffledPool: List<Tile>,
    val tilePool: Set<Tile>,
    val tileOffer: List<Tile>,
    val sortTileHints: Set<Tile>,
    val secretTiles: Set<Tile>,
    val notes: Set<Tile>,
) {
    sealed class Phase {
        object PickFromPool : Phase()
        object PickFromOffer : Phase()
    }

    fun pickOfferTile(tile: Tile): GameState =
        if (phase is Phase.PickFromPool && tile in tilePool) {
            copy(
                phase = Phase.PickFromOffer,
                tilePool = tilePool - tile,
                tileOffer = tileOffer + tile,
            )
        } else {
            this
        }

    fun pickSortHint(tile: Tile): GameState =
        if (tile in tileOffer) {
            copy(
                phase = Phase.PickFromPool,
                tileOffer = tileOffer - tile,
                sortTileHints = sortTileHints + tile,
            )
        } else {
            this
        }

    fun toggleNote(tile: Tile): GameState = copy(
        notes = if (tile in notes) notes - tile else notes + tile,
    )

    fun setNote(tile: Tile, selected: Boolean): GameState = copy(
        notes = if (selected) notes + tile else notes - tile,
    )

    companion object {
        fun create(): GameState {
            val shuffledPool = Tiles.all.shuffled()
            var tilePool = Tiles.all.toSet()

            val tileOffer = tilePool.shuffled().take(5)
            tilePool -= tileOffer.toSet()

            val secretTiles = Color.entries
                .map { color ->
                    tilePool
                        .filter { it.color == color }
                        .random()
                        .also { tilePool -= it }
                }.toSet()

            return GameState(
                phase = Phase.PickFromPool,
                shuffledPool = shuffledPool,
                tilePool = tilePool,
                tileOffer = tileOffer,
                sortTileHints = emptySet(),
                secretTiles = secretTiles,
                notes = Tiles.all.toSet(),
            )
        }
    }
}
