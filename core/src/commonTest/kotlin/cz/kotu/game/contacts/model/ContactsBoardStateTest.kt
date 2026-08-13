package cz.kotu.game.contacts.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ContactsBoardStateTest {
    @Test
    fun createBuildsAndDistributesTheFullContactPool() {
        val players = listOf(
            ContactsBoardState.Player("alice"),
            ContactsBoardState.Player("bob"),
            ContactsBoardState.Player("carol"),
            ContactsBoardState.Player("dave"),
        )

        val state = ContactsBoardState.create(players)

        assertEquals(48, state.pool.size)
        assertEquals((1..48).toSet(), state.pool.map { it.id }.toSet())
        assertEquals(
            (1..12).associateWith { 4 },
            state.pool.groupingBy { it.number }.eachCount(),
        )

        assertEquals(players, state.racks.map { it.owner })
        assertEquals(listOf(12, 12, 12, 12), state.racks.map { it.contacts.size })
        assertEquals(
            state.pool.map { it.id }.toSet(),
            state.racks.flatMap { it.contacts }.map { it.id }.toSet(),
        )
        assertEquals(emptySet(), state.solved)
    }

    @Test
    fun createAssignsOwnersCyclicallyWhenThereAreFewerThanFourPlayers() {
        val players = listOf(
            ContactsBoardState.Player("alice"),
            ContactsBoardState.Player("bob"),
        )

        val state = ContactsBoardState.create(players)

        assertEquals(
            listOf(players[0], players[1], players[0], players[1]),
            state.racks.map { it.owner },
        )
    }
}
