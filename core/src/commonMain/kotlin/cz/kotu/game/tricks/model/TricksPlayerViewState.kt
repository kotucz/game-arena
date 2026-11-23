package cz.kotu.game.tricks.model

import kotlinx.serialization.Serializable

@Serializable
data class OpponentPlayerView(
    val player: TricksPlayer,
    val cardCount: Int,
    val tricksWon: Int,
)

@Serializable
data class TricksPlayerViewState(
    val myPlayer: TricksPlayer,
    val myHand: List<Card>,
    val opponents: List<OpponentPlayerView>,
    val currentTrick: Trick,
    val completedTricks: List<CompletedTrick>,
    val trumpSuit: Suit?,
    val phase: TricksGamePhase,
    val legalPlays: Set<Card>,
) {
    val isMyTurn: Boolean
        get() = phase is TricksGamePhase.PlayerTurn && phase.activePlayer == myPlayer.id
}
