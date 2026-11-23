package cz.kotu.game.tricks.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TricksPlayerGameAdapter(
    override val player: TricksPlayer,
    private val gameFacade: TricksGameFacade,
) : TricksPlayerFacade {

    override val viewState: Flow<TricksPlayerViewState>
        get() = gameFacade.gameState.map { state ->
            TricksGameRules.toPlayerViewState(state, player.id)
        }

    override suspend fun playCard(card: Card): Result<Unit> {
        return gameFacade.playCard(player.id, card)
    }
}
