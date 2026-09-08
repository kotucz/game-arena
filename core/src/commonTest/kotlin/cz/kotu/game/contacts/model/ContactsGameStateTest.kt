package cz.kotu.game.contacts.model

import cz.kotu.game.contacts.model.ContactsBoardState.ActionType
import cz.kotu.game.contacts.model.ContactsBoardState.Contact
import cz.kotu.game.contacts.model.ContactsBoardState.ContactId
import cz.kotu.game.contacts.model.ContactsBoardState.ContactType
import cz.kotu.game.contacts.model.ContactsBoardState.Player
import cz.kotu.game.contacts.model.ContactsBoardState.Rack
import cz.kotu.game.contacts.model.ContactsBoardState.ResolveMultiConnect
import cz.kotu.game.contacts.model.ContactsGameState.GamePhase
import cz.kotu.game.contacts.model.ContactsGameState.GamePhase.StandardTurn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ContactsGameStateTest {

    private val alice = Player("alice")
    private val bob = Player("bob")
    private val players = listOf(alice, bob)

    private fun createBaseState(
        board: ContactsBoardState = ContactsBoardState(
            pool = listOf(
                Contact(ContactId(1), number = 7, type = ContactType.Blue),
                Contact(ContactId(2), number = 7, type = ContactType.Blue),
            ),
            racks = listOf(
                Rack(alice, listOf(ContactId(1))),
                Rack(bob, listOf(ContactId(2))),
            ),
            solved = emptySet(),
        ),
        activePlayer: Player = alice,
    ): ContactsGameState {
        return ContactsGameState(
            board = board,
            players = players,
            gamePhase = StandardTurn(activePlayer = activePlayer),
        )
    }

    @Test
    fun activePlayerGetsStandardAvailableActionsWhenNoMultiConnectPending() {
        val state = createBaseState(activePlayer = alice)

        val actions = state.getAvailablePlayerActions(alice)

        val expectedActions = setOf(
            ActionType.StandardConnect,
            ActionType.DoubleConnect,
            ActionType.TripleConnect,
            ActionType.MyDoubleConnect,
            ActionType.SoloConnectRest,
            ActionType.FinishReds,
            ActionType.AddHint,
        )
        assertEquals(expectedActions, actions.allowedActionTypes)
        assertNull(actions.actionStatusText)
        assertNull(actions.resolveMultiConnectContacts)
    }

    @Test
    fun inactivePlayerGetsNoActionsAndTurnStatusWhenNoMultiConnectPending() {
        val state = createBaseState(activePlayer = alice)

        val actions = state.getAvailablePlayerActions(bob)

        assertEquals(emptySet(), actions.allowedActionTypes)
        assertEquals("It is alice's turn", actions.actionStatusText)
        assertNull(actions.resolveMultiConnectContacts)
    }

    @Test
    fun targetPlayerGetsResolveMultiConnectAndTargetContactsWhenPending() {
        val originalContact = Contact(ContactId(1), number = 7, type = ContactType.Blue)
        val target1 = ContactId(2)
        val target2 = ContactId(3)
        val board = ContactsBoardState(
            pool = listOf(originalContact),
            racks = emptyList(),
            solved = emptySet(),
        )
        val state = createBaseState(board = board, activePlayer = alice).copy(
            gamePhase = GamePhase.ResolveMultiConnect(
                resolveMultiConnect = ResolveMultiConnect(
                    originalPlayer = alice,
                    targetPlayer = bob,
                    originalContact = originalContact.id,
                    targetContacts = setOf(target1, target2),
                ),
            ),
        )

        val actions = state.getAvailablePlayerActions(bob)

        assertEquals(setOf(ActionType.ResolveMultiConnect), actions.allowedActionTypes)
        assertEquals("Original contact: 7", actions.actionStatusText)
        assertEquals(setOf(target1, target2), actions.resolveMultiConnectContacts)
    }

    @Test
    fun nonTargetPlayerWaitsForTargetPlayerWhenMultiConnectPending() {
        val originalContact = Contact(ContactId(1), number = 7, type = ContactType.Blue)
        val board = ContactsBoardState(
            pool = listOf(originalContact),
            racks = emptyList(),
            solved = emptySet(),
        )
        val state = createBaseState(board = board, activePlayer = alice).copy(
            gamePhase = GamePhase.ResolveMultiConnect(
                resolveMultiConnect = ResolveMultiConnect(
                    originalPlayer = alice,
                    targetPlayer = bob,
                    originalContact = originalContact.id,
                    targetContacts = setOf(ContactId(2)),
                ),
            ),
        )

        val actions = state.getAvailablePlayerActions(alice)

        assertEquals(emptySet(), actions.allowedActionTypes)
        assertEquals("Waiting for bob to resolve the multi-connect", actions.actionStatusText)
        assertNull(actions.resolveMultiConnectContacts)
    }
}
