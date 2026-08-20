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
    private val aliceMatchingSoloContact = ContactsBoardState.Contact(id = ContactsBoardState.ContactId(6), number = 8)

    private val initialState = ContactsBoardState(
        pool = listOf(aliceContact, aliceOtherContact, bobContact, bobOtherContact, aliceMatchingContact, aliceMatchingSoloContact),
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

    @Test
    fun myDoubleConnectSolvesWhenEitherPlayerContactMatches() {
        val facade = ContactsGameFacadeImpl(initialState)

        facade.action(
            player = alice,
            actionType = ContactsBoardState.ActionType.MyDoubleConnect,
            playerContacts = setOf(aliceOtherContact, aliceContact),
            otherContacts = setOf(bobContact),
        )

        assertEquals(setOf(aliceContact.id, bobContact.id), facade.gameState.value.solved)
        assertEquals(0, facade.gameState.value.faults)
    }

    @Test
    fun myDoubleConnectUsesStandardConnectResultWhenNeitherContactMatches() {
        val facade = ContactsGameFacadeImpl(initialState)

        facade.action(
            player = alice,
            actionType = ContactsBoardState.ActionType.MyDoubleConnect,
            playerContacts = setOf(aliceOtherContact, aliceMatchingContact),
            otherContacts = setOf(bobOtherContact),
        )

        assertEquals(emptySet(), facade.gameState.value.solved)
        assertEquals(1, facade.gameState.value.faults)
        assertEquals(
            bobOtherContact.number.toString(),
            facade.gameState.value.racks[1].hint(bobOtherContact),
        )
    }

    @Test
    fun targetPlayerResolvesMultiConnectUsingStandardConnectResult() {
        val facade = ContactsGameFacadeImpl(initialState)

        facade.multiConnect(
            player = alice,
            actionType = ContactsBoardState.ActionType.DoubleConnect,
            playerContact = aliceContact,
            otherContacts = setOf(bobContact, bobOtherContact),
        )
        facade.action(bob, ContactsBoardState.ActionType.ResolveMultiConnect, setOf(bobContact), emptySet())

        assertEquals(setOf(aliceContact.id, bobContact.id), facade.gameState.value.solved)
        assertEquals(null, facade.gameState.value.resolveMultiConnect)
    }

    @Test
    fun targetPlayerResolvesMismatchUsingStandardConnectResult() {
        val facade = ContactsGameFacadeImpl(initialState)

        facade.multiConnect(
            player = alice,
            actionType = ContactsBoardState.ActionType.DoubleConnect,
            playerContact = aliceContact,
            otherContacts = setOf(bobContact, bobOtherContact),
        )
        facade.action(bob, ContactsBoardState.ActionType.ResolveMultiConnect, setOf(bobOtherContact), emptySet())

        assertEquals(emptySet(), facade.gameState.value.solved)
        assertEquals(1, facade.gameState.value.faults)
        assertEquals(bobOtherContact.number.toString(), facade.gameState.value.racks[1].hint(bobOtherContact))
        assertEquals(null, facade.gameState.value.resolveMultiConnect)
    }

    @Test
    fun onlyTargetPlayerCanResolveMultiConnect() {
        val facade = ContactsGameFacadeImpl(initialState)

        facade.multiConnect(
            player = alice,
            actionType = ContactsBoardState.ActionType.DoubleConnect,
            playerContact = aliceContact,
            otherContacts = setOf(bobContact, bobOtherContact),
        )
        facade.action(alice, ContactsBoardState.ActionType.ResolveMultiConnect, setOf(bobContact), emptySet())

        assertEquals(emptySet(), facade.gameState.value.solved)
        assertEquals(2, facade.gameState.value.resolveMultiConnect?.targetContacts?.size)
    }

    @Test
    fun soloConnectRestSolvesAllRemainingSameNumberContactsAcrossRacks() {
        val state = initialState.copy(
            racks = listOf(
                ContactsBoardState.Rack(alice, listOf(aliceContact.id, aliceOtherContact.id, aliceMatchingSoloContact.id)),
                ContactsBoardState.Rack(alice, listOf(aliceMatchingContact.id)),
                ContactsBoardState.Rack(bob, listOf(bobContact.id, bobOtherContact.id)),
            ),
        )
        val facade = ContactsGameFacadeImpl(state)

        facade.action(
            alice,
            ContactsBoardState.ActionType.SoloConnectRest,
            setOf(aliceOtherContact, aliceMatchingSoloContact),
            emptySet(),
        )

        assertEquals(setOf(aliceOtherContact.id, aliceMatchingSoloContact.id), facade.gameState.value.solved)
    }

    @Test
    fun soloConnectRestRejectsIncompleteSelection() {
        val facade = ContactsGameFacadeImpl(initialState)

        facade.action(
            alice,
            ContactsBoardState.ActionType.SoloConnectRest,
            setOf(aliceContact),
            emptySet(),
        )

        assertEquals(emptySet(), facade.gameState.value.solved)
    }

    @Test
    fun finishRedsSolvesAllRemainingRedContacts() {
        val redContact = aliceContact.copy(type = ContactsBoardState.ContactType.Red)
        val otherRedContact = bobContact.copy(type = ContactsBoardState.ContactType.Red)
        val state = initialState.copy(
            pool = listOf(redContact, otherRedContact, aliceOtherContact),
            racks = listOf(
                ContactsBoardState.Rack(alice, listOf(redContact.id, otherRedContact.id)),
                ContactsBoardState.Rack(bob, listOf(aliceOtherContact.id)),
            ),
            solved = setOf(aliceOtherContact.id),
        )
        val facade = ContactsGameFacadeImpl(state)

        facade.action(
            alice,
            ContactsBoardState.ActionType.FinishReds,
            setOf(redContact, otherRedContact),
            emptySet(),
        )

        assertEquals(setOf(aliceOtherContact.id, redContact.id, otherRedContact.id), facade.gameState.value.solved)
    }

    @Test
    fun finishRedsRejectsWhenAnyOtherUnsolvedContactRemains() {
        val redContact = aliceContact.copy(type = ContactsBoardState.ContactType.Red)
        val state = initialState.copy(
            pool = listOf(redContact, aliceOtherContact),
            racks = listOf(
                ContactsBoardState.Rack(alice, listOf(redContact.id, aliceOtherContact.id)),
            ),
        )
        val facade = ContactsGameFacadeImpl(state)

        facade.action(
            alice,
            ContactsBoardState.ActionType.FinishReds,
            setOf(redContact),
            emptySet(),
        )

        assertEquals(emptySet(), facade.gameState.value.solved)
    }

}
