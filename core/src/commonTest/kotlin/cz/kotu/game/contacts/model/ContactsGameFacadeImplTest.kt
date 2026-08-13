package cz.kotu.game.contacts.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ContactsGameFacadeImplTest {
    private val alice = ContactsBoardState.Player("alice")
    private val bob = ContactsBoardState.Player("bob")

    private val aliceContact = ContactsBoardState.Contact(id = 1, number = 7)
    private val aliceOtherContact = ContactsBoardState.Contact(id = 2, number = 8)
    private val bobContact = ContactsBoardState.Contact(id = 3, number = 7)
    private val bobOtherContact = ContactsBoardState.Contact(id = 4, number = 9)
    private val aliceMatchingContact = ContactsBoardState.Contact(id = 5, number = 7)

    private val initialState = ContactsBoardState(
        pool = listOf(aliceContact, aliceOtherContact, bobContact, bobOtherContact, aliceMatchingContact),
        racks = listOf(
            ContactsBoardState.Rack(alice, listOf(aliceContact, aliceOtherContact, aliceMatchingContact)),
            ContactsBoardState.Rack(bob, listOf(bobContact, bobOtherContact)),
        ),
        solved = emptySet(),
    )

    @Test
    fun connectSolvesMatchingContactsOwnedByDifferentPlayers() {
        val facade = ContactsGameFacadeImpl(initialState)

        facade.action(alice, connectAction(aliceContact, bobContact))

        assertEquals(setOf(aliceContact, bobContact), facade.gameState.value.solved)
    }

    @Test
    fun connectRejectsContactNotOwnedByActingPlayer() {
        val facade = ContactsGameFacadeImpl(initialState)

        facade.action(alice, connectAction(bobContact, aliceContact))

        assertEquals(emptySet(), facade.gameState.value.solved)
    }

    @Test
    fun connectRejectsOtherContactOwnedByTheSamePlayer() {
        val facade = ContactsGameFacadeImpl(initialState)

        facade.action(alice, connectAction(aliceContact, aliceMatchingContact))

        assertEquals(emptySet(), facade.gameState.value.solved)
    }

    @Test
    fun connectRejectsContactsWithDifferentNumbers() {
        val facade = ContactsGameFacadeImpl(initialState)

        facade.action(alice, connectAction(aliceContact, bobOtherContact))

        assertEquals(emptySet(), facade.gameState.value.solved)
        assertEquals(1, facade.gameState.value.faults)
        assertEquals(
            bobOtherContact.number.toString(),
            facade.gameState.value.racks[1].hints[bobOtherContact],
        )
    }

    @Test
    fun connectRejectsAlreadySolvedContacts() {
        val facade = ContactsGameFacadeImpl(initialState)

        facade.action(alice, connectAction(aliceContact, bobContact))
        facade.action(alice, connectAction(aliceContact, bobContact))

        assertEquals(setOf(aliceContact, bobContact), facade.gameState.value.solved)
    }

    private fun connectAction(
        playerContact: ContactsBoardState.Contact,
        otherContact: ContactsBoardState.Contact,
    ) = ContactsGameFacade.Action.Connect(playerContact, otherContact)
}
