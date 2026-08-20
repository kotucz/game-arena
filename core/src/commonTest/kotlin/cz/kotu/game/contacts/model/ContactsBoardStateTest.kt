package cz.kotu.game.contacts.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ContactsBoardStateTest {
    @Test
    fun emptyCreatesAPlaceholderStateWithoutPlayers() {
        val state = ContactsBoardState.empty()

        assertEquals(emptyList(), state.pool)
        assertEquals(emptyList(), state.racks)
        assertEquals(emptySet(), state.solved)
        assertEquals(0, state.faults)
    }

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
        val expectedContactIds = (1..48).map { ContactsBoardState.ContactId(it) }.toSet()
        assertEquals(expectedContactIds, state.pool.map { it.id }.toSet())
        assertEquals(
            (1..12).associateWith { 4 },
            state.pool.groupingBy { it.number }.eachCount(),
        )

        assertEquals(players, state.racks.map { it.owner })
        assertEquals(listOf(12, 12, 12, 12), state.racks.map { it.contactIds.size })
        state.racks.forEach { rack ->
            assertEquals(state.contacts(rack).sorted(), state.contacts(rack))
        }
        assertEquals(
            expectedContactIds,
            state.racks.flatMap { it.contactIds }.toSet(),
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

    @Test
    fun isActionLegalReturnsNullForAValidConnect() {
        val (state, alice, bob, aliceContact, bobContact) = validationState()

        assertNull(
            state.isActionLegal(
                alice,
                ContactsBoardState.ActionType.StandardConnect,
                setOf(aliceContact),
                setOf(bobContact),
            ),
        )
    }

    @Test
    fun isActionLegalExplainsInvalidSelectionAndOwnership() {
        val (state, alice, bob, aliceContact, bobContact) = validationState()

        assertEquals(
            "Invalid number of selected contacts",
            state.isActionLegal(alice, ContactsBoardState.ActionType.StandardConnect, emptySet(), setOf(bobContact)),
        )
        assertEquals(
            "Player does not own the selected contact",
            state.isActionLegal(bob, ContactsBoardState.ActionType.StandardConnect, setOf(aliceContact), setOf(bobContact)),
        )
    }

    @Test
    fun isActionLegalExplainsSolvedAndDisallowedActions() {
        val (state, alice, _, aliceContact, bobContact) = validationState()

        assertEquals(
            "Selected contact is already solved",
            state.withSolvedContacts(aliceContact).isActionLegal(
                alice,
                ContactsBoardState.ActionType.StandardConnect,
                setOf(aliceContact),
                setOf(bobContact),
            ),
        )
        assertEquals(
            "Action type is not allowed",
            state.copy(allowedActionTypes = emptySet()).isActionLegal(
                alice,
                ContactsBoardState.ActionType.StandardConnect,
                setOf(aliceContact),
                setOf(bobContact),
            ),
        )
    }

    private fun validationState(): ValidationState {
        val alice = ContactsBoardState.Player("alice")
        val bob = ContactsBoardState.Player("bob")
        val aliceContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(1), 1)
        val bobContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(2), 2)
        return ValidationState(
            ContactsBoardState(
                pool = listOf(aliceContact, bobContact),
                racks = listOf(
                    ContactsBoardState.Rack(alice, listOf(aliceContact.id)),
                    ContactsBoardState.Rack(bob, listOf(bobContact.id)),
                ),
                solved = emptySet(),
            ),
            alice,
            bob,
            aliceContact,
            bobContact,
        )
    }

    private data class ValidationState(
        val state: ContactsBoardState,
        val alice: ContactsBoardState.Player,
        val bob: ContactsBoardState.Player,
        val aliceContact: ContactsBoardState.Contact,
        val bobContact: ContactsBoardState.Contact,
    )
}
