package cz.kotu.game.tricks.model

import kotlinx.serialization.Serializable

@Serializable
data class PlayedCard(
    val player: TricksPlayerId,
    val card: Card,
)

@Serializable
data class Trick(
    val plays: List<PlayedCard> = emptyList(),
) {
    val leadPlay: PlayedCard? get() = plays.firstOrNull()
    val leadSuit: Suit? get() = leadPlay?.card?.suit

    fun evaluateWinner(trumpSuit: Suit? = Suit.Trump): PlayedCard? {
        val lead = leadSuit ?: return null
        val trumps = if (trumpSuit != null) {
            plays.filter { it.card.suit == trumpSuit }
        } else {
            emptyList()
        }

        return if (trumps.isNotEmpty()) {
            trumps.maxByOrNull { it.card.rank }
        } else {
            plays.filter { it.card.suit == lead }.maxByOrNull { it.card.rank }
        }
    }
}

@Serializable
data class CompletedTrick(
    val trick: Trick,
    val winner: TricksPlayerId,
)
