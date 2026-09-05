package cz.kotu.game.contacts.model

import cz.kotu.game.contacts.model.ContactsBoardState.ActionType
import cz.kotu.game.contacts.model.ContactsBoardState.ResolveMultiConnect
import kotlinx.serialization.Serializable

@Serializable
data class ContactsGameState(
    val board: ContactsBoardState,

    val players: List<ContactsBoardState.Player>,
    val activePlayer: ContactsBoardState.Player,

    ) {

    @Serializable
    data class PlayerActions(
        val actionStatusText: String? = null,
        val allowedActionTypes: Set<ActionType> = setOf(),
        val resolveMultiConnect: ResolveMultiConnect? = null,
    )

    fun getAvailablePlayerActions(player: ContactsBoardState.Player): PlayerActions {
        val resolveMultiConnect = board.resolveMultiConnect
        return if (resolveMultiConnect != null) {
            if (player == resolveMultiConnect.targetPlayer) {
                PlayerActions(
                    actionStatusText = "Original contact: ${board.requireContact(resolveMultiConnect.originalContact).matchKey}",
                    allowedActionTypes = setOf(ActionType.ResolveMultiConnect),
                    resolveMultiConnect = resolveMultiConnect,
                )
            } else {
                PlayerActions(
                    actionStatusText = "Waiting for ${resolveMultiConnect.targetPlayer.username} to resolve the multi-connect",
                    resolveMultiConnect = resolveMultiConnect,
                )
            }
        } else {
            if (player == activePlayer) {
                PlayerActions(
                    allowedActionTypes = setOf(
                        ActionType.StandardConnect,
                        ActionType.DoubleConnect,
                        ActionType.TripleConnect,
                        ActionType.MyDoubleConnect,
                        ActionType.SoloConnectRest,
                        ActionType.FinishReds,
                        ActionType.AddHint,
                    ),
                )
            } else {
                PlayerActions(
                    actionStatusText = "It is $activePlayer's turn",
                )
            }
        }
    }

    companion object {
        fun empty(): ContactsGameState = ContactsGameState(
            board = ContactsBoardState.empty(),
            players = listOf(),
            activePlayer = ContactsBoardState.Player(""),
        )
    }
}
