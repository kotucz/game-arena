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

    private var previousResolution: ContactsBoardState.ResolveMultiConnect? = null

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

    var actionSelectionState by mutableStateOf<ActionSelectionState>(ActionSelectionState.None)
        private set

    var isLogsExpanded by mutableStateOf(false)

    var selectedActionType by mutableStateOf<ContactsBoardState.ActionType?>(null)

    var actionInProgress by mutableStateOf(false)

    var actionResultError by mutableStateOf<String?>(null)

    // keep validation on backend only for now
    var clientValidationEnabled by mutableStateOf(false)

    private fun onGameStateChanged(newState: PlayerViewState) {
        if (newState.resolveMultiConnect != previousResolution) {
            previousResolution = newState.resolveMultiConnect
            resetActionSelection()
        }

        actionSelectionState = actionSelectionState.copy(
            playerContacts = actionSelectionState.playerContacts.filterNot(newState::isSolved).toSet(),
            otherContacts = actionSelectionState.otherContacts.filterNot(newState::isSolved).toSet(),
        )

        updateSelectedActionTypeIfNeeded(availableActionTypes(newState))
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

    fun resetActionSelection() {
        dismissActionResultError()
        actionSelectionState = ActionSelectionState.None
    }

    fun updateActionSelectionForSolved() {
        dismissActionResultError()
        val playerViewState = gameState.value ?: return
        actionSelectionState = actionSelectionState.copy(
            playerContacts = actionSelectionState.playerContacts.filterNot(playerViewState::isSolved).toSet(),
            otherContacts = actionSelectionState.otherContacts.filterNot(playerViewState::isSolved).toSet(),
        )
    }

    fun availableActionTypes(state: PlayerViewState? = gameState.value): Set<ContactsBoardState.ActionType> {
        if (state == null) return emptySet()
        val player = state.you
        val resolution = state.resolveMultiConnect
        return when {
            resolution == null -> state.allowedActionTypes
            resolution.targetPlayer == player -> setOf(ContactsBoardState.ActionType.ResolveMultiConnect)
            else -> emptySet()
        }
    }

    fun resolutionTargetContacts(): Set<ContactsBoardState.ContactId>? {
        val state = gameState.value ?: return null
        val resolution = state.resolveMultiConnect ?: return null
        if (resolution.targetPlayer != state.you) return null
        return resolution.targetContacts.toSet()
    }

    fun resolutionClickableContacts(): Set<ContactsBoardState.ContactId>? {
        val state = gameState.value ?: return null
        if (state.resolveMultiConnect == null) return null
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
