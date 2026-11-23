package cz.kotu.game.tricks.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CardTest {

    @Test
    fun testCardComparison() {
        val blue3 = Card(Suit.Blue, Rank(3))
        val blue7 = Card(Suit.Blue, Rank(7))
        val green3 = Card(Suit.Green, Rank(3))

        assertTrue(blue3 < blue7)
        assertTrue(blue3 < green3) // Suit ordinal
        assertEquals(blue3, Card(Suit.Blue, Rank(3)))
    }

    @Test
    fun testCardToString() {
        val card = Card(Suit.Blue, Rank(5))
        assertEquals("B5", card.toString())

        val trump4 = Card(Suit.Trump, Rank(4))
        assertEquals("T4", trump4.toString())
    }
}
