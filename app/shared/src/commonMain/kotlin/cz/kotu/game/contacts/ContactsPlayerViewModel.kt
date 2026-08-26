package cz.kotu.game.contacts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cz.kotu.game.contacts.model.ActionSelectionState
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsGameFacade

class ContactsPlayerViewModel(
    val gameFacade: ContactsGameFacade,
    val username: String,
) : ViewModel() {

    val gameState: ContactsBoardState
        get() = gameFacade.gameState.value

    val player: ContactsBoardState.Player?
        get() = gameState.racks.map { it.owner }.firstOrNull { it.username == username }

    var actionSelectionState by mutableStateOf<ActionSelectionState>(ActionSelectionState.None)
        private set

    var isLogsExpanded by mutableStateOf(false)

    var selectedActionType by mutableStateOf<ContactsBoardState.ActionType?>(null)

    fun onPlayerContactClick(contact: ContactsBoardState.Contact) {
        val state = actionSelectionState
        val newPlayerContacts = if (contact in state.playerContacts) {
            state.playerContacts - contact
        } else {
            state.playerContacts + contact
        }
        actionSelectionState = ActionSelectionState.MultiConnect(
            playerContacts = newPlayerContacts,
            otherContacts = state.otherContacts,
        )
    }

    fun onOtherContactClick(contact: ContactsBoardState.Contact) {
        val state = actionSelectionState
        val newOtherContacts = if (contact in state.otherContacts) {
            state.otherContacts - contact
        } else {
            state.otherContacts + contact
        }
        actionSelectionState = ActionSelectionState.MultiConnect(
            playerContacts = state.playerContacts,
            otherContacts = newOtherContacts,
        )
    }

    fun resetActionSelection() {
        actionSelectionState = ActionSelectionState.None
    }

    fun updateActionSelectionForSolved() {
        val currentGameState = gameState
        val state = actionSelectionState
        val newPlayerContacts = state.playerContacts.filter { !currentGameState.isSolved(it) }.toSet()
        val newOtherContacts = state.otherContacts.filter { !currentGameState.isSolved(it) }.toSet()
        if (newPlayerContacts != state.playerContacts || newOtherContacts != state.otherContacts) {
            actionSelectionState = if (newPlayerContacts.isEmpty() && newOtherContacts.isEmpty()) {
                ActionSelectionState.None
            } else {
                ActionSelectionState.MultiConnect(
                    playerContacts = newPlayerContacts,
                    otherContacts = newOtherContacts
                )
            }
        }
    }

    fun availableActionTypes(): Set<ContactsBoardState.ActionType> {
        val resolution = gameState.resolveMultiConnect
        return when {
            resolution == null -> gameState.allowedActionTypes
            resolution.targetPlayer == player -> setOf(ContactsBoardState.ActionType.ResolveMultiConnect)
            else -> emptySet()
        }
    }

    fun resolutionTargetContacts(): Set<ContactsBoardState.Contact>? {
        val resolution = gameState.resolveMultiConnect ?: return null
        if (resolution.targetPlayer != player) return null
        return resolution.targetContacts.mapNotNull(gameState::contact).toSet()
    }

    fun resolutionClickableContacts(): Set<ContactsBoardState.Contact>? {
        if (gameState.resolveMultiConnect == null) return null
        val targetContacts = resolutionTargetContacts() ?: return emptySet()
        return when {
            selectedActionType == ContactsBoardState.ActionType.ResolveMultiConnect -> targetContacts
            else -> emptySet()
        }
    }

    fun updateSelectedActionTypeIfNeeded(availableActionTypes: Set<ContactsBoardState.ActionType>) {
        if (selectedActionType !in availableActionTypes) {
            selectedActionType = availableActionTypes.firstOrNull()
        }
    }

    fun selectActionType(actionType: ContactsBoardState.ActionType) {
        selectedActionType = actionType
    }

    fun validationError(): String? {
        val actionType = selectedActionType ?: return null
        val currentPlayer = player ?: return null
        return gameState.isActionLegal(
            currentPlayer,
            actionType,
            actionSelectionState.playerContacts,
            actionSelectionState.otherContacts,
        )
    }

    fun validAction(): Boolean {
        return selectedActionType != null && validationError() == null
    }

    fun confirmAction() {
        val currentPlayer = player ?: return
        val actionType = selectedActionType ?: return
        gameFacade.action(
            player = currentPlayer,
            actionType = actionType,
            playerContacts = actionSelectionState.playerContacts,
            otherContacts = actionSelectionState.otherContacts,
        )
        actionSelectionState = ActionSelectionState.None
    }
}
