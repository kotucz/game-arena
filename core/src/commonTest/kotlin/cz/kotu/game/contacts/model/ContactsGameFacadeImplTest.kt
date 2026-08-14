package cz.kotu.game.contacts.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ContactsGameFacadeImplTest {
    private val alice = ContactsBoardState.Player("alice")
    private val bob = ContactsBoardState.Player("bob")

    private val aliceContact = ContactsBoardState.Contact(id = ContactsBoardState.ContactId(1), number = 7)
    private val aliceOtherContact = ContactsBoardState.Contact(id = ContactsBoardState.ContactId(2), number = 8)
    private val bobContact = ContactsBoardState.Contact(id = ContactsBoardState.ContactId(3), number = 7)
    private val bobOtherContact = ContactsBoardState.Contact(id = ContactsBoardState.ContactId(4), number = 9)
    private val aliceMatchingContact = ContactsBoardState.Contact(id = ContactsBoardState.ContactId(5), number = 7)

    private val initialState = ContactsBoardState(
        pool = listOf(aliceContact, aliceOtherContact, bobContact, bobOtherContact, aliceMatchingContact),
        racks = listOf(
            ContactsBoardState.Rack(alice, listOf(aliceContact.id, aliceOtherContact.id, aliceMatchingContact.id)),
            ContactsBoardState.Rack(bob, listOf(bobContact.id, bobOtherContact.id)),
        ),
        solved = emptySet(),
    )

    @Test
    fun connectSolvesMatchingContactsOwnedByDifferentPlayers() {
        val facade = ContactsGameFacadeImpl(initialState)

        facade.connect(alice, aliceContact, bobContact)

        assertEquals(setOf(aliceContact.id, bobContact.id), facade.gameState.value.solved)
    }

    @Test
    fun connectRejectsContactNotOwnedByActingPlayer() {
        val facade = ContactsGameFacadeImpl(initialState)

        facade.connect(alice, bobContact, aliceContact)

        assertEquals(emptySet(), facade.gameState.value.solved)
    }

    @Test
    fun connectRejectsOtherContactOwnedByTheSamePlayer() {
        val facade = ContactsGameFacadeImpl(initialState)

        facade.connect(alice, aliceContact, aliceMatchingContact)

        assertEquals(emptySet(), facade.gameState.value.solved)
    }

    @Test
    fun connectRejectsContactsWithDifferentNumbers() {
        val facade = ContactsGameFacadeImpl(initialState)

        facade.connect(alice, aliceContact, bobOtherContact)

        assertEquals(emptySet(), facade.gameState.value.solved)
        assertEquals(1, facade.gameState.value.faults)
        assertEquals(
            bobOtherContact.number.toString(),
            facade.gameState.value.racks[1].hint(bobOtherContact),
        )
    }

    @Test
    fun connectRejectsAlreadySolvedContacts() {
        val facade = ContactsGameFacadeImpl(initialState)

        facade.connect(alice, aliceContact, bobContact)
        facade.connect(alice, aliceContact, bobContact)

        assertEquals(setOf(aliceContact.id, bobContact.id), facade.gameState.value.solved)
    }

}
