package cz.kotu.game.tricks.model

import kotlinx.coroutines.flow.StateFlow

interface TricksGameFacade {
    val gameState: StateFlow<TricksGameState>
    suspend fun playCard(player: TricksPlayerId, card: Card): Result<Unit>
}
