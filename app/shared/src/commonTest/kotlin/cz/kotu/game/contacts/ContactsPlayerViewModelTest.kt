package cz.kotu.game.contacts

import cz.kotu.game.contacts.model.ActionSelectionState
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsGameFacade
import cz.kotu.game.contacts.model.ContactsGameFacadeImpl
import cz.kotu.game.contacts.model.ContactsGameState
import cz.kotu.game.contacts.model.ContactsPlayerGameAdapter
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContactsPlayerViewModelTest {

    @Test
    fun playerIsResolvedFromUsernameAndDefaultActionsAreAvailable() {
        val facade = TestContactsGameFacade(createState())
        val viewModel = ContactsPlayerViewModel(
            ContactsPlayerGameAdapter(ContactsBoardState.Player("alice"), facade),
            "alice",
        )

        assertEquals("alice", viewModel.player?.username)
        assertEquals(facade.gameState.value.board.allowedActionTypes, viewModel.availableActionTypes())
    }

    @Test
    fun togglingContactsBuildsSelectionStateAndValidatesSuccessfully() {
        val state = createState()
        val facade = TestContactsGameFacade(state)
        val viewModel = ContactsPlayerViewModel(
            ContactsPlayerGameAdapter(ContactsBoardState.Player("alice"), facade),
            "alice",
        )

        val (playerContactId, otherContactId) = matchingContacts(state)
        viewModel.selectedActionType = ContactsBoardState.ActionType.StandardConnect

        viewModel.onPlayerContactClick(playerContactId)
        viewModel.onOtherContactClick(otherContactId)

        assertEquals(setOf(playerContactId), viewModel.actionSelectionState.playerContacts)
        assertEquals(setOf(otherContactId), viewModel.actionSelectionState.otherContacts)
        assertNull(viewModel.validationError())
    }

    @Test
    fun clientValidationCanBeDisabledToUseBackendOnlyChecks() {
        val state = createState()
        val facade = TestContactsGameFacade(state)
        val viewModel = ContactsPlayerViewModel(
            ContactsPlayerGameAdapter(ContactsBoardState.Player("alice"), facade), "alice",
        )

        val (playerContactId, otherContactId) = matchingContacts(state)
        viewModel.selectedActionType = ContactsBoardState.ActionType.StandardConnect
        viewModel.onPlayerContactClick(playerContactId)
        viewModel.onOtherContactClick(otherContactId)

        assertTrue(!viewModel.clientValidationEnabled)
        assertTrue(viewModel.validAction())

        viewModel.clientValidationEnabled = true
        assertNull(viewModel.validationError())
    }

    @Test
    fun resolutionTargetsAreOnlyClickableWhenResolvingAMultiConnect() {
        val state = createState()
        val alice = state.racks.first { it.owner.username == "alice" }.owner
        val bob = state.racks.first { it.owner.username == "bob" }.owner
        val aliceRack = state.racks.first { it.owner == alice }
        val bobRack = state.racks.first { it.owner == bob }

        val playerContactId = aliceRack.contactIds.first()
        val targetContactIds = bobRack.contactIds.take(2).toSet()

        val facade = TestContactsGameFacade(state)
        runTest {
            facade.action(
                player = alice,
                actionType = ContactsBoardState.ActionType.DoubleConnect,
                playerContacts = setOf(playerContactId),
                otherContacts = targetContactIds,
            )
        }

        val viewModel = ContactsPlayerViewModel(
            ContactsPlayerGameAdapter(ContactsBoardState.Player("alice"), facade), "bob",
        )
        val resolution = facade.gameState.value.board.resolveMultiConnect

        assertEquals(targetContactIds, resolution?.targetContacts)
        assertEquals(emptySet(), viewModel.resolutionClickableContacts())

        viewModel.selectedActionType = ContactsBoardState.ActionType.ResolveMultiConnect
        assertEquals(targetContactIds, viewModel.resolutionClickableContacts())
        assertEquals(targetContactIds, viewModel.resolutionTargetContacts())
    }

    @Test
    fun solvedSelectionsAreClearedFromCurrentActionState() {
        val state = createState()
        val (playerContactId, otherContactId) = matchingContacts(state)
        val solvedState = state.withSolvedContacts(
            state.requireContact(playerContactId),
            state.requireContact(otherContactId),
        )
        val facade = TestContactsGameFacade(solvedState)
        val viewModel = ContactsPlayerViewModel(
            ContactsPlayerGameAdapter(ContactsBoardState.Player("alice"), facade), "alice",
        )

        viewModel.onPlayerContactClick(playerContactId)
        viewModel.onOtherContactClick(otherContactId)
        viewModel.updateActionSelectionForSolved()

        assertEquals(ActionSelectionState.None, viewModel.actionSelectionState)
    }

    private fun createState(): ContactsBoardState {
        return ContactsBoardState.create(
            players = listOf(
                ContactsBoardState.Player("alice"),
                ContactsBoardState.Player("bob"),
            ),
            config = ContactsBoardState.ContactsGameConfig(
                blueCount = 12,
                yellowCount = 0,
                redCount = 0,
            ),
        )
    }

    private fun matchingContacts(
        state: ContactsBoardState,
    ): Pair<ContactsBoardState.ContactId, ContactsBoardState.ContactId> {
        val aliceRack = state.racks.first { it.owner.username == "alice" }
        val bobRack = state.racks.first { it.owner.username == "bob" }

        val sharedNumber = state.rackContacts(aliceRack)
            .map { it.number }
            .intersect(state.rackContacts(bobRack).map { it.number }.toSet())
            .first()

        val playerContactId = state.rackContacts(aliceRack).first { it.number == sharedNumber }.id
        val otherContactId = state.rackContacts(bobRack).first { it.number == sharedNumber }.id
        return playerContactId to otherContactId
    }

    private class TestContactsGameFacade(
        initialState: ContactsBoardState,
    ) : ContactsGameFacade {
        private val delegate = ContactsGameFacadeImpl(
            ContactsGameState(
                board = initialState,
                players = listOf(),
                activePlayer = ContactsBoardState.Player("alice"),
            ),
        )
        override val gameState = delegate.gameState
        override val logs = delegate.logs

        override suspend fun action(
            player: ContactsBoardState.Player,
            actionType: ContactsBoardState.ActionType,
            playerContacts: Set<ContactsBoardState.ContactId>,
            otherContacts: Set<ContactsBoardState.ContactId>,
        ): Result<Unit> {
            return delegate.action(player, actionType, playerContacts, otherContacts)
        }
    }
}
