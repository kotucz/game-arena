package cz.kotu.game.gotfive.model

data class GameState private constructor(
    val phase: Phase,
    val shuffledPool: List<Tile>,
    val tilePool: Set<Tile>,
    val tileOffer: List<Tile>,
    val sortTileHints: Set<Tile>,
    val dotHints: Map<Tile, List<Tile>>,
    val secretTiles: Set<Tile>,
    val notes: Set<Tile>,
) {
    sealed class Phase {
        object PickFromPool : Phase()
        object PickFromOffer : Phase()
        data class ChooseHint(val tile: Tile) : Phase()
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

    fun pickHintTile(tile: Tile): GameState =
        if ((phase is Phase.PickFromOffer || phase is Phase.ChooseHint) && tile in tileOffer) {
            copy(
                phase = Phase.ChooseHint(tile),
            )
        } else {
            this
        }

    fun pickSortHint(tile: Tile): GameState =
        if (phase is Phase.ChooseHint && phase.tile == tile && tile in tileOffer) {
            copy(
                phase = Phase.PickFromPool,
                tileOffer = tileOffer - tile,
                sortTileHints = sortTileHints + tile,
            )
        } else {
            this
        }

    fun pickDotsHintSecretTile(secretTile: Tile): GameState =
        if (phase is Phase.ChooseHint &&
            phase.tile in tileOffer &&
            secretTile in secretTiles
        ) {
            val tile = phase.tile
            copy(
                phase = Phase.PickFromPool,
                tileOffer = tileOffer - tile,
                dotHints = dotHints.withDotsHint(tile, secretTile),
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
                dotHints = emptyMap(),
                secretTiles = secretTiles,
                notes = Tiles.all.toSet(),
            )
        }
    }
}

private fun Map<Tile, List<Tile>>.withDotsHint(
    tile: Tile,
    secretTile: Tile
): Map<Tile, List<Tile>> = this + (secretTile to (getOrElse(secretTile) { emptyList() } + tile))
