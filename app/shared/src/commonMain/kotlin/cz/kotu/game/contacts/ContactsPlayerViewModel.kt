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
import cz.kotu.game.contacts.model.isSolved
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactsPlayerViewModel(
    val gameFacade: ContactsPlayerFacade,
) : ViewModel() {

    var actionSelectionState by mutableStateOf<ActionSelectionState>(ActionSelectionState.None)
        private set

    var isLogsExpanded by mutableStateOf(false)

    var selectedActionType by mutableStateOf<ContactsBoardState.ActionType?>(null)

    var actionInProgress by mutableStateOf(false)

    var actionResultError by mutableStateOf<String?>(null)

    // keep validation on backend only for now
    var clientValidationEnabled by mutableStateOf(false)

    val gameState: StateFlow<PlayerViewState?> = gameFacade.gameState
        .onEach { newState ->
            onGameStateChanged(newState)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    val player: ContactsBoardState.Player?
        get() = gameState.value?.you

    private fun onGameStateChanged(newState: PlayerViewState) {

        // unselect solved contacts
        actionSelectionState = actionSelectionState.copy(
            playerContacts = actionSelectionState.playerContacts.filterNot(newState::isSolved).toSet(),
            otherContacts = actionSelectionState.otherContacts.filterNot(newState::isSolved).toSet(),
        )

        // update if action is no more available
        val availableActionTypes = newState.actions.allowedActionTypes
        if (selectedActionType !in availableActionTypes) {
            selectedActionType = availableActionTypes.firstOrNull()
        }

        // clear selection when player resolves multi-connect
        if (newState.actions.resolveMultiConnectContacts != null) {
            actionSelectionState = ActionSelectionState.None
        }
    }

    fun actionError(): String? = actionResultError ?: validationError()

    private fun dismissActionResultError() {
        actionResultError = null
    }

    fun onPlayerContactClick(contact: ContactsBoardState.ContactId) {
        dismissActionResultError()
        actionSelectionState = actionSelectionState.copy(
            playerContacts = actionSelectionState.playerContacts.toggled(contact),
        )
    }

    fun onOtherContactClick(contact: ContactsBoardState.ContactId) {
        dismissActionResultError()
        actionSelectionState = actionSelectionState.copy(
            otherContacts = actionSelectionState.otherContacts.toggled(contact),
        )
    }

    private fun Set<ContactsBoardState.ContactId>.toggled(
        contact: ContactsBoardState.ContactId,
    ): Set<ContactsBoardState.ContactId> {
        return if (contact in this) this - contact else this + contact
    }

    fun selectActionType(actionType: ContactsBoardState.ActionType) {
        dismissActionResultError()
        selectedActionType = actionType
    }

    fun validationError(): String? {
        val actionType = selectedActionType ?: return null
        val state = gameState.value ?: return null
        val currentPlayer = state.you
        return (state.board.applyActionIds(
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
