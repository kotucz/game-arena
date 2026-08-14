package cz.kotu.game.games.model

import cz.kotu.gamearena.model.RunningGame
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class GamesNetworkProtocolTest {

    @Test
    fun runningGameRoundTripsThroughJson() {
        val game = RunningGame(
            id = "game-1",
            type = "gotfive",
            players = listOf("alice", "bob"),
            createdAt = "2026-08-14T18:30:00Z",
        )

        val encoded = Json.encodeToString(RunningGame.serializer(), game)
        val decoded = Json.decodeFromString(RunningGame.serializer(), encoded)

        assertEquals(game, decoded)
    }
}
