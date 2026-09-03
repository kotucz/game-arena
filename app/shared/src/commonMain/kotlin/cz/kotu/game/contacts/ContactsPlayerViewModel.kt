package cz.kotu.game.contacts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.kotu.game.contacts.model.ActionExecutionResult
import cz.kotu.game.contacts.model.ActionSelectionState
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsPlayerFacade
import cz.kotu.game.contacts.model.PlayerViewState
import cz.kotu.game.contacts.model.applyActionIds
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactsPlayerViewModel(
    val gameFacade: ContactsPlayerFacade,
    val username: String,
) : ViewModel() {

    val gameState: StateFlow<PlayerViewState> = gameFacade.gameState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlayerViewState.empty(),
    )

    val player: ContactsBoardState.Player
        get() = gameState.value.you

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

    fun onPlayerContactClick(contact: ContactsBoardState.ContactId) {
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

    fun onOtherContactClick(contact: ContactsBoardState.ContactId) {
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
        val currentGameState = gameState.value.board
        val state = actionSelectionState
        val newPlayerContacts = state.playerContacts.filter { !currentGameState.isSolved(it) }.toSet()
        val newOtherContacts = state.otherContacts.filter { !currentGameState.isSolved(it) }.toSet()
        if (newPlayerContacts != state.playerContacts || newOtherContacts != state.otherContacts) {
            actionSelectionState = if (newPlayerContacts.isEmpty() && newOtherContacts.isEmpty()) {
                ActionSelectionState.None
            } else {
                ActionSelectionState.MultiConnect(
                    playerContacts = newPlayerContacts,
                    otherContacts = newOtherContacts,
                )
            }
        }
    }

    fun availableActionTypes(): Set<ContactsBoardState.ActionType> {
        val resolution = gameState.value.board.resolveMultiConnect
        return when {
            resolution == null -> gameState.value.board.allowedActionTypes
            resolution.targetPlayer == player -> setOf(ContactsBoardState.ActionType.ResolveMultiConnect)
            else -> emptySet()
        }
    }

    fun resolutionTargetContacts(): Set<ContactsBoardState.ContactId>? {
        val resolution = gameState.value.board.resolveMultiConnect ?: return null
        if (resolution.targetPlayer != player) return null
        return resolution.targetContacts.toSet()
    }

    fun resolutionClickableContacts(): Set<ContactsBoardState.ContactId>? {
        if (gameState.value.board.resolveMultiConnect == null) return null
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
        return (gameState.value.board.applyActionIds(
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
