package cz.kotu.game.tricks.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TricksGameFacadeImpl(
    initialState: TricksGameState,
) : TricksGameFacade {

    constructor(
        players: List<TricksPlayer>,
        config: TricksGameConfig = TricksGameConfig(),
        deckShuffler: (List<Card>) -> List<Card> = { it.shuffled() },
        startingPlayerId: TricksPlayerId? = null,
    ) : this(
        initialState = TricksGameRules.initialGameState(
            players = players,
            config = config,
            deckShuffler = deckShuffler,
            startingPlayerId = startingPlayerId,
        ),
    )

    private val _gameState = MutableStateFlow(initialState)
    override val gameState: StateFlow<TricksGameState> = _gameState.asStateFlow()

    private val mutex = Mutex()

    override suspend fun playCard(player: TricksPlayerId, card: Card): Result<Unit> = mutex.withLock {
        val current = _gameState.value
        val result = TricksGameRules.playCard(current, player, card)
        result.map { newState ->
            _gameState.value = newState
        }
    }
}
