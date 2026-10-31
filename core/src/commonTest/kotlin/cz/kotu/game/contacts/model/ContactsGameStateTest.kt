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
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
            players = players.map {
                ContactsPlayer(username = it.username, uid = "test:${it.username}")
            },
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
            ActionType.AddHint,
        )
        assertEquals(expectedActions, actions.allowedActionTypes)
        assertNull(actions.actionStatusText)
        assertNull(actions.resolveMultiConnectContacts)
    }

    @Test
    fun standardConnectRemainsAvailableAlongsideSoloGroupsAndReds() {
        val sharedBlue = Contact(ContactId(1), number = 3)
        val soloBlue = Contact(ContactId(2), number = 7)
        val red = Contact(ContactId(3), number = 9, type = ContactType.Red)
        val teammateBlue = Contact(ContactId(4), number = 3)
        val board = ContactsBoardState(
            pool = listOf(sharedBlue, soloBlue, red, teammateBlue),
            racks = listOf(
                Rack(alice, listOf(sharedBlue.id, soloBlue.id, red.id)),
                Rack(bob, listOf(teammateBlue.id)),
            ),
            solved = emptySet(),
        )
        val state = createBaseState(board)

        val actions = state.getAvailablePlayerActions(alice)

        assertTrue(ActionType.StandardConnect in actions.allowedActionTypes)
        assertEquals(listOf(setOf(soloBlue.id)), actions.soloConnectRestGroups)
        assertFalse(ActionType.FinishReds in actions.allowedActionTypes)
        assertFalse(ActionType.StandardConnect in state.getAvailablePlayerActions(bob).allowedActionTypes)
        assertFalse(
            ActionType.StandardConnect in state.copy(gamePhase = GamePhase.GameOver("Done"))
                .getAvailablePlayerActions(alice).allowedActionTypes,
        )

        val afterSolvingSharedGroup = createBaseState(board.withSolvedContacts(sharedBlue, teammateBlue))
        assertFalse(ActionType.StandardConnect in afterSolvingSharedGroup.getAvailablePlayerActions(alice).allowedActionTypes)
    }

    @Test
    fun standardConnectIsUnavailableWhenOnlySoloGroupsAndRedsRemain() {
        val blue = Contact(ContactId(1), number = 3)
        val yellow = Contact(ContactId(2), number = 5, type = ContactType.Yellow)
        val otherYellow = Contact(ContactId(3), number = 9, type = ContactType.Yellow)
        val red = Contact(ContactId(4), number = 9, type = ContactType.Red)
        val teammateBlue = Contact(ContactId(5), number = 8)
        val board = ContactsBoardState(
            pool = listOf(blue, yellow, otherYellow, red, teammateBlue),
            racks = listOf(
                Rack(alice, listOf(blue.id, yellow.id)),
                Rack(alice, listOf(otherYellow.id, red.id)),
                Rack(bob, listOf(teammateBlue.id)),
            ),
            solved = emptySet(),
        )

        val actions = createBaseState(board).getAvailablePlayerActions(alice)

        assertFalse(ActionType.StandardConnect in actions.allowedActionTypes)
        assertTrue(ActionType.SoloConnectRest in actions.allowedActionTypes)
    }

    @Test
    fun standardConnectForYellowRemainsAvailableUntilTeammateYellowsAreSolved() {
        val yellow = Contact(ContactId(1), number = 3, type = ContactType.Yellow)
        val teammateYellow = Contact(ContactId(2), number = 9, type = ContactType.Yellow)
        val board = ContactsBoardState(
            pool = listOf(yellow, teammateYellow),
            racks = listOf(
                Rack(alice, listOf(yellow.id)),
                Rack(bob, listOf(teammateYellow.id)),
            ),
            solved = emptySet(),
        )

        assertTrue(ActionType.StandardConnect in createBaseState(board).getAvailablePlayerActions(alice).allowedActionTypes)
        assertFalse(
            ActionType.StandardConnect in createBaseState(board.withSolvedContacts(teammateYellow))
                .getAvailablePlayerActions(alice).allowedActionTypes,
        )
    }

    @Test
    fun standardConnectIsUnavailableWithoutOwnUnsolvedNonRedContacts() {
        val red = Contact(ContactId(1), number = 3, type = ContactType.Red)
        val blue = Contact(ContactId(2), number = 7)
        val teammateBlue = Contact(ContactId(3), number = 7)
        val board = ContactsBoardState(
            pool = listOf(red, blue, teammateBlue),
            racks = listOf(
                Rack(alice, listOf(red.id, blue.id)),
                Rack(bob, listOf(teammateBlue.id)),
            ),
            solved = setOf(blue.id),
        )

        for (candidate in listOf(board, board.withSolvedContacts(red), ContactsBoardState.empty())) {
            assertFalse(candidate.canStandardConnect(alice))
            assertFalse(ActionType.StandardConnect in createBaseState(candidate).getAvailablePlayerActions(alice).allowedActionTypes)
        }
    }

    @Test
    fun soloConnectRestIsAvailableForAnyNonemptyBlueGroupOwnedAcrossRacks() {
        for (count in 1..4) {
            val ownContacts = (1..count).map { Contact(ContactId(it), number = 7) }
            val teammateContact = Contact(ContactId(5), number = 8)
            val board = ContactsBoardState(
                pool = ownContacts + teammateContact,
                racks = ownContacts.map { Rack(alice, listOf(it.id)) } +
                    Rack(bob, listOf(teammateContact.id)),
                solved = emptySet(),
            )

            val actions = createBaseState(board).getAvailablePlayerActions(alice)

            assertTrue(ActionType.SoloConnectRest in actions.allowedActionTypes, "Group size: $count")
            assertEquals(listOf(ownContacts.map { it.id }.toSet()), actions.soloConnectRestGroups)
        }
    }

    @Test
    fun soloConnectRestIgnoresSolvedTeammateMatchesAndOtherColors() {
        val ownBlue = Contact(ContactId(1), number = 7)
        val teammateBlue = Contact(ContactId(2), number = 7)
        val teammateYellow = Contact(ContactId(3), number = 7, type = ContactType.Yellow)
        val teammateRed = Contact(ContactId(4), number = 7, type = ContactType.Red)
        val board = ContactsBoardState(
            pool = listOf(ownBlue, teammateBlue, teammateYellow, teammateRed),
            racks = listOf(
                Rack(alice, listOf(ownBlue.id)),
                Rack(bob, listOf(teammateBlue.id, teammateYellow.id, teammateRed.id)),
            ),
            solved = setOf(teammateBlue.id),
        )

        val actions = createBaseState(board).getAvailablePlayerActions(alice)

        assertTrue(ActionType.SoloConnectRest in actions.allowedActionTypes)
        assertEquals(listOf(setOf(ownBlue.id)), actions.soloConnectRestGroups)
    }

    @Test
    fun soloConnectRestForYellowIgnoresNumbersButRequiresAllRemainingYellows() {
        val yellow = Contact(ContactId(1), number = 3, type = ContactType.Yellow)
        val otherYellow = Contact(ContactId(2), number = 9, type = ContactType.Yellow)
        val teammateYellow = Contact(ContactId(3), number = 5, type = ContactType.Yellow)
        val board = ContactsBoardState(
            pool = listOf(yellow, otherYellow, teammateYellow),
            racks = listOf(
                Rack(alice, listOf(yellow.id)),
                Rack(alice, listOf(otherYellow.id)),
                Rack(bob, listOf(teammateYellow.id)),
            ),
            solved = emptySet(),
        )

        assertFalse(
            ActionType.SoloConnectRest in createBaseState(board).getAvailablePlayerActions(alice).allowedActionTypes,
        )
        assertEquals(emptyList(), createBaseState(board).getAvailablePlayerActions(alice).soloConnectRestGroups)
        val readyBoard = board.withSolvedContacts(teammateYellow)
        assertEquals(
            listOf(setOf(yellow.id, otherYellow.id)),
            createBaseState(readyBoard).getAvailablePlayerActions(alice).soloConnectRestGroups,
        )
        assertTrue(
            ActionType.SoloConnectRest in createBaseState(readyBoard).getAvailablePlayerActions(alice).allowedActionTypes,
        )
        val result = readyBoard.applyAction(alice, ActionType.SoloConnectRest, setOf(yellow, otherYellow), emptySet())
        assertEquals(setOf(yellow.id, otherYellow.id, teammateYellow.id), result.state.solved)
        assertFailsWith<InvalidActionException> {
            readyBoard.applyAction(alice, ActionType.SoloConnectRest, setOf(yellow), emptySet())
        }
    }

    @Test
    fun soloConnectRestIsAvailableWhenAtLeastOneGroupIsOwnedEntirely() {
        val sharedBlue = Contact(ContactId(1), number = 7)
        val teammateBlue = Contact(ContactId(2), number = 7)
        val ownBlue = Contact(ContactId(3), number = 8)
        val board = ContactsBoardState(
            pool = listOf(sharedBlue, teammateBlue, ownBlue),
            racks = listOf(
                Rack(alice, listOf(sharedBlue.id, ownBlue.id)),
                Rack(bob, listOf(teammateBlue.id)),
            ),
            solved = emptySet(),
        )

        val actions = createBaseState(board).getAvailablePlayerActions(alice)

        assertTrue(ActionType.SoloConnectRest in actions.allowedActionTypes)
        assertEquals(listOf(setOf(ownBlue.id)), actions.soloConnectRestGroups)
        val error = assertFailsWith<InvalidActionException> {
            board.applyAction(alice, ActionType.SoloConnectRest, setOf(sharedBlue), emptySet())
        }
        assertEquals("Some other player still has that contact", error.message)
    }

    @Test
    fun soloGroupsReachPlayerViewAndJsonAsSeparateCompleteSelections() {
        val blue = Contact(ContactId(1), number = 3)
        val matchingBlue = Contact(ContactId(2), number = 3)
        val otherBlue = Contact(ContactId(3), number = 7)
        val yellow = Contact(ContactId(4), number = 3, type = ContactType.Yellow)
        val otherYellow = Contact(ContactId(5), number = 9, type = ContactType.Yellow)
        val red = Contact(ContactId(6), number = 9, type = ContactType.Red)
        val solvedBlue = Contact(ContactId(7), number = 3)
        val teammateBlue = Contact(ContactId(8), number = 8)
        val board = ContactsBoardState(
            pool = listOf(blue, matchingBlue, otherBlue, yellow, otherYellow, red, solvedBlue, teammateBlue),
            racks = listOf(
                Rack(alice, listOf(blue.id, yellow.id, otherBlue.id)),
                Rack(alice, listOf(matchingBlue.id, solvedBlue.id, otherYellow.id, red.id)),
                Rack(bob, listOf(teammateBlue.id)),
            ),
            solved = setOf(solvedBlue.id),
        )
        val state = createBaseState(board)
        val actions = state.getAvailablePlayerActions(alice)
        val view = state.sanitizedPlayerView(alice, actions)
        val expectedGroups = setOf(setOf(blue.id, matchingBlue.id), setOf(otherBlue.id), setOf(yellow.id, otherYellow.id))

        assertEquals(expectedGroups, view.actions.soloConnectRestGroups.toSet())
        assertEquals(expectedGroups.size, view.actions.soloConnectRestGroups.size)
        val encoded = Json.encodeToString(PlayerViewState.serializer(), view)
        val decoded = Json.decodeFromString(PlayerViewState.serializer(), encoded)
        assertEquals(view, decoded)
        for (group in view.actions.soloConnectRestGroups) {
            val result = board.applyActionIds(alice, ActionType.SoloConnectRest, group, emptySet())
            assertEquals(board.solved + group, result.state.solved)
        }

        val unavailableStates = listOf(
            state.copy(gamePhase = StandardTurn(bob)),
            state.copy(gamePhase = GamePhase.GameOver("Done")),
            state.copy(
                gamePhase = GamePhase.ResolveMultiConnect(
                    ResolveMultiConnect(alice, bob, blue.id, setOf(teammateBlue.id)),
                ),
            ),
        )
        assertEquals(emptyList(), state.getAvailablePlayerActions(bob).soloConnectRestGroups)
        for (unavailableState in unavailableStates) {
            assertEquals(emptyList(), unavailableState.getAvailablePlayerActions(alice).soloConnectRestGroups)
        }
        assertEquals(emptyList(), unavailableStates.last().getAvailablePlayerActions(bob).soloConnectRestGroups)
    }

    @Test
    fun soloConnectRestIsUnavailableWithoutOwnUnsolvedNonRedContacts() {
        val red = Contact(ContactId(1), number = 7, type = ContactType.Red)
        val solvedBlue = Contact(ContactId(2), number = 8)
        val board = ContactsBoardState(
            pool = listOf(red, solvedBlue),
            racks = listOf(Rack(alice, listOf(red.id, solvedBlue.id))),
            solved = setOf(solvedBlue.id),
        )

        for (candidate in listOf(board, board.withSolvedContacts(red), ContactsBoardState.empty())) {
            assertFalse(candidate.canSoloConnectRest(alice))
            assertEquals(emptyList(), createBaseState(candidate).getAvailablePlayerActions(alice).soloConnectRestGroups)
            assertFalse(
                ActionType.SoloConnectRest in createBaseState(candidate).getAvailablePlayerActions(alice).allowedActionTypes,
            )
        }
    }

    @Test
    fun finishRedsIsTheOnlyActionWhenOnlyOwnUnsolvedContactsAreRed() {
        val red = Contact(ContactId(1), number = 7, type = ContactType.Red)
        val otherRed = Contact(ContactId(2), number = 9, type = ContactType.Red)
        val solvedBlue = Contact(ContactId(3), number = 8)
        val solvedYellow = Contact(ContactId(4), number = 8, type = ContactType.Yellow)
        val teammateBlue = Contact(ContactId(5), number = 8)
        val board = ContactsBoardState(
            pool = listOf(red, otherRed, solvedBlue, solvedYellow, teammateBlue),
            racks = listOf(
                Rack(alice, listOf(red.id, solvedBlue.id)),
                Rack(alice, listOf(solvedYellow.id, otherRed.id)),
                Rack(bob, listOf(teammateBlue.id)),
            ),
            solved = setOf(solvedBlue.id, solvedYellow.id),
        )
        val state = createBaseState(board)

        val actions = state.getAvailablePlayerActions(alice)

        assertEquals(setOf(ActionType.FinishReds), actions.allowedActionTypes)
        assertNull(actions.actionStatusText)
        assertNull(actions.resolveMultiConnectContacts)
        assertEquals(emptySet(), state.getAvailablePlayerActions(bob).allowedActionTypes)
        assertEquals(
            emptySet(),
            state.copy(gamePhase = StandardTurn(bob)).getAvailablePlayerActions(alice).allowedActionTypes,
        )
        assertEquals(
            emptySet(),
            state.copy(gamePhase = GamePhase.GameOver("Done")).getAvailablePlayerActions(alice).allowedActionTypes,
        )
    }

    @Test
    fun finishRedsIsUnavailableWhileAnyOwnNonRedContactRemainsUnsolved() {
        for (type in listOf(ContactType.Blue, ContactType.Yellow)) {
            val red = Contact(ContactId(1), number = 7, type = ContactType.Red)
            val nonRed = Contact(ContactId(2), number = 8, type = type)
            val board = ContactsBoardState(
                pool = listOf(red, nonRed),
                racks = listOf(
                    Rack(alice, listOf(red.id)),
                    Rack(alice, listOf(nonRed.id)),
                ),
                solved = emptySet(),
            )

            val actions = createBaseState(board).getAvailablePlayerActions(alice)

            assertFalse(ActionType.FinishReds in actions.allowedActionTypes, "Unsolved $type remains")
        }
    }

    @Test
    fun finishRedsIsUnavailableWhenAllOwnContactsAreSolved() {
        val red = Contact(ContactId(1), number = 7, type = ContactType.Red)
        val board = ContactsBoardState(
            pool = listOf(red),
            racks = listOf(Rack(alice, listOf(red.id))),
            solved = setOf(red.id),
        )

        val actions = createBaseState(board).getAvailablePlayerActions(alice)

        assertFalse(ActionType.FinishReds in actions.allowedActionTypes)
    }

    @Test
    fun finishRedsIsUnavailableWhenPlayerHasNoContacts() {
        val actions = createBaseState(ContactsBoardState.empty()).getAvailablePlayerActions(alice)

        assertFalse(ActionType.FinishReds in actions.allowedActionTypes)
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
