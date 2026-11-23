package cz.kotu.game.tricks.model

import kotlinx.serialization.Serializable

@Serializable
data class TricksGameConfig(
    val maxRank: Int = 9,
    val includeTrumpSuit: Boolean = true,
    val trumpMaxRank: Int = 4,
    val trumpSuit: Suit? = Suit.Trump,
)

@Serializable
sealed class TricksGamePhase {
    @Serializable
    data class PlayerTurn(val activePlayer: TricksPlayerId) : TricksGamePhase()

    @Serializable
    data class GameOver(val tricksWon: Map<TricksPlayerId, Int>) : TricksGamePhase()
}

@Serializable
data class TricksGameState(
    val players: List<TricksPlayer>,
    val hands: Map<TricksPlayerId, List<Card>>,
    val currentTrick: Trick = Trick(),
    val completedTricks: List<CompletedTrick> = emptyList(),
    val trumpSuit: Suit? = Suit.Trump,
    val phase: TricksGamePhase,
) {
    fun tricksWon(playerId: TricksPlayerId): Int =
        completedTricks.count { it.winner == playerId }

    fun allTricksWon(): Map<TricksPlayerId, Int> =
        players.associate { it.id to tricksWon(it.id) }
}
