package cz.kotu.game.tricks.model

import kotlinx.coroutines.flow.Flow

interface TricksPlayerFacade {
    val player: TricksPlayer
    val viewState: Flow<TricksPlayerViewState>
    suspend fun playCard(card: Card): Result<Unit>
}
