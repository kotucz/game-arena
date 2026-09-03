package cz.kotu.game.contacts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.kotu.game.contacts.model.ActionExecutionResult
import cz.kotu.game.contacts.model.ActionSelectionState
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsGameState
import cz.kotu.game.contacts.model.ContactsPlayerFacade
import cz.kotu.game.contacts.model.applyAction
import kotlinx.coroutines.launch

class ContactsPlayerViewModel(
    val gameFacade: ContactsPlayerFacade,
    val username: String,
) : ViewModel() {

    val gameState: ContactsGameState
        get() = gameFacade.gameState.value

    val player: ContactsBoardState.Player?
        get() = gameState.board.racks.map { it.owner }.firstOrNull { it.username == username }

    var actionSelectionState by mutableStateOf<ActionSelectionState>(ActionSelectionState.None)
        private set

    var isLogsExpanded by mutableStateOf(false)

    var selectedActionType by mutableStateOf<ContactsBoardState.ActionType?>(null)

    var actionInProgress by mutableStateOf(false)

    var actionResultError by mutableStateOf<String?>(null)

    // keep validation on backend only for now
    var clientValidationEnabled by mutableStateOf(false)

    fun actionError(): String? = actionResultError ?: validationError()

    private fun dismissActionResultError() {
        actionResultError = null
    }

    fun onPlayerContactClick(contact: ContactsBoardState.Contact) {
        dismissActionResultError()
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
        dismissActionResultError()
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
        dismissActionResultError()
        actionSelectionState = ActionSelectionState.None
    }

    fun updateActionSelectionForSolved() {
        dismissActionResultError()
        val currentGameState = gameState.board
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
        val resolution = gameState.board.resolveMultiConnect
        return when {
            resolution == null -> gameState.board.allowedActionTypes
            resolution.targetPlayer == player -> setOf(ContactsBoardState.ActionType.ResolveMultiConnect)
            else -> emptySet()
        }
    }

    fun resolutionTargetContacts(): Set<ContactsBoardState.Contact>? {
        val resolution = gameState.board.resolveMultiConnect ?: return null
        if (resolution.targetPlayer != player) return null
        return resolution.targetContacts.mapNotNull(gameState.board::contact).toSet()
    }

    fun resolutionClickableContacts(): Set<ContactsBoardState.Contact>? {
        if (gameState.board.resolveMultiConnect == null) return null
        val targetContacts = resolutionTargetContacts() ?: return emptySet()
        return when {
            selectedActionType == ContactsBoardState.ActionType.ResolveMultiConnect -> targetContacts
            else -> emptySet()
        }
    }

    fun updateSelectedActionTypeIfNeeded(availableActionTypes: Set<ContactsBoardState.ActionType>) {
        if (selectedActionType !in availableActionTypes) {
            dismissActionResultError()
            selectedActionType = availableActionTypes.firstOrNull()
        }
    }

    fun selectActionType(actionType: ContactsBoardState.ActionType) {
        dismissActionResultError()
        selectedActionType = actionType
    }

    fun validationError(): String? {
        val actionType = selectedActionType ?: return null
        val currentPlayer = player ?: return null
        return (gameState.board.applyAction(
            currentPlayer,
            actionType,
            actionSelectionState.playerContacts,
            actionSelectionState.otherContacts,
        ) as? ActionExecutionResult.Failure)?.message
    }

    fun validAction(): Boolean {
        val isValid = if (clientValidationEnabled) validationError() == null else true
        return !actionInProgress && selectedActionType != null && isValid
    }

    fun confirmAction() {
        if (actionInProgress) return

        val actionType = selectedActionType ?: return
        if (clientValidationEnabled && validationError() != null) return

        actionResultError = null
        actionInProgress = true

        viewModelScope.launch {
            val result = gameFacade.action(
                actionType = actionType,
                playerContacts = actionSelectionState.playerContacts,
                otherContacts = actionSelectionState.otherContacts,
            )

            actionInProgress = false
            result.onFailure { error ->
                actionResultError = error.message ?: "Action failed"
            }
        }
    }
}
