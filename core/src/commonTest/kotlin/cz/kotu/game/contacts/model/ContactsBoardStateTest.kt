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
            "Invalid number of selected contacts: player (0/1) other (1/1)",
            state.isActionLegal(alice, ContactsBoardState.ActionType.StandardConnect, emptySet(), setOf(bobContact)),
        )
        assertEquals(
            "Player does not own the selected contact",
            state.isActionLegal(
                bob,
                ContactsBoardState.ActionType.StandardConnect,
                setOf(aliceContact),
                setOf(bobContact)
            ),
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

        assertEquals(
            "Selected contacts must have the same number or all yellow",
            state.isActionLegal(
                alice,
                ContactsBoardState.ActionType.SoloConnectRest,
                setOf(blue, red),
                emptySet(),
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
