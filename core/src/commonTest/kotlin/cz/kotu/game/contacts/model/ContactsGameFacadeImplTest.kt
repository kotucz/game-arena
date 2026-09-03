package cz.kotu.game.contacts.model

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContactsGameFacadeImplTest {
    private val alice = ContactsBoardState.Player("alice")
    private val bob = ContactsBoardState.Player("bob")

    private val aliceContact = ContactsBoardState.Contact(id = ContactsBoardState.ContactId(1), number = 7)
    private val aliceOtherContact = ContactsBoardState.Contact(id = ContactsBoardState.ContactId(2), number = 8)
    private val bobContact = ContactsBoardState.Contact(id = ContactsBoardState.ContactId(3), number = 7)
    private val bobOtherContact = ContactsBoardState.Contact(id = ContactsBoardState.ContactId(4), number = 9)

    private val initialState = ContactsGameState(
        ContactsBoardState(
            pool = listOf(aliceContact, aliceOtherContact, bobContact, bobOtherContact),
            racks = listOf(
                ContactsBoardState.Rack(alice, listOf(aliceContact.id, aliceOtherContact.id)),
                ContactsBoardState.Rack(bob, listOf(bobContact.id, bobOtherContact.id)),
            ),
            solved = emptySet(),
        ),
        players = listOf(alice, bob),
        activePlayer = alice,
    )

    @Test
    fun connectSolvesMatchingContactsOwnedByDifferentPlayers() = runTest {
        val facade = ContactsGameFacadeImpl(initialState)

        val result =
            facade.action(alice, ContactsBoardState.ActionType.StandardConnect, setOf(aliceContact), setOf(bobContact))

        assertTrue(result.isSuccess)
        assertEquals(setOf(aliceContact.id, bobContact.id), facade.gameState.value.board.solved)
    }

    @Test
    fun connectRejectsContactNotOwnedByActingPlayer() = runTest {
        val facade = ContactsGameFacadeImpl(initialState)

        val result =
            facade.action(alice, ContactsBoardState.ActionType.StandardConnect, setOf(bobContact), setOf(aliceContact))

        assertTrue(result.isFailure)
        assertEquals("Player does not own the selected contact", result.exceptionOrNull()?.message)
        assertEquals(emptySet(), facade.gameState.value.board.solved)
    }

    @Test
    fun connectRejectsContactsWithDifferentNumbers() = runTest {
        val facade = ContactsGameFacadeImpl(initialState)

        val result = facade.action(
            alice,
            ContactsBoardState.ActionType.StandardConnect,
            setOf(aliceContact),
            setOf(bobOtherContact)
        )

        assertTrue(result.isSuccess)
        assertEquals(emptySet(), facade.gameState.value.board.solved)
        assertEquals(1, facade.gameState.value.board.faults)
        assertEquals(
            bobOtherContact.number.toString(),
            facade.gameState.value.board.racks[1].hint(bobOtherContact),
        )
    }
}
