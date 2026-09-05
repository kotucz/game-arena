package cz.kotu.game.contacts

import cz.kotu.game.contacts.model.ActionSelectionState
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsBoardState.ActionType
import cz.kotu.game.contacts.model.ContactsGameFacade
import cz.kotu.game.contacts.model.ContactsGameFacadeImpl
import cz.kotu.game.contacts.model.ContactsGameState
import cz.kotu.game.contacts.model.ContactsPlayerGameAdapter
import cz.kotu.game.contacts.model.PlayerViewState
import cz.kotu.game.contacts.model.isSolved
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContactsPlayerViewModelTest {

    private suspend fun waitForGameState(viewModel: ContactsPlayerViewModel): PlayerViewState {
        return viewModel.gameState.filterNotNull().first()
    }

    @Test
    fun togglingContactsBuildsSelectionStateAndValidatesSuccessfully() = runTest {
        val state = createState()
        val facade = TestContactsGameFacade(state)
        val viewModel = ContactsPlayerViewModel(
            ContactsPlayerGameAdapter(ContactsBoardState.Player("alice"), facade),
        )
        waitForGameState(viewModel)

        val (playerContactId, otherContactId) = matchingContacts(state)
        viewModel.selectedActionType = ActionType.StandardConnect

        viewModel.onPlayerContactClick(playerContactId)
        viewModel.onOtherContactClick(otherContactId)

        assertEquals(setOf(playerContactId), viewModel.actionSelectionState.playerContacts)
        assertEquals(setOf(otherContactId), viewModel.actionSelectionState.otherContacts)
        assertNull(viewModel.validationError())
    }

    @Test
    fun clientValidationCanBeDisabledToUseBackendOnlyChecks() = runTest {
        val state = createState()
        val facade = TestContactsGameFacade(state)
        val viewModel = ContactsPlayerViewModel(
            ContactsPlayerGameAdapter(ContactsBoardState.Player("alice"), facade),
        )
        waitForGameState(viewModel)

        val (playerContactId, otherContactId) = matchingContacts(state)
        viewModel.selectedActionType = ActionType.StandardConnect
        viewModel.onPlayerContactClick(playerContactId)
        viewModel.onOtherContactClick(otherContactId)

        assertTrue(!viewModel.clientValidationEnabled)
        assertTrue(viewModel.validAction())

        viewModel.clientValidationEnabled = true
        assertNull(viewModel.validationError())
    }

    @Test
    fun resolutionTargetsAreOnlyClickableWhenResolvingAMultiConnect() = runTest {
        val state = createState()
        val alice = state.board.racks.first { it.owner.username == "alice" }.owner
        val bob = state.board.racks.first { it.owner.username == "bob" }.owner
        val aliceRack = state.board.racks.first { it.owner == alice }
        val bobRack = state.board.racks.first { it.owner == bob }

        val playerContactId = aliceRack.contactIds.first()
        val targetContactIds = bobRack.contactIds.take(2).toSet()

        val facade = TestContactsGameFacade(state)
        facade.action(
            player = alice,
            actionType = ActionType.DoubleConnect,
            playerContacts = setOf(playerContactId),
            otherContacts = targetContactIds,
        )

        val viewModel = ContactsPlayerViewModel(
            ContactsPlayerGameAdapter(bob, facade),
        )
        waitForGameState(viewModel)

        val resolution = facade.gameState.value.board.resolveMultiConnect

        assertEquals(targetContactIds, resolution?.targetContacts)
        // ResolveMultiConnect is automatically selected since it's the only available action for Bob
        assertEquals(ActionType.ResolveMultiConnect, viewModel.selectedActionType)
        assertEquals(targetContactIds, viewModel.gameState.value!!.actions.resolveMultiConnectContacts)
    }

    @Test
    fun solvedSelectionsAreClearedFromCurrentActionState() = runTest {
        val state = createState()
        val (playerContactId, otherContactId) = matchingContacts(state)

        val facade = TestContactsGameFacade(state)
        val viewModel = ContactsPlayerViewModel(
            ContactsPlayerGameAdapter(ContactsBoardState.Player("alice"), facade),
        )
        waitForGameState(viewModel)

        viewModel.onPlayerContactClick(playerContactId)
        viewModel.onOtherContactClick(otherContactId)

        // solve contacts from other player
        facade.action(
            ContactsBoardState.Player("bob"), ActionType.StandardConnect,
            playerContacts = setOf(otherContactId),
            otherContacts = setOf(playerContactId),
        )
        waitForGameState(viewModel)

        assertEquals(ActionSelectionState.None, viewModel.actionSelectionState)
    }

    @Test
    fun solvedSelectionsAreAutomaticallyClearedWhenNewStateArrives() = runTest {
        val state = createState()
        val (playerContactId, otherContactId) = matchingContacts(state)
        val facade = TestContactsGameFacade(state)
        val viewModel = ContactsPlayerViewModel(
            ContactsPlayerGameAdapter(ContactsBoardState.Player("alice"), facade),
        )
        waitForGameState(viewModel)

        viewModel.onPlayerContactClick(playerContactId)
        viewModel.onOtherContactClick(otherContactId)
        assertEquals(setOf(playerContactId), viewModel.actionSelectionState.playerContacts)

        // Action solving the contacts triggers newState emission
        facade.action(
            player = ContactsBoardState.Player("alice"),
            actionType = ActionType.StandardConnect,
            playerContacts = setOf(playerContactId),
            otherContacts = setOf(otherContactId),
        )
        viewModel.gameState.filterNotNull().first { it.isSolved(playerContactId) }

        assertEquals(ActionSelectionState.None, viewModel.actionSelectionState)
    }

    private fun createState(): ContactsGameState {
        val players = listOf(
            ContactsBoardState.Player("alice"),
            ContactsBoardState.Player("bob"),
        )
        val board = ContactsBoardState.create(
            players = players,
            config = ContactsBoardState.ContactsGameConfig(
                blueCount = 12,
                yellowCount = 0,
                redCount = 0,
            ),
        )
        return ContactsGameState(
            board = board,
            players = players,
            activePlayer = ContactsBoardState.Player("alice"),
        )
    }

    private fun matchingContacts(
        state: ContactsGameState,
    ): Pair<ContactsBoardState.ContactId, ContactsBoardState.ContactId> {
        val aliceRack = state.board.racks.first { it.owner.username == "alice" }
        val bobRack = state.board.racks.first { it.owner.username == "bob" }

        val sharedNumber = state.board.rackContacts(aliceRack)
            .map { it.number }
            .intersect(state.board.rackContacts(bobRack).map { it.number }.toSet())
            .first()

        val playerContactId = state.board.rackContacts(aliceRack).first { it.number == sharedNumber }.id
        val otherContactId = state.board.rackContacts(bobRack).first { it.number == sharedNumber }.id
        return playerContactId to otherContactId
    }

    private class TestContactsGameFacade(
        initialState: ContactsGameState,
    ) : ContactsGameFacade {
        private val delegate = ContactsGameFacadeImpl(
            initialState,
        )
        override val gameState = delegate.gameState
        override val logs = delegate.logs

        override suspend fun action(
            player: ContactsBoardState.Player,
            actionType: ActionType,
            playerContacts: Set<ContactsBoardState.ContactId>,
            otherContacts: Set<ContactsBoardState.ContactId>,
        ): Result<Unit> {
            return delegate.action(player, actionType, playerContacts, otherContacts)
        }
    }
}
