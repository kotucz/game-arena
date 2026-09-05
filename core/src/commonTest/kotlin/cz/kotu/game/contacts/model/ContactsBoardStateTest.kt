package cz.kotu.game.contacts.model

import cz.kotu.game.contacts.model.ContactsBoardState.ActionType
import cz.kotu.game.contacts.model.ContactsBoardState.Contact
import cz.kotu.game.contacts.model.ContactsBoardState.ContactId
import cz.kotu.game.contacts.model.ContactsBoardState.ContactType
import cz.kotu.game.contacts.model.ContactsBoardState.Player
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

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
            Player("alice"),
            Player("bob"),
            Player("carol"),
            Player("dave"),
        )

        val state = ContactsBoardState.create(players, ContactsBoardState.ContactsGameConfig())

        assertEquals(48, state.pool.size)
        val expectedContactIds = (1..48).map { ContactId(it) }.toSet()
        assertEquals(expectedContactIds, state.pool.map { it.id }.toSet())
        assertEquals(
            (1..12).associateWith { 4 },
            state.pool.groupingBy { it.number }.eachCount(),
        )

        assertEquals(players, state.racks.map { it.owner })
        assertEquals(listOf(12, 12, 12, 12), state.racks.map { it.contactIds.size })
        state.racks.forEach { rack ->
            assertEquals(state.rackContacts(rack).sorted(), state.rackContacts(rack))
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
            Player("alice"),
            Player("bob"),
        )

        val state = ContactsBoardState.create(players, ContactsBoardState.ContactsGameConfig())

        assertEquals(
            listOf(players[0], players[1], players[0], players[1]),
            state.racks.map { it.owner },
        )
    }

    @Test
    fun createAddsConfiguredSpecialContactsWithUniqueNumbersInRange() {
        val state = ContactsBoardState.create(
            players = listOf(Player("alice")),
            ContactsBoardState.ContactsGameConfig(
                yellowCount = 3,
                redCount = 2,
            ),
        )

        assertEquals(3, state.pool.count { it.type == ContactType.Yellow })
        assertEquals(2, state.pool.count { it.type == ContactType.Red })
        val specialNumbers = state.pool.filter { it.type != ContactType.Blue }.map { it.number }
        assertEquals(specialNumbers.size, specialNumbers.toSet().size)
        assertEquals(true, specialNumbers.all { it in 1..12 })
        assertEquals(48, state.pool.count { it.type == ContactType.Blue })
        assertEquals(
            (1..12).associateWith { 4 },
            state.pool.filter { it.type == ContactType.Blue }
                .groupingBy { it.number }
                .eachCount(),
        )
    }

    @Test
    fun contactsWithEqualNumbersAreSortedByContactType() {
        val contacts = ContactType.entries.mapIndexed { index, type ->
            Contact(
                id = ContactId(index + 1),
                number = 1,
                type = type,
            )
        }.reversed()

        assertEquals(
            ContactType.entries,
            contacts.sorted().map { it.type },
        )
    }

    @Test
    fun contactsMatchRequiresTheSameType() {
        val state = ContactsBoardState.empty()
        val blue = Contact(ContactId(1), 7, ContactType.Blue)
        val yellow =
            Contact(ContactId(2), 7, ContactType.Yellow)

        assertEquals(false, state.contactsMatch(blue, yellow))
    }

    @Test
    fun contactMatchKeyUsesYForYellowAndNumberForOtherContacts() {
        assertEquals(
            "Y",
            Contact(ContactId(1), 7, ContactType.Yellow).matchKey,
        )
        assertEquals(
            "7",
            Contact(ContactId(2), 7, ContactType.Blue).matchKey,
        )
        assertEquals(
            "R",
            Contact(ContactId(3), 7, ContactType.Red).matchKey,
        )
    }

    @Test
    fun blueContactsMatchOnlyWhenNumbersMatch() {
        val state = ContactsBoardState.empty()
        val first = Contact(ContactId(1), 7, ContactType.Blue)
        val sameNumber =
            Contact(ContactId(2), 7, ContactType.Blue)
        val differentNumber =
            Contact(ContactId(3), 8, ContactType.Blue)

        assertEquals(true, state.contactsMatch(first, sameNumber))
        assertEquals(false, state.contactsMatch(first, differentNumber))
    }

    @Test
    fun yellowContactsMatchRegardlessOfNumber() {
        val state = ContactsBoardState.empty()
        val first =
            Contact(ContactId(1), 7, ContactType.Yellow)
        val second =
            Contact(ContactId(2), 8, ContactType.Yellow)

        assertEquals(true, state.contactsMatch(first, second))
    }

    @Test
    fun isActionLegalReturnsNullForAValidConnect() {
        val (state, alice, _, aliceContact, bobContact) = validationState()

        val result = state.applyAction(
            alice,
            ActionType.StandardConnect,
            setOf(aliceContact),
            setOf(bobContact),
        )
        assertTrue(result is ActionExecutionResult.Success)
    }

    @Test
    fun isActionLegalExplainsInvalidSelectionAndOwnership() {
        val (state, alice, bob, aliceContact, bobContact) = validationState()

        val result1 = state.applyAction(alice, ActionType.StandardConnect, emptySet(), setOf(bobContact))
        assertIs<ActionExecutionResult.Failure>(result1)
        assertEquals("Invalid number of selected contacts: player (0/1) other (1/1)", result1.message)

        val result2 = state.applyAction(bob, ActionType.StandardConnect, setOf(aliceContact), setOf(bobContact))
        assertIs<ActionExecutionResult.Failure>(result2)
        assertEquals("Player does not own the selected contact", result2.message)
    }

    @Test
    fun errorSelectingSolvedContact() {
        val (state, alice, _, aliceContact, bobContact) = validationState()

        val result1 = state.withSolvedContacts(aliceContact).applyAction(
            alice,
            ActionType.StandardConnect,
            setOf(aliceContact),
            setOf(bobContact),
        )
        assertIs<ActionExecutionResult.Failure>(result1)
        assertEquals("Selected contact is already solved", result1.message)
    }

    @Test
    fun soloConnectRestRequiresContactsWithTheSameNumberOrAllYellow() {
        val alice = Player("alice")
        val blue = Contact(
            ContactId(1),
            number = 1,
            type = ContactType.Blue,
        )
        val red = Contact(
            ContactId(2),
            number = 2,
            type = ContactType.Blue,
        )
        val state = ContactsBoardState(
            pool = listOf(blue, red),
            racks = listOf(ContactsBoardState.Rack(alice, listOf(blue.id, red.id))),
            solved = emptySet(),
        )

        val result = state.applyAction(
            alice,
            ActionType.SoloConnectRest,
            setOf(blue, red),
            emptySet(),
        )
        assertIs<ActionExecutionResult.Failure>(result)
        assertEquals("Selected contacts must have the same number or all yellow", result.message)
    }

    @Test
    fun myDoubleConnectSolvesWhenEitherPlayerContactMatches() {
        val alice = Player("alice")
        val bob = Player("bob")
        val aliceContact = Contact(ContactId(1), 7)
        val aliceOtherContact = Contact(ContactId(2), 8)
        val bobContact = Contact(ContactId(3), 7)
        val state = ContactsBoardState(
            pool = listOf(aliceContact, aliceOtherContact, bobContact),
            racks = listOf(
                ContactsBoardState.Rack(alice, listOf(aliceContact.id, aliceOtherContact.id)),
                ContactsBoardState.Rack(bob, listOf(bobContact.id)),
            ),
            solved = emptySet(),
        )

        val result = state.applyAction(
            alice,
            ActionType.MyDoubleConnect,
            setOf(aliceOtherContact, aliceContact),
            setOf(bobContact),
        )

        assertIs<ActionExecutionResult.Success>(result)
        assertEquals(setOf(aliceContact.id, bobContact.id), result.state.solved)
        assertEquals(0, result.state.faults)
    }

    @Test
    fun myDoubleConnectUsesStandardConnectResultWhenNeitherContactMatches() {
        val alice = Player("alice")
        val bob = Player("bob")
        val aliceOtherContact = Contact(ContactId(2), 8)
        val aliceMatchingContact = Contact(ContactId(5), 7)
        val bobOtherContact = Contact(ContactId(4), 9)
        val state = ContactsBoardState(
            pool = listOf(aliceOtherContact, aliceMatchingContact, bobOtherContact),
            racks = listOf(
                ContactsBoardState.Rack(alice, listOf(aliceOtherContact.id, aliceMatchingContact.id)),
                ContactsBoardState.Rack(bob, listOf(bobOtherContact.id)),
            ),
            solved = emptySet(),
        )

        val result = state.applyAction(
            alice,
            ActionType.MyDoubleConnect,
            setOf(aliceOtherContact, aliceMatchingContact),
            setOf(bobOtherContact),
        )

        assertIs<ActionExecutionResult.Success>(result)
        assertEquals(emptySet(), result.state.solved)
        assertEquals(1, result.state.faults)
        assertEquals(bobOtherContact.number.toString(), result.state.racks[1].hint(bobOtherContact))
    }

    @Test
    fun targetPlayerResolvesMultiConnectUsingStandardConnectResult() {
        val alice = Player("alice")
        val bob = Player("bob")
        val aliceContact = Contact(ContactId(1), 7)
        val bobContact = Contact(ContactId(3), 7)
        val bobOtherContact = Contact(ContactId(4), 9)
        val state = ContactsBoardState(
            pool = listOf(aliceContact, bobContact, bobOtherContact),
            racks = listOf(
                ContactsBoardState.Rack(alice, listOf(aliceContact.id)),
                ContactsBoardState.Rack(bob, listOf(bobContact.id, bobOtherContact.id)),
            ),
            solved = emptySet(),
        )

        val first = state.applyAction(alice, ActionType.DoubleConnect, setOf(aliceContact), setOf(bobContact, bobOtherContact))
        assertIs<ActionExecutionResult.Success>(first)

        val second = first.state.applyAction(bob, ActionType.ResolveMultiConnect, setOf(bobContact), emptySet())

        assertIs<ActionExecutionResult.Success>(second)
        assertEquals(setOf(aliceContact.id, bobContact.id), second.state.solved)
        assertEquals(null, second.state.resolveMultiConnect)
    }

    @Test
    fun targetPlayerResolvesMismatchUsingStandardConnectResult() {
        val alice = Player("alice")
        val bob = Player("bob")
        val aliceContact = Contact(ContactId(1), 7)
        val bobContact = Contact(ContactId(3), 7)
        val bobOtherContact = Contact(ContactId(4), 9)
        val state = ContactsBoardState(
            pool = listOf(aliceContact, bobContact, bobOtherContact),
            racks = listOf(
                ContactsBoardState.Rack(alice, listOf(aliceContact.id)),
                ContactsBoardState.Rack(bob, listOf(bobContact.id, bobOtherContact.id)),
            ),
            solved = emptySet(),
        )

        val first = state.applyAction(alice, ActionType.DoubleConnect, setOf(aliceContact), setOf(bobContact, bobOtherContact))
        assertIs<ActionExecutionResult.Success>(first)

        val second = first.state.applyAction(bob, ActionType.ResolveMultiConnect, setOf(bobOtherContact), emptySet())

        assertIs<ActionExecutionResult.Success>(second)
        assertEquals(emptySet(), second.state.solved)
        assertEquals(1, second.state.faults)
        assertEquals(bobOtherContact.number.toString(), second.state.racks[1].hint(bobOtherContact))
        assertEquals(null, second.state.resolveMultiConnect)
    }

    @Test
    fun onlyTargetPlayerCanResolveMultiConnect() {
        val alice = Player("alice")
        val bob = Player("bob")
        val aliceContact = Contact(ContactId(1), 7)
        val bobContact = Contact(ContactId(3), 7)
        val bobOtherContact = Contact(ContactId(4), 9)
        val state = ContactsBoardState(
            pool = listOf(aliceContact, bobContact, bobOtherContact),
            racks = listOf(
                ContactsBoardState.Rack(alice, listOf(aliceContact.id)),
                ContactsBoardState.Rack(bob, listOf(bobContact.id, bobOtherContact.id)),
            ),
            solved = emptySet(),
        )

        val first = state.applyAction(alice, ActionType.DoubleConnect, setOf(aliceContact), setOf(bobContact, bobOtherContact))
        assertIs<ActionExecutionResult.Success>(first)

        val second = first.state.applyAction(alice, ActionType.ResolveMultiConnect, setOf(bobContact), emptySet())

        assertIs<ActionExecutionResult.Failure>(second)
        assertEquals("Only the target player can resolve the multi-connect", second.message)
        assertEquals(2, first.state.resolveMultiConnect?.targetContacts?.size)
    }

    @Test
    fun soloConnectRestSolvesAllRemainingSameNumberContactsAcrossRacks() {
        val alice = Player("alice")
        val aliceContact = Contact(ContactId(1), 7)
        val aliceOtherContact = Contact(ContactId(2), 8)
        val aliceMatchingContact = Contact(ContactId(5), 7)
        val aliceMatchingSoloContact = Contact(ContactId(6), 8)
        val bob = Player("bob")
        val bobContact = Contact(ContactId(3), 7)
        val bobOtherContact = Contact(ContactId(4), 9)
        val state = ContactsBoardState(
            pool = listOf(aliceContact, aliceOtherContact, aliceMatchingContact, aliceMatchingSoloContact, bobContact, bobOtherContact),
            racks = listOf(
                ContactsBoardState.Rack(alice, listOf(aliceContact.id, aliceOtherContact.id, aliceMatchingSoloContact.id)),
                ContactsBoardState.Rack(alice, listOf(aliceMatchingContact.id)),
                ContactsBoardState.Rack(bob, listOf(bobContact.id, bobOtherContact.id)),
            ),
            solved = emptySet(),
        )

        val result = state.applyAction(
            alice,
            ActionType.SoloConnectRest,
            setOf(aliceOtherContact, aliceMatchingSoloContact),
            emptySet(),
        )

        assertIs<ActionExecutionResult.Success>(result)
        assertEquals(setOf(aliceOtherContact.id, aliceMatchingSoloContact.id), result.state.solved)
    }

    @Test
    fun finishRedsSolvesAllRemainingRedContacts() {
        val alice = Player("alice")
        val bob = Player("bob")
        val aliceOtherContact = Contact(ContactId(2), 8)
        val redContact = Contact(ContactId(1), 7, ContactType.Red)
        val otherRedContact = Contact(ContactId(3), 7, ContactType.Red)
        val state = ContactsBoardState(
            pool = listOf(redContact, otherRedContact, aliceOtherContact),
            racks = listOf(
                ContactsBoardState.Rack(alice, listOf(redContact.id, otherRedContact.id)),
                ContactsBoardState.Rack(bob, listOf(aliceOtherContact.id)),
            ),
            solved = setOf(aliceOtherContact.id),
        )

        val result = state.applyAction(
            alice,
            ActionType.FinishReds,
            setOf(redContact, otherRedContact),
            emptySet(),
        )

        assertIs<ActionExecutionResult.Success>(result)
        assertEquals(setOf(aliceOtherContact.id, redContact.id, otherRedContact.id), result.state.solved)
    }

    @Test
    fun finishRedsRejectsWhenAnyOtherUnsolvedContactRemains() {
        val alice = Player("alice")
        val aliceOtherContact = Contact(ContactId(2), 8)
        val redContact = Contact(ContactId(1), 7, ContactType.Red)
        val state = ContactsBoardState(
            pool = listOf(redContact, aliceOtherContact),
            racks = listOf(
                ContactsBoardState.Rack(alice, listOf(redContact.id, aliceOtherContact.id)),
            ),
            solved = emptySet(),
        )

        val result = state.applyAction(
            alice,
            ActionType.FinishReds,
            setOf(redContact),
            emptySet(),
        )

        assertIs<ActionExecutionResult.Failure>(result)
        assertEquals("All other contacts must be solved", result.message)
    }

    private fun validationState(): ValidationState {
        val alice = Player("alice")
        val bob = Player("bob")
        val aliceContact = Contact(ContactId(1), 1)
        val bobContact = Contact(ContactId(2), 2)
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
        val alice: Player,
        val bob: Player,
        val aliceContact: Contact,
        val bobContact: Contact,
    )
}
