package cz.kotu.game.games.model

import cz.kotu.game.contacts.model.GameLogEntry
import cz.kotu.game.contacts.model.ContactsGameDetails
import cz.kotu.game.contacts.model.ContactsGamePlayer
import cz.kotu.gamearena.model.RunningGame
import cz.kotu.gamearena.model.RunningGamePlayer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class GamesNetworkProtocolTest {

    @Test
    fun runningGameRoundTripsThroughJson() {
        val game = RunningGame(
            id = "game-1",
            type = "gotfive",
            players = listOf(
                RunningGamePlayer(uid = "uid-alice", username = "alice", pictureUrl = "https://example.com/alice.png"),
                RunningGamePlayer(username = "bob"),
            ),
            createdAt = Instant.parse("2026-08-14T18:30:00Z"),
            status = "status: test",
        )

        val encoded = Json.encodeToString(RunningGame.serializer(), game)
        val decoded = Json.decodeFromString(RunningGame.serializer(), encoded)

        assertEquals(game, decoded)
    }

    @Test
    fun gameLogEntryRoundTripsThroughJson() {
        val log = GameLogEntry(
            timestamp = Instant.parse("2026-08-14T18:30:00Z"),
            text = "alice: StandardConnect [1] other: [?]",
        )

        val encoded = Json.encodeToString(GameLogEntry.serializer(), log)
        val decoded = Json.decodeFromString(GameLogEntry.serializer(), encoded)

        assertEquals(log, decoded)
    }

    @Test
    fun contactsGameDetailsRoundTripsThroughJson() {
        val details = ContactsGameDetails(
            gameId = "game-1",
            players = listOf(
                ContactsGamePlayer("uid-alice", "alice", "https://example.com/alice.png"),
                ContactsGamePlayer(null, "guest", null),
            ),
        )

        val encoded = Json.encodeToString(ContactsGameDetails.serializer(), details)
        val decoded = Json.decodeFromString(ContactsGameDetails.serializer(), encoded)

        assertEquals(details, decoded)
    }
}
