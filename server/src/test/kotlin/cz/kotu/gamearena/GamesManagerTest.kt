package cz.kotu.gamearena

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

class GamesManagerTest {

    @Test
    fun createsAndFindsContactsGameByStringId() {
        val manager = GamesManager(idGenerator = { "game-1" })

        val game = manager.createContactsGame()

        assertEquals("game-1", game.id)
        assertSame(game, manager.game("game-1"))
    }

    @Test
    fun createsIndependentGames() {
        val ids = ArrayDeque(listOf("game-1", "game-2"))
        val manager = GamesManager(idGenerator = { ids.removeFirst() })

        val first = manager.createContactsGame()
        val second = manager.createContactsGame()

        assertNotSame(first, second)
        assertNotSame(first.contacts, second.contacts)
        assertSame(first, manager.game("game-1"))
        assertSame(second, manager.game("game-2"))
        assertNull(manager.game("missing"))
    }
}
