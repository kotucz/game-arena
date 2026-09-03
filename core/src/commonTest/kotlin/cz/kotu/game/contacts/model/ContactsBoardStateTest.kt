package cz.kotu.game.contacts.model

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
            ContactsBoardState.Player("alice"),
            ContactsBoardState.Player("bob"),
            ContactsBoardState.Player("carol"),
            ContactsBoardState.Player("dave"),
        )

        val state = ContactsBoardState.create(players, ContactsBoardState.ContactsGameConfig())

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
            ContactsBoardState.Player("alice"),
            ContactsBoardState.Player("bob"),
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
            players = listOf(ContactsBoardState.Player("alice")),
            ContactsBoardState.ContactsGameConfig(
                yellowCount = 3,
                redCount = 2,
            ),
        )

        assertEquals(3, state.pool.count { it.type == ContactsBoardState.ContactType.Yellow })
        assertEquals(2, state.pool.count { it.type == ContactsBoardState.ContactType.Red })
        val specialNumbers = state.pool.filter { it.type != ContactsBoardState.ContactType.Blue }.map { it.number }
        assertEquals(specialNumbers.size, specialNumbers.toSet().size)
        assertEquals(true, specialNumbers.all { it in 1..12 })
        assertEquals(48, state.pool.count { it.type == ContactsBoardState.ContactType.Blue })
        assertEquals(
            (1..12).associateWith { 4 },
            state.pool.filter { it.type == ContactsBoardState.ContactType.Blue }
                .groupingBy { it.number }
                .eachCount(),
        )
    }

    @Test
    fun contactsWithEqualNumbersAreSortedByContactType() {
        val contacts = ContactsBoardState.ContactType.entries.mapIndexed { index, type ->
            ContactsBoardState.Contact(
                id = ContactsBoardState.ContactId(index + 1),
                number = 1,
                type = type,
            )
        }.reversed()

        assertEquals(
            ContactsBoardState.ContactType.entries,
            contacts.sorted().map { it.type },
        )
    }

    @Test
    fun contactsMatchRequiresTheSameType() {
        val state = ContactsBoardState.empty()
        val blue = ContactsBoardState.Contact(ContactsBoardState.ContactId(1), 7, ContactsBoardState.ContactType.Blue)
        val yellow =
            ContactsBoardState.Contact(ContactsBoardState.ContactId(2), 7, ContactsBoardState.ContactType.Yellow)

        assertEquals(false, state.contactsMatch(blue, yellow))
    }

    @Test
    fun contactMatchKeyUsesYForYellowAndNumberForOtherContacts() {
        assertEquals(
            "Y",
            ContactsBoardState.Contact(
                ContactsBoardState.ContactId(1),
                7,
                ContactsBoardState.ContactType.Yellow
            ).matchKey
        )
        assertEquals(
            "7",
            ContactsBoardState.Contact(ContactsBoardState.ContactId(2), 7, ContactsBoardState.ContactType.Blue).matchKey
        )
        assertEquals(
            "R",
            ContactsBoardState.Contact(ContactsBoardState.ContactId(3), 7, ContactsBoardState.ContactType.Red).matchKey
        )
    }

    @Test
    fun blueContactsMatchOnlyWhenNumbersMatch() {
        val state = ContactsBoardState.empty()
        val first = ContactsBoardState.Contact(ContactsBoardState.ContactId(1), 7, ContactsBoardState.ContactType.Blue)
        val sameNumber =
            ContactsBoardState.Contact(ContactsBoardState.ContactId(2), 7, ContactsBoardState.ContactType.Blue)
        val differentNumber =
            ContactsBoardState.Contact(ContactsBoardState.ContactId(3), 8, ContactsBoardState.ContactType.Blue)

        assertEquals(true, state.contactsMatch(first, sameNumber))
        assertEquals(false, state.contactsMatch(first, differentNumber))
    }

    @Test
    fun yellowContactsMatchRegardlessOfNumber() {
        val state = ContactsBoardState.empty()
        val first =
            ContactsBoardState.Contact(ContactsBoardState.ContactId(1), 7, ContactsBoardState.ContactType.Yellow)
        val second =
            ContactsBoardState.Contact(ContactsBoardState.ContactId(2), 8, ContactsBoardState.ContactType.Yellow)

        assertEquals(true, state.contactsMatch(first, second))
    }

    @Test
    fun isActionLegalReturnsNullForAValidConnect() {
        val (state, alice, _, aliceContact, bobContact) = validationState()

        val result = state.applyAction(
            alice,
            ContactsBoardState.ActionType.StandardConnect,
            setOf(aliceContact),
            setOf(bobContact),
        )
        assertTrue(result is ActionExecutionResult.Success)
    }

    @Test
    fun isActionLegalExplainsInvalidSelectionAndOwnership() {
        val (state, alice, bob, aliceContact, bobContact) = validationState()

        val result1 = state.applyAction(alice, ContactsBoardState.ActionType.StandardConnect, emptySet(), setOf(bobContact))
        assertIs<ActionExecutionResult.Failure>(result1)
        assertEquals("Invalid number of selected contacts: player (0/1) other (1/1)", result1.message)

        val result2 = state.applyAction(bob, ContactsBoardState.ActionType.StandardConnect, setOf(aliceContact), setOf(bobContact))
        assertIs<ActionExecutionResult.Failure>(result2)
        assertEquals("Player does not own the selected contact", result2.message)
    }

    @Test
    fun isActionLegalExplainsSolvedAndDisallowedActions() {
        val (state, alice, _, aliceContact, bobContact) = validationState()

        val result1 = state.withSolvedContacts(aliceContact).applyAction(
            alice,
            ContactsBoardState.ActionType.StandardConnect,
            setOf(aliceContact),
            setOf(bobContact),
        )
        assertIs<ActionExecutionResult.Failure>(result1)
        assertEquals("Selected contact is already solved", result1.message)

        // TODO allowed action types will change respecting player on turn etc
        val result2 = state.copy(allowedActionTypes = emptySet()).applyAction(
            alice,
            ContactsBoardState.ActionType.StandardConnect,
            setOf(aliceContact),
            setOf(bobContact),
        )
        // allowedActionTypes is now only advisory; action itself will be authoritative
        // assert that action fails because handlers perform validation (ActionType not allowed should be reflected here)
        assertIs<ActionExecutionResult.Failure>(result2)
    }

    @Test
    fun soloConnectRestRequiresContactsWithTheSameNumberOrAllYellow() {
        val alice = ContactsBoardState.Player("alice")
        val blue = ContactsBoardState.Contact(
            ContactsBoardState.ContactId(1),
            number = 1,
            type = ContactsBoardState.ContactType.Blue,
        )
        val red = ContactsBoardState.Contact(
            ContactsBoardState.ContactId(2),
            number = 2,
            type = ContactsBoardState.ContactType.Blue,
        )
        val state = ContactsBoardState(
            pool = listOf(blue, red),
            racks = listOf(ContactsBoardState.Rack(alice, listOf(blue.id, red.id))),
            solved = emptySet(),
        )

        val result = state.applyAction(
            alice,
            ContactsBoardState.ActionType.SoloConnectRest,
            setOf(blue, red),
            emptySet(),
        )
        assertIs<ActionExecutionResult.Failure>(result)
        assertEquals("Selected contacts must have the same number or all yellow", result.message)
    }

    @Test
    fun myDoubleConnectSolvesWhenEitherPlayerContactMatches() {
        val alice = ContactsBoardState.Player("alice")
        val bob = ContactsBoardState.Player("bob")
        val aliceContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(1), 7)
        val aliceOtherContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(2), 8)
        val bobContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(3), 7)
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
            ContactsBoardState.ActionType.MyDoubleConnect,
            setOf(aliceOtherContact, aliceContact),
            setOf(bobContact),
        )

        assertIs<ActionExecutionResult.Success>(result)
        assertEquals(setOf(aliceContact.id, bobContact.id), result.state.solved)
        assertEquals(0, result.state.faults)
    }

    @Test
    fun myDoubleConnectUsesStandardConnectResultWhenNeitherContactMatches() {
        val alice = ContactsBoardState.Player("alice")
        val bob = ContactsBoardState.Player("bob")
        val aliceOtherContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(2), 8)
        val aliceMatchingContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(5), 7)
        val bobOtherContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(4), 9)
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
            ContactsBoardState.ActionType.MyDoubleConnect,
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
        val alice = ContactsBoardState.Player("alice")
        val bob = ContactsBoardState.Player("bob")
        val aliceContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(1), 7)
        val bobContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(3), 7)
        val bobOtherContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(4), 9)
        val state = ContactsBoardState(
            pool = listOf(aliceContact, bobContact, bobOtherContact),
            racks = listOf(
                ContactsBoardState.Rack(alice, listOf(aliceContact.id)),
                ContactsBoardState.Rack(bob, listOf(bobContact.id, bobOtherContact.id)),
            ),
            solved = emptySet(),
        )

        val first = state.applyAction(alice, ContactsBoardState.ActionType.DoubleConnect, setOf(aliceContact), setOf(bobContact, bobOtherContact))
        assertIs<ActionExecutionResult.Success>(first)

        val second = first.state.applyAction(bob, ContactsBoardState.ActionType.ResolveMultiConnect, setOf(bobContact), emptySet())

        assertIs<ActionExecutionResult.Success>(second)
        assertEquals(setOf(aliceContact.id, bobContact.id), second.state.solved)
        assertEquals(null, second.state.resolveMultiConnect)
    }

    @Test
    fun targetPlayerResolvesMismatchUsingStandardConnectResult() {
        val alice = ContactsBoardState.Player("alice")
        val bob = ContactsBoardState.Player("bob")
        val aliceContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(1), 7)
        val bobContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(3), 7)
        val bobOtherContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(4), 9)
        val state = ContactsBoardState(
            pool = listOf(aliceContact, bobContact, bobOtherContact),
            racks = listOf(
                ContactsBoardState.Rack(alice, listOf(aliceContact.id)),
                ContactsBoardState.Rack(bob, listOf(bobContact.id, bobOtherContact.id)),
            ),
            solved = emptySet(),
        )

        val first = state.applyAction(alice, ContactsBoardState.ActionType.DoubleConnect, setOf(aliceContact), setOf(bobContact, bobOtherContact))
        assertIs<ActionExecutionResult.Success>(first)

        val second = first.state.applyAction(bob, ContactsBoardState.ActionType.ResolveMultiConnect, setOf(bobOtherContact), emptySet())

        assertIs<ActionExecutionResult.Success>(second)
        assertEquals(emptySet(), second.state.solved)
        assertEquals(1, second.state.faults)
        assertEquals(bobOtherContact.number.toString(), second.state.racks[1].hint(bobOtherContact))
        assertEquals(null, second.state.resolveMultiConnect)
    }

    @Test
    fun onlyTargetPlayerCanResolveMultiConnect() {
        val alice = ContactsBoardState.Player("alice")
        val bob = ContactsBoardState.Player("bob")
        val aliceContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(1), 7)
        val bobContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(3), 7)
        val bobOtherContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(4), 9)
        val state = ContactsBoardState(
            pool = listOf(aliceContact, bobContact, bobOtherContact),
            racks = listOf(
                ContactsBoardState.Rack(alice, listOf(aliceContact.id)),
                ContactsBoardState.Rack(bob, listOf(bobContact.id, bobOtherContact.id)),
            ),
            solved = emptySet(),
        )

        val first = state.applyAction(alice, ContactsBoardState.ActionType.DoubleConnect, setOf(aliceContact), setOf(bobContact, bobOtherContact))
        assertIs<ActionExecutionResult.Success>(first)

        val second = first.state.applyAction(alice, ContactsBoardState.ActionType.ResolveMultiConnect, setOf(bobContact), emptySet())

        assertIs<ActionExecutionResult.Failure>(second)
        assertEquals("Only the target player can resolve the multi-connect", second.message)
        assertEquals(2, first.state.resolveMultiConnect?.targetContacts?.size)
    }

    @Test
    fun soloConnectRestSolvesAllRemainingSameNumberContactsAcrossRacks() {
        val alice = ContactsBoardState.Player("alice")
        val aliceContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(1), 7)
        val aliceOtherContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(2), 8)
        val aliceMatchingContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(5), 7)
        val aliceMatchingSoloContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(6), 8)
        val bob = ContactsBoardState.Player("bob")
        val bobContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(3), 7)
        val bobOtherContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(4), 9)
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
            ContactsBoardState.ActionType.SoloConnectRest,
            setOf(aliceOtherContact, aliceMatchingSoloContact),
            emptySet(),
        )

        assertIs<ActionExecutionResult.Success>(result)
        assertEquals(setOf(aliceOtherContact.id, aliceMatchingSoloContact.id), result.state.solved)
    }

    @Test
    fun finishRedsSolvesAllRemainingRedContacts() {
        val alice = ContactsBoardState.Player("alice")
        val bob = ContactsBoardState.Player("bob")
        val aliceOtherContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(2), 8)
        val redContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(1), 7, ContactsBoardState.ContactType.Red)
        val otherRedContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(3), 7, ContactsBoardState.ContactType.Red)
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
            ContactsBoardState.ActionType.FinishReds,
            setOf(redContact, otherRedContact),
            emptySet(),
        )

        assertIs<ActionExecutionResult.Success>(result)
        assertEquals(setOf(aliceOtherContact.id, redContact.id, otherRedContact.id), result.state.solved)
    }

    @Test
    fun finishRedsRejectsWhenAnyOtherUnsolvedContactRemains() {
        val alice = ContactsBoardState.Player("alice")
        val aliceOtherContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(2), 8)
        val redContact = ContactsBoardState.Contact(ContactsBoardState.ContactId(1), 7, ContactsBoardState.ContactType.Red)
        val state = ContactsBoardState(
            pool = listOf(redContact, aliceOtherContact),
            racks = listOf(
                ContactsBoardState.Rack(alice, listOf(redContact.id, aliceOtherContact.id)),
            ),
            solved = emptySet(),
        )

        val result = state.applyAction(
            alice,
            ContactsBoardState.ActionType.FinishReds,
            setOf(redContact),
            emptySet(),
        )

        assertIs<ActionExecutionResult.Failure>(result)
        assertEquals("All other contacts must be solved", result.message)
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
