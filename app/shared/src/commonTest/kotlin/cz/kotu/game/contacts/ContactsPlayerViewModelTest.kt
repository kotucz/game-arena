package cz.kotu.game.contacts

import cz.kotu.game.contacts.model.ActionSelectionState
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsBoardState.ActionType
import cz.kotu.game.contacts.model.ContactsBoardState.ContactId
import cz.kotu.game.contacts.model.ContactsBoardState.Player
import cz.kotu.game.contacts.model.ContactsGameState
import cz.kotu.game.contacts.model.ContactsPlayerFacade
import cz.kotu.game.contacts.model.GameLogEntry
import cz.kotu.game.contacts.model.PlayerViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ContactsPlayerViewModelTest {

    private val alice = Player("alice")
    private val bob = Player("bob")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun waitForGameState(viewModel: ContactsPlayerViewModel): PlayerViewState {
        return viewModel.gameState.filterNotNull().first()
    }

    private fun createPlayerViewState(
        you: Player = alice,
        allowedActionTypes: Set<ActionType> = setOf(ActionType.StandardConnect),
        resolveMultiConnectContacts: Set<ContactId>? = null,
        racks: List<PlayerViewState.Rack> = emptyList(),
    ): PlayerViewState {
        return PlayerViewState(
            you = you,
            pool = emptyList(),
            racks = racks,
            faults = 0,
            actions = ContactsGameState.PlayerActions(
                allowedActionTypes = allowedActionTypes,
                resolveMultiConnectContacts = resolveMultiConnectContacts,
            ),
            lastActionResult = ContactsBoardState.ActionResult(),
        )
    }

    @Test
    fun togglingContactSelectionsUpdatesSelectionState() = runTest {
        val initialViewState = createPlayerViewState()
        val facade = FakeContactsPlayerFacade(initialViewState)
        val viewModel = ContactsPlayerViewModel(facade)
        waitForGameState(viewModel)

        val playerContact = ContactId(1)
        val otherContact = ContactId(2)

        viewModel.onPlayerContactClick(playerContact)
        assertEquals(setOf(playerContact), viewModel.actionSelectionState.playerContacts)

        viewModel.onPlayerContactClick(playerContact)
        assertEquals(emptySet(), viewModel.actionSelectionState.playerContacts)

        viewModel.onOtherContactClick(otherContact)
        assertEquals(setOf(otherContact), viewModel.actionSelectionState.otherContacts)

        viewModel.onOtherContactClick(otherContact)
        assertEquals(emptySet(), viewModel.actionSelectionState.otherContacts)
    }

    @Test
    fun solvedContactsAreAutomaticallyClearedFromSelectionWhenNewStateArrives() = runTest {
        val playerContact = ContactId(1)
        val otherContact = ContactId(2)

        val initialViewState = createPlayerViewState(
            racks = listOf(
                PlayerViewState.Rack(
                    owner = alice,
                    contacts = listOf(
                        PlayerViewState.RackContact(id = playerContact, value = null, solved = false, hint = null),
                    ),
                ),
                PlayerViewState.Rack(
                    owner = bob,
                    contacts = listOf(
                        PlayerViewState.RackContact(id = otherContact, value = null, solved = false, hint = null),
                    ),
                ),
            ),
        )
        val facade = FakeContactsPlayerFacade(initialViewState)
        val viewModel = ContactsPlayerViewModel(facade)
        waitForGameState(viewModel)

        viewModel.onPlayerContactClick(playerContact)
        viewModel.onOtherContactClick(otherContact)
        assertEquals(setOf(playerContact), viewModel.actionSelectionState.playerContacts)
        assertEquals(setOf(otherContact), viewModel.actionSelectionState.otherContacts)

        // New state where playerContact is solved, but otherContact remains unsolved
        val updatedViewState = initialViewState.copy(
            racks = listOf(
                PlayerViewState.Rack(
                    owner = alice,
                    contacts = listOf(
                        PlayerViewState.RackContact(id = playerContact, value = null, solved = true, hint = null),
                    ),
                ),
                PlayerViewState.Rack(
                    owner = bob,
                    contacts = listOf(
                        PlayerViewState.RackContact(id = otherContact, value = null, solved = false, hint = null),
                    ),
                ),
            ),
        )
        facade.stateFlow.value = updatedViewState

        // Selection should automatically drop solved contact
        assertEquals(emptySet(), viewModel.actionSelectionState.playerContacts)
        assertEquals(setOf(otherContact), viewModel.actionSelectionState.otherContacts)
    }

    @Test
    fun resolveMultiConnectInNewStateClearsActionSelectionState() = runTest {
        val initialViewState = createPlayerViewState()
        val facade = FakeContactsPlayerFacade(initialViewState)
        val viewModel = ContactsPlayerViewModel(facade)
        waitForGameState(viewModel)

        viewModel.onPlayerContactClick(ContactId(1))
        viewModel.onOtherContactClick(ContactId(2))
        assertEquals(setOf(ContactId(1)), viewModel.actionSelectionState.playerContacts)

        val updatedViewState = createPlayerViewState(
            resolveMultiConnectContacts = setOf(ContactId(3), ContactId(4)),
            allowedActionTypes = setOf(ActionType.ResolveMultiConnect),
        )
        facade.stateFlow.value = updatedViewState

        assertEquals(ActionSelectionState.None, viewModel.actionSelectionState)
    }

    @Test
    fun availableActionSelectionAutomaticallySelectsFirstAvailableWhenInvalidOrNone() = runTest {
        val initialViewState = createPlayerViewState(
            allowedActionTypes = setOf(ActionType.StandardConnect, ActionType.DoubleConnect),
        )
        val facade = FakeContactsPlayerFacade(initialViewState)
        val viewModel = ContactsPlayerViewModel(facade)
        waitForGameState(viewModel)

        // Automatically initialized to first allowed action type
        assertEquals(ActionType.StandardConnect, viewModel.selectedActionType)

        // Explicitly switch to DoubleConnect
        viewModel.selectActionType(ActionType.DoubleConnect)
        assertEquals(ActionType.DoubleConnect, viewModel.selectedActionType)

        // If new state still allows DoubleConnect, it remains selected
        facade.stateFlow.value = createPlayerViewState(
            allowedActionTypes = setOf(ActionType.DoubleConnect, ActionType.TripleConnect),
        )
        assertEquals(ActionType.DoubleConnect, viewModel.selectedActionType)

        // If new state no longer allows DoubleConnect, it falls back to first allowed
        facade.stateFlow.value = createPlayerViewState(
            allowedActionTypes = setOf(ActionType.TripleConnect),
        )
        assertEquals(ActionType.TripleConnect, viewModel.selectedActionType)
    }

    @Test
    fun userInteractionsDismissActionResultError() = runTest {
        val facade = FakeContactsPlayerFacade(createPlayerViewState())
        val viewModel = ContactsPlayerViewModel(facade)
        waitForGameState(viewModel)

        viewModel.actionResultError = "An error occurred"
        viewModel.onPlayerContactClick(ContactId(1))
        assertNull(viewModel.actionResultError)

        viewModel.actionResultError = "An error occurred"
        viewModel.onOtherContactClick(ContactId(2))
        assertNull(viewModel.actionResultError)

        viewModel.actionResultError = "An error occurred"
        viewModel.selectActionType(ActionType.StandardConnect)
        assertNull(viewModel.actionResultError)
    }

    @Test
    fun confirmActionCallsFacadeAndUpdatesErrorOnFailure() = runTest {
        val initialViewState = createPlayerViewState(
            allowedActionTypes = setOf(ActionType.StandardConnect),
        )
        val facade = FakeContactsPlayerFacade(initialViewState).apply {
            actionResult = Result.failure(IllegalStateException("Invalid move"))
        }
        val viewModel = ContactsPlayerViewModel(facade)
        waitForGameState(viewModel)

        val playerContact = ContactId(1)
        val otherContact = ContactId(2)
        viewModel.onPlayerContactClick(playerContact)
        viewModel.onOtherContactClick(otherContact)

        viewModel.confirmAction()

        assertEquals(ActionType.StandardConnect, facade.lastActionType)
        assertEquals(setOf(playerContact), facade.lastPlayerContacts)
        assertEquals(setOf(otherContact), facade.lastOtherContacts)
        assertFalse(viewModel.actionInProgress)
        assertEquals("Invalid move", viewModel.actionResultError)
    }

    @Test
    fun confirmActionCallsFacadeSuccessfully() = runTest {
        val initialViewState = createPlayerViewState(
            allowedActionTypes = setOf(ActionType.StandardConnect),
        )
        val facade = FakeContactsPlayerFacade(initialViewState).apply {
            actionResult = Result.success(Unit)
        }
        val viewModel = ContactsPlayerViewModel(facade)
        waitForGameState(viewModel)

        val playerContact = ContactId(1)
        val otherContact = ContactId(2)
        viewModel.onPlayerContactClick(playerContact)
        viewModel.onOtherContactClick(otherContact)

        viewModel.confirmAction()

        assertEquals(ActionType.StandardConnect, facade.lastActionType)
        assertEquals(setOf(playerContact), facade.lastPlayerContacts)
        assertEquals(setOf(otherContact), facade.lastOtherContacts)
        assertFalse(viewModel.actionInProgress)
        assertNull(viewModel.actionResultError)
    }

    private class FakeContactsPlayerFacade(
        initialState: PlayerViewState,
    ) : ContactsPlayerFacade {
        val stateFlow = MutableStateFlow(initialState)
        override val gameState: Flow<PlayerViewState> = stateFlow
        override val logs: StateFlow<List<GameLogEntry>> = MutableStateFlow(emptyList())

        var lastActionType: ActionType? = null
        var lastPlayerContacts: Set<ContactId> = emptySet()
        var lastOtherContacts: Set<ContactId> = emptySet()
        var actionResult: Result<Unit> = Result.success(Unit)

        override suspend fun action(
            actionType: ActionType,
            playerContacts: Set<ContactId>,
            otherContacts: Set<ContactId>,
        ): Result<Unit> {
            lastActionType = actionType
            lastPlayerContacts = playerContacts
            lastOtherContacts = otherContacts
            return actionResult
        }
    }
}
