package cz.kotu.game.tricks.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TricksGameRulesTest {

    private val p1 = TricksPlayer(TricksPlayerId("alice"), "Alice")
    private val p2 = TricksPlayer(TricksPlayerId("bob"), "Bob")
    private val players = listOf(p1, p2)

    @Test
    fun testGenerateDeck() {
        val config = TricksGameConfig(maxRank = 9, includeTrumpSuit = true, trumpMaxRank = 4)
        val deck = TricksGameRules.generateDeck(config)
        // 4 color suits * 9 + 4 trump = 40 cards
        assertEquals(40, deck.size)
        assertEquals(4, deck.count { it.suit == Suit.Trump })
        assertEquals(9, deck.count { it.suit == Suit.Blue })
    }

    @Test
    fun testFollowSuitValidation() {
        val state = TricksGameState(
            players = players,
            hands = mapOf(
                p1.id to listOf(Card(Suit.Blue, Rank(3)), Card(Suit.Green, Rank(5))),
                p2.id to listOf(Card(Suit.Blue, Rank(7)), Card(Suit.Yellow, Rank(2))),
            ),
            currentTrick = Trick(
                listOf(PlayedCard(p1.id, Card(Suit.Blue, Rank(3)))),
            ),
            trumpSuit = Suit.Trump,
            phase = TricksGamePhase.PlayerTurn(p2.id),
        )

        // Bob has Blue 7 and Yellow 2. Lead is Blue 3.
        // Bob must play Blue. Playing Yellow 2 should fail.
        val illegalError = TricksGameRules.isPlayLegal(state, p2.id, Card(Suit.Yellow, Rank(2)))
        assertNotNull(illegalError)
        assertTrue(illegalError.contains("Must follow lead suit"))

        // Playing Blue 7 is legal
        val legalCheck = TricksGameRules.isPlayLegal(state, p2.id, Card(Suit.Blue, Rank(7)))
        assertNull(legalCheck)

        // Legal plays for Bob should only be Blue 7
        val legalPlays = TricksGameRules.legalPlays(state, p2.id)
        assertEquals(setOf(Card(Suit.Blue, Rank(7))), legalPlays)
    }

    @Test
    fun testCanPlayAnyCardWhenVoidInLeadSuit() {
        val state = TricksGameState(
            players = players,
            hands = mapOf(
                p1.id to listOf(Card(Suit.Blue, Rank(3))),
                p2.id to listOf(Card(Suit.Green, Rank(7)), Card(Suit.Trump, Rank(1))),
            ),
            currentTrick = Trick(
                listOf(PlayedCard(p1.id, Card(Suit.Blue, Rank(3)))),
            ),
            trumpSuit = Suit.Trump,
            phase = TricksGamePhase.PlayerTurn(p2.id),
        )

        // Bob has no Blue cards. Bob may play Green 7 or Trump 1.
        assertNull(TricksGameRules.isPlayLegal(state, p2.id, Card(Suit.Green, Rank(7))))
        assertNull(TricksGameRules.isPlayLegal(state, p2.id, Card(Suit.Trump, Rank(1))))

        val legalPlays = TricksGameRules.legalPlays(state, p2.id)
        assertEquals(2, legalPlays.size)
    }

    @Test
    fun testPlayCardCompletesTrickAndAdvancesRound() {
        // Deterministic setup: each player has 2 cards
        val state = TricksGameState(
            players = players,
            hands = mapOf(
                p1.id to listOf(Card(Suit.Blue, Rank(5)), Card(Suit.Yellow, Rank(3))),
                p2.id to listOf(Card(Suit.Blue, Rank(8)), Card(Suit.Yellow, Rank(9))),
            ),
            currentTrick = Trick(),
            completedTricks = emptyList(),
            trumpSuit = Suit.Trump,
            phase = TricksGamePhase.PlayerTurn(p1.id),
        )

        // P1 plays Blue 5
        val stateAfterP1 = TricksGameRules.playCard(state, p1.id, Card(Suit.Blue, Rank(5))).getOrThrow()
        assertEquals(TricksGamePhase.PlayerTurn(p2.id), stateAfterP1.phase)
        assertEquals(1, stateAfterP1.currentTrick.plays.size)
        assertEquals(1, stateAfterP1.hands.getValue(p1.id).size)

        // P2 plays Blue 8 -> P2 wins trick
        val stateAfterP2 = TricksGameRules.playCard(stateAfterP1, p2.id, Card(Suit.Blue, Rank(8))).getOrThrow()
        assertEquals(0, stateAfterP2.currentTrick.plays.size)
        assertEquals(1, stateAfterP2.completedTricks.size)
        assertEquals(p2.id, stateAfterP2.completedTricks.first().winner)
        // P2 won the trick, so P2 leads next trick!
        assertEquals(TricksGamePhase.PlayerTurn(p2.id), stateAfterP2.phase)

        // Trick 2: P2 leads Yellow 9
        val stateAfterP2Lead = TricksGameRules.playCard(stateAfterP2, p2.id, Card(Suit.Yellow, Rank(9))).getOrThrow()
        assertEquals(TricksGamePhase.PlayerTurn(p1.id), stateAfterP2Lead.phase)

        // P1 plays Yellow 3 -> P2 wins again and game is over (all cards played)
        val finalState = TricksGameRules.playCard(stateAfterP2Lead, p1.id, Card(Suit.Yellow, Rank(3))).getOrThrow()
        assertTrue(finalState.phase is TricksGamePhase.GameOver)
        val gameOver = finalState.phase as TricksGamePhase.GameOver
        assertEquals(2, gameOver.tricksWon[p2.id])
        assertEquals(0, gameOver.tricksWon[p1.id])
    }

    @Test
    fun testDataSanitizationInPlayerViewState() {
        val state = TricksGameState(
            players = players,
            hands = mapOf(
                p1.id to listOf(Card(Suit.Blue, Rank(5)), Card(Suit.Pink, Rank(2))),
                p2.id to listOf(Card(Suit.Green, Rank(8)), Card(Suit.Yellow, Rank(4))),
            ),
            currentTrick = Trick(),
            completedTricks = emptyList(),
            trumpSuit = Suit.Trump,
            phase = TricksGamePhase.PlayerTurn(p1.id),
        )

        val p1View = TricksGameRules.toPlayerViewState(state, p1.id)
        // P1 sees their own hand
        assertEquals(listOf(Card(Suit.Blue, Rank(5)), Card(Suit.Pink, Rank(2))), p1View.myHand)
        assertEquals(players.map { it.id }, p1View.playerOrder)
        assertTrue(p1View.isMyTurn)

        // P1 only sees Bob's card count (2), NOT Bob's actual cards!
        assertEquals(1, p1View.opponents.size)
        val bobView = p1View.opponents.first()
        assertEquals(p2.id, bobView.player.id)
        assertEquals(2, bobView.cardCount)
        assertEquals(0, bobView.tricksWon)
    }
}
