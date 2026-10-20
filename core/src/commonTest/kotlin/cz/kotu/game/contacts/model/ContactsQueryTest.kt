package cz.kotu.game.contacts.model

import cz.kotu.game.contacts.model.ContactsBoardState.Contact
import cz.kotu.game.contacts.model.ContactsBoardState.ContactId
import cz.kotu.game.contacts.model.ContactsBoardState.ContactType
import cz.kotu.game.contacts.model.ContactsBoardState.Player
import cz.kotu.game.contacts.model.ContactsBoardState.Rack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContactsQueryTest {
    private val alice = Player("alice")
    private val bob = Player("bob")
    private val blue = Contact(ContactId(1), 3)
    private val yellow = Contact(ContactId(2), 3, ContactType.Yellow)
    private val red = Contact(ContactId(3), 7, ContactType.Red)
    private val teammateBlue = Contact(ContactId(4), 3)
    private val teammateYellow = Contact(ContactId(5), 9, ContactType.Yellow)
    private val solvedTeammateBlue = Contact(ContactId(6), 3)
    private val solvedBlue = Contact(ContactId(7), 8)
    private val board = ContactsBoardState(
        pool = listOf(blue, yellow, red, teammateBlue, teammateYellow, solvedTeammateBlue, solvedBlue),
        racks = listOf(
            Rack(alice, listOf(blue.id, yellow.id, red.id)),
            Rack(alice, listOf(solvedBlue.id)),
            Rack(bob, listOf(teammateBlue.id, solvedTeammateBlue.id, teammateYellow.id)),
        ),
        solved = setOf(solvedBlue.id, solvedTeammateBlue.id),
    )

    @Test
    fun ownershipSolvedStateAndColorFiltersComposeAcrossRacks() = with(board) {
        assertEquals(
            setOf(blue.id, yellow.id),
            contacts().ownedBy(alice).unsolved().excludingReds().ids(),
        )
        assertEquals(
            setOf(teammateBlue.id, teammateYellow.id),
            contacts().unsolved().ownedByOthers(alice).ids(),
        )
        assertEquals(
            setOf(blue.id, yellow.id, red.id, solvedBlue.id),
            contacts().ownedBy(alice).ids(),
        )
    }

    @Test
    fun matchingUsesGameCategoriesAndIgnoresSolvedContactsWhenRequested() = with(board) {
        assertEquals(
            setOf(teammateBlue.id),
            contacts().unsolved().ownedByOthers(alice).matching(blue).ids(),
        )
        assertEquals(setOf(yellow.id, teammateYellow.id), contacts().matching(yellow).ids())
        assertEquals(
            setOf(blue.id, teammateBlue.id, solvedTeammateBlue.id),
            contacts().matching(blue).ids(),
        )
    }

    @Test
    fun queriesCanBeBranchedAndReusedWithStandardSequenceOperations() = with(board) {
        val unsolved = contacts().unsolved()
        val own = unsolved.ownedBy(alice)
        val others = unsolved.ownedByOthers(alice)

        assertEquals(listOf(blue, yellow, red), own.toList())
        assertEquals(listOf(teammateBlue, teammateYellow), others.toList())
        assertTrue(own.matching(blue).any())
        assertEquals(listOf(blue, yellow, red), own.toList())
        assertEquals(5, unsolved.count())
        assertFalse(with(ContactsBoardState.empty()) { contacts().unsolved().any() })
    }
}
