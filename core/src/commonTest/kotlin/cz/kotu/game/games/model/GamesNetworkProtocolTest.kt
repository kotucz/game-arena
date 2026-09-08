package cz.kotu.game.games.model

import cz.kotu.game.contacts.model.GameLogEntry
import cz.kotu.gamearena.model.RunningGame
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
            players = listOf("alice", "bob"),
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
}
