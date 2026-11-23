package cz.kotu.game.tricks.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TrickTest {

    private val p1 = TricksPlayerId("p1")
    private val p2 = TricksPlayerId("p2")
    private val p3 = TricksPlayerId("p3")

    @Test
    fun testEmptyTrickWinner() {
        val trick = Trick()
        assertNull(trick.evaluateWinner(Suit.Trump))
    }

    @Test
    fun testLeadSuitWinsWhenNoTrump() {
        val trick = Trick(
            listOf(
                PlayedCard(p1, Card(Suit.Blue, Rank(5))),
                PlayedCard(p2, Card(Suit.Blue, Rank(8))),
                PlayedCard(p3, Card(Suit.Green, Rank(9))), // Off-suit without trump
            )
        )

        val winner = trick.evaluateWinner(Suit.Trump)
        assertEquals(p2, winner?.player)
        assertEquals(Card(Suit.Blue, Rank(8)), winner?.card)
    }

    @Test
    fun testTrumpWinsOverHigherLeadSuit() {
        val trick = Trick(
            listOf(
                PlayedCard(p1, Card(Suit.Yellow, Rank(9))), // Highest non-trump
                PlayedCard(p2, Card(Suit.Trump, Rank(1))),  // Lowest trump
                PlayedCard(p3, Card(Suit.Yellow, Rank(4))),
            )
        )

        val winner = trick.evaluateWinner(Suit.Trump)
        assertEquals(p2, winner?.player)
        assertEquals(Card(Suit.Trump, Rank(1)), winner?.card)
    }

    @Test
    fun testHighestTrumpWinsWhenMultipleTrumpsPlayed() {
        val trick = Trick(
            listOf(
                PlayedCard(p1, Card(Suit.Pink, Rank(7))),
                PlayedCard(p2, Card(Suit.Trump, Rank(2))),
                PlayedCard(p3, Card(Suit.Trump, Rank(4))),
            )
        )

        val winner = trick.evaluateWinner(Suit.Trump)
        assertEquals(p3, winner?.player)
        assertEquals(Card(Suit.Trump, Rank(4)), winner?.card)
    }
}
