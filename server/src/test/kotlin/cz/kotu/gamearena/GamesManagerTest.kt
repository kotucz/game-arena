package cz.kotu.gamearena

import cz.kotu.game.contacts.ai.NoopContactsAiStart
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsPlayer
import cz.kotu.game.contacts.model.ContactsGameState
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.time.Instant

class GamesManagerTest {

    private val players = listOf(
        ContactsPlayer(username = "alice", uid = "uid-alice"),
        ContactsPlayer(username = "bob", uid = "uid-bob"),
    )
    private val playerUids = players.mapNotNull(ContactsPlayer::uid)

    @Test
    fun createsAndFindsContactsGameByStringId() = runBlocking {
        val createdAt = Instant.parse("2026-08-14T18:30:00Z")
        val manager = GamesManager(
            idGenerator = { "game-1" },
            clock = { createdAt },
            userDao = mockk(),
            contactsAi = NoopContactsAiStart,
        )

        val game = manager.createContactsGameInternal(
            players = players,
            config = ContactsBoardState.ContactsGameConfig(
                blueCount = 12,
                yellowCount = 4,
                redCount = 2,
            )
        )

        assertEquals("game-1", game.metadata.id)
        assertEquals(playerUids, game.metadata.playerUids)
        assertEquals(
            players.map(ContactsPlayer::username),
            game.contactsGameFacade.gameState.value.players.map { it.username },
        )
        assertEquals(
            players,
            game.contactsGameFacade.gameState.value.players,
        )
        assertEquals(createdAt, game.metadata.createdAt)
        assertSame(game, manager.game("game-1"))
        assertEquals("contacts", manager.runningGames().single().type)
    }

    @Test
    fun notificationRecipientsAreResolvedFromTheContactsRoster() = runBlocking {
        val manager = GamesManager(
            userDao = mockk(),
            contactsAi = NoopContactsAiStart,
        )
        val game = manager.createContactsGameInternal(
            players = players,
            config = ContactsBoardState.ContactsGameConfig(),
        )

        assertEquals(
            listOf("uid-alice"),
            manager.notificationRecipientUids(
                game,
                ContactsGameState.GamePhase.StandardTurn(ContactsBoardState.Player("alice")),
            ),
        )
        assertEquals(
            playerUids,
            manager.notificationRecipientUids(
                game,
                ContactsGameState.GamePhase.GameOver("finished"),
            ),
        )
    }

    @Test
    fun createsIndependentGames() = runBlocking {
        val ids = ArrayDeque(listOf("game-1", "game-2"))
        val manager = GamesManager(
            idGenerator = { ids.removeFirst() },
            userDao = mockk(),
            contactsAi = NoopContactsAiStart,
        )

        val first = manager.createContactsGameInternal(
            players = players,
            config = ContactsBoardState.ContactsGameConfig(
                blueCount = 12,
                yellowCount = 4,
                redCount = 2,
            )
        )
        val second = manager.createContactsGameInternal(
            players = players,
            config = ContactsBoardState.ContactsGameConfig(
                blueCount = 12,
                yellowCount = 4,
                redCount = 2,
            )
        )

        assertNotSame(first, second)
        assertNotSame(first.contactsGameFacade, second.contactsGameFacade)
        assertSame(first, manager.game("game-1"))
        assertSame(second, manager.game("game-2"))
        assertNull(manager.game("missing"))
        assertEquals(2, manager.runningGames().size)
    }

    @Test
    fun contactsLookupDoesNotReturnOtherGameTypes() = runBlocking {
        val manager = GamesManager(
            idGenerator = { "gotfive-1" },
            userDao = mockk(),
            contactsAi = NoopContactsAiStart,
        )
        val otherGame = object : ManagedGame {
            override val metadata = GameMetadata(
                id = "gotfive-1",
                type = "gotfive",
                playerUids = playerUids,
                createdAt = Instant.parse("2026-08-14T18:30:00Z"),
            )
            override val statusText: Flow<String>
                get() = flowOf("status: test")
        }

        manager.register(otherGame)

        assertNull(manager.contactsGame("gotfive-1"))
        assertEquals("gotfive", manager.runningGames().single().type)
    }

    @Test
    fun runningGamesFlowEmitsUpdatesOnGameCreation() = runBlocking {
        val manager = GamesManager(
            idGenerator = { "game-1" },
            userDao = mockk(),
            contactsAi = NoopContactsAiStart,
        )
        val emissions = mutableListOf<List<RunningGameInfo>>()
        val job = launch(Dispatchers.Unconfined) {
            manager.runningGames.collect { emissions.add(it) }
        }

        assertEquals(1, emissions.size) // Initial emission
        assertEquals(0, emissions[0].size)

        manager.createContactsGameInternal(
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
