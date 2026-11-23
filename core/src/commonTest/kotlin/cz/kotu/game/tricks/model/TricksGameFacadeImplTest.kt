package cz.kotu.game.tricks.model

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TricksGameFacadeImplTest {

    private val p1 = TricksPlayer(TricksPlayerId("alice"), "Alice")
    private val p2 = TricksPlayer(TricksPlayerId("bob"), "Bob")
    private val players = listOf(p1, p2)

    @Test
    fun testFacadePlayCardUpdatesState() = runTest {
        val initialState = TricksGameState(
            players = players,
            hands = mapOf(
                p1.id to listOf(Card(Suit.Blue, Rank(5))),
                p2.id to listOf(Card(Suit.Blue, Rank(8))),
            ),
            currentTrick = Trick(),
            completedTricks = emptyList(),
            trumpSuit = Suit.Trump,
            phase = TricksGamePhase.PlayerTurn(p1.id),
        )

        val facade = TricksGameFacadeImpl(initialState)
        val p1Adapter = TricksPlayerGameAdapter(p1, facade)
        val p2Adapter = TricksPlayerGameAdapter(p2, facade)

        var p1View = p1Adapter.viewState.first()
        assertTrue(p1View.isMyTurn)
        assertEquals(1, p1View.legalPlays.size)

        // P1 plays card via adapter
        val p1Result = p1Adapter.playCard(Card(Suit.Blue, Rank(5)))
        assertTrue(p1Result.isSuccess)

        var p2View = p2Adapter.viewState.first()
        assertTrue(p2View.isMyTurn)

        // P2 plays card via adapter -> trick completes, game over
        val p2Result = p2Adapter.playCard(Card(Suit.Blue, Rank(8)))
        assertTrue(p2Result.isSuccess)

        val finalState = facade.gameState.value
        assertTrue(finalState.phase is TricksGamePhase.GameOver)
        assertEquals(1, (finalState.phase as TricksGamePhase.GameOver).tricksWon[p2.id])
    }
}
