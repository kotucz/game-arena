package cz.kotu.gamearena

import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.gamearena.model.RunningGame
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

class GamesManagerTest {

    private val players = listOf("alice", "bob")

    @Test
    fun createsAndFindsContactsGameByStringId() {
        val createdAt = Instant.parse("2026-08-14T18:30:00Z")
        val manager = GamesManager(
            idGenerator = { "game-1" },
            clock = { createdAt },
        )

        val game = manager.createContactsGame(
            players = players,
            config = ContactsBoardState.ContactsGameConfig(
                blueCount = 12,
                yellowCount = 4,
                redCount = 2,
            )
        )

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

        val first = manager.createContactsGame(
            players = players,
            config = ContactsBoardState.ContactsGameConfig(
                blueCount = 12,
                yellowCount = 4,
                redCount = 2,
            )
        )
        val second = manager.createContactsGame(
            players = players,
            config = ContactsBoardState.ContactsGameConfig(
                blueCount = 12,
                yellowCount = 4,
                redCount = 2,
            )
        )

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

    @Test
    fun runningGamesFlowEmitsUpdatesOnGameCreation() = runBlocking {
        val manager = GamesManager(idGenerator = { "game-1" })
        val emissions = mutableListOf<List<RunningGame>>()
        val job = launch(kotlinx.coroutines.Dispatchers.Unconfined) {
            manager.runningGames.collect { emissions.add(it) }
        }

        assertEquals(1, emissions.size) // Initial emission
        assertEquals(0, emissions[0].size)

        manager.createContactsGame(
            players = players,
            config = ContactsBoardState.ContactsGameConfig(
                blueCount = 12,
                yellowCount = 4,
                redCount = 2,
            )
        )

        assertEquals(2, emissions.size)
        assertEquals(1, emissions[1].size)
        assertEquals("game-1", emissions[1][0].id)
        job.cancel()
    }
}
