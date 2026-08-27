package cz.kotu.game.contacts

import cz.kotu.game.contacts.model.ActionSelectionState
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsGameFacade
import cz.kotu.game.contacts.model.ContactsGameFacadeImpl
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContactsPlayerViewModelTest {

    @Test
    fun playerIsResolvedFromUsernameAndDefaultActionsAreAvailable() {
        val facade = TestContactsGameFacade(createState())
        val viewModel = ContactsPlayerViewModel(facade, "alice")

        assertEquals("alice", viewModel.player?.username)
        assertEquals(facade.gameState.value.allowedActionTypes, viewModel.availableActionTypes())
    }

    @Test
    fun togglingContactsBuildsSelectionStateAndValidatesSuccessfully() {
        val state = createState()
        val facade = TestContactsGameFacade(state)
        val viewModel = ContactsPlayerViewModel(facade, "alice")

        val (playerContact, otherContact) = matchingContacts(state)
        viewModel.selectedActionType = ContactsBoardState.ActionType.StandardConnect

        viewModel.onPlayerContactClick(playerContact)
        viewModel.onOtherContactClick(otherContact)

        assertEquals(setOf(playerContact), viewModel.actionSelectionState.playerContacts)
        assertEquals(setOf(otherContact), viewModel.actionSelectionState.otherContacts)
        assertNull(viewModel.validationError())
    }

    @Test
    fun clientValidationCanBeDisabledToUseBackendOnlyChecks() {
        val state = createState()
        val facade = TestContactsGameFacade(state)
        val viewModel = ContactsPlayerViewModel(facade, "alice")

        val (playerContact, otherContact) = matchingContacts(state)
        viewModel.selectedActionType = ContactsBoardState.ActionType.StandardConnect
        viewModel.onPlayerContactClick(playerContact)
        viewModel.onOtherContactClick(otherContact)

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

        val playerContact = aliceRack.contactIds.map(state::requireContact).first()
        val targetContacts = bobRack.contactIds.map(state::requireContact).take(2).toSet()

        val facade = TestContactsGameFacade(state)
        runBlocking {
            facade.action(
                player = alice,
                actionType = ContactsBoardState.ActionType.DoubleConnect,
                playerContacts = setOf(playerContact),
                otherContacts = targetContacts,
            )
        }

        val viewModel = ContactsPlayerViewModel(facade, "bob")
        val resolution = facade.gameState.value.resolveMultiConnect

        assertEquals(targetContacts, resolution?.targetContacts?.map(state::requireContact)?.toSet())
        assertEquals(emptySet(), viewModel.resolutionClickableContacts())

        viewModel.selectedActionType = ContactsBoardState.ActionType.ResolveMultiConnect
        assertEquals(targetContacts, viewModel.resolutionClickableContacts())
        assertEquals(targetContacts, viewModel.resolutionTargetContacts())
    }

    @Test
    fun solvedSelectionsAreClearedFromCurrentActionState() {
        val state = createState()
        val solvedState = state.withSolvedContacts(
            matchingContacts(state).first,
            matchingContacts(state).second,
        )
        val facade = TestContactsGameFacade(solvedState)
        val viewModel = ContactsPlayerViewModel(facade, "alice")

        val (playerContact, otherContact) = matchingContacts(state)
        viewModel.onPlayerContactClick(playerContact)
        viewModel.onOtherContactClick(otherContact)
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
    ): Pair<ContactsBoardState.Contact, ContactsBoardState.Contact> {
        val aliceRack = state.racks.first { it.owner.username == "alice" }
        val bobRack = state.racks.first { it.owner.username == "bob" }

        val sharedNumber = state.rackContacts(aliceRack)
            .map { it.number }
            .intersect(state.rackContacts(bobRack).map { it.number }.toSet())
            .first()

        val playerContact = state.rackContacts(aliceRack).first { it.number == sharedNumber }
        val otherContact = state.rackContacts(bobRack).first { it.number == sharedNumber }
        return playerContact to otherContact
    }

    private class TestContactsGameFacade(
        initialState: ContactsBoardState,
    ) : ContactsGameFacade {
        private val delegate = ContactsGameFacadeImpl(initialState)
        override val gameState = delegate.gameState
        override val logs = delegate.logs

        override suspend fun action(
            player: ContactsBoardState.Player,
            actionType: ContactsBoardState.ActionType,
            playerContacts: Set<ContactsBoardState.Contact>,
            otherContacts: Set<ContactsBoardState.Contact>,
        ): Result<Unit> {
            return delegate.action(player, actionType, playerContacts, otherContacts)
        }
    }
}
