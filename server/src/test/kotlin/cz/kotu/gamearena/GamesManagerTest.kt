package cz.kotu.gamearena

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import java.time.Instant

class GamesManagerTest {

    @Test
    fun createsAndFindsContactsGameByStringId() {
        val createdAt = Instant.parse("2026-08-14T18:30:00Z")
        val manager = GamesManager(
            idGenerator = { "game-1" },
            clock = { createdAt },
        )

        val game = manager.createContactsGame()

        assertEquals("game-1", game.metadata.id)
        assertEquals(listOf("alice", "bob"), game.metadata.players)
        assertEquals(createdAt, game.metadata.createdAt)
        assertSame(game, manager.game("game-1"))
        assertEquals("contacts", manager.runningGames().single().type)
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
        assertEquals(2, manager.runningGames().size)
    }

    @Test
    fun contactsLookupDoesNotReturnOtherGameTypes() {
        val manager = GamesManager(idGenerator = { "gotfive-1" })
        val otherGame = object : ManagedGame {
            override val metadata = GameMetadata(
                id = "gotfive-1",
                type = "gotfive",
                players = listOf("alice", "bob"),
                createdAt = Instant.parse("2026-08-14T18:30:00Z"),
            )
        }

        manager.register(otherGame)

        assertNull(manager.contactsGame("gotfive-1"))
        assertEquals("gotfive", manager.runningGames().single().type)
    }
}
